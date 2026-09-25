package com.bienCriollas.stock.employee.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.entity.*;
import com.bienCriollas.stock.employee.enums.WorkDayPeriod;
import com.bienCriollas.stock.employee.exception.*;
import com.bienCriollas.stock.employee.interfaces.IEmployeeWorkDayService;
import com.bienCriollas.stock.employee.mapper.EmployeeWorkDayMapper;
import com.bienCriollas.stock.employee.repository.*;
import com.bienCriollas.stock.employee.service.EmployeePeriodResolver.DateRange;
import com.bienCriollas.stock.employee.service.EmployeeWorkDayCalculator.Calculation;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeWorkDayService implements IEmployeeWorkDayService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "workDate", "totalWorkedMinutes", "totalAmount", "createdAt", "updatedAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employeeRepository;
    private final EmployeeWorkDayRepository workDayRepository;
    private final EmployeeWorkDayCalculator calculator;
    private final EmployeePeriodResolver periodResolver;
    private final EmployeeWorkDayMapper mapper;
    private final Validator validator;

    @Override
    @Transactional
    public EmployeeWorkDayResponseDTO create(EmployeeWorkDayCreateRequestDTO request) {
        validate(request);
        Employee employee = findActiveEmployeeForUpdate(request.employeeId());
        ensureDateAvailable(employee, request.workDate(), null);
        EmployeeWorkDay workDay = buildWorkDay(
                employee, request.workDate(), request.notes(), request.shifts());
        return save(workDay);
    }

    @Override
    @Transactional
    public List<EmployeeWorkDayResponseDTO> createBulk(
            EmployeeWorkDayBulkCreateRequestDTO request) {
        validate(request);
        List<Long> employeeIds = request.employeeIds().stream().distinct().sorted().toList();
        if (employeeIds.size() != request.employeeIds().size()) {
            throw new InvalidWorkDayException(
                    "La selección masiva no puede contener empleados repetidos.");
        }

        List<Employee> employees = employeeRepository.findAllByIdForUpdate(employeeIds);
        if (employees.size() != employeeIds.size()) {
            Set<Long> found = employees.stream().map(Employee::getId).collect(Collectors.toSet());
            Long missingId = employeeIds.stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new EmployeeNotFoundException(missingId);
        }
        employees.stream()
                .filter(employee -> !Boolean.TRUE.equals(employee.getActive()))
                .findFirst()
                .ifPresent(employee -> { throw new EmployeeInactiveException(employee.getName()); });

        List<EmployeeConflictDTO> conflicts = workDayRepository
                .findAllByEmployeeIdInAndWorkDate(employeeIds, request.workDate()).stream()
                .map(workDay -> new EmployeeConflictDTO(
                        workDay.getEmployee().getId(), workDay.getEmployee().getName()))
                .sorted(Comparator.comparing(EmployeeConflictDTO::employeeName)
                        .thenComparing(EmployeeConflictDTO::employeeId))
                .toList();
        if (!conflicts.isEmpty()) {
            throw new BulkWorkDayConflictException(conflicts);
        }

        List<EmployeeWorkDay> workDays = employees.stream()
                .map(employee -> buildWorkDay(employee, request.workDate(), request.notes(),
                        request.shifts()))
                .toList();
        try {
            return workDayRepository.saveAllAndFlush(workDays).stream()
                    .map(mapper::toResponse)
                    .toList();
        } catch (DataIntegrityViolationException exception) {
            throw new WorkDayAlreadyExistsException("uno de los empleados", request.workDate());
        }
    }

    @Override
    public EmployeeWorkDayResponseDTO getById(Long id) {
        validateId(id, "jornada");
        return mapper.toResponse(workDayRepository.findDetailedById(id)
                .orElseThrow(() -> new WorkDayNotFoundException(id)));
    }

    @Override
    @Transactional
    public EmployeeWorkDayResponseDTO update(
            Long id, EmployeeWorkDayUpdateRequestDTO request) {
        validateId(id, "jornada");
        validate(request);
        EmployeeWorkDay workDay = workDayRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new WorkDayNotFoundException(id));
        ensureDateAvailable(workDay.getEmployee(), request.workDate(), id);
        workDay.setWorkDate(request.workDate());
        workDay.setNotes(normalizeNotes(request.notes()));
        applyCalculation(workDay, request.shifts());
        return save(workDay);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        validateId(id, "jornada");
        EmployeeWorkDay workDay = workDayRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new WorkDayNotFoundException(id));
        workDayRepository.delete(workDay);
        workDayRepository.flush();
    }

    @Override
    @Transactional
    public EmployeeWorkDayResponseDTO copy(Long id, EmployeeWorkDayCopyRequestDTO request) {
        validateId(id, "jornada");
        validate(request);
        EmployeeWorkDay source = workDayRepository.findDetailedById(id)
                .orElseThrow(() -> new WorkDayNotFoundException(id));
        Employee employee = findActiveEmployeeForUpdate(source.getEmployee().getId());
        ensureDateAvailable(employee, request.targetDate(), null);
        List<WorkShiftRequestDTO> shifts = source.getShifts().stream()
                .map(mapper::toShiftRequest)
                .toList();
        return save(buildWorkDay(
                employee, request.targetDate(), source.getNotes(), shifts));
    }

    @Override
    public Page<EmployeeWorkDayResponseDTO> history(
            Long employeeId, LocalDate date, LocalDate from, LocalDate to,
            WorkDayPeriod period, Pageable pageable) {
        if (employeeId != null) {
            validateId(employeeId, "empleado");
        }
        validatePageable(pageable);
        DateRange range = periodResolver.resolveHistory(date, from, to, period);
        Specification<EmployeeWorkDay> specification = specification(employeeId, range);
        return workDayRepository.findAll(specification, persistencePageable(pageable))
                .map(mapper::toResponse);
    }

    @Override
    public EmployeeWorkDayHistoryResponseDTO employeeHistory(
            Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        validateId(employeeId, "empleado");
        validatePageable(pageable);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        DateRange range = from == null && to == null
                ? new DateRange(null, null)
                : periodResolver.custom(from, to);
        Page<EmployeeWorkDayResponseDTO> workDays = workDayRepository
                .findAll(specification(employeeId, range), persistencePageable(pageable))
                .map(mapper::toResponse);
        EmployeeWorkSummaryProjection summary = workDayRepository
                .summarizeEmployee(employeeId, range.from(), range.to()).orElse(null);
        long minutes = summary == null ? 0L : summary.getWorkedMinutes();
        BigDecimal amount = summary == null ? moneyZero() : summary.getAmount();
        return new EmployeeWorkDayHistoryResponseDTO(
                new EmployeeReferenceDTO(employee.getId(), employee.getName()),
                range.from(), range.to(), minutes, calculator.hours(minutes), amount, workDays);
    }

    @Override
    public EmployeeWeekResponseDTO week(LocalDate date) {
        DateRange range = periodResolver.week(date);
        List<EmployeeWorkDay> workDays = workDayRepository
                .findDetailedBetween(range.from(), range.to());

        Map<Long, Employee> employees = employeeRepository.findByActiveTrueOrderByNameAscIdAsc()
                .stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        workDays.forEach(workDay -> employees.putIfAbsent(
                workDay.getEmployee().getId(), workDay.getEmployee()));

        Map<Long, List<EmployeeWorkDay>> byEmployee = workDays.stream()
                .collect(Collectors.groupingBy(workDay -> workDay.getEmployee().getId()));
        List<EmployeeWeekRowDTO> rows = employees.values().stream()
                .sorted(Comparator.comparing(Employee::getName).thenComparing(Employee::getId))
                .map(employee -> toWeekRow(employee,
                        byEmployee.getOrDefault(employee.getId(), List.of())))
                .toList();
        long totalMinutes = rows.stream().mapToLong(EmployeeWeekRowDTO::weekWorkedMinutes).sum();
        BigDecimal totalAmount = rows.stream().map(EmployeeWeekRowDTO::weekAmount)
                .reduce(moneyZero(), BigDecimal::add);
        return new EmployeeWeekResponseDTO(
                range.from(), range.to(), rows, totalMinutes,
                calculator.hours(totalMinutes), totalAmount);
    }

    @Override
    public EmployeePeriodSummaryResponseDTO weekSummary(LocalDate date) {
        DateRange range = periodResolver.week(date);
        return periodSummary(range);
    }

    @Override
    public EmployeeMonthSummaryResponseDTO monthSummary(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new InvalidWorkDayException("Los parámetros year y month son obligatorios.");
        }
        DateRange range = periodResolver.month(year, month);
        EmployeePeriodSummaryResponseDTO summary = periodSummary(range);
        return new EmployeeMonthSummaryResponseDTO(
                year, month, summary.employees(), summary.totalWorkedMinutes(),
                summary.totalWorkedHours(), summary.totalAmount());
    }

    @Override
    public EmployeeDaySummaryResponseDTO daySummary(LocalDate date) {
        LocalDate selectedDate = date == null
                ? LocalDate.now(EmployeePeriodResolver.ARGENTINA_ZONE)
                : date;
        EmployeePeriodSummaryResponseDTO summary = periodSummary(
                new DateRange(selectedDate, selectedDate));
        return new EmployeeDaySummaryResponseDTO(
                selectedDate, summary.employees().size(), summary.totalWorkedMinutes(),
                summary.totalWorkedHours(), summary.totalAmount(), summary.employees());
    }

    @Override
    public EmployeeDashboardResponseDTO dashboard() {
        LocalDate today = LocalDate.now(EmployeePeriodResolver.ARGENTINA_ZONE);
        EmployeePeriodSummaryResponseDTO day = periodSummary(new DateRange(today, today));
        EmployeePeriodSummaryResponseDTO week = periodSummary(periodResolver.week(today));
        EmployeePeriodSummaryResponseDTO month = periodSummary(
                periodResolver.month(today.getYear(), today.getMonthValue()));
        return new EmployeeDashboardResponseDTO(
                employeeRepository.countByActiveTrue(), metric(day), metric(week), metric(month));
    }

    private EmployeeWorkDay buildWorkDay(Employee employee, LocalDate date, String notes,
            List<WorkShiftRequestDTO> requestedShifts) {
        EmployeeWorkDay workDay = EmployeeWorkDay.builder()
                .employee(employee)
                .workDate(date)
                .hourlyRateSnapshot(employee.getHourlyRate())
                .notes(normalizeNotes(notes))
                .build();
        applyCalculation(workDay, requestedShifts);
        return workDay;
    }

    private void applyCalculation(
            EmployeeWorkDay workDay, List<WorkShiftRequestDTO> requestedShifts) {
        Calculation calculation = calculator.calculate(
                requestedShifts, workDay.getHourlyRateSnapshot());
        workDay.replaceShifts(calculation.shifts());
        workDay.setTotalWorkedMinutes(calculation.totalWorkedMinutes());
        workDay.setTotalAmount(calculation.totalAmount());
    }

    private Employee findActiveEmployeeForUpdate(Long employeeId) {
        validateId(employeeId, "empleado");
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new EmployeeInactiveException(employee.getName());
        }
        return employee;
    }

    private void ensureDateAvailable(Employee employee, LocalDate date, Long currentId) {
        if (date == null) {
            throw new InvalidWorkDayException("La fecha de la jornada es obligatoria.");
        }
        workDayRepository.findAllByEmployeeIdInAndWorkDate(List.of(employee.getId()), date)
                .stream()
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new WorkDayAlreadyExistsException(employee.getName(), date);
                });
    }

    private EmployeeWorkDayResponseDTO save(EmployeeWorkDay workDay) {
        validate(workDay);
        try {
            return mapper.toResponse(workDayRepository.saveAndFlush(workDay));
        } catch (DataIntegrityViolationException exception) {
            throw new WorkDayAlreadyExistsException(
                    workDay.getEmployee().getName(), workDay.getWorkDate());
        }
    }

    private Specification<EmployeeWorkDay> specification(Long employeeId, DateRange range) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("employee").get("id"), employeeId));
            }
            if (range.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("workDate"), range.from()));
            }
            if (range.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("workDate"), range.to()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private EmployeeWeekRowDTO toWeekRow(
            Employee employee, List<EmployeeWorkDay> employeeWorkDays) {
        List<EmployeeWeekDayDTO> days = employeeWorkDays.stream()
                .sorted(Comparator.comparing(EmployeeWorkDay::getWorkDate))
                .map(workDay -> new EmployeeWeekDayDTO(
                        workDay.getWorkDate(), workDay.getId(), workDay.getShifts().size(),
                        workDay.getTotalWorkedMinutes(),
                        calculator.hours(workDay.getTotalWorkedMinutes()),
                        workDay.getTotalAmount()))
                .toList();
        long minutes = employeeWorkDays.stream()
                .mapToLong(EmployeeWorkDay::getTotalWorkedMinutes).sum();
        BigDecimal amount = employeeWorkDays.stream().map(EmployeeWorkDay::getTotalAmount)
                .reduce(moneyZero(), BigDecimal::add);
        return new EmployeeWeekRowDTO(
                employee.getId(), employee.getName(), employee.getHourlyRate(), employee.getActive(),
                days, minutes, calculator.hours(minutes), amount);
    }

    private EmployeePeriodSummaryResponseDTO periodSummary(DateRange range) {
        List<EmployeePeriodSummaryItemDTO> items = workDayRepository
                .summarizeByEmployee(range.from(), range.to()).stream()
                .map(summary -> new EmployeePeriodSummaryItemDTO(
                        summary.getEmployeeId(), summary.getEmployeeName(),
                        summary.getWorkedMinutes(), calculator.hours(summary.getWorkedMinutes()),
                        summary.getAmount()))
                .toList();
        long totalMinutes = items.stream()
                .mapToLong(EmployeePeriodSummaryItemDTO::workedMinutes).sum();
        BigDecimal totalAmount = items.stream().map(EmployeePeriodSummaryItemDTO::amount)
                .reduce(moneyZero(), BigDecimal::add);
        return new EmployeePeriodSummaryResponseDTO(
                range.from(), range.to(), items, totalMinutes,
                calculator.hours(totalMinutes), totalAmount);
    }

    private EmployeeDashboardMetricDTO metric(EmployeePeriodSummaryResponseDTO summary) {
        return new EmployeeDashboardMetricDTO(
                summary.totalWorkedMinutes(), summary.totalWorkedHours(),
                summary.totalAmount(), summary.employees().size());
    }

    private Pageable persistencePageable(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream().toList());
        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null || !pageable.isPaged()) {
            throw new InvalidWorkDayException("La paginación es obligatoria.");
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new InvalidWorkDayException("El tamaño máximo de página es 100.");
        }
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidWorkDayException(
                        "Campo de ordenamiento inválido: " + order.getProperty());
            }
        });
    }

    private void validate(Object value) {
        if (value == null) {
            throw new InvalidWorkDayException("Los datos de la jornada son obligatorios.");
        }
        validator.validate(value).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new InvalidWorkDayException(
                            violation.getPropertyPath() + ": " + violation.getMessage());
                });
    }

    private void validateId(Long id, String resource) {
        if (id == null || id <= 0) {
            throw new InvalidWorkDayException(
                    "El id de " + resource + " debe ser mayor a cero.");
        }
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.strip();
    }

    private BigDecimal moneyZero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
