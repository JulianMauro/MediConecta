package com.mediconecta.modulos.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/** Simula la respuesta de la pasarela de pago externa. */
public record PagoConfirmarRequest(

		@NotBlank(message = "la referencia de la pasarela es obligatoria")
		@Size(max = 100, message = "la referencia no puede superar los 100 caracteres")
		String referenciaPasarela
) {
}
