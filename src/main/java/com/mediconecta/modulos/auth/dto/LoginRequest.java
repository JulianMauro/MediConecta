package com.mediconecta.modulos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record LoginRequest(

		@NotBlank(message = "el email es obligatorio")
		@Email(message = "el email no tiene un formato valido")
		String email,

		@NotBlank(message = "la password es obligatoria")
		String password
) {
}
