package com.mediconecta.modulos.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.auth.dto.AuthResponse;
import com.mediconecta.modulos.auth.dto.LoginRequest;
import com.mediconecta.modulos.auth.dto.RefreshRequest;
import com.mediconecta.modulos.usuario.dto.UsuarioResponse;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.seguridad.jwt.JwtTokenProvider;


@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UsuarioRepository usuarioRepository;
	private final JwtTokenProvider tokenProvider;

	public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
			JwtTokenProvider tokenProvider) {
		this.authenticationManager = authenticationManager;
		this.tokenProvider = tokenProvider;
		this.usuarioRepository = usuarioRepository;
	}

	/**
	 * Valida email + password y, si estan bien, devuelve los tokens.
	 * Este es el unico momento en que se consulta la base para autenticar:
	 * despues alcanza con el token.
	 */
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();

		try {
			// Internamente compara la password con el hash BCrypt guardado.
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.password()));
		} catch (AuthenticationException ex) {
			// Mensaje generico a proposito: no se revela si fallo el email o la password.
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "credenciales invalidas");
		}

		Usuario usuario = usuarioRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "credenciales invalidas"));

		return construirRespuesta(usuario);
	}

	/** Entrega un access token nuevo a cambio de un refresh token valido. */
	@Transactional(readOnly = true)
	public AuthResponse refresh(RefreshRequest request) {
		String refreshToken = request.refreshToken();

		if (!tokenProvider.validarToken(refreshToken) || !tokenProvider.esRefreshToken(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token invalido");
		}

		Usuario usuario = usuarioRepository.findById(tokenProvider.getUsuarioIdDelToken(refreshToken))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token invalido"));

		if (!usuario.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "el usuario esta inactivo");
		}

		return construirRespuesta(usuario);
	}

	private AuthResponse construirRespuesta(Usuario usuario) {
		return AuthResponse.de(
				tokenProvider.generarAccessToken(usuario),
				tokenProvider.generarRefreshToken(usuario),
				tokenProvider.getAccessTokenSegundos(),
				UsuarioResponse.desde(usuario));
	}
}
