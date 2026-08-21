package com.mediconecta.modulos.saludo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.saludo.dto.SaludoRequest;
import com.mediconecta.modulos.saludo.dto.SaludoResponse;
import com.mediconecta.modulos.saludo.service.SaludoService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/saludos")
public class SaludoController {

	private final SaludoService saludoService;

	public SaludoController(SaludoService saludoService) {
		this.saludoService = saludoService;
	}

	@PostMapping
	public ResponseEntity<SaludoResponse> saludar(@Valid @RequestBody SaludoRequest request) {
		SaludoResponse response = saludoService.saludar(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
