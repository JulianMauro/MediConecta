package com.mediconecta.modulos.soporte.dto;

import java.time.Instant;

import com.mediconecta.modulos.soporte.entity.AccionSoporte;
import com.mediconecta.modulos.soporte.entity.TipoAccionSoporte;

public record AccionSoporteResponse(
		Long id,
		Long ticketId,
		Long adminId,
		Long usuarioId,
		TipoAccionSoporte tipo,
		String detalle,
		String motivo,
		Instant fecha
) {

	public static AccionSoporteResponse desde(AccionSoporte accion) {
		return new AccionSoporteResponse(
				accion.getId(),
				accion.getTicket().getId(),
				accion.getAdmin().getId(),
				accion.getUsuario().getId(),
				accion.getTipo(),
				accion.getDetalle(),
				accion.getMotivo(),
				accion.getFecha());
	}
}
