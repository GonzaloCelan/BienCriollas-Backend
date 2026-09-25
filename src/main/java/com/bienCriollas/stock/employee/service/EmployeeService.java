package com.bienCriollas.stock.employee.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.entity.Employee;
import com.bienCriollas.stock.employee.enums.EmployeeStatus;
import com.bienCriollas.stock.employee.exception.*;
import com.bienCriollas.stock.employee.interfaces.IEmployeeService;
import com.bienCriollas.stock.employee.mapper.EmployeeMapper;
import com.bienCriollas.stock.employee.repository.EmployeeRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService implements IEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final Validator validator;

    @Override
    @Transactional
    public EmployeeResponseDTO create(EmployeeRequestDTO request) {
        validate(request);
        Employee employee = employeeMapper.toEntity(request);
        employee.setName(normalizeName(request.name()));
        employee.setNotes(normalizeNotes(request.notes()));
        employee.setActive(true);
        return employeeMapper.toResponse(employeeRepository.saveAndFlush(employee));
    }

    @Override
    public List<EmployeeResponseDTO> list(EmployeeStatus status) {
        EmployeeStatus selected = status == null ? EmployeeStatus.ACTIVE : status;
        List<Employee> employees = switch (selected) {
            case ACTIVE -> employeeRepository.findByActiveTrueOrderByNameAscIdAsc();
            case INACTIVE -> employeeRepository.findByActiveFalseOrderByNameAscIdAsc();
            case ALL -> employeeRepository.findAllByOrderByNameAscIdAsc();
        };
        return employees.stream().map(employeeMapper::toResponse).toList();
    }

    @Override
    public List<EmployeeResponseDTO> search(String query) {
        if (query == null || query.isBlank() || query.length() > 150) {
            throw new InvalidEmployeeException(
                    "La búsqueda debe contener entre 1 y 150 caracteres.");
        }
        return employeeRepository.searchActive(query.strip()).stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    public EmployeeResponseDTO getById(Long id) {
        validateId(id);
        return employeeMapper.toResponse(employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id)));
    }

    @Override
    @Transactional
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO request) {
        validate(request);
        Employee employee = findForUpdate(id);
        employeeMapper.update(request, employee);
        employee.setName(normalizeName(request.name()));
        employee.setNotes(normalizeNotes(request.notes()));
        return employeeMapper.toResponse(employeeRepository.saveAndFlush(employee));
    }

    @Override
    @Transactional
    public EmployeeResponseDTO activate(Long id) {
        Employee employee = findForUpdate(id);
        employee.setActive(true);
        return employeeMapper.toResponse(employeeRepository.saveAndFlush(employee));
    }

    @Override
    @Transactional
    public EmployeeResponseDTO deactivate(Long id) {
        Employee employee = findForUpdate(id);
        employee.setActive(false);
        return employeeMapper.toResponse(employeeRepository.saveAndFlush(employee));
    }

    private Employee findForUpdate(Long id) {
        validateId(id);
        return employeeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void validate(Object value) {
        if (value == null) {
            throw new InvalidEmployeeException("Los datos del empleado son obligatorios.");
        }
        validator.validate(value).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new InvalidEmployeeException(
                            violation.getPropertyPath() + ": " + violation.getMessage());
                });
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidEmployeeException("El id del empleado debe ser mayor a cero.");
        }
    }

    private String normalizeName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.strip();
    }
}
