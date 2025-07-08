package com.example.pokemonapi;

import com.example.pokemonapi.model.PokeBasicModel;
import com.example.pokemonapi.service.PokeService;
import com.example.pokemonapi.service.PokeDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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
    public List<PokeBasicModel> getPokemonList(@RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {
        return pokeService.getPokemonList(page, pageSize);
    }

    @GetMapping("/{id}")
    public PokeBasicModel getPokemonById(@PathVariable Integer id) {
        return pokeDataService.parseDataPoke(id);
    }

    @GetMapping("/clear-cache")
    @CacheEvict(value = "pokemon", allEntries = true)
    public String clearCache() {
        return "Cache limpiado exitosamente";
    }
    
} 