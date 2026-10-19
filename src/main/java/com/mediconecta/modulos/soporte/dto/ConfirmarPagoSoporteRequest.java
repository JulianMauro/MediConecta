package com.mediconecta.modulos.soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarPagoSoporteRequest(
		/** Id de la operacion en la pasarela, el que el usuario muestra como comprobante. */
		@NotBlank(message = "la referencia del pago es obligatoria")
		@Size(max = 100, message = "la referencia no puede superar los 100 caracteres")
		String referenciaPasarela,
		@NotBlank(message = "el motivo es obligatorio")
		@Size(max = 300, message = "el motivo no puede superar los 300 caracteres")
		String motivo
) {
}
