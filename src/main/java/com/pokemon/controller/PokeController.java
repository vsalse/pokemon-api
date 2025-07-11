package com.pokemon.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pokemon.model.PokeDetailModel;
import com.pokemon.model.PokeListModel;
import com.pokemon.service.PokeService;
import com.pokemon.util.CustomException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/pokemon")
@Tag(name = "Pokemon", description = "Endpoints para obtener información de los pokemones")
public class PokeController extends BaseExceptionHandler {

    @Value("${pokeapi.page-size}")
    private int pageSize;

    @Value("${pokeapi.max-page-size:20}")
    private int maxPageSize;

    @Value("${pokeapi.default-language:es}")
    private String defaultLanguage;

    // Lista de idiomas válidos según la pokeapi
    private static final java.util.Set<String> VALID_LANGUAGES = java.util.Set.of(
        "ja-Hrkt", "roomaji", "ko", "zh-Hant", "fr", "de", "es", "it", "en", "cs", "ja", "zh-Hans", "pt-BR"
    );

    private final PokeService pokeService;

    public PokeController(PokeService pokeService) {
        this.pokeService = pokeService;
    }

    @GetMapping("")
    @Operation(summary = "Obtener lista de Pokemon", description = "Retorna una lista paginada de pokemones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pokemones obtenida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<PokeListModel> getPokemonList(
            @Parameter(description = "Número de página (base 0)", example = "0") @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de la página", example = "10") @RequestParam(name = "size", required = false) Integer size,
            @Parameter(description = "Idioma de la respuesta. Valores posibles: ja-Hrkt, roomaji, ko, zh-Hant, fr, de, es, it, en, cs, ja, zh-Hans, pt-BR", example = "es") @RequestParam(name = "language", required = false) String language) {
        int effectiveSize = (size == null) ? pageSize : size;
        if (effectiveSize > maxPageSize) {
            throw new CustomException("El máximo valor del tamaño de la pagina es de " + maxPageSize, 400);
        }
        String lang = (language != null && !language.isBlank()) ? language : defaultLanguage;
        if (!VALID_LANGUAGES.contains(lang)) {
            throw new CustomException("Idioma no soportado. Valores permitidos: " + VALID_LANGUAGES, 400);
        }
        return pokeService.getPokemonList(page, effectiveSize, lang);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de Pokemon", description = "Retorna información detallada de un Pokemon específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle del Pokemon obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pokemon no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<PokeDetailModel> getPokemonById(
            @Parameter(description = "Número del Pokemon", example = "1") @PathVariable Integer id,
            @Parameter(description = "Idioma de la respuesta. Valores posibles: ja-Hrkt, roomaji, ko, zh-Hant, fr, de, es, it, en, cs, ja, zh-Hans, pt-BR", example = "es") @RequestParam(name = "language", required = false) String language) {
        String lang = (language != null && !language.isBlank()) ? language : defaultLanguage;
        if (!VALID_LANGUAGES.contains(lang)) {
            throw new CustomException("Idioma no soportado. Valores permitidos: " + VALID_LANGUAGES, 400);
        }
        return pokeService.getPokemonDetail(id, lang);
    }

    @GetMapping("/clear-cache")
    @Operation(summary = "Limpiar caché", description = "Limpia toda la caché de pokemones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caché limpiada exitosamente")
    })
    @CacheEvict(value = "pokemon", allEntries = true)
    public String clearCache() {
        return "Cache limpiado exitosamente";
    }
}