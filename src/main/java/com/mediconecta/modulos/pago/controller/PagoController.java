package com.mediconecta.modulos.pago.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.pago.dto.PagoConfirmarRequest;
import com.mediconecta.modulos.pago.dto.PagoResponse;
import com.mediconecta.modulos.pago.service.PagoService;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/pagos")
public class PagoController {

	private final PagoService pagoService;
	private final UsuarioRepository usuarioRepository;

	public PagoController(PagoService pagoService, UsuarioRepository usuarioRepository) {
		this.pagoService = pagoService;
		this.usuarioRepository = usuarioRepository;
	}

	@GetMapping
	public ResponseEntity<List<PagoResponse>> listarPropios(@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(pagoService.listarPropios(principal.getId()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PagoResponse> obtener(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(pagoService.obtener(usuarioActual(principal), id));
	}

	/** Confirma el pago. En un sistema real lo dispararia el webhook de la pasarela, no el usuario. */
	@PostMapping("/{id}/confirmar")
	public ResponseEntity<PagoResponse> confirmar(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id, @Valid @RequestBody PagoConfirmarRequest request) {
		return ResponseEntity.ok(pagoService.confirmar(usuarioActual(principal), id, request.referenciaPasarela()));
	}

	@PostMapping("/{id}/rechazar")
	public ResponseEntity<PagoResponse> rechazar(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		return ResponseEntity.ok(pagoService.rechazar(usuarioActual(principal), id));
	}

	private Usuario usuarioActual(UserPrincipal principal) {
		return usuarioRepository.findById(principal.getId())
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));
	}
}
