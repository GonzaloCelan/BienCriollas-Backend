package com.bienCriollas.stock.production.mapper;

import org.mapstruct.*;
import com.bienCriollas.stock.production.dto.*;
import com.bienCriollas.stock.production.entity.Production;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variety", ignore = true)
    @Mapping(target = "recipe", ignore = true)
    @Mapping(target = "process", ignore = true)
    @Mapping(target = "finalUnits", ignore = true)
    @Mapping(target = "totalMinutes", ignore = true)
    @Mapping(target = "peopleCount", ignore = true)
    @Mapping(target = "wasteUnits", ignore = true)
    @Mapping(target = "wasteReason", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "finalizedAt", ignore = true)
    @Mapping(target = "laborHourlyCostSnapshot", ignore = true)
    @Mapping(target = "energyPercentageSnapshot", ignore = true)
    Production toEntity(ProductionCreateRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variety", ignore = true)
    @Mapping(target = "recipe", ignore = true)
    @Mapping(target = "process", ignore = true)
    @Mapping(target = "productionDate", ignore = true)
    @Mapping(target = "plannedUnits", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "finalizedAt", ignore = true)
    @Mapping(target = "laborHourlyCostSnapshot", ignore = true)
    @Mapping(target = "energyPercentageSnapshot", ignore = true)
    void update(@MappingTarget Production production, ProductionUpdateDTO dto);
}
