package com.mediconecta.modulos.flota.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.flota.dto.BicicletaEstadoRequest;
import com.mediconecta.modulos.flota.dto.BicicletaRequest;
import com.mediconecta.modulos.flota.dto.BicicletaResponse;
import com.mediconecta.modulos.flota.service.BicicletaService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/bicicletas")
public class BicicletaController {

	private final BicicletaService bicicletaService;

	public BicicletaController(BicicletaService bicicletaService) {
		this.bicicletaService = bicicletaService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BicicletaResponse> crear(@Valid @RequestBody BicicletaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(bicicletaService.crear(request));
	}

	@GetMapping
	public ResponseEntity<List<BicicletaResponse>> listar() {
		return ResponseEntity.ok(bicicletaService.listar());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BicicletaResponse> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(bicicletaService.obtenerPorId(id));
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BicicletaResponse> cambiarEstado(@PathVariable Long id,
			@Valid @RequestBody BicicletaEstadoRequest request) {
		return ResponseEntity.ok(bicicletaService.cambiarEstadoEnAlmacen(id, request.estado()));
	}
}
