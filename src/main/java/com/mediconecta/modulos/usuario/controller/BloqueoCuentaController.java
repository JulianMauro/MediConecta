package com.mediconecta.modulos.usuario.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.usuario.dto.BloqueoCuentaResponse;
import com.mediconecta.modulos.usuario.service.BloqueoCuentaService;
import com.mediconecta.seguridad.UserPrincipal;


@RestController
@RequestMapping("/api/usuarios/{usuarioId}/bloqueo")
@PreAuthorize("hasRole('ADMIN')")
public class BloqueoCuentaController {

	private final BloqueoCuentaService bloqueoCuentaService;

	public BloqueoCuentaController(BloqueoCuentaService bloqueoCuentaService) {
		this.bloqueoCuentaService = bloqueoCuentaService;
	}

	@GetMapping
	public ResponseEntity<BloqueoCuentaResponse> obtenerActivo(@PathVariable Long usuarioId) {
		return ResponseEntity.ok(bloqueoCuentaService.obtenerActivo(usuarioId));
	}

	@PostMapping("/desbloquear")
	public ResponseEntity<BloqueoCuentaResponse> desbloquear(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long usuarioId) {
		return ResponseEntity.ok(bloqueoCuentaService.desbloquear(usuarioId, principal.getId()));
	}
}
