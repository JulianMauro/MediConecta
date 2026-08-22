package com.mediconecta.modulos.logistica.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.logistica.dto.MovimientoBicisRequest;
import com.mediconecta.modulos.logistica.dto.MovimientoBicisResponse;
import com.mediconecta.modulos.logistica.service.MovimientoBicisService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


/** Solo administradores mueven bicis en lote entre estaciones/almacenes (simula el camion de redistribucion). */
@RestController
@RequestMapping("/api/movimientos-bicis")
@PreAuthorize("hasRole('ADMIN')")
public class MovimientoBicisController {

	private final MovimientoBicisService movimientoBicisService;

	public MovimientoBicisController(MovimientoBicisService movimientoBicisService) {
		this.movimientoBicisService = movimientoBicisService;
	}

	@PostMapping
	public ResponseEntity<MovimientoBicisResponse> registrar(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody MovimientoBicisRequest request) {
		MovimientoBicisResponse response = movimientoBicisService.registrar(principal.getId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
