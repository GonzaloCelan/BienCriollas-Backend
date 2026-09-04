package com.bienCriollas.stock.expense.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.expense.dto.ExpenseResponseDTO;
import com.bienCriollas.stock.expense.dto.ExpenseTypeDTO;
import com.bienCriollas.stock.expense.dto.ExpenseTotalByTypeDTO;
import com.bienCriollas.stock.expense.dto.ExpensePercentageDTO;
import com.bienCriollas.stock.expense.interfaces.ExpenseTotalByTypeProjection;
import com.bienCriollas.stock.expense.interfaces.IExpenseService;
import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.bienCriollas.stock.expense.entity.Expense;
import com.bienCriollas.stock.expense.exception.InvalidExpenseException;
import com.bienCriollas.stock.expense.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService implements IExpenseService {

    private final ExpenseRepository expenseRepository;

    private static final ZoneId ARGENTINA_ZONE =
            ZoneId.of("America/Argentina/Buenos_Aires");

    /*
     * ==========================
     * CREACIÓN
     * ==========================
     */

    @Override
    @Transactional
    public Expense registerExpense(ExpenseTypeDTO request) {

        if (request == null) {
            throw new InvalidExpenseException("El egreso no puede ser nulo.");
        }

        if (request.expenseType() == null) {
            throw new InvalidExpenseException("El tipo de egreso es obligatorio.");
        }

        if (request.description() == null || request.description().isBlank()) {
            throw new InvalidExpenseException("La descripción es obligatoria.");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidExpenseException("El monto debe ser mayor a cero.");
        }

        LocalDateTime now = LocalDateTime.now(ARGENTINA_ZONE);

        Expense expense = new Expense();
        expense.setExpenseType(request.expenseType());
        expense.setDescription(request.description().trim());
        expense.setAmount(request.amount());
        expense.setTime(now.toLocalTime());
        expense.setCreatedAt(now);

        return expenseRepository.save(expense);
    }

    /*
     * ==========================
     * RESÚMENES / KPIS
     * ==========================
     */

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponseDTO calculateAccumulatedExpenses() {

        DateRange currentMonthRange = getCurrentMonthRange();

        BigDecimal personnelTotal = expenseRepository.sumTotalByTypeBetweenDates(
                ExpenseType.PERSONAL,
                currentMonthRange.start(),
                currentMonthRange.end()
        );

        BigDecimal productionTotal = expenseRepository.sumTotalByTypeBetweenDates(
                ExpenseType.PRODUCCION,
                currentMonthRange.start(),
                currentMonthRange.end()
        );

        BigDecimal otherTotal = expenseRepository.sumTotalByTypeBetweenDates(
                ExpenseType.OTROS,
                currentMonthRange.start(),
                currentMonthRange.end()
        );

        return new ExpenseResponseDTO(
                normalize(personnelTotal),
                normalize(productionTotal),
                normalize(otherTotal)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpensePercentageDTO> getCurrentVsPreviousMonthMetrics() {

        DateRange currentMonth = getCurrentMonthRange();
        DateRange previousMonth = getPreviousMonthRange();

        List<ExpensePercentageDTO> result = new ArrayList<>();

        for (ExpenseType expenseType : ExpenseType.values()) {

            BigDecimal currentTotal = expenseRepository.sumTotalByTypeBetweenDates(
                    expenseType,
                    currentMonth.start(),
                    currentMonth.end()
            );

            BigDecimal previousTotal = expenseRepository.sumTotalByTypeBetweenDates(
                    expenseType,
                    previousMonth.start(),
                    previousMonth.end()
            );

            BigDecimal percentage = calculatePercentageChange(
                    normalize(currentTotal),
                    normalize(previousTotal)
            );

            result.add(
                    new ExpensePercentageDTO(
                            expenseType,
                            normalize(currentTotal),
                            percentage
                    )
            );
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseTotalByTypeDTO> getTotalsByType(int year, int month) {

        DateRange range = getMonthRange(year, month);

        List<ExpenseTotalByTypeProjection> rows =
                expenseRepository.getTotalsByTypeBetweenDates(
                        range.start(),
                        range.end()
                );

        return rows.stream()
                .map(row -> new ExpenseTotalByTypeDTO(
                        row.getExpenseType(),
                        normalize(row.getTotal())
                ))
                .toList();
    }

    /*
     * ==========================
     * LISTADOS
     * ==========================
     */

    @Override
    @Transactional(readOnly = true)
    public List<Expense> getTodaysExpenses() {

        LocalDate today = LocalDate.now(ARGENTINA_ZONE);
        DateRange range = getDayRange(today);

        return expenseRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        range.start(),
                        range.end()
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> getHistory(
            int year,
            int month,
            ExpenseType expenseType,
            Pageable pageable
    ) {
        DateRange range = getMonthRange(year, month);

        if (expenseType == null) {
            return getHistory(year, month, pageable);
        }

        return expenseRepository
                .findByExpenseTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        expenseType,
                        range.start(),
                        range.end(),
                        pageable
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> getHistory(
            int year,
            int month,
            Pageable pageable
    ) {
        DateRange range = getMonthRange(year, month);

        return expenseRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        range.start(),
                        range.end(),
                        pageable
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> getByExpenseType(
            ExpenseType expenseType,
            Pageable pageable
    ) {
        return expenseRepository.findByExpenseTypeOrderByCreatedAtDesc(
                expenseType,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> getLatestMovements() {
        return expenseRepository.findTop5ByOrderByCreatedAtDesc();
    }

    /*
     * ==========================
     * HELPERS
     * ==========================
     */

    private DateRange getDayRange(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return new DateRange(start, end);
    }

    private DateRange getCurrentMonthRange() {
        YearMonth currentMonth = YearMonth.now(ARGENTINA_ZONE);
        return getMonthRange(currentMonth.getYear(), currentMonth.getMonthValue());
    }

    private DateRange getPreviousMonthRange() {
        YearMonth previousMonth = YearMonth.now(ARGENTINA_ZONE).minusMonths(1);
        return getMonthRange(previousMonth.getYear(), previousMonth.getMonthValue());
    }

    private DateRange getMonthRange(int year, int month) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new InvalidExpenseException("El año o el mes no son válidos", exception);
        }

        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        return new DateRange(start, end);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calculatePercentageChange(
            BigDecimal current,
            BigDecimal previous
    ) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private record DateRange(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }
}
