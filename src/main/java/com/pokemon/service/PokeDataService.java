package com.pokemon.service;

import com.pokemon.model.PokeBasicModel;
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

    @Cacheable(value = "pokemon", key = "#id")
    public PokeBasicModel parseDataPoke(Integer id) {

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

        // foto del pokemon para lista
        String frontDefault = (String) ((Map<String, Object>) response.get("sprites")).get("front_default");
        

        // Obtener typeList
        List<Map<String, Object>> types = (List<Map<String, Object>>) response.get("types");
        List<String> typeList = types.stream()
                .map(type -> (String) ((Map<String, Object>) type.get("type")).get("name"))
                .collect(Collectors.toList());

        // Obtener abilitiesList
        List<Map<String, Object>> abilities = (List<Map<String, Object>>) response.get("abilities");
        List<String> abilitiesList = abilities.stream()
                .map(ability -> (String) ((Map<String, Object>) ability.get("ability")).get("name"))
                .collect(Collectors.toList());

        PokeBasicModel pokemon = PokeBasicModel.builder()
                .id(id)
                .name(name)
                .frontDefault(frontDefault)
                .typeList(typeList)
                .abilitiesList(abilitiesList)
                .weight(weight)
                .build();

        log.info("✅ Pokemon {} (ID: {}) obtenido de la API y cacheado", name, id);
        return pokemon;
    }
}