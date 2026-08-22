package com.mediconecta.modulos.almacen.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.almacen.dto.AlmacenRequest;
import com.mediconecta.modulos.almacen.dto.AlmacenResponse;
import com.mediconecta.modulos.almacen.service.AlmacenService;

import jakarta.validation.Valid;


/** Gestion exclusiva de administradores: los almacenes no son visibles/operables por clientes. */
@RestController
@RequestMapping("/api/almacenes")
@PreAuthorize("hasRole('ADMIN')")
public class AlmacenController {

	private final AlmacenService almacenService;

	public AlmacenController(AlmacenService almacenService) {
		this.almacenService = almacenService;
	}

	@PostMapping
	public ResponseEntity<AlmacenResponse> crear(@Valid @RequestBody AlmacenRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(almacenService.crear(request));
	}

	@GetMapping
	public ResponseEntity<List<AlmacenResponse>> listar() {
		return ResponseEntity.ok(almacenService.listar());
	}

	@GetMapping("/{id}")
	public ResponseEntity<AlmacenResponse> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(almacenService.obtenerPorId(id));
	}
}
