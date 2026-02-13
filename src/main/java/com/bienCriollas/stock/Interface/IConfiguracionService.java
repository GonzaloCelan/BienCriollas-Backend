package com.bienCriollas.stock.Interface;

import java.util.List;

import com.bienCriollas.stock.Dto.PrecioCostoVariedadDTO;
import com.bienCriollas.stock.Dto.VariedadEmpanadaDTO;
import com.bienCriollas.stock.Dto.VariedadRequestDTO;
import com.bienCriollas.stock.Model.VariedadEmpanada;

public interface IConfiguracionService {

	
	public List<VariedadEmpanadaDTO> actualizarPrecioCostoVariedad(List<PrecioCostoVariedadDTO> requestList);
	
	public VariedadEmpanada añadirVariedadNueva(VariedadRequestDTO variedad);
	
	public VariedadEmpanada setActivoVariedad(Long idVariedad, boolean activo);
	
	public List<VariedadEmpanada> obtenerVariedadesActivas();
}
