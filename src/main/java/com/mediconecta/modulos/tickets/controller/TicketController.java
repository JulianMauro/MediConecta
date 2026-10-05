package com.mediconecta.modulos.tickets.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.tickets.dto.TicketRequest;
import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.dto.TicketResponse;
import com.mediconecta.modulos.tickets.service.TicketService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService ticketService;

	public TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@PostMapping
	public ResponseEntity<TicketResponse> crear(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody TicketRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.crear(principal.getId(), request));
	}

	@GetMapping
	public ResponseEntity<List<TicketResponse>> listarPropios(@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) CategoriaTicket categoria) {
		return ResponseEntity.ok(ticketService.listarPropios(principal.getId(), categoria));
	}

	@GetMapping("/abiertos")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<TicketResponse>> listarAbiertos(@RequestParam(required = false) CategoriaTicket categoria) {
		return ResponseEntity.ok(ticketService.listarAbiertos(categoria));
	}

	@GetMapping("/asignados")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<TicketResponse>> listarAsignados(@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) CategoriaTicket categoria) {
		return ResponseEntity.ok(ticketService.listarAsignados(principal.getId(), categoria));
	}

	@PatchMapping("/{id}/tomar")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<TicketResponse> tomar(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(ticketService.tomar(principal.getId(), id));
	}

	@PatchMapping("/{id}/resolver")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<TicketResponse> resolver(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(ticketService.resolver(principal.getId(), id));
	}
}
