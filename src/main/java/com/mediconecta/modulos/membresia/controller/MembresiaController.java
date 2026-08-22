package com.mediconecta.modulos.membresia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.membresia.dto.MembresiaRequest;
import com.mediconecta.modulos.membresia.dto.MembresiaResponse;
import com.mediconecta.modulos.membresia.service.MembresiaService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/membresias")
public class MembresiaController {

	private final MembresiaService membresiaService;

	public MembresiaController(MembresiaService membresiaService) {
		this.membresiaService = membresiaService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MembresiaResponse> crear(@Valid @RequestBody MembresiaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(membresiaService.crear(request));
	}

	@GetMapping
	public ResponseEntity<List<MembresiaResponse>> listar() {
		return ResponseEntity.ok(membresiaService.listar());
	}

	@GetMapping("/{id}")
	public ResponseEntity<MembresiaResponse> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(membresiaService.obtenerPorId(id));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		membresiaService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
