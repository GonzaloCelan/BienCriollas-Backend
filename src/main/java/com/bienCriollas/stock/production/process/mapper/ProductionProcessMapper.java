package com.bienCriollas.stock.production.process.mapper;

import org.mapstruct.*;

import com.bienCriollas.stock.production.process.dto.*;
import com.bienCriollas.stock.production.process.entity.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductionProcessMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variety", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductionProcess toEntity(ProductionProcessRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variety", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductionProcess toEntity(ProductionProcessVersionRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "process", ignore = true)
    @Mapping(target = "stepOrder", ignore = true)
    ProductionProcessStep toEntity(ProductionProcessStepRequestDTO dto);
}
