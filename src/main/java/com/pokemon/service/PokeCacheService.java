package com.pokemon.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pokemon.model.PokeCacheModel;
import com.pokemon.model.PokeSpecieModel;
import com.pokemon.util.PokeUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PokeCacheService {
        private final WebClient webClient;

        @Value("${pokeapi.url}")
        private String pokeApiUrl;
        @Value("${pokeapi.image-not-available}")
        private String imageNotAvailableUrl;

        public PokeCacheService(WebClient webClient) {
                this.webClient = webClient;
        }

        @Cacheable(value = "pokemon")
        public Mono<PokeCacheModel> getDataPoke(Integer id, String language) {
                log.info("🔍 Buscando Pokemon con ID: {} - Llamada REAL a la API", id);

                String urlDataPoke = pokeApiUrl + "/" + id;

                Mono<Map> responseMono = webClient.get()
                                .uri(urlDataPoke)
                                .retrieve()
                                .bodyToMono(Map.class);

                return responseMono.flatMap(response -> {
                        if (response == null)
                                return Mono.empty();

                        String name = (String) response.get("name");
                        Integer weightRaw = (Integer) response.get("weight");
                        Double weight = weightRaw != null ? weightRaw / 10.0 : null;
                        Integer heightRaw = (Integer) response.get("height");
                        Double height = heightRaw != null ? heightRaw / 10.0 : null;

                        // Imagen principal
                        String tmpImageList = null;
                        String tmpImageDetail = null;
                        Map<String, Object> sprites = (Map<String, Object>) response.get("sprites");
                        if (sprites != null) {
                            tmpImageList = (String) sprites.get("front_default");
                            Map<String, Object> other = (Map<String, Object>) sprites.get("other");
                            if (other != null) {
                                Map<String, Object> dreamWorld = (Map<String, Object>) other.get("dream_world");
                                if (dreamWorld != null) {
                                    tmpImageDetail = (String) dreamWorld.get("front_default");
                                }
                            }
                        }
                        final String imageList = (tmpImageList == null || tmpImageList.isEmpty()) ? imageNotAvailableUrl : tmpImageList;
                        final String imageDetail = (tmpImageDetail == null || tmpImageDetail.isEmpty()) ? imageList : tmpImageDetail;

                // Tipos
                List<Map<String, Object>> types = (List<Map<String, Object>>) response.get("types");
                if (types == null) types = List.of();
                List<Mono<String>> typeMonos = types.stream()
                    .map(type -> {
                        Map<String, Object> typeObj = (Map<String, Object>) type.get("type");
                        if (typeObj == null) return Mono.just("Desconocido");
                        String url = (String) typeObj.get("url");
                        return url != null ? traduceItemAsync(url, language, "names", "name").defaultIfEmpty("Desconocido") : Mono.just("Desconocido");
                    })
                    .collect(Collectors.toList());

                // Habilidades
                List<Map<String, Object>> abilities = (List<Map<String, Object>>) response.get("abilities");
                if (abilities == null) abilities = List.of();
                List<Mono<String>> abilityMonos = abilities.stream()
                    .map(ability -> {
                        Map<String, Object> abObj = (Map<String, Object>) ability.get("ability");
                        if (abObj == null) return Mono.just("Desconocido");
                        String url = (String) abObj.get("url");
                        return url != null ? traduceItemAsync(url, language, "flavor_text_entries", "flavor_text").defaultIfEmpty("Desconocido") : Mono.just("Desconocido");
                    })
                    .collect(Collectors.toList());

                // Especie
                Map<String, Object> speciesMap = (Map<String, Object>) response.get("species");
                String speciesUrl = null;
                if (speciesMap != null) {
                    speciesUrl = (String) speciesMap.get("url");
                }
                Mono<PokeSpecieModel> speciesMono = getSpecie(speciesUrl, language);

                        return Mono.zip(
                                        Mono.zip(typeMonos,
                                                        arr -> Arrays.stream(arr).map(String.class::cast)
                                                                        .collect(Collectors.toList())),
                                        Mono.zip(abilityMonos,
                                                        arr -> Arrays.stream(arr).map(String.class::cast)
                                                                        .collect(Collectors.toList())),
                                        speciesMono).map(tuple -> {
                                                List<String> typeList = (List<String>) tuple.getT1();
                                                List<String> abilitiesList = (List<String>) tuple.getT2();
                                                PokeSpecieModel species = tuple.getT3();
                                                return PokeCacheModel.builder()
                                                                .id(id)
                                                                .name(name)
                                                                .imageList(imageList)
                                                                .imageDetail(imageDetail)
                                                                .typeList(typeList)
                                                                .abilitiesList(abilitiesList)
                                                                .species(species)
                                                                .weight(weight)
                                                                .height(height)
                                                                .build();
                                        });
                });
        }

        private Mono<String> traduceItemAsync(String url, String language, String collectionName, String propertyName) {
                return webClient.get()
                                .uri(url)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(response -> {
                                        if (response == null)
                                                return "Desconocido";
                                        List<Map<String, Object>> collectionList = (List<Map<String, Object>>) response
                                                        .get(collectionName);
                                        if (collectionList != null) {
                                                return collectionList.stream()
                                                                .filter(entry -> {
                                                                        Map<String, Object> languageMap = (Map<String, Object>) entry
                                                                                        .get("language");
                                                                        return languageMap != null && language.equals(
                                                                                        languageMap.get("name"));
                                                                })
                                                                .map(entry -> (String) entry.get(propertyName))
                                                                .filter(val -> val != null && !val.isEmpty())
                                                                .findFirst()
                                                                .orElse("Desconocido");
                                        }
                                        return "Desconocido";
                                });
        }

        private Mono<PokeSpecieModel> getSpecie(String url, String language) {
                return webClient.get()
                                .uri(url)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(response -> {
                                        String evolutionChainUrl = PokeUtils.getStringFromNestedMap(response,
                                                        "evolution_chain.url");
                                        List<Map<String, Object>> collectionList = (List<Map<String, Object>>) response
                                                        .get("flavor_text_entries");
                                        String flavorText = collectionList.stream()
                                                        .filter(entry -> {
                                                                Map<String, Object> languageMap = (Map<String, Object>) entry
                                                                                .get("language");
                                                                return languageMap != null && language
                                                                                .equals(languageMap.get("name"));
                                                        })
                                                        .map(entry -> (String) entry.get("flavor_text"))
                                                        .findFirst()
                                                        .orElse(null);
                                        return PokeSpecieModel.builder()
                                                        .evolutionChainUrl(evolutionChainUrl)
                                                        .flavorText(flavorText)
                                                        .build();
                                });
        }

}