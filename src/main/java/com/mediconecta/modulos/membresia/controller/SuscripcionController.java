package com.mediconecta.modulos.membresia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.membresia.dto.SuscripcionRequest;
import com.mediconecta.modulos.membresia.dto.SuscripcionResponse;
import com.mediconecta.modulos.membresia.service.SuscripcionUsuarioService;
import com.mediconecta.modulos.pago.dto.PagoResponse;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/suscripciones")
public class SuscripcionController {

	private final SuscripcionUsuarioService suscripcionUsuarioService;

	public SuscripcionController(SuscripcionUsuarioService suscripcionUsuarioService) {
		this.suscripcionUsuarioService = suscripcionUsuarioService;
	}

	/** Genera el pago de la suscripcion; se activa cuando ese pago se confirma. */
	@PostMapping
	public ResponseEntity<PagoResponse> contratar(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody SuscripcionRequest request) {
		PagoResponse pago = suscripcionUsuarioService.contratar(principal.getId(), request.membresiaId());
		return ResponseEntity.status(HttpStatus.CREATED).body(pago);
	}

	@GetMapping
	public ResponseEntity<List<SuscripcionResponse>> listarPropias(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(suscripcionUsuarioService.listarPropias(principal.getId()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancelar(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		suscripcionUsuarioService.cancelar(principal.getId(), id);
		return ResponseEntity.noContent().build();
	}
}
