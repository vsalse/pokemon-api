package com.example.pokemonapi.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.pokemonapi.model.PokeBasicModel;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PokeService {
        private final WebClient webClient;
        private final PokeDataService pokeDataService;

        @Value("${pokeapi.url}")
        private String pokeApiUrl;

        @Autowired
        public PokeService(PokeDataService pokeDataService) {
                this.webClient = WebClient.builder()
                                .exchangeStrategies(ExchangeStrategies.builder()
                                                .codecs(configurer -> configurer.defaultCodecs()
                                                                .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                                                .build())
                                .build();
                this.pokeDataService = pokeDataService;
        }

        public List<PokeBasicModel> getPokemonList(Integer page, Integer pageSize) {
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

                log.info("🔗 URLs de Pokémon encontradas: {}",
                                results.stream().map(p -> p.get("url")).collect(Collectors.toList()));

                return results.stream()
                                .map(pokemon -> pokeDataService.parseDataPoke(getIdFromUrl(pokemon.get("url"))))
                                .collect(Collectors.toList());
        }

        private Integer getIdFromUrl(String urlDataPoke) {
                String[] parts = urlDataPoke.split("/");
                return Integer.parseInt(parts[parts.length - 1]);
        }

}