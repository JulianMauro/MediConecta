package com.mediconecta.modulos.auth.dto;

import jakarta.validation.constraints.NotBlank;


public record RefreshRequest(

		@NotBlank(message = "el refresh token es obligatorio")
		String refreshToken
) {
}
