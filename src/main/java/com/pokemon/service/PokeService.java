package com.pokemon.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.pokemon.model.PokeBasicModel;
import com.pokemon.model.PokeDetailModel;
import com.pokemon.model.PokeListModel;
import com.pokemon.model.PokeMapper;
import com.pokemon.util.PokeUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PokeService {
    private final WebClient webClient;
    private final PokeCacheService pokeCacheService;

    @Value("${pokeapi.url}")
    private String pokeApiUrl;

    public PokeService(WebClient webClient, PokeCacheService pokeCacheService) {
        this.webClient = webClient;
        this.pokeCacheService = pokeCacheService;
    }

    public Mono<PokeListModel> getPokemonList(Integer page, Integer pageSize, String language) {
        log.info("📄 Obteniendo lista de Pokémon - Página: {}, Tamaño: {}", page, pageSize);

        int limit = pageSize;
        int offset = (page != null ? page : 0) * limit;
        String url = pokeApiUrl + "?offset=" + offset + "&limit=" + limit;

        System.out.println("@@@url: " + url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException ||
                                throwable instanceof java.net.SocketException))
                .doOnError(error -> log.error("❌ Error obteniendo lista de Pokémon: {}", error.getMessage()))
                .flatMap(response -> {
                    Integer count = (Integer) response.get("count");
                    List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");
                    System.out.println("@@@results: " + results);

                    // Filtrar resultados nulos o sin URL
                    List<Map<String, String>> filteredResults = results == null ? List.of() : results.stream()
                        .filter(p -> p != null && p.get("url") != null && !p.get("url").isEmpty())
                        .collect(Collectors.toList());

                    if (filteredResults.size() != (results == null ? 0 : results.size())) {
                        log.warn("⚠️ Se encontraron resultados nulos o sin URL en la respuesta de la API externa. Filtrados: {} de {}", (results == null ? 0 : results.size()) - filteredResults.size(), results == null ? 0 : results.size());
                    }

                    log.info("🔗 URLs de Pokemon válidas: {}",
                            filteredResults.stream().map(p -> p.get("url")).collect(Collectors.toList()));

                    if (filteredResults.isEmpty()) {
                        return Mono.just(PokeListModel.builder()
                                .recordCount(count)
                                .list(List.of())
                                .build());
                    }

                    return Flux.fromIterable(filteredResults)
                            .flatMap(pokemon -> pokeCacheService.getDataPoke(PokeUtils.getIdFromUrl(pokemon.get("url")),
                                    language))
                            .filter(poke -> poke != null)
                            .map(PokeMapper.INSTANCE::toBasic)
                            .collectList()
                            .map(list -> PokeListModel.builder()
                                    .recordCount(count)
                                    .list(list)
                                    .build()
                            );
                });
    }

    public Mono<PokeDetailModel> getPokemonDetail(Integer id, String language) {
        log.info("📄 Obteniendo detalle del Pokemon - id: {}", id);
        return pokeCacheService.getDataPoke(id, language)
                .flatMap(pokeCacheModel -> getEvolutionChain(pokeCacheModel.getSpecies().getEvolutionChainUrl(),
                        language)
                        .map(evolutionChain -> PokeDetailModel.builder()
                                .data(pokeCacheModel)
                                .evolutionList(evolutionChain)
                                .build()));
    }

    private Mono<List<List<PokeBasicModel>>> getEvolutionChain(String url, String language) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException ||
                                throwable instanceof java.net.SocketException))
                .doOnError(error -> log.error("❌ Error obteniendo cadena de evolución: {}", error.getMessage()))
                .flatMap(response -> {
                    Map<String, Object> chainMap = (Map<String, Object>) response.get("chain");
                    String speciesUrl = PokeUtils.getStringFromNestedMap(chainMap, "species.url");
                    Mono<List<PokeBasicModel>> first = pokeCacheService
                            .getDataPoke(PokeUtils.getIdFromUrl(speciesUrl), language)
                            .map(poke -> List.of(PokeMapper.INSTANCE.toBasic(poke)));

                    List<Map<String, Object>> evolvesToList = (List<Map<String, Object>>) chainMap.get("evolves_to");

                    // Recursivo para toda la cadena de evolución
                    return getEvolutionChainRecursive(evolvesToList, language)
                            .flatMap(rest -> first.map(f -> {
                                rest.add(0, f);
                                return rest;
                            }));
                });
    }

    private Mono<List<List<PokeBasicModel>>> getEvolutionChainRecursive(List<Map<String, Object>> evolvesToList,
            String language) {
        if (evolvesToList == null || evolvesToList.isEmpty()) {
            return Mono.just(new ArrayList<>());
        }

        // Si solo hay un elemento, aplicar la lógica actual
        if (evolvesToList.size() == 1) {
            List<Mono<List<PokeBasicModel>>> monos = new ArrayList<>();
            Map<String, Object> evolvesToMap = evolvesToList.get(0);
            String speciesUrl = PokeUtils.getStringFromNestedMap(evolvesToMap, "species.url");
            monos.add(pokeCacheService.getDataPoke(PokeUtils.getIdFromUrl(speciesUrl), language)
                    .map(poke -> List.of(PokeMapper.INSTANCE.toBasic(poke))));

            return Flux.concat(monos)
                    .collectList()
                    .flatMap(list -> {
                        List<Map<String, Object>> nextEvolves = (List<Map<String, Object>>) evolvesToMap
                                .get("evolves_to");
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
        } else {
            // Si hay más de un elemento, iterar todos los items
            List<Mono<List<PokeBasicModel>>> monos = new ArrayList<>();
            List<Mono<PokeBasicModel>> pokeMonos = new ArrayList<>();

            for (Map<String, Object> evolvesToMap : evolvesToList) {
                String speciesUrl = PokeUtils.getStringFromNestedMap(evolvesToMap, "species.url");
                pokeMonos.add(pokeCacheService.getDataPoke(PokeUtils.getIdFromUrl(speciesUrl), language)
                        .map(poke -> PokeMapper.INSTANCE.toBasic(poke)));
            }

            // Agrupar todos los PokeBasicModel en una sola lista
            return Flux.concat(pokeMonos)
                    .collectList()
                    .map(pokeList -> {
                        monos.add(Mono.just(pokeList));
                        return monos;
                    })
                    .flatMap(monosList -> Flux.concat(monosList).collectList());
        }
    }

}