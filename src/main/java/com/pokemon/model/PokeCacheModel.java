package com.pokemon.model;

import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class PokeCacheModel extends PokeBasicModel {
    
    private Double height;
    private PokeSpecieModel species;
    private String imageDetail;
    
} 