package com.mediconecta.modulos.strike.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.strike.dto.StrikeResponse;
import com.mediconecta.modulos.strike.service.StrikeService;
import com.mediconecta.seguridad.UserPrincipal;


@RestController
@RequestMapping("/api/strikes")
public class StrikeController {

	private final StrikeService strikeService;

	public StrikeController(StrikeService strikeService) {
		this.strikeService = strikeService;
	}

	@GetMapping
	public ResponseEntity<List<StrikeResponse>> listarPropios(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(strikeService.listarPropios(principal.getId()));
	}

	@GetMapping("/usuario/{usuarioId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<StrikeResponse>> listarDeUsuario(@PathVariable Long usuarioId) {
		return ResponseEntity.ok(strikeService.listarDeUsuario(usuarioId));
	}
}
