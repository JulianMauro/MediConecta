package com.mediconecta.modulos.viaje.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.viaje.dto.ViajeFinalizarRequest;
import com.mediconecta.modulos.viaje.dto.ViajeIniciarRequest;
import com.mediconecta.modulos.viaje.dto.ViajeResponse;
import com.mediconecta.modulos.viaje.service.ViajeService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

	private final ViajeService viajeService;

	public ViajeController(ViajeService viajeService) {
		this.viajeService = viajeService;
	}

	@PostMapping("/iniciar")
	public ResponseEntity<ViajeResponse> iniciar(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ViajeIniciarRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(viajeService.iniciar(principal.getId(), request));
	}

	@PostMapping("/finalizar")
	public ResponseEntity<ViajeResponse> finalizar(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ViajeFinalizarRequest request) {
		return ResponseEntity.ok(viajeService.finalizar(principal.getId(), request));
	}

	@GetMapping("/actual")
	public ResponseEntity<ViajeResponse> obtenerActual(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(viajeService.obtenerActual(principal.getId()));
	}

	@GetMapping
	public ResponseEntity<List<ViajeResponse>> listarPropios(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(viajeService.listarPropios(principal.getId()));
	}
}
