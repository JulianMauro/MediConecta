package com.mediconecta.modulos.soporte.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.soporte.dto.CerrarViajeRequest;
import com.mediconecta.modulos.soporte.dto.ConfirmarPagoSoporteRequest;
import com.mediconecta.modulos.soporte.dto.CorregirViajeRequest;
import com.mediconecta.modulos.soporte.dto.MotivoRequest;
import com.mediconecta.modulos.soporte.dto.ResultadoSoporteResponse;
import com.mediconecta.modulos.soporte.dto.ResumenUsuarioResponse;
import com.mediconecta.modulos.soporte.service.SoporteService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;

/**
 * Todo cuelga del ticket: el usuario sobre el que se opera es siempre el dueño del ticket,
 * y que el admin sea el que lo tomo lo valida el service (el rol solo no alcanza).
 */
@RestController
@RequestMapping("/api/soporte/tickets/{ticketId}")
@PreAuthorize("hasRole('ADMIN')")
public class SoporteController {

	private final SoporteService soporteService;

	public SoporteController(SoporteService soporteService) {
		this.soporteService = soporteService;
	}

	@GetMapping("/usuario")
	public ResponseEntity<ResumenUsuarioResponse> resumen(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId) {
		return ResponseEntity.ok(soporteService.resumen(principal.getId(), ticketId));
	}

	@PostMapping("/cerrar-viaje")
	public ResponseEntity<ResultadoSoporteResponse> cerrarViaje(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @Valid @RequestBody CerrarViajeRequest request) {
		return ResponseEntity.ok(soporteService.cerrarViaje(principal.getId(), ticketId, request));
	}

	@PostMapping("/viajes/{viajeId}/corregir")
	public ResponseEntity<ResultadoSoporteResponse> corregirViaje(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @PathVariable Long viajeId, @Valid @RequestBody CorregirViajeRequest request) {
		return ResponseEntity.ok(soporteService.corregirViaje(principal.getId(), ticketId, viajeId, request));
	}

	@PostMapping("/strikes/{strikeId}/anular")
	public ResponseEntity<ResultadoSoporteResponse> anularStrike(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @PathVariable Long strikeId, @Valid @RequestBody MotivoRequest request) {
		return ResponseEntity.ok(soporteService.anularStrike(principal.getId(), ticketId, strikeId, request.motivo()));
	}

	@PostMapping("/pagos/{pagoId}/confirmar")
	public ResponseEntity<ResultadoSoporteResponse> confirmarPago(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @PathVariable Long pagoId, @Valid @RequestBody ConfirmarPagoSoporteRequest request) {
		return ResponseEntity.ok(soporteService.confirmarPago(principal.getId(), ticketId, pagoId, request));
	}

	@PostMapping("/desbloquear")
	public ResponseEntity<ResultadoSoporteResponse> desbloquear(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @Valid @RequestBody MotivoRequest request) {
		return ResponseEntity.ok(soporteService.desbloquear(principal.getId(), ticketId, request.motivo()));
	}

	@PostMapping("/anclajes/{anclajeId}/fuera-servicio")
	public ResponseEntity<ResultadoSoporteResponse> anclajeFueraDeServicio(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @PathVariable Long anclajeId, @Valid @RequestBody MotivoRequest request) {
		return ResponseEntity.ok(soporteService.anclajeFueraDeServicio(principal.getId(), ticketId, anclajeId, request.motivo()));
	}
}
