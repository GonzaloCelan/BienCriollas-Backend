package com.bienCriollas.stock.production.process.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.production.process.dto.*;
import com.bienCriollas.stock.production.process.entity.*;
import com.bienCriollas.stock.production.process.enums.ProcessTimeType;
import com.bienCriollas.stock.production.process.exception.*;
import com.bienCriollas.stock.production.process.interfaces.IProductionProcessService;
import com.bienCriollas.stock.production.process.mapper.ProductionProcessMapper;
import com.bienCriollas.stock.production.process.repository.ProductionProcessRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionProcessService implements IProductionProcessService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "varietyName", "version", "referenceYieldUnits",
            "active", "createdAt", "updatedAt");

    private final ProductionProcessRepository processRepository;
    private final EmpanadaVarietyRepository varietyRepository;
    private final ProductionProcessMapper processMapper;
    private final Validator validator;

    @Override
    @Transactional
    public ProductionProcessResponseDTO createProcess(ProductionProcessRequestDTO dto) {
        validateRequest(dto);
        EmpanadaVariety variety = lockVariety(dto.varietyId());
        if (processRepository.existsByVarietyVarietyIdAndActiveTrue(variety.getVarietyId())) {
            throw new ProcessAlreadyExistsException(variety.getName());
        }

        ProductionProcess process = buildProcess(
                processMapper.toEntity(dto), variety, 1, dto.steps());
        return toResponse(processRepository.saveAndFlush(process));
    }

    @Override
    public ProductionProcessResponseDTO getProcessById(Long id) {
        return toResponse(findDetailed(id));
    }

    @Override
    public ProductionProcessResponseDTO getActiveProcessByVariety(Long varietyId) {
        validateId(varietyId, "El id de la variedad debe ser mayor a cero.");
        if (!varietyRepository.existsById(varietyId)) {
            throw new VarietyNotFoundException(varietyId);
        }
        return toResponse(processRepository.findByVarietyVarietyIdAndActiveTrue(varietyId)
                .orElseThrow(() -> ProductionProcessNotFoundException.forVariety(varietyId)));
    }

    @Override
    public Page<ProductionProcessResponseDTO> getActiveProcesses(Pageable pageable) {
        validatePageable(pageable);
        return processRepository.findByActiveTrue(toPersistencePageable(pageable))
                .map(this::toResponse);
    }

    @Override
    public Page<ProductionProcessResponseDTO> getProcessesByStatus(
            Boolean active, Pageable pageable) {
        validatePageable(pageable);
        if (active == null) {
            throw new InvalidProductionProcessException("El estado activo es obligatorio.");
        }
        Page<ProductionProcess> processes = active
                ? processRepository.findByActiveTrue(toPersistencePageable(pageable))
                : processRepository.findByActiveFalse(toPersistencePageable(pageable));
        return processes.map(this::toResponse);
    }

    @Override
    public List<ProductionProcessResponseDTO> getProcessHistory(Long varietyId) {
        validateId(varietyId, "El id de la variedad debe ser mayor a cero.");
        if (!varietyRepository.existsById(varietyId)) {
            throw new VarietyNotFoundException(varietyId);
        }
        return processRepository.findByVarietyVarietyIdOrderByVersionDesc(varietyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductionProcessResponseDTO createNewVersion(
            Long processId, ProductionProcessVersionRequestDTO dto) {
        validateId(processId, "El id del proceso debe ser mayor a cero.");
        validateVersionRequest(dto);

        ProductionProcess original = processRepository.findById(processId)
                .orElseThrow(() -> new ProductionProcessNotFoundException(processId));
        EmpanadaVariety variety = lockVariety(original.getVariety().getVarietyId());

        ProductionProcess latest = processRepository
                .findTopByVarietyVarietyIdOrderByVersionDesc(variety.getVarietyId())
                .orElseThrow(() -> new ProductionProcessNotFoundException(processId));
        ProductionProcess current = processRepository
                .findActiveByVarietyForUpdate(variety.getVarietyId())
                .orElseThrow(() -> ProductionProcessNotFoundException
                        .forVariety(variety.getVarietyId()));

        current.setActive(false);
        processRepository.saveAndFlush(current);

        ProductionProcess next = buildProcess(
                processMapper.toEntity(dto),
                variety,
                Math.addExact(latest.getVersion(), 1),
                dto.steps());
        return toResponse(processRepository.saveAndFlush(next));
    }

    private ProductionProcess buildProcess(
            ProductionProcess process,
            EmpanadaVariety variety,
            int version,
            List<ProductionProcessStepRequestDTO> stepRequests) {
        process.setVariety(variety);
        process.setVersion(version);
        process.setActive(true);
        process.setNotes(normalizeOptional(process.getNotes()));
        process.setSteps(new ArrayList<>());

        for (int index = 0; index < stepRequests.size(); index++) {
            ProductionProcessStepRequestDTO request = stepRequests.get(index);
            ProductionProcessStep step = processMapper.toEntity(request);
            step.setStepOrder(index + 1);
            step.setName(request.name().strip());
            step.setDescription(normalizeOptional(request.description()));
            step.setNotes(normalizeOptional(request.notes()));
            process.addStep(step);
        }
        return process;
    }

    private ProductionProcessResponseDTO toResponse(ProductionProcess process) {
        List<ProductionProcessStepResponseDTO> stepResponses = new ArrayList<>();
        int totalMinutes = 0;
        int activeMinutes = 0;
        int waitingMinutes = 0;
        int personMinutes = 0;

        for (ProductionProcessStep step : process.getSteps()) {
            int stepPersonMinutes = Math.multiplyExact(
                    step.getEstimatedMinutes(), step.getRequiredPeople());
            totalMinutes = Math.addExact(totalMinutes, step.getEstimatedMinutes());
            personMinutes = Math.addExact(personMinutes, stepPersonMinutes);
            if (step.getTimeType() == ProcessTimeType.ACTIVE) {
                activeMinutes = Math.addExact(activeMinutes, step.getEstimatedMinutes());
            } else {
                waitingMinutes = Math.addExact(waitingMinutes, step.getEstimatedMinutes());
            }
            stepResponses.add(new ProductionProcessStepResponseDTO(
                    step.getId(),
                    step.getStepOrder(),
                    step.getName(),
                    step.getDescription(),
                    step.getEstimatedMinutes(),
                    step.getRequiredPeople(),
                    step.getTimeType(),
                    step.getNotes(),
                    stepPersonMinutes));
        }

        BigDecimal personHours = BigDecimal.valueOf(personMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return new ProductionProcessResponseDTO(
                process.getId(),
                process.getVariety().getVarietyId(),
                process.getVariety().getName(),
                process.getVersion(),
                process.getReferenceYieldUnits(),
                process.getNotes(),
                stepResponses.size(),
                totalMinutes,
                activeMinutes,
                waitingMinutes,
                personMinutes,
                personHours,
                process.getActive(),
                List.copyOf(stepResponses),
                process.getCreatedAt(),
                process.getUpdatedAt());
    }

    private ProductionProcess findDetailed(Long id) {
        validateId(id, "El id del proceso debe ser mayor a cero.");
        return processRepository.findDetailedById(id)
                .orElseThrow(() -> new ProductionProcessNotFoundException(id));
    }

    private EmpanadaVariety lockVariety(Long varietyId) {
        validateId(varietyId, "El id de la variedad debe ser mayor a cero.");
        return varietyRepository.findByIdForUpdate(varietyId)
                .orElseThrow(() -> new VarietyNotFoundException(varietyId));
    }

    private void validateRequest(ProductionProcessRequestDTO dto) {
        if (dto == null) {
            throw new InvalidProductionProcessException(
                    "Los datos del proceso son obligatorios.");
        }
        validateReferenceYield(dto.referenceYieldUnits());
        validateStepsPresent(dto.steps());
        validateBean(dto);
        validateBusinessRules(dto.steps());
    }

    private void validateVersionRequest(ProductionProcessVersionRequestDTO dto) {
        if (dto == null) {
            throw new InvalidProductionProcessException(
                    "Los datos del proceso son obligatorios.");
        }
        validateReferenceYield(dto.referenceYieldUnits());
        validateStepsPresent(dto.steps());
        validateBean(dto);
        validateBusinessRules(dto.steps());
    }

    private void validateReferenceYield(Integer referenceYieldUnits) {
        if (referenceYieldUnits == null || referenceYieldUnits <= 0) {
            throw new InvalidProcessReferenceYieldException();
        }
    }

    private void validateStepsPresent(List<?> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new ProcessWithoutStepsException();
        }
    }

    private void validateBusinessRules(List<ProductionProcessStepRequestDTO> steps) {
        for (ProductionProcessStepRequestDTO step : steps) {
            if (step == null) {
                throw new InvalidProcessStepException("Los datos del paso son obligatorios.");
            }
            if (step.timeType() == ProcessTimeType.ACTIVE
                    && Integer.valueOf(0).equals(step.requiredPeople())) {
                throw InvalidProcessStepException.activeWithoutPeople(step.name().strip());
            }
        }
    }

    private void validateBean(Object value) {
        validator.validate(value).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new InvalidProductionProcessException(
                            violation.getPropertyPath() + ": " + violation.getMessage());
                });
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new InvalidProductionProcessException(message);
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidProductionProcessException("La paginación es obligatoria.");
        }
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidProductionProcessException(
                        "Campo de ordenamiento inválido: " + order.getProperty());
            }
        });
    }

    private Pageable toPersistencePageable(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> "varietyName".equals(order.getProperty())
                        ? order.withProperty("variety.name") : order)
                .toList());
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }
}
