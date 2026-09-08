package com.mediconecta.modulos.monitoreo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.monitoreo.dto.UsuariosActivosResponse;
import com.mediconecta.modulos.monitoreo.service.UsuariosActivosService;


@RestController
@RequestMapping("/api/monitoreo/usuarios-activos")
public class UsuariosActivosController {

	private final UsuariosActivosService usuariosActivosService;

	public UsuariosActivosController(UsuariosActivosService usuariosActivosService) {
		this.usuariosActivosService = usuariosActivosService;
	}

	/** Cuantos usuarios estan en viaje ahora mismo. Cualquier usuario autenticado lo puede ver. */
	@GetMapping
	public ResponseEntity<UsuariosActivosResponse> obtenerActual() {
		return ResponseEntity.ok(usuariosActivosService.obtenerActual());
	}

	/** Historial de fotos horarias, para graficar la demanda a lo largo del tiempo. */
	@GetMapping("/historial")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UsuariosActivosResponse>> listarHistorial() {
		return ResponseEntity.ok(usuariosActivosService.listarHistorial());
	}
}
