package com.bienCriollas.stock.employee.mapper;

import org.mapstruct.*;

import com.bienCriollas.stock.employee.dto.EmployeeRequestDTO;
import com.bienCriollas.stock.employee.dto.EmployeeResponseDTO;
import com.bienCriollas.stock.employee.entity.Employee;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EmployeeMapper {

    EmployeeResponseDTO toResponse(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Employee toEntity(EmployeeRequestDTO request);

    @InheritConfiguration(name = "toEntity")
    void update(EmployeeRequestDTO request, @MappingTarget Employee employee);
}
