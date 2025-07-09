package com.pokemon.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PokeMapper {
    PokeMapper INSTANCE = Mappers.getMapper(PokeMapper.class);
    PokeBasicModel toBasic(PokeCacheModel detail);
} 