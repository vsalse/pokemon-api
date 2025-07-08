package com.pokemon.model;

import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PokeBasicModel {
    private Integer id;
    private String name;
    private String imageList;
    
    @Builder.Default
    private List<String> typeList = null;
    @Builder.Default
    private List<String> abilitiesList = null;
    @Builder.Default
    private Integer weight = null;
} 