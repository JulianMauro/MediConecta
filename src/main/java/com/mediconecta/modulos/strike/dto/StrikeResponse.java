package com.mediconecta.modulos.strike.dto;

import java.time.Instant;

import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;

public record StrikeResponse(
		Long id,
		Long usuarioId,
		Long viajeId,
		Instant fechaGeneracion,
		EstadoStrike estado,
		Long pagoId
) {

	public static StrikeResponse desde(Strike strike) {
		return new StrikeResponse(
				strike.getId(),
				strike.getUsuario().getId(),
				strike.getViaje().getId(),
				strike.getFechaGeneracion(),
				strike.getEstado(),
				strike.getPago() != null ? strike.getPago().getId() : null);
	}
}
