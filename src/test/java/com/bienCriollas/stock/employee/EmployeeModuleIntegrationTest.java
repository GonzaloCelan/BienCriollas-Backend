package com.bienCriollas.stock.employee;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.enums.EmployeeStatus;
import com.bienCriollas.stock.employee.exception.*;
import com.bienCriollas.stock.employee.interfaces.*;
import com.bienCriollas.stock.employee.repository.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employees-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class EmployeeModuleIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);
    private static final PageRequest PAGE = PageRequest.of(0, 20);

    @Autowired private IEmployeeService employeeService;
    @Autowired private IEmployeeWorkDayService workDayService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private EmployeeWorkDayRepository workDayRepository;
    @Autowired private EmployeeWorkShiftRepository shiftRepository;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        workDayRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void managesEmployeeLifecycleAndOnlySearchesActiveEmployees() {
        EmployeeResponseDTO ana = employeeService.create(
                new EmployeeRequestDTO("  Ana   Perez  ", money("3500"), "  Cocina  "));
        EmployeeResponseDTO bea = employeeService.create(
                new EmployeeRequestDTO("Beatriz Gomez", money("4000"), null));

        assertThat(ana.name()).isEqualTo("Ana Perez");
        assertThat(ana.notes()).isEqualTo("Cocina");
        assertThat(ana.active()).isTrue();
        assertThat(employeeService.list(EmployeeStatus.ACTIVE))
                .extracting(EmployeeResponseDTO::name)
                .containsExactly("Ana Perez", "Beatriz Gomez");

        employeeService.deactivate(ana.id());
        assertThat(employeeService.list(EmployeeStatus.INACTIVE))
                .extracting(EmployeeResponseDTO::id).containsExactly(ana.id());
        assertThat(employeeService.search("ana")).isEmpty();

        EmployeeResponseDTO updated = employeeService.update(
                bea.id(), new EmployeeRequestDTO("Beatriz Gomez", money("4500"), "Caja"));
        assertThat(updated.hourlyRate()).isEqualByComparingTo("4500.00");
        assertThat(employeeService.activate(ana.id()).active()).isTrue();
    }

    @Test
    void calculatesContinuousSplitAndBreakShifts() {
        EmployeeResponseDTO employee = employee("Ana", "3500");

        EmployeeWorkDayResponseDTO continuous = create(employee.id(), MONDAY,
                shift("08:00", "12:00", null));
        EmployeeWorkDayResponseDTO split = create(employee.id(), MONDAY.plusDays(1),
                shift("08:00", "12:00", 0), shift("16:00", "20:00", 0));
        EmployeeWorkDayResponseDTO withBreak = create(employee.id(), MONDAY.plusDays(2),
                shift("08:00", "14:00", 30));

        assertThat(continuous.totalWorkedMinutes()).isEqualTo(240);
        assertThat(continuous.totalWorkedHours()).isEqualByComparingTo("4.00");
        assertThat(continuous.totalAmount()).isEqualByComparingTo("14000.00");
        assertThat(split.totalWorkedMinutes()).isEqualTo(480);
        assertThat(split.totalAmount()).isEqualByComparingTo("28000.00");
        assertThat(withBreak.totalWorkedMinutes()).isEqualTo(330);
        assertThat(withBreak.totalAmount()).isEqualByComparingTo("19250.00");
    }

    @Test
    void ordersShiftsAndAllowsContiguousButRejectsOverlapAndInvalidBreaks() {
        EmployeeResponseDTO employee = employee("Ana", "3500");

        EmployeeWorkDayResponseDTO valid = create(employee.id(), MONDAY,
                shift("12:00", "16:00", 0), shift("08:00", "12:00", 0));
        assertThat(valid.shifts()).extracting(WorkShiftResponseDTO::startTime)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertThat(valid.shifts()).extracting(WorkShiftResponseDTO::sortOrder)
                .containsExactly(1, 2);

        assertThatThrownBy(() -> create(employee.id(), MONDAY.plusDays(1),
                shift("08:00", "12:30", 0), shift("12:00", "16:00", 0)))
                .isInstanceOf(OverlappingWorkShiftException.class);
        assertThatThrownBy(() -> create(employee.id(), MONDAY.plusDays(1),
                shift("08:00", "09:00", 60)))
                .isInstanceOf(InvalidWorkShiftException.class);
        assertThatThrownBy(() -> create(employee.id(), MONDAY.plusDays(1),
                shift("20:00", "04:00", 0)))
                .isInstanceOf(InvalidWorkShiftException.class);
    }

    @Test
    void blocksInactiveAndDuplicateEmployeeWorkDays() {
        EmployeeResponseDTO employee = employee("Ana", "3500");
        create(employee.id(), MONDAY, shift("08:00", "12:00", 0));

        assertThatThrownBy(() -> create(employee.id(), MONDAY,
                shift("13:00", "17:00", 0)))
                .isInstanceOf(WorkDayAlreadyExistsException.class);

        employeeService.deactivate(employee.id());
        assertThatThrownBy(() -> create(employee.id(), MONDAY.plusDays(1),
                shift("08:00", "12:00", 0)))
                .isInstanceOf(EmployeeInactiveException.class);
    }

    @Test
    void preservesHistoricalRateWhenEmployeeRateAndWorkDayChange() {
        EmployeeResponseDTO employee = employee("Ana", "3500");
        EmployeeWorkDayResponseDTO original = create(employee.id(), MONDAY,
                shift("08:00", "12:00", 0));

        employeeService.update(employee.id(),
                new EmployeeRequestDTO("Ana", money("4000"), null));
        EmployeeWorkDayResponseDTO edited = workDayService.update(original.id(),
                new EmployeeWorkDayUpdateRequestDTO(MONDAY, "corregida",
                        List.of(shift("08:00", "13:00", 0))));
        EmployeeWorkDayResponseDTO newDay = create(employee.id(), MONDAY.plusDays(1),
                shift("08:00", "12:00", 0));

        assertThat(edited.hourlyRateSnapshot()).isEqualByComparingTo("3500.00");
        assertThat(edited.totalAmount()).isEqualByComparingTo("17500.00");
        assertThat(newDay.hourlyRateSnapshot()).isEqualByComparingTo("4000.00");
        assertThat(newDay.totalAmount()).isEqualByComparingTo("16000.00");
    }

    @Test
    void bulkUsesEachRateAndRollsBackCompletelyWhenThereIsAConflict() {
        EmployeeResponseDTO ana = employee("Ana", "3000");
        EmployeeResponseDTO bea = employee("Bea", "4000");
        EmployeeResponseDTO carla = employee("Carla", "5000");

        List<EmployeeWorkDayResponseDTO> created = workDayService.createBulk(
                new EmployeeWorkDayBulkCreateRequestDTO(
                        List.of(ana.id(), bea.id(), carla.id()), MONDAY, null,
                        List.of(shift("08:00", "12:00", 0))));
        assertThat(created).extracting(EmployeeWorkDayResponseDTO::totalAmount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(money("12000"), money("16000"), money("20000"));

        LocalDate nextDay = MONDAY.plusDays(1);
        create(bea.id(), nextDay, shift("08:00", "12:00", 0));
        long before = workDayRepository.count();
        assertThatThrownBy(() -> workDayService.createBulk(
                new EmployeeWorkDayBulkCreateRequestDTO(
                        List.of(ana.id(), bea.id(), carla.id()), nextDay, null,
                        List.of(shift("13:00", "17:00", 0)))))
                .isInstanceOf(BulkWorkDayConflictException.class)
                .satisfies(exception -> assertThat(
                        ((BulkWorkDayConflictException) exception).getConflicts())
                        .extracting(EmployeeConflictDTO::employeeId)
                        .containsExactly(bea.id()));
        assertThat(workDayRepository.count()).isEqualTo(before);
    }

    @Test
    void copyUsesCurrentRateAndDeleteCascadesShifts() {
        EmployeeResponseDTO employee = employee("Ana", "3500");
        EmployeeWorkDayResponseDTO source = create(employee.id(), MONDAY,
                shift("08:00", "12:00", 0), shift("13:00", "16:00", 0));
        employeeService.update(employee.id(),
                new EmployeeRequestDTO("Ana", money("4000"), null));

        EmployeeWorkDayResponseDTO copy = workDayService.copy(source.id(),
                new EmployeeWorkDayCopyRequestDTO(MONDAY.plusDays(1)));
        assertThat(copy.hourlyRateSnapshot()).isEqualByComparingTo("4000.00");
        assertThat(copy.shifts()).hasSize(2);

        long shiftCount = shiftRepository.count();
        workDayService.delete(copy.id());
        assertThat(workDayRepository.findById(copy.id())).isEmpty();
        assertThat(shiftRepository.count()).isEqualTo(shiftCount - 2);
    }

    @Test
    void historyAndSummariesKeepInactiveEmployees() {
        EmployeeResponseDTO ana = employee("Ana", "3000");
        EmployeeResponseDTO bea = employee("Bea", "4000");
        create(ana.id(), MONDAY, shift("08:00", "12:00", 0));
        create(ana.id(), MONDAY.plusDays(1), shift("08:00", "10:00", 0));
        create(bea.id(), MONDAY, shift("08:00", "11:00", 0));
        employeeService.deactivate(ana.id());

        EmployeePeriodSummaryResponseDTO week = workDayService.weekSummary(MONDAY);
        assertThat(week.totalWorkedMinutes()).isEqualTo(540);
        assertThat(week.totalAmount()).isEqualByComparingTo("30000.00");
        assertThat(week.employees()).extracting(EmployeePeriodSummaryItemDTO::employeeName)
                .containsExactly("Ana", "Bea");

        EmployeeDaySummaryResponseDTO day = workDayService.daySummary(MONDAY);
        assertThat(day.employeesWorked()).isEqualTo(2);
        assertThat(day.totalWorkedMinutes()).isEqualTo(420);
        assertThat(workDayService.monthSummary(2026, 9).totalAmount())
                .isEqualByComparingTo("30000.00");
        assertThat(workDayService.history(null, MONDAY, null, null, null, PAGE))
                .hasSize(2);

        EmployeeWorkDayHistoryResponseDTO history = workDayService.employeeHistory(
                ana.id(), MONDAY, MONDAY.plusDays(6), PAGE);
        assertThat(history.workDays()).hasSize(2);
        assertThat(history.totalWorkedMinutes()).isEqualTo(360);
        assertThat(history.totalAmount()).isEqualByComparingTo("18000.00");

        EmployeeWeekResponseDTO grid = workDayService.week(MONDAY);
        assertThat(grid.employees()).filteredOn(row -> row.employeeId().equals(ana.id()))
                .singleElement().satisfies(row -> {
                    assertThat(row.active()).isFalse();
                    assertThat(row.days()).hasSize(2);
                });
    }

    @Test
    void endpointsRequireAdministratorAndExposeBulkConflictDetails() throws Exception {
        String employeeJson = """
                {"name":"Ana Perez","hourlyRate":3500,"notes":"Cocina"}
                """;
        mockMvc.perform(post("/api/v2/employees")
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v2/employees").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson))
                .andExpect(status().isForbidden());

        EmployeeResponseDTO employee = employee("Ana", "3500");
        String createJson = """
                {"employeeId":%d,"workDate":"2026-09-21","shifts":[
                  {"startTime":"08:00","endTime":"12:00","breakMinutes":0}
                ]}
                """.formatted(employee.id());
        mockMvc.perform(post("/api/v2/employee-workdays")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalWorkedMinutes").value(240))
                .andExpect(jsonPath("$.totalAmount").value(14000.0));

        String bulkJson = """
                {"employeeIds":[%d],"workDate":"2026-09-21","shifts":[
                  {"startTime":"13:00","endTime":"17:00","breakMinutes":0}
                ]}
                """.formatted(employee.id());
        mockMvc.perform(post("/api/v2/employee-workdays/bulk")
                        .with(adminJwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(bulkJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts[0].employeeId").value(employee.id()))
                .andExpect(jsonPath("$.conflicts[0].employeeName").value("Ana"));
    }

    @Test
    void validatesHistoryFiltersAndMaximumPageSize() {
        assertThatThrownBy(() -> workDayService.history(
                null, MONDAY, MONDAY, MONDAY, null, PAGE))
                .isInstanceOf(InvalidWorkDayException.class);
        assertThatThrownBy(() -> workDayService.history(
                null, null, null, null, null, PageRequest.of(0, 101)))
                .isInstanceOf(InvalidWorkDayException.class);
        assertThatThrownBy(() -> workDayService.monthSummary(2026, 13))
                .isInstanceOf(InvalidWorkDayException.class);
    }

    private EmployeeResponseDTO employee(String name, String hourlyRate) {
        return employeeService.create(new EmployeeRequestDTO(name, money(hourlyRate), null));
    }

    private EmployeeWorkDayResponseDTO create(
            Long employeeId, LocalDate date, WorkShiftRequestDTO... shifts) {
        return workDayService.create(new EmployeeWorkDayCreateRequestDTO(
                employeeId, date, null, List.of(shifts)));
    }

    private WorkShiftRequestDTO shift(String start, String end, Integer breakMinutes) {
        return new WorkShiftRequestDTO(
                LocalTime.parse(start), LocalTime.parse(end), breakMinutes);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }
}
