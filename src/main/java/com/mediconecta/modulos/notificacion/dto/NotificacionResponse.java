package com.mediconecta.modulos.notificacion.dto;

import java.time.Instant;

import com.mediconecta.modulos.notificacion.entity.Notificacion;
import com.mediconecta.modulos.notificacion.entity.TipoNotificacion;

public record NotificacionResponse(
		Long id,
		Long usuarioId,
		Long viajeId,
		TipoNotificacion tipo,
		String mensaje,
		Instant fechaEnvio,
		boolean leida
) {

	public static NotificacionResponse desde(Notificacion notificacion) {
		return new NotificacionResponse(
				notificacion.getId(),
				notificacion.getUsuario().getId(),
				notificacion.getViaje() != null ? notificacion.getViaje().getId() : null,
				notificacion.getTipo(),
				notificacion.getMensaje(),
				notificacion.getFechaEnvio(),
				notificacion.isLeida());
	}
}
