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
@Schema(description = "Modelo con el detalle de un Pokemon, incluyendo su evolución, este modelo es usado para la pagina de detalle del Pokemon")
public class PokeDetailModel {

    @Schema(description = "Datos cacheados del Pokemon")
    private PokeCacheModel data;
    @Schema(description = "Lista de listas con la cadena evolutiva del Pokemon, los items de las listas usan el modelo basico para optimizar el renderizado")
    private List<List<PokeBasicModel>> evolutionList;

}