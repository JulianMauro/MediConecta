package com.mediconecta.modulos.estacion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.estacion.dto.AnclajeResponse;
import com.mediconecta.modulos.estacion.service.AnclajeService;


@RestController
@RequestMapping("/api/anclajes")
public class AnclajeController {

	private final AnclajeService anclajeService;

	public AnclajeController(AnclajeService anclajeService) {
		this.anclajeService = anclajeService;
	}

	@PatchMapping("/{id}/fuera-servicio")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AnclajeResponse> marcarFueraServicio(@PathVariable Long id) {
		return ResponseEntity.ok(anclajeService.marcarFueraServicio(id));
	}

	@PatchMapping("/{id}/habilitar")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AnclajeResponse> habilitar(@PathVariable Long id) {
		return ResponseEntity.ok(anclajeService.habilitar(id));
	}
}
