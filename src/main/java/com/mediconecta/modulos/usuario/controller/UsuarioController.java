package com.mediconecta.modulos.usuario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.usuario.dto.UsuarioRequest;
import com.mediconecta.modulos.usuario.dto.UsuarioResponse;
import com.mediconecta.modulos.usuario.dto.UsuarioUpdateRequest;
import com.mediconecta.modulos.usuario.service.UsuarioService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	/** Alta de usuario. Solo un ADMIN puede crear usuarios. */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
		UsuarioResponse response = usuarioService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UsuarioResponse>> listar() {
		return ResponseEntity.ok(usuarioService.listar());
	}

	/** Sin @PreAuthorize: el permiso no es por rol sino por propiedad, y eso se
	    resuelve en el service (mismo patron que pagos y notificaciones). */
	@GetMapping("/{id}")
	public ResponseEntity<UsuarioResponse> obtener(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(usuarioService.obtenerPorId(principal, id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
			@Valid @RequestBody UsuarioUpdateRequest request) {
		return ResponseEntity.ok(usuarioService.actualizar(id, request));
	}

	/** Baja logica: el usuario queda inactivo pero no se borra de la base. */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		usuarioService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
