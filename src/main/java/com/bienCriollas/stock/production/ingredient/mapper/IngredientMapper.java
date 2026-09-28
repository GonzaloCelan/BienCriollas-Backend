package com.bienCriollas.stock.production.ingredient.mapper;

import org.mapstruct.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.bienCriollas.stock.production.ingredient.dto.IngredientRequestDTO;
import com.bienCriollas.stock.production.ingredient.dto.IngredientResponseDTO;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
import com.bienCriollas.stock.production.ingredient.enums.ReferencePriceUnit;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IngredientMapper {

    @Mapping(target = "purchaseDataComplete", expression = "java(entity.hasCompletePurchaseData())")
    @Mapping(target = "referencePrice", expression = "java(referencePrice(entity))")
    @Mapping(target = "referencePriceUnit", expression = "java(referencePriceUnit(entity.getMeasurementUnit()))")
    IngredientResponseDTO toResponseDTO(Ingredient entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "costPerBaseUnit", ignore = true)
    @Mapping(target = "currentStock", ignore = true)
    @Mapping(target = "minimumStock", ignore = true)
    Ingredient toEntity(IngredientRequestDTO dto);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(IngredientRequestDTO dto, @MappingTarget Ingredient entity);

    default BigDecimal referencePrice(Ingredient entity) {
        BigDecimal multiplier = entity.getMeasurementUnit() == MeasurementUnit.UNIT
                ? BigDecimal.ONE : BigDecimal.valueOf(1000);
        return entity.getCostPerBaseUnit().multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    default ReferencePriceUnit referencePriceUnit(MeasurementUnit unit) {
        return switch (unit) {
            case GRAM -> ReferencePriceUnit.KG;
            case MILLILITER -> ReferencePriceUnit.LITER;
            case UNIT -> ReferencePriceUnit.UNIT;
        };
    }

}
