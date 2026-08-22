package com.mediconecta.modulos.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.auth.dto.AuthResponse;
import com.mediconecta.modulos.auth.dto.LoginRequest;
import com.mediconecta.modulos.auth.dto.RefreshRequest;
import com.mediconecta.modulos.auth.dto.RegisterRequest;
import com.mediconecta.modulos.auth.service.AuthService;
import com.mediconecta.modulos.usuario.dto.UsuarioResponse;
import com.mediconecta.modulos.usuario.service.UsuarioService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final UsuarioService usuarioService;

	public AuthController(AuthService authService, UsuarioService usuarioService) {
		this.authService = authService;
		this.usuarioService = usuarioService;
	}

	/** Endpoint publico: recibe email + password y devuelve los tokens. */
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/** Endpoint publico: crea el usuario como CLIENTE y devuelve los tokens ya logueado. */
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		usuarioService.registrarCliente(request.nombre(), request.apellido(), request.email(), request.password(),
				request.dni(), request.telefono());
		AuthResponse tokens = authService.login(new LoginRequest(request.email(), request.password()));
		return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
	}

	/** Endpoint publico: renueva el access token vencido usando el refresh token. */
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	/** Devuelve los datos del usuario dueño del token que vino en el request. */
	@GetMapping("/me")
	public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(usuarioService.obtenerPorId(principal.getId()));
	}
}
