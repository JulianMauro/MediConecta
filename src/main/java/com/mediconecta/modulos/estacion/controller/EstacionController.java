package com.mediconecta.modulos.estacion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.estacion.dto.AnclajeRequest;
import com.mediconecta.modulos.estacion.dto.AnclajeResponse;
import com.mediconecta.modulos.estacion.dto.EstacionRequest;
import com.mediconecta.modulos.estacion.dto.EstacionUpdateRequest;
import com.mediconecta.modulos.estacion.dto.EstacionResponse;
import com.mediconecta.modulos.estacion.service.AnclajeService;
import com.mediconecta.modulos.estacion.service.EstacionService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/estaciones")
public class EstacionController {

	private final EstacionService estacionService;
	private final AnclajeService anclajeService;

	public EstacionController(EstacionService estacionService, AnclajeService anclajeService) {
		this.estacionService = estacionService;
		this.anclajeService = anclajeService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EstacionResponse> crear(@Valid @RequestBody EstacionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(estacionService.crear(request));
	}

	@GetMapping
	public ResponseEntity<List<EstacionResponse>> listar() {
		return ResponseEntity.ok(estacionService.listar());
	}

	@GetMapping("/{id}")
	public ResponseEntity<EstacionResponse> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(estacionService.obtenerPorId(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EstacionResponse> actualizar(@PathVariable Long id,
			@Valid @RequestBody EstacionUpdateRequest request) {
		return ResponseEntity.ok(estacionService.actualizar(id, request));
	}

	/** Completa las coordenadas de una estacion existente a partir de su direccion. */
	@PostMapping("/{id}/geocodificar")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EstacionResponse> geocodificar(@PathVariable Long id) {
		return ResponseEntity.ok(estacionService.geocodificar(id));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		estacionService.desactivar(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/anclajes")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AnclajeResponse> agregarAnclaje(@PathVariable Long id,
			@Valid @RequestBody AnclajeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(anclajeService.agregar(id, request));
	}

	@GetMapping("/{id}/anclajes")
	public ResponseEntity<List<AnclajeResponse>> listarAnclajes(@PathVariable Long id) {
		return ResponseEntity.ok(anclajeService.listarPorEstacion(id));
	}
}
