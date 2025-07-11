package com.pokemon.model;

import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@Schema(description = "Modelo con el cual se realiza el cacheo de la informacion de cada Pokemon, incluye los del basico mas detalles adicionales")
public class PokeCacheModel extends PokeBasicModel {
    
    @Schema(description = "Altura del Pokemon en metros", example = "7.5")
    private Double height;
    @Schema(description = "Información de especie del Pokemon")
    private PokeSpecieModel species;
    @Schema(description = "URL de la imagen de detalle del Pokemon (mas grande que la imagen basica)", example = "https://.../bulbasaur-detail.png")
    private String imageDetail;
    
} 