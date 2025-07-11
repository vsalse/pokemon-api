package com.pokemon.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Modelo de especie de Pokemon, con información adicional de la especie")
public class PokeSpecieModel {

    @Schema(description = "URL de la cadena evolutiva, usada para obtener la evoluvión del Pokemon", example = "https://pokeapi.co/api/v2/evolution-chain/1/")
    private String evolutionChainUrl;
    @Schema(description = "Texto descriptivo de la especie", example = "Una rara semilla fue plantada en su espalda al nacer.")
    private String flavorText;

}