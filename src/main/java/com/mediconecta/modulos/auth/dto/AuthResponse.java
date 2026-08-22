package com.mediconecta.modulos.auth.dto;

import com.mediconecta.modulos.usuario.dto.UsuarioResponse;


/** Lo que devuelve el login: los tokens mas los datos basicos del usuario. */
public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiraEnSegundos,
		UsuarioResponse usuario
) {

	public static AuthResponse de(String accessToken, String refreshToken, long expiraEnSegundos,
			UsuarioResponse usuario) {
		return new AuthResponse(accessToken, refreshToken, "Bearer", expiraEnSegundos, usuario);
	}
}
