package com.pokemon.controller;

import com.pokemon.model.PokeDetailModel;
import com.pokemon.model.PokeListModel;
import com.pokemon.service.PokeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest(PokeController.class)
class PokeControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PokeService pokeService;

    @Test
    @DisplayName("Debería retornar la lista de pokemones")
    void deberiaRetornarListaDePokemones() {
        PokeListModel lista = PokeListModel.builder().recordCount(1).list(java.util.List.of()).build();
        Mockito.when(pokeService.getPokemonList(any(), any(), eq("es"))).thenReturn(Mono.just(lista));

        webTestClient.get().uri("/pokemon?page=0&size=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PokeListModel.class)
                .isEqualTo(lista);
    }

    @Test
    @DisplayName("Debería retornar el detalle de un pokemon")
    void deberiaRetornarDetalleDeUnPokemon() {
        PokeDetailModel detalle = PokeDetailModel.builder().build();
        Mockito.when(pokeService.getPokemonDetail(eq(1), eq("es"))).thenReturn(Mono.just(detalle));

        webTestClient.get().uri("/pokemon/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PokeDetailModel.class)
                .isEqualTo(detalle);
    }

    @Test
    @DisplayName("Debería limpiar el caché de pokemones")
    void deberiaLimpiarCacheDePokemones() {
        webTestClient.get().uri("/pokemon/clear-cache")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Cache limpiado exitosamente");
    }

    @Test
    @DisplayName("Debería retornar error 400 si el tamaño de página supera el máximo permitido")
    void deberiaRetornarErrorSiSizeSuperaMaximo() {
        // el valor por defecto de maxPageSize es 20 (ver application.properties)
        int sizeMayor = 999;
        webTestClient.get().uri("/pokemon?page=0&size=" + sizeMayor)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .value(msg -> {
                    // el mensaje debe contener el texto del CustomException
                    org.assertj.core.api.Assertions.assertThat(msg)
                        .contains("El máximo valor del tamaño de la pagina es de 20");
                });
    }

    @Test
    @DisplayName("Debería retornar error 500 y modelo de error estándar ante excepción genérica")
    void deberiaRetornarError500AnteExcepcionGenerica() {
        // Forzar que el servicio lance una excepción genérica
        Mockito.when(pokeService.getPokemonList(Mockito.any(), Mockito.any(), Mockito.eq("es")))
                .thenThrow(new RuntimeException("Error inesperado"));

        webTestClient.get().uri("/pokemon?page=0&size=1")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.severity").isEqualTo("fatal")
                .jsonPath("$.message").isEqualTo("Error inesperado")
                .jsonPath("$.codigo").isEqualTo(500)
                .jsonPath("$.trace").isEqualTo("Error inesperado");
    }
} 