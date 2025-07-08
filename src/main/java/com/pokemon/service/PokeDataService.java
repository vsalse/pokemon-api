package com.pokemon.service;

import com.pokemon.model.PokeBasicModel;
import com.pokemon.model.PokeDetailModel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public PokeDetailModel parseDataPoke(Integer id, String language) {

        log.info("🔍 Buscando Pokemon con ID: {} - Llamada REAL a la API", id);

        String urlDataPoke = pokeApiUrl + "/" + id;

        Mono<Map> responseMono = webClient.get()
                .uri(urlDataPoke)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> response = responseMono.block();

        if (response == null)
            return null;

        String name = (String) response.get("name");
        Integer weight = (Integer) response.get("weight");
        Integer height = (Integer) response.get("height");

        // foto del pokemon para lista
        String imageList = (String) ((Map<String, Object>) response.get("sprites")).get("front_default");

        // foto del pokemon para el formulario de detalle
        String imageDetail = getValueFromNestedMap(response, "sprites.other.dream_world.front_default");

        // lista de tipos
        List<Map<String, Object>> types = (List<Map<String, Object>>) response.get("types");
        List<String> typeList = types.stream()
                .map(type -> traduceItem((String) ((Map<String, Object>) type.get("type")).get("url"), language,
                        "names", "name"))
                .collect(Collectors.toList());

        // lista de abilidades
        List<Map<String, Object>> abilities = (List<Map<String, Object>>) response.get("abilities");
        List<String> abilitiesList = abilities.stream()
                .map(ability -> traduceItem((String) ((Map<String, Object>) ability.get("ability")).get("url"),
                        language,
                        "flavor_text_entries", "flavor_text"))
                .collect(Collectors.toList());

        // especie
        Map<String, Object> speciesMap = (Map<String, Object>) response.get("species");
        String species = traduceItem((String) speciesMap.get("url"), language, "flavor_text_entries", "flavor_text");

        PokeDetailModel pokemon = PokeDetailModel.builder()
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

        log.info("✅ Pokemon {} (ID: {}) obtenido de la API y cacheado", name, id);
        return pokemon;
    }

    private String traduceItem(String url, String idioma, String collectionName, String propertyName) {

        Mono<Map> responseMono = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> response = responseMono.block();

        if (response == null)
            return null;

        List<Map<String, Object>> collectionList = (List<Map<String, Object>>) response.get(collectionName);
        if (collectionList != null) {
            return collectionList.stream()
                    .filter(entry -> {
                        Map<String, Object> language = (Map<String, Object>) entry.get("language");
                        return language != null && idioma.equals(language.get("name"));
                    })
                    .map(entry -> (String) entry.get(propertyName))
                    .findFirst()
                    .orElse(null);
        }
        return "";
    }

    private String getValueFromNestedMap(Map<String, Object> map, String key) {

        Map<String, Object> mapValue = map;
        String[] parts = key.split("\\.");

        for (int i = 0; i < parts.length - 1; i++) {
            mapValue = (Map<String, Object>) mapValue.get(parts[i]);
        }
        return (String) mapValue.get(parts[parts.length - 1]);
    }

}