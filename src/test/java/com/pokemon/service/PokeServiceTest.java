package com.pokemon.service;

import com.pokemon.model.PokeBasicModel;
import com.pokemon.model.PokeCacheModel;
import com.pokemon.model.PokeDetailModel;
import com.pokemon.model.PokeListModel;
import com.pokemon.model.PokeSpecieModel;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PokeServiceTest {
    private MockWebServer mockWebServer;
    private PokeService pokeService;
    private PokeCacheService pokeCacheService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        pokeCacheService = Mockito.mock(PokeCacheService.class);
        pokeService = new PokeService(webClient, pokeCacheService);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Debería obtener el detalle de un pokemon desde el caché y la cadena evolutiva")
    void deberiaObtenerDetalleDeUnPokemonDesdeCache() throws Exception {
        // mock modelo cacheado
        PokeSpecieModel specie = PokeSpecieModel.builder()
                .evolutionChainUrl(mockWebServer.url("/evolution-chain/1/").toString())
                .flavorText("Texto de especie")
                .build();
        PokeCacheModel cacheModel = PokeCacheModel.builder()
                .id(1)
                .name("bulbasaur")
                .imageList("img-list")
                .imageDetail("img-detail")
                .typeList(List.of("grass"))
                .abilitiesList(List.of("overgrow"))
                .species(specie)
                .weight(69.2)
                .height(0.7)
                .build();
        when(pokeCacheService.getDataPoke(anyInt(), anyString())).thenReturn(Mono.just(cacheModel));

        // mock respuesta de la cadena evolutiva
        String evolutionJson = "{" +
                "  \"chain\": {" +
                "    \"species\": {\"url\": \"" + mockWebServer.url("/pokemon/1/") + "\"}," +
                "    \"evolves_to\": []" +
                "  }" +
                "}";
        mockWebServer.enqueue(new MockResponse().setBody(evolutionJson).addHeader("Content-Type", "application/json"));

        Mono<PokeDetailModel> result = pokeService.getPokemonDetail(1, "es");
        StepVerifier.create(result)
                .assertNext(detail -> {
                    Assertions.assertEquals("bulbasaur", detail.getData().getName());
                    Assertions.assertEquals("Texto de especie", detail.getData().getSpecies().getFlavorText());
                    Assertions.assertNotNull(detail.getEvolutionList());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería obtener la lista de pokemones")
    void deberiaObtenerListaDePokemones() throws Exception {
        // mock respuesta de la pokeapi para la lista
        String listJson = "{" +
                "  \"count\": 1," +
                "  \"results\": [ { \"url\": \"" + mockWebServer.url("/pokemon/1/") + "\" } ]" +
                "}";
        mockWebServer.enqueue(new MockResponse().setBody(listJson).addHeader("Content-Type", "application/json"));

        // mock modelo cacheado para el pokemon de la lista
        PokeCacheModel cacheModel = PokeCacheModel.builder()
                .id(1)
                .name("bulbasaur")
                .imageList("img-list")
                .imageDetail("img-detail")
                .typeList(List.of("grass"))
                .abilitiesList(List.of("overgrow"))
                .species(PokeSpecieModel.builder().evolutionChainUrl("url").flavorText("texto").build())
                .weight(69.2)
                .height(0.7)
                .build();
        when(pokeCacheService.getDataPoke(anyInt(), anyString())).thenReturn(Mono.just(cacheModel));

        Mono<PokeListModel> result = pokeService.getPokemonList(0, 1, "es");
        StepVerifier.create(result)
                .assertNext(list -> {
                    Assertions.assertEquals(1, list.getRecordCount());
                    Assertions.assertEquals(1, list.getList().size());
                    Assertions.assertEquals("bulbasaur", list.getList().get(0).getName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería devolver una lista vacía si la pokeapi retorna results vacío")
    void deberiaDevolverListaVaciaSiNoHayResultados() throws Exception {
        // mock respuesta de la pokeapi con results vacío
        String listJson = "{" +
                "  \"count\": 1302," +
                "  \"next\": \"https://pokeapi.co/api/v2/pokemon?offset=2&limit=1\"," +
                "  \"previous\": \"https://pokeapi.co/api/v2/pokemon?offset=0&limit=1\"," +
                "  \"results\": []" +
                "}";
        mockWebServer.enqueue(new MockResponse().setBody(listJson).addHeader("Content-Type", "application/json"));

        // no se debe llamar a pokeCacheService.getDataPoke, pero si se llama, que devuelva vacío
        when(pokeCacheService.getDataPoke(anyInt(), anyString())).thenReturn(Mono.empty());

        Mono<PokeListModel> result = pokeService.getPokemonList(0, 1, "es");
        StepVerifier.create(result)
                .assertNext(list -> {
                    Assertions.assertEquals(1302, list.getRecordCount());
                    Assertions.assertNotNull(list.getList());
                    Assertions.assertTrue(list.getList().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería obtener el detalle de un pokemon con su cadena evolutiva completa")
    void deberiaObtenerDetalleConEvoluciones() throws Exception {
        // mock modelo cacheado para cada especie
        PokeSpecieModel specie = PokeSpecieModel.builder()
                .evolutionChainUrl(mockWebServer.url("/evolution-chain/2").toString())
                .flavorText("Texto de especie")
                .build();
        // Charmander (id 4)
        PokeCacheModel charmander = PokeCacheModel.builder()
                .id(4).name("charmander").species(specie).build();
        // Charmeleon (id 5)
        PokeCacheModel charmeleon = PokeCacheModel.builder()
                .id(5).name("charmeleon").species(specie).build();
        // Charizard (id 6)
        PokeCacheModel charizard = PokeCacheModel.builder()
                .id(6).name("charizard").species(specie).build();

        // mock getDataPoke según el id
        when(pokeCacheService.getDataPoke(Mockito.eq(4), anyString())).thenReturn(Mono.just(charmander));
        when(pokeCacheService.getDataPoke(Mockito.eq(5), anyString())).thenReturn(Mono.just(charmeleon));
        when(pokeCacheService.getDataPoke(Mockito.eq(6), anyString())).thenReturn(Mono.just(charizard));
        // para el primer llamado (el del detalle principal)
        when(pokeCacheService.getDataPoke(Mockito.eq(1), anyString())).thenReturn(Mono.just(
                PokeCacheModel.builder().id(1).name("bulbasaur").species(specie).build()
        ));

        // mock respuesta de la cadena evolutiva
        String evolutionJson = "{\n" +
                "    \"baby_trigger_item\": null,\n" +
                "    \"chain\": {\n" +
                "        \"evolution_details\": [],\n" +
                "        \"evolves_to\": [\n" +
                "            {\n" +
                "                \"evolution_details\": [\n" +
                "                    {\n" +
                "                        \"min_level\": 16,\n" +
                "                        \"trigger\": {\"name\": \"level-up\"}\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"evolves_to\": [\n" +
                "                    {\n" +
                "                        \"evolution_details\": [\n" +
                "                            {\n" +
                "                                \"min_level\": 36,\n" +
                "                                \"trigger\": {\"name\": \"level-up\"}\n" +
                "                            }\n" +
                "                        ],\n" +
                "                        \"evolves_to\": [],\n" +
                "                        \"is_baby\": false,\n" +
                "                        \"species\": {\n" +
                "                            \"name\": \"charizard\",\n" +
                "                            \"url\": \"" + mockWebServer.url("/pokemon-species/6/") + "\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"is_baby\": false,\n" +
                "                \"species\": {\n" +
                "                    \"name\": \"charmeleon\",\n" +
                "                    \"url\": \"" + mockWebServer.url("/pokemon-species/5/") + "\"\n" +
                "                }\n" +
                "            }\n" +
                "        ],\n" +
                "        \"is_baby\": false,\n" +
                "        \"species\": {\n" +
                "            \"name\": \"charmander\",\n" +
                "            \"url\": \"" + mockWebServer.url("/pokemon-species/4/") + "\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"id\": 2\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setBody(evolutionJson).addHeader("Content-Type", "application/json"));

        // llama al método (usamos id=1 para el detalle, pero la cadena evolutiva es la de charmander)
        Mono<PokeDetailModel> result = pokeService.getPokemonDetail(1, "es");
        StepVerifier.create(result)
                .assertNext(detail -> {
                    // La lista de evolución debe tener 3 etapas: charmander, charmeleon, charizard
                    List<List<PokeBasicModel>> evolution = detail.getEvolutionList();
                    Assertions.assertNotNull(evolution);
                    Assertions.assertEquals(3, evolution.size());
                    Assertions.assertEquals("charmander", evolution.get(0).get(0).getName());
                    Assertions.assertEquals("charmeleon", evolution.get(1).get(0).getName());
                    Assertions.assertEquals("charizard", evolution.get(2).get(0).getName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería obtener la cadena evolutiva múltiple como Eevee por ej")
    void deberiaObtenerEvolucionesMultiplesParaEevee() throws Exception {
        // mock de Eevee y sus evoluciones
        int eeveeId = 133;
        int vaporeonId = 134, jolteonId = 135, flareonId = 136, espeonId = 196, umbreonId = 197, leafeonId = 470, glaceonId = 471, sylveonId = 700;
        String eeveeName = "eevee";
        String[] evolNames = {"vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon"};
        int[] evolIds = {vaporeonId, jolteonId, flareonId, espeonId, umbreonId, leafeonId, glaceonId, sylveonId};

        PokeSpecieModel specie = PokeSpecieModel.builder()
                .evolutionChainUrl(mockWebServer.url("/evolution-chain/67").toString())
                .flavorText("Un extraño Pokémon que se adapta a los entornos más hostiles gracias a sus diferentes evoluciones.")
                .build();
        // Eevee
        PokeCacheModel eevee = PokeCacheModel.builder().id(eeveeId).name(eeveeName).species(specie).build();
        when(pokeCacheService.getDataPoke(Mockito.eq(eeveeId), anyString())).thenReturn(Mono.just(eevee));
        // evoluciones
        for (int i = 0; i < evolNames.length; i++) {
            PokeCacheModel evo = PokeCacheModel.builder().id(evolIds[i]).name(evolNames[i]).species(specie).build();
            when(pokeCacheService.getDataPoke(Mockito.eq(evolIds[i]), anyString())).thenReturn(Mono.just(evo));
        }
        // para el primer llamado (el del detalle principal)
        when(pokeCacheService.getDataPoke(Mockito.eq(1), anyString())).thenReturn(Mono.just(
                PokeCacheModel.builder().id(1).name("bulbasaur").species(specie).build()
        ));

        // mock respuesta de la cadena evolutiva de Eevee
        StringBuilder evolvesToJson = new StringBuilder();
        for (int i = 0; i < evolNames.length; i++) {
            evolvesToJson.append("{\n")
                    .append("  \"evolution_details\": [],\n")
                    .append("  \"evolves_to\": [],\n")
                    .append("  \"is_baby\": false,\n")
                    .append("  \"species\": {\n")
                    .append("    \"name\": \"").append(evolNames[i]).append("\",\n")
                    .append("    \"url\": \"").append(mockWebServer.url("/pokemon-species/" + evolIds[i] + "/")).append("\"\n")
                    .append("  }\n")
                    .append("}");
            if (i < evolNames.length - 1) evolvesToJson.append(",");
        }
        String evolutionJson = "{\n" +
                "  \"baby_trigger_item\": null,\n" +
                "  \"chain\": {\n" +
                "    \"evolution_details\": [],\n" +
                "    \"evolves_to\": [" + evolvesToJson + "],\n" +
                "    \"is_baby\": false,\n" +
                "    \"species\": {\n" +
                "      \"name\": \"eevee\",\n" +
                "      \"url\": \"" + mockWebServer.url("/pokemon-species/133/") + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"id\": 67\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setBody(evolutionJson).addHeader("Content-Type", "application/json"));

        // llama al método (usamos id=1 para el detalle, pero la cadena evolutiva es la de eevee)
        Mono<PokeDetailModel> result = pokeService.getPokemonDetail(1, "es");
        StepVerifier.create(result)
                .assertNext(detail -> {
                    List<List<PokeBasicModel>> evolution = detail.getEvolutionList();
                    Assertions.assertNotNull(evolution);
                    Assertions.assertEquals(2, evolution.size());
                    // Primer sublista: eevee
                    Assertions.assertEquals(1, evolution.get(0).size());
                    Assertions.assertEquals("eevee", evolution.get(0).get(0).getName());
                    // Segunda sublista: todas las evoluciones
                    Assertions.assertEquals(evolNames.length, evolution.get(1).size());
                    for (int i = 0; i < evolNames.length; i++) {
                        Assertions.assertEquals(evolNames[i], evolution.get(1).get(i).getName());
                    }
                })
                .verifyComplete();
    }
} 