package com.pokemon.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.swagger.v3.oas.annotations.media.Schema;

@Mapper
@Schema(description = "Mapper con el cual se copia la informacion basica desde el cache del Pokemon")
public interface PokeMapper {
    PokeMapper INSTANCE = Mappers.getMapper(PokeMapper.class);
    PokeBasicModel toBasic(PokeCacheModel detail);
} 