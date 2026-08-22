package com.mediconecta.modulos.logistica.dto;

import java.time.Instant;
import java.util.List;

import com.mediconecta.modulos.logistica.entity.MovimientoBicis;

public record MovimientoBicisResponse(
		Long id,
		Long adminId,
		Instant fecha,
		Long origenEstacionId,
		Long origenAlmacenId,
		Long destinoEstacionId,
		Long destinoAlmacenId,
		List<Long> bicicletaIds
) {

	public static MovimientoBicisResponse desde(MovimientoBicis movimiento, List<Long> bicicletaIds) {
		return new MovimientoBicisResponse(
				movimiento.getId(),
				movimiento.getAdmin().getId(),
				movimiento.getFecha(),
				movimiento.getOrigenEstacion() != null ? movimiento.getOrigenEstacion().getId() : null,
				movimiento.getOrigenAlmacen() != null ? movimiento.getOrigenAlmacen().getId() : null,
				movimiento.getDestinoEstacion() != null ? movimiento.getDestinoEstacion().getId() : null,
				movimiento.getDestinoAlmacen() != null ? movimiento.getDestinoAlmacen().getId() : null,
				bicicletaIds);
	}
}
