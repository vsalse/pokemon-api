package com.pokemon.controller;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/test")
public class RedisTestController {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTestController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/redis")
    @Operation(summary = "Testeo de Redis", description = "Realiza un testeo de la conexión a Redis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "conexión exitosa")
    })
    public ResponseEntity<String> testRedis() {
        try {
            redisTemplate.opsForValue().set("testKey", "ok", Duration.ofSeconds(10));
            String value = redisTemplate.opsForValue().get("testKey");
            return ResponseEntity.ok("Conexión a Redis exitosa. Valor leído: " + value);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fallo al conectar con Redis: " + e.getMessage());
        }
    }
}
