package com.mediconecta.modulos.chatP2P.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediconecta.modulos.chatP2P.dto.ChatMensajeRequest;
import com.mediconecta.modulos.chatP2P.dto.ChatMensajeResponse;
import com.mediconecta.modulos.chatP2P.service.ChatService;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.validation.Valid;


/** Sin @PreAuthorize: escriben tanto CLIENTE como ADMIN. Quien participa lo valida el service. */
@RestController
@RequestMapping("/api/chat/{ticketId}/mensajes")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping
	public ResponseEntity<ChatMensajeResponse> enviar(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId, @Valid @RequestBody ChatMensajeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(chatService.enviar(principal.getId(), ticketId, request));
	}

	@GetMapping("/nuevos")
	public ResponseEntity<List<ChatMensajeResponse>> recibirNuevos(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId) {
		return ResponseEntity.ok(chatService.recibirNuevos(principal.getId(), ticketId));
	}

	@GetMapping
	public ResponseEntity<List<ChatMensajeResponse>> historial(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long ticketId) {
		return ResponseEntity.ok(chatService.historial(principal.getId(), ticketId));
	}
}
