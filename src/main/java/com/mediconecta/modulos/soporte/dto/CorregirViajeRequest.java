package com.mediconecta.modulos.soporte.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorregirViajeRequest(
		/** Nueva hora de fin: entre el inicio y la hora de fin actual (solo se acorta). */
		@NotNull(message = "la hora de fin es obligatoria")
		Instant fechaFin,
		@NotBlank(message = "el motivo es obligatorio")
		@Size(max = 300, message = "el motivo no puede superar los 300 caracteres")
		String motivo
) {
}
