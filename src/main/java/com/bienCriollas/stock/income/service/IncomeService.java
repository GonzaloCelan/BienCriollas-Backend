package com.bienCriollas.stock.income.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.income.dto.IncomeSummaryDTO;
import com.bienCriollas.stock.income.dto.PedidosYaSettlementRequestDTO;
import com.bienCriollas.stock.income.exception.InvalidIncomeException;
import com.bienCriollas.stock.income.repository.IncomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;

    @Transactional(readOnly = true)
    public IncomeSummaryDTO getSummary(LocalDate start, LocalDate end) {
        validateRange(start, end);

        BigDecimal cash = normalize(
                incomeRepository.sumDirectCash(start, end)
        );

        BigDecimal transfer = normalize(
                incomeRepository.sumDirectTransfers(start, end)
        );

        BigDecimal estimatedPedidosYa = normalize(
                incomeRepository.sumEstimatedPedidosYa(start, end)
        );

        BigDecimal pedidosYaSettlements = normalize(
                incomeRepository.sumPedidosYaSettlements(start, end)
        );

        /*
         * Plata realmente recibida:
         * - efectivo de pedidos particulares
         * - transferencia de pedidos particulares
         * - liquidaciones reales recibidas de Pedidos Ya
         */
        BigDecimal totalActuallyReceived = cash
                .add(transfer)
                .add(pedidosYaSettlements);

        /*
         * Acumulado del período:
         * representa plata real ya cobrada.
         * NO incluye pedidosYaEstimado porque todavía está pendiente.
         */
        BigDecimal accumulatedPeriod = totalActuallyReceived;

        Integer directOrderCount =
                incomeRepository.countDirectOrders(start, end);

        Integer pedidosYaOrderCount =
                incomeRepository.countPedidosYaOrders(start, end);

        List<IncomeSummaryDTO.IncomeByDayDTO> incomeByDay =
                incomeRepository.getIncomeByDay(start, end);

        /*
         * Distribución del acumulado real:
         * efectivo + transferencia + liquidaciones Pedidos Ya.
         */
        List<IncomeSummaryDTO.IncomeDistributionDTO> distribution =
                buildDistribution(
                        cash,
                        transfer,
                        pedidosYaSettlements
                );

        List<IncomeSummaryDTO.IncomeMovementDTO> movements =
                incomeRepository.getMovements(start, end);

        return new IncomeSummaryDTO(
                cash,
                transfer,
                estimatedPedidosYa,
                pedidosYaSettlements,
                totalActuallyReceived,
                accumulatedPeriod,
                directOrderCount,
                pedidosYaOrderCount,
                incomeByDay,
                distribution,
                movements
        );
    }

    @Transactional
    public void registerPedidosYaSettlement(PedidosYaSettlementRequestDTO request) {
        validateSettlement(request);
        incomeRepository.registerPedidosYaSettlement(request);
    }

    private List<IncomeSummaryDTO.IncomeDistributionDTO> buildDistribution(
            BigDecimal cash,
            BigDecimal transfer,
            BigDecimal pedidosYaSettlements
    ) {
        BigDecimal total = cash
                .add(transfer)
                .add(pedidosYaSettlements);

        return List.of(
                new IncomeSummaryDTO.IncomeDistributionDTO(
                        "EFECTIVO",
                        cash,
                        calculatePercentage(cash, total)
                ),
                new IncomeSummaryDTO.IncomeDistributionDTO(
                        "TRANSFERENCIA",
                        transfer,
                        calculatePercentage(transfer, total)
                ),
                new IncomeSummaryDTO.IncomeDistributionDTO(
                        "LIQUIDACION_PEDIDOS_YA",
                        pedidosYaSettlements,
                        calculatePercentage(pedidosYaSettlements, total)
                )
        );
    }

    private BigDecimal calculatePercentage(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return value
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new InvalidIncomeException("Las fechas desde y hasta son obligatorias");
        }

        if (!start.isBefore(end)) {
            throw new InvalidIncomeException("La fecha desde debe ser menor que la fecha hasta");
        }
    }

    private void validateSettlement(PedidosYaSettlementRequestDTO request) {
        if (request == null) {
            throw new InvalidIncomeException("La liquidación no puede ser nula");
        }

        if (request.date() == null) {
            throw new InvalidIncomeException("La fecha es obligatoria");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidIncomeException("El monto debe ser mayor a cero");
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
