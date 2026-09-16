package com.bienCriollas.stock.production.ingredient.mapper;

import org.mapstruct.*;
import com.bienCriollas.stock.production.ingredient.dto.IngredientRequestDTO;
import com.bienCriollas.stock.production.ingredient.dto.IngredientResponseDTO;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IngredientMapper {

    @Mapping(target = "stockValue", expression = "java(entity.getCurrentStockGrams().multiply(entity.getCostPerKilogram()).movePointLeft(3))")
    @Mapping(target = "lowStock", expression = "java(entity.getCurrentStockGrams().compareTo(entity.getMinimumStockGrams()) <= 0)")
    @Mapping(target = "costPerGram", expression = "java(entity.getCostPerKilogram().movePointLeft(3))")
    IngredientResponseDTO toResponseDTO(Ingredient entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Ingredient toEntity(IngredientRequestDTO dto);

    @InheritConfiguration(name = "toEntity")
    void updateEntityFromDTO(IngredientRequestDTO dto, @MappingTarget Ingredient entity);

}
