package com.pokemon.controller;

import com.pokemon.model.PokeBasicModel;
import com.pokemon.model.PokeDetailModel;
import com.pokemon.service.PokeService;
import com.pokemon.service.PokeDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/pokemon")
public class PokeController {

    @Value("${pokeapi.page-size}")
    private int pageSize;

    private final PokeService pokeService;
    private final PokeDataService pokeDataService;

    public PokeController(PokeService pokeService, PokeDataService pokeDataService) {
        this.pokeService = pokeService;
        this.pokeDataService = pokeDataService;
    }

    @GetMapping("")
    public Mono<List<PokeBasicModel>> getPokemonList(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        int effectiveSize = (size == null) ? pageSize : size;
        return pokeService.getPokemonList(page, effectiveSize, "es");
    }

    @GetMapping("/{id}")
    public Mono<PokeDetailModel> getPokemonById(@PathVariable Integer id) {
        return pokeService.getPokemonDetail(id, "es");
    }

    @GetMapping("/clear-cache")
    @CacheEvict(value = "pokemon", allEntries = true)
    public String clearCache() {
        return "Cache limpiado exitosamente";
    }

}