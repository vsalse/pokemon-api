package com.pokemon.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pokemon.model.PokeBasicModel;
import com.pokemon.model.PokeDetailModel;
import com.pokemon.model.PokeMapper;
import com.pokemon.util.PokeUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PokeService {
    private final WebClient webClient;
    private final PokeDataService pokeDataService;

    @Value("${pokeapi.url}")
    private String pokeApiUrl;

    public PokeService(WebClient webClient, PokeDataService pokeDataService) {
        this.webClient = webClient;
        this.pokeDataService = pokeDataService;
    }

    public Mono<List<PokeBasicModel>> getPokemonList(Integer page, Integer pageSize, String language) {
        log.info("📄 Obteniendo lista de Pokémon - Página: {}, Tamaño: {}", page, pageSize);

        int limit = pageSize;
        int offset = (page != null ? page : 0) * limit;
        String url = pokeApiUrl + "?offset=" + offset + "&limit=" + limit;

        Mono<Map> responseMono = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> response = responseMono.block();

        List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");

        log.info("🔗 URLs de Pokemon encontradas: {}",
                results.stream().map(p -> p.get("url")).collect(Collectors.toList()));

        return Flux.fromIterable(results)
                .flatMap(pokemon -> pokeDataService.parseDataPoke(PokeUtils.getIdFromUrl(pokemon.get("url")), language))
                .map(PokeMapper.INSTANCE::toBasic)
                .collectList();
    }

    public Mono<PokeDetailModel> getPokemonDetail(Integer id, String language) {
        log.info("📄 Obteniendo detalle del Pokemon - id: {}", id);
        return pokeDataService.parseDataPoke(id, language)
            .flatMap(pokeCacheModel ->
                pokeDataService.getEvolutionChain(pokeCacheModel.getSpecies().getEvolutionChainUrl(), language)
                    .map(evolutionChain -> PokeDetailModel.builder()
                        .data(pokeCacheModel)
                        .evolutionList(evolutionChain)
                        .build()
                    )
            );
    }
}