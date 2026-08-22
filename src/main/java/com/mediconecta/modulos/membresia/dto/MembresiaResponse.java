package com.mediconecta.modulos.membresia.dto;

import java.math.BigDecimal;

import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.entity.TipoMembresia;

public record MembresiaResponse(
		Long id,
		String nombre,
		TipoMembresia tipo,
		int duracionDias,
		int tiempoPermitidoMinutos,
		BigDecimal precio,
		BigDecimal tarifaMinutoExtra,
		int viajesPorDia,
		int tiempoEsperaMinutos,
		boolean activa
) {

	public static MembresiaResponse desde(Membresia membresia) {
		return new MembresiaResponse(
				membresia.getId(),
				membresia.getNombre(),
				membresia.getTipo(),
				membresia.getTipo().duracionDias(),
				membresia.getTiempoPermitidoMinutos(),
				membresia.getPrecio(),
				membresia.getTarifaMinutoExtra(),
				membresia.getViajesPorDia(),
				membresia.getTiempoEsperaMinutos(),
				membresia.isActiva());
	}
}
