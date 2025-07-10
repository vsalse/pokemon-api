package com.pokemon.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pokemon.model.PokeDetailModel;
import com.pokemon.model.PokeListModel;
import com.pokemon.service.PokeService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/pokemon")
public class PokeController {

    @Value("${pokeapi.page-size}")
    private int pageSize;

    private final PokeService pokeService;

    public PokeController(PokeService pokeService) {
        this.pokeService = pokeService;
    }

    @GetMapping("")
    public Mono<PokeListModel> getPokemonList(
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