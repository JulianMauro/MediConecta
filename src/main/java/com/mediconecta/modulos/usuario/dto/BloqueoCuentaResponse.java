package com.mediconecta.modulos.usuario.dto;

import java.time.Instant;

import com.mediconecta.modulos.usuario.entity.BloqueoCuenta;

public record BloqueoCuentaResponse(
		Long id,
		Long usuarioId,
		Instant fechaBloqueo,
		String motivo,
		boolean activo,
		Instant fechaDesbloqueo
) {

	public static BloqueoCuentaResponse desde(BloqueoCuenta bloqueo) {
		return new BloqueoCuentaResponse(
				bloqueo.getId(),
				bloqueo.getUsuario().getId(),
				bloqueo.getFechaBloqueo(),
				bloqueo.getMotivo(),
				bloqueo.isActivo(),
				bloqueo.getFechaDesbloqueo());
	}
}
