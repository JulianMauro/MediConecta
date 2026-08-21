package com.mediconecta.modulos.saludo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediconecta.modulos.saludo.dto.SaludoRequest;
import com.mediconecta.modulos.saludo.dto.SaludoResponse;
import com.mediconecta.modulos.saludo.entity.Saludo;
import com.mediconecta.modulos.saludo.repository.SaludoRepository;


@Service
public class SaludoService {

	private final SaludoRepository saludoRepository;

	/** Inyeccion por constructor: Spring pasa el repository solo. */
	public SaludoService(SaludoRepository saludoRepository) {
		this.saludoRepository = saludoRepository;
	}

	@Transactional
	public SaludoResponse saludar(SaludoRequest request) {
		String nombre = request.nombre().trim();

		// DTO -> Entity, y se persiste en la tabla "saludo"
		Saludo saludo = saludoRepository.save(new Saludo(nombre));

		// Entity -> DTO de salida
		return new SaludoResponse(saludo.getId(), "hola " + saludo.getNombre());
	}
}
