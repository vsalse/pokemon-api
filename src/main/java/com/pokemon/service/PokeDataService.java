package com.pokemon.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pokemon.model.PokeBasicModel;
import com.pokemon.model.PokeCacheModel;
import com.pokemon.model.PokeSpecieModel;
import com.pokemon.util.PokeUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PokeDataService {
    private final WebClient webClient;

    @Value("${pokeapi.url}")
    private String pokeApiUrl;

    public PokeDataService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Cacheable(value = "pokemon")
    public Mono<PokeCacheModel> parseDataPoke(Integer id, String language) {
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
            Integer weight = (Integer) response.get("weight");
            Integer height = (Integer) response.get("height");

            String imageList = (String) ((Map<String, Object>) response.get("sprites")).get("front_default");
            String imageDetail = PokeUtils.getStringFromNestedMap(response, "sprites.other.dream_world.front_default");

            List<Map<String, Object>> types = (List<Map<String, Object>>) response.get("types");
            List<Mono<String>> typeMonos = types.stream()
                    .map(type -> traduceItemAsync((String) ((Map<String, Object>) type.get("type")).get("url"),
                            language, "names", "name"))
                    .collect(Collectors.toList());

            List<Map<String, Object>> abilities = (List<Map<String, Object>>) response.get("abilities");
            List<Mono<String>> abilityMonos = abilities.stream()
                    .map(ability -> traduceItemAsync(
                            (String) ((Map<String, Object>) ability.get("ability")).get("url"),
                            language,
                            "flavor_text_entries",
                            "flavor_text"))
                    .collect(Collectors.toList());

            Mono<PokeSpecieModel> speciesMono = getSpecie(PokeUtils.getStringFromNestedMap(response, "species.url"),
                    language);

            return Mono.zip(
                    Mono.zip(typeMonos, arr -> Arrays.stream(arr).map(String.class::cast).collect(Collectors.toList())),
                    Mono.zip(abilityMonos,
                            arr -> Arrays.stream(arr).map(String.class::cast).collect(Collectors.toList())),
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
                        return null;
                    List<Map<String, Object>> collectionList = (List<Map<String, Object>>) response.get(collectionName);
                    if (collectionList != null) {
                        return collectionList.stream()
                                .filter(entry -> {
                                    Map<String, Object> languageMap = (Map<String, Object>) entry.get("language");
                                    return languageMap != null && language.equals(languageMap.get("name"));
                                })
                                .map(entry -> (String) entry.get(propertyName))
                                .findFirst()
                                .orElse(null);
                    }
                    return "";
                });
    }

    public Mono<PokeSpecieModel> getSpecie(String url, String language) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String evolutionChainUrl = PokeUtils.getStringFromNestedMap(response, "evolution_chain.url");
                    List<Map<String, Object>> collectionList = (List<Map<String, Object>>) response.get("flavor_text_entries");
                    String flavorText = collectionList.stream()
                            .filter(entry -> {
                                Map<String, Object> languageMap = (Map<String, Object>) entry.get("language");
                                return languageMap != null && language.equals(languageMap.get("name"));
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

    public Mono<List<List<PokeBasicModel>>> getEvolutionChain(String url, String language) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Map<String, Object> chainMap = (Map<String, Object>) response.get("chain");
                    String speciesUrl = PokeUtils.getStringFromNestedMap(chainMap, "species.url");
                    Mono<List<PokeBasicModel>> first = parseDataPoke(PokeUtils.getIdFromUrl(speciesUrl), language)
                        .map(poke -> List.of((PokeBasicModel) poke));

                    List<Map<String, Object>> evolvesToList = (List<Map<String, Object>>) chainMap.get("evolves_to");

                    // Recursivo para toda la cadena de evolución
                    return getEvolutionChainRecursive(evolvesToList, language)
                        .flatMap(rest -> first.map(f -> {
                            rest.add(0, f);
                            return rest;
                        }));
                });
    }

    private Mono<List<List<PokeBasicModel>>> getEvolutionChainRecursive(List<Map<String, Object>> evolvesToList, String language) {
        if (evolvesToList == null || evolvesToList.isEmpty()) {
            return Mono.just(new ArrayList<>());
        }
        List<Mono<List<PokeBasicModel>>> monos = new ArrayList<>();
        for (Map<String, Object> evolvesToMap : evolvesToList) {
            String speciesUrl = PokeUtils.getStringFromNestedMap(evolvesToMap, "species.url");
            monos.add(parseDataPoke(PokeUtils.getIdFromUrl(speciesUrl), language)
                .map(poke -> List.of((PokeBasicModel) poke)));
        }
        return Flux.concat(monos)
            .collectList()
            .flatMap(list -> {
                List<Map<String, Object>> nextEvolves = (List<Map<String, Object>>) evolvesToList.get(0).get("evolves_to");
                if (nextEvolves != null && !nextEvolves.isEmpty()) {
                    return getEvolutionChainRecursive(nextEvolves, language)
                        .map(rest -> {
                            list.addAll(rest);
                            return list;
                        });
                } else {
                    return Mono.just(list);
                }
            });
    }
}