package com.pokemon.service;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.pokemon.model.PokeCacheModel;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PokeCacheServiceTest {

    private MockWebServer mockWebServer;
    private PokeCacheService pokeCacheService;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // inyectamos WebClient con baseUrl a MockWebServer
        pokeCacheService = new PokeCacheService(WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build());

        // respuesta para GET /pokemon/10271 (tu primer JSON)
        String pokemonJson = "{\n" +
                "  \"abilities\": [\n" +
                "    {\n" +
                "      \"ability\": {\n" +
                "        \"name\": \"hadron-engine\",\n" +
                "        \"url\": \"" + mockWebServer.url("/ability/289/") + "\"\n" +
                "      },\n" +
                "      \"is_hidden\": false,\n" +
                "      \"slot\": 1\n" +
                "    }\n" +
                "  ],\n" +
                "  \"height\": 28,\n" +
                "  \"id\": 10271,\n" +
                "  \"name\": \"miraidon-glide-mode\",\n" +
                "  \"species\": {\n" +
                "    \"name\": \"miraidon\",\n" +
                "    \"url\": \"" + mockWebServer.url("/pokemon-species/1008/") + "\"\n" +
                "  },\n" +
                "  \"sprites\": {\n" +
                "    \"front_default\": \"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png\",\n"
                +
                "    \"other\": {\n" +
                "      \"dream_world\": {\n" +
                "        \"front_default\": \"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/dream-world/1.svg\"\n"
                +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"types\": [\n" +
                "    {\n" +
                "      \"slot\": 1,\n" +
                "      \"type\": {\n" +
                "        \"name\": \"electric\",\n" +
                "        \"url\": \"" + mockWebServer.url("/type/13/") + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"weight\": 2400\n" +
                "}";

        // respuesta para GET /type/13/
        String typeJson = "{\n" +
                "  \"generation\": {\n" +
                "    \"name\": \"generation-i\",\n" +
                "    \"url\": \"https://pokeapi.co/api/v2/generation/1/\"\n" +
                "  },\n" +
                "  \"id\": 10,\n" +
                "  \"move_damage_class\": {\n" +
                "    \"name\": \"special\",\n" +
                "    \"url\": \"https://pokeapi.co/api/v2/move-damage-class/3/\"\n" +
                "  },\n" +
                "  \"name\": \"fire\",\n" +
                "  \"names\": [\n" +
                "    {\n" +
                "      \"language\": {\n" +
                "        \"name\": \"es\",\n" +
                "        \"url\": \"https://pokeapi.co/api/v2/language/7/\"\n" +
                "      },\n" +
                "      \"name\": \"Fuego\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // respuesta para GET /ability/289/
        String abilityJson = "{\n" +
                "    \"flavor_text_entries\": [\n" +
                "        {\n" +
                "            \"flavor_text\": \"Potencia los ataques de tipo Planta en un apuro\",\n" +
                "            \"language\": {\n" +
                "                \"name\": \"es\",\n" +
                "                \"url\": \"https://pokeapi.co/api/v2/language/5/\"\n" +
                "            },\n" +
                "            \"version_group\": {\n" +
                "                \"name\": \"scarlet-violet\",\n" +
                "                \"url\": \"https://pokeapi.co/api/v2/version-group/25/\"\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        // respuesta para GET /pokemon-species/1008/
        String speciesJson = "{\n" +
                "  \"evolution_chain\": {\n" +
                "    \"url\": \"https://pokeapi.co/api/v2/evolution-chain/2/\"\n" +
                "  },\n" +
                "  \"flavor_text_entries\": [\n" +
                "    {\n" +
                "      \"flavor_text\": \"Con las alas que tiene puede alcanzar una altura de\\ncasi 1400 m. Suele escupir fuego por la boca.\",\n"
                +
                "      \"language\": {\n" +
                "        \"name\": \"es\",\n" +
                "        \"url\": \"https://pokeapi.co/api/v2/language/7/\"\n" +
                "      },\n" +
                "      \"version\": {\n" +
                "        \"name\": \"y\",\n" +
                "        \"url\": \"https://pokeapi.co/api/v2/version/24/\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // encolamos las respuestas en el orden en que el servicio las consume
        mockWebServer.enqueue(new MockResponse()
                .setBody(pokemonJson)
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setBody(typeJson)
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setBody(abilityJson)
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setBody(speciesJson)
                .addHeader("Content-Type", "application/json"));
    }

    @AfterEach
    void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetDataPoke() {
        Mono<PokeCacheModel> result = pokeCacheService.getDataPoke(10271, "es");

        StepVerifier.create(result)
                .assertNext(p -> {
                    Assertions.assertEquals(10271, p.getId());
                    Assertions.assertEquals("miraidon-glide-mode", p.getName());
                    Assertions.assertEquals(240.0, p.getWeight());
                    Assertions.assertEquals(2.8, p.getHeight());
                    Assertions.assertEquals(
                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                            p.getImageList());
                    Assertions.assertEquals(
                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/dream-world/1.svg",
                            p.getImageDetail());
                    Assertions.assertEquals(List.of("Fuego"), p.getTypeList());
                    Assertions.assertEquals(List.of("Potencia los ataques de tipo Planta en un apuro"),
                            p.getAbilitiesList());

                    Assertions.assertEquals("https://pokeapi.co/api/v2/evolution-chain/2/",
                            p.getSpecies().getEvolutionChainUrl());
                    Assertions.assertEquals(
                            "Con las alas que tiene puede alcanzar una altura de\ncasi 1400 m. Suele escupir fuego por la boca.",
                            p.getSpecies().getFlavorText());

                })
                .verifyComplete();
    }
}
