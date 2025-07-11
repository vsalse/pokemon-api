package com.pokemon.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Modelo que representa un Pokemon con información basica, para armar los items de la lista, con el objetivo de minimizar el tamaño del response")
public class PokeBasicModel {
    @Schema(description = "Nro del Pokemon", example = "1")
    private Integer id;
    @Schema(description = "Nombre del Pokemon", example = "bulbasaur")
    private String name;
    @Schema(description = "URL de la imagen del Pokemon - imagen pegueña para optimizar el renderizado en la lista", example = "https://.../bulbasaur.png")
    private String imageList;
    @Schema(description = "Peso del Pokemon en kilogramos", example = "69.2")
    private Double weight;
    @Schema(description = "Lista de tipos del Pokemon", example = "[\"grass\", \"poison\"]")
    private List<String> typeList;
    @Schema(description = "Lista de habilidades del Pokemon", example = "[\"overgrow\", \"chlorophyll\"]")
    private List<String> abilitiesList;
}