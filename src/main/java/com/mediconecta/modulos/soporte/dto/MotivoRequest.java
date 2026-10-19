package com.mediconecta.modulos.soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de las acciones de soporte que no necesitan otro dato: el motivo es obligatorio en todas. */
public record MotivoRequest(
		@NotBlank(message = "el motivo es obligatorio")
		@Size(max = 300, message = "el motivo no puede superar los 300 caracteres")
		String motivo
) {
}
