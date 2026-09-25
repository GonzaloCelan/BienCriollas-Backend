package com.bienCriollas.stock.employee.interfaces;

import java.util.List;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.enums.EmployeeStatus;

public interface IEmployeeService {
    EmployeeResponseDTO create(EmployeeRequestDTO request);
    List<EmployeeResponseDTO> list(EmployeeStatus status);
    List<EmployeeResponseDTO> search(String query);
    EmployeeResponseDTO getById(Long id);
    EmployeeResponseDTO update(Long id, EmployeeRequestDTO request);
    EmployeeResponseDTO activate(Long id);
    EmployeeResponseDTO deactivate(Long id);
}
