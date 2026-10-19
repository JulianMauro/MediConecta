package com.mediconecta.modulos.soporte.dto;

import java.time.Instant;

import com.mediconecta.modulos.flota.entity.EstadoBicicleta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Donde queda la bici: un anclaje libre (anclajeDestinoId) o, si no volvio a ninguno, un
 * almacen (almacenId + estadoBicicleta EN_REPARACION o DESACTIVADA). Uno de los dos, no ambos.
 */
public record CerrarViajeRequest(
		Long anclajeDestinoId,
		Long almacenId,
		EstadoBicicleta estadoBicicleta,
		/** Hora real de llegada. Null = ahora. */
		Instant fechaFin,
		/** true = si se paso del tiempo, no se genera strike ni cobro. */
		boolean condonarExceso,
		@NotBlank(message = "el motivo es obligatorio")
		@Size(max = 300, message = "el motivo no puede superar los 300 caracteres")
		String motivo
) {
}
