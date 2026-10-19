package com.mediconecta.modulos.soporte.dto;

import java.time.Instant;
import java.util.List;

import com.mediconecta.modulos.pago.dto.PagoResponse;
import com.mediconecta.modulos.usuario.dto.BloqueoCuentaResponse;
import com.mediconecta.modulos.usuario.dto.UsuarioResponse;

/**
 * Todo lo que el admin ve del usuario de un ticket antes de tocar nada. Los campos null
 * significan "no tiene": sin viaje en curso, sin plan vigente, sin bloqueo.
 */
public record ResumenUsuarioResponse(
		Long ticketId,
		/** Viaje que el usuario asocio al ticket, si asocio uno. */
		Long viajeDelTicketId,
		UsuarioResponse usuario,
		ViajeSoporteResponse viajeEnCurso,
		List<ViajeSoporteResponse> ultimosViajes,
		PlanVigente plan,
		long strikesActivos,
		int strikesParaBloquear,
		List<StrikeSoporteResponse> strikes,
		List<PagoResponse> pagosPendientes,
		BloqueoCuentaResponse bloqueo,
		List<AccionSoporteResponse> acciones
) {

	public record PlanVigente(
			Long suscripcionId,
			String nombre,
			int minutosPorViaje,
			int viajesPorDia,
			Instant vence
	) {
	}
}
