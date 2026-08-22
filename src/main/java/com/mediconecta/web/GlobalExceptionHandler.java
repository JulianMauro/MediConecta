package com.mediconecta.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;


/**
 * Sin esto, el body de error que arma Spring por default no trae el motivo real
 * (queda algo como {"status":409,"error":"Conflict"} sin "message"), y el frontend
 * no tiene como mostrarle al usuario un error entendible.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> manejarResponseStatus(ResponseStatusException ex) {
		HttpStatusCode status = ex.getStatusCode();
		return ResponseEntity.status(status).body(Map.of(
				"status", status.value(),
				"message", ex.getReason() != null ? ex.getReason() : "error"));
	}

	/** Errores de @Valid en los DTO de entrada: se muestra el primero, alcanza para que el usuario entienda que corregir. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
		String mensaje = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(FieldError::getDefaultMessage)
				.orElse("datos invalidos");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
				"status", HttpStatus.BAD_REQUEST.value(),
				"message", mensaje));
	}
}
