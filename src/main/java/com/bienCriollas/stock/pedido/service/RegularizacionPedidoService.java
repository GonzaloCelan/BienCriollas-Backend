package com.bienCriollas.stock.pedido.service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoConsultaDTO;
import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoItemDTO;
import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoResultadoDTO;
import com.bienCriollas.stock.pedido.dto.RegularizarPedidosRequestDTO;
import com.bienCriollas.stock.pedido.entity.Pedido;
import com.bienCriollas.stock.pedido.entity.RegularizacionPedido;
import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.exception.PedidoNoEncontradoException;
import com.bienCriollas.stock.pedido.repository.PedidoRepository;
import com.bienCriollas.stock.pedido.repository.RegularizacionPedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegularizacionPedidoService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final List<TipoEstado> ESTADOS_REGULARIZABLES = List.of(
            TipoEstado.PENDIENTE,
            TipoEstado.PREPARADO);
    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private final PedidoRepository pedidoRepository;
    private final RegularizacionPedidoRepository regularizacionPedidoRepository;

    @Transactional(readOnly = true)
    public RegularizacionPedidoConsultaDTO consultarMes(
            Integer anio,
            Integer mes,
            int pagina,
            int tamano) {
        YearMonth periodo = validarPeriodo(anio, mes);
        validarPaginacion(pagina, tamano);

        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.plusMonths(1).atDay(1);
        Pageable pageable = PageRequest.of(
                pagina,
                tamano,
                Sort.by(
                        Sort.Order.asc("fechaCreacion"),
                        Sort.Order.asc("idPedido")));

        Page<Pedido> resultado = pedidoRepository
                .findByEstadoInAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(
                        ESTADOS_REGULARIZABLES,
                        desde,
                        hasta,
                        pageable);
        BigDecimal ingresoPotencial = pedidoRepository.sumarTotalPorEstadosYPeriodo(
                ESTADOS_REGULARIZABLES,
                desde,
                hasta);

        return new RegularizacionPedidoConsultaDTO(
                periodo.getYear(),
                periodo.getMonthValue(),
                resultado.getTotalElements(),
                ingresoPotencial == null ? BigDecimal.ZERO : ingresoPotencial,
                resultado.getContent().stream().map(this::toItemDto).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalPages());
    }

    @Transactional
    public RegularizacionPedidoResultadoDTO regularizar(
            RegularizarPedidosRequestDTO request,
            String realizadoPor) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de regularización es obligatoria");
        }
        validarSolicitud(request);
        if (!Boolean.TRUE.equals(request.confirmar())) {
            throw new IllegalArgumentException("Debés confirmar expresamente la regularización");
        }
        if (realizadoPor == null || realizadoPor.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al administrador");
        }

        YearMonth periodo = validarPeriodo(request.anio(), request.mes());
        LinkedHashSet<Long> ids = new LinkedHashSet<>(request.idsPedidos());
        List<Pedido> pedidos = pedidoRepository.findAllByIdParaRegularizar(ids);

        validarPedidosEncontrados(ids, pedidos);
        validarPedidosRegularizables(periodo, pedidos);

        String idLote = UUID.randomUUID().toString();
        OffsetDateTime realizadoEn = OffsetDateTime.now(ZONA_ARGENTINA);
        String motivo = request.motivo().trim();
        BigDecimal ingresoIncorporado = BigDecimal.ZERO;
        List<RegularizacionPedido> auditorias = new ArrayList<>(pedidos.size());

        for (Pedido pedido : pedidos) {
            TipoEstado estadoAnterior = pedido.getEstado();
            ingresoIncorporado = ingresoIncorporado.add(pedido.getTotalPedido());
            pedido.setEstado(TipoEstado.ENTREGADO);
            auditorias.add(RegularizacionPedido.builder()
                    .idLote(idLote)
                    .pedido(pedido)
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(TipoEstado.ENTREGADO)
                    .realizadoPor(realizadoPor)
                    .motivo(motivo)
                    .realizadoEn(realizadoEn)
                    .build());
        }

        pedidoRepository.saveAll(pedidos);
        regularizacionPedidoRepository.saveAll(auditorias);

        List<Long> idsActualizados = pedidos.stream()
                .map(Pedido::getIdPedido)
                .sorted()
                .toList();
        return new RegularizacionPedidoResultadoDTO(
                idLote,
                periodo.getYear(),
                periodo.getMonthValue(),
                pedidos.size(),
                ingresoIncorporado,
                idsActualizados,
                realizadoPor,
                realizadoEn);
    }

    private RegularizacionPedidoItemDTO toItemDto(Pedido pedido) {
        return new RegularizacionPedidoItemDTO(
                pedido.getIdPedido(),
                pedido.getFechaCreacion(),
                pedido.getCliente(),
                pedido.getTipoVenta(),
                pedido.getTipoPago(),
                pedido.getNumeroPedidoPedidosYa(),
                pedido.getHorarioEntrega(),
                pedido.getTotalPedido(),
                pedido.getEstado());
    }

    private YearMonth validarPeriodo(Integer anio, Integer mes) {
        if (anio == null || mes == null) {
            throw new IllegalArgumentException("El año y el mes son obligatorios");
        }
        if (anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException("El año debe estar entre 2000 y 2100");
        }
        try {
            return YearMonth.of(anio, mes);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("El año o el mes no son válidos", exception);
        }
    }

    private void validarSolicitud(RegularizarPedidosRequestDTO request) {
        if (request.idsPedidos() == null || request.idsPedidos().isEmpty()) {
            throw new IllegalArgumentException("Debés seleccionar al menos un pedido");
        }
        if (request.idsPedidos().size() > 200) {
            throw new IllegalArgumentException("No se pueden regularizar más de 200 pedidos por operación");
        }
        for (Long id : request.idsPedidos()) {
            if (id == null || id.longValue() <= 0) {
                throw new IllegalArgumentException("Todos los IDs de pedido deben ser válidos");
            }
        }
        if (request.motivo() == null || request.motivo().isBlank()) {
            throw new IllegalArgumentException("El motivo de la regularización es obligatorio");
        }
        if (request.motivo().trim().length() > 250) {
            throw new IllegalArgumentException("El motivo no puede superar los 250 caracteres");
        }
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");
        }
    }

    private void validarPedidosEncontrados(Set<Long> idsSolicitados, List<Pedido> pedidos) {
        Set<Long> idsEncontrados = new HashSet<>();
        for (Pedido pedido : pedidos) {
            idsEncontrados.add(pedido.getIdPedido());
        }
        for (Long id : idsSolicitados) {
            if (!idsEncontrados.contains(id)) {
                throw new PedidoNoEncontradoException(id);
            }
        }
    }

    private void validarPedidosRegularizables(YearMonth periodo, List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            if (pedido.getFechaCreacion() == null
                    || !YearMonth.from(pedido.getFechaCreacion()).equals(periodo)) {
                throw new IllegalStateException(
                        "El pedido " + pedido.getIdPedido() + " no pertenece al período seleccionado");
            }
            if (!ESTADOS_REGULARIZABLES.contains(pedido.getEstado())) {
                throw new IllegalStateException(
                        "El pedido " + pedido.getIdPedido()
                                + " ya no está PENDIENTE o PREPARADO");
            }
            if (pedido.getTotalPedido() == null || pedido.getTotalPedido().signum() < 0) {
                throw new IllegalStateException(
                        "El pedido " + pedido.getIdPedido() + " tiene un total inválido");
            }
        }
    }
}
