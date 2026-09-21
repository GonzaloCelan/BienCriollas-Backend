package com.bienCriollas.stock.production.ingredient.mapper;

import org.mapstruct.*;
import com.bienCriollas.stock.production.ingredient.dto.IngredientRequestDTO;
import com.bienCriollas.stock.production.ingredient.dto.IngredientResponseDTO;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IngredientMapper {

    @Mapping(target = "stockValue", expression = "java(entity.getCurrentStock().multiply(entity.getCostPerBaseUnit()))")
    @Mapping(target = "lowStock", expression = "java(entity.getCurrentStock().compareTo(entity.getMinimumStock()) <= 0)")
    @Mapping(target = "purchaseDataComplete", expression = "java(entity.hasCompletePurchaseData())")
    IngredientResponseDTO toResponseDTO(Ingredient entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "costPerBaseUnit", ignore = true)
    Ingredient toEntity(IngredientRequestDTO dto);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(IngredientRequestDTO dto, @MappingTarget Ingredient entity);

}
