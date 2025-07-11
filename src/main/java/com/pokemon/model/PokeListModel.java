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
@Schema(description = "Modelo para la respuesta de la lista de Pokemones")
public class PokeListModel {
    @Schema(description = "Cantidad total de registros", example = "151")
    private Integer recordCount;
    @Schema(description = "Lista de Pokemones usa el modelo básico que lo copia de la info guardada en cache")
    private List<PokeBasicModel> list;

}