package com.mediconecta.modulos.notificacion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.notificacion.dto.NotificacionResponse;
import com.mediconecta.modulos.notificacion.service.NotificacionService;
import com.mediconecta.seguridad.UserPrincipal;


@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

	private final NotificacionService notificacionService;

	public NotificacionController(NotificacionService notificacionService) {
		this.notificacionService = notificacionService;
	}

	@GetMapping
	public ResponseEntity<List<NotificacionResponse>> listarPropias(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(notificacionService.listarPropias(principal.getId()));
	}

	@GetMapping("/no-leidas")
	public ResponseEntity<List<NotificacionResponse>> listarNoLeidas(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(notificacionService.listarNoLeidas(principal.getId()));
	}

	@PatchMapping("/{id}/leida")
	public ResponseEntity<NotificacionResponse> marcarLeida(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(notificacionService.marcarLeida(principal.getId(), id));
	}
}
