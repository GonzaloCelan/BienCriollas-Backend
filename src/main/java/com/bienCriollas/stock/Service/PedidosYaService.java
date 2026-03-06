package com.bienCriollas.stock.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.PedidosYaRequestDTO;
import com.bienCriollas.stock.Interface.IPedidosYaService;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Repository.IngresoPedidosYaRepository;



@Service
public class PedidosYaService implements IPedidosYaService {
	
	
	@Autowired
	private IngresoPedidosYaRepository pedidosYaRepository;

	@Override
	@Transactional
	public IngresoPedidosYa registrarLiquidacionPedidosYa(PedidosYaRequestDTO request) {
		
		if (request.fecha() == null || request.monto() == null) {
			throw new IllegalArgumentException("La fecha y el monto son obligatorios");
		}
		
		IngresoPedidosYa ingresoLiquidacion = IngresoPedidosYa.builder()
				.fecha(request.fecha())
				.monto(request.monto())
				.build();
		
		return pedidosYaRepository.save(ingresoLiquidacion);
	}

	
	@Override
	@Transactional(readOnly = true)
	public List<IngresoPedidosYa> obtenerLiquidacionesPorMes(int año, int mes) {
		
		return pedidosYaRepository.findByMes(año, mes);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IngresoPedidosYa> obtenerTodasLasLiquidaciones() {
		
		return pedidosYaRepository.findAll();
	}

	

}
