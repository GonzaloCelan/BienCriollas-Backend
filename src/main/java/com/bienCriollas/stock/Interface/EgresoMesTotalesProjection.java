package com.bienCriollas.stock.Interface;

import java.math.BigDecimal;

public interface EgresoMesTotalesProjection {

	 	String getTipoEgreso();
	    BigDecimal getTotalMesActual();
	    BigDecimal getTotalMesAnterior();
}
