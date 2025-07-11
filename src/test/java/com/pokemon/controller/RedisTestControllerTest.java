package com.pokemon.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(RedisTestController.class)
class RedisTestControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Debería retornar éxito si Redis responde correctamente")
    void deberiaRetornarExitoSiRedisFunciona() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq("testKey"))).thenReturn("ok");
        // No es necesario mockear set, solo que no lance excepción

        webTestClient.get().uri("/test/redis")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(msg -> org.assertj.core.api.Assertions.assertThat(msg)
                        .contains("Conexión a Redis exitosa. Valor leído: ok"));
    }

    @Test
    @DisplayName("Debería retornar error si Redis lanza excepción")
    void deberiaRetornarErrorSiRedisFalla() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis no disponible"));

        webTestClient.get().uri("/test/redis")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class)
                .value(msg -> org.assertj.core.api.Assertions.assertThat(msg)
                        .contains("Fallo al conectar con Redis: Redis no disponible"));
    }
}