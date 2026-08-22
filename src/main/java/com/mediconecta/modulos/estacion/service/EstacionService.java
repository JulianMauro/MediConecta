package com.mediconecta.modulos.estacion.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.estacion.dto.EstacionRequest;
import com.mediconecta.modulos.estacion.dto.EstacionResponse;
import com.mediconecta.modulos.estacion.entity.Estacion;
import com.mediconecta.modulos.estacion.repository.EstacionRepository;


@Service
public class EstacionService {

	private final EstacionRepository estacionRepository;

	public EstacionService(EstacionRepository estacionRepository) {
		this.estacionRepository = estacionRepository;
	}

	@Transactional
	public EstacionResponse crear(EstacionRequest request) {
		Estacion estacion = new Estacion(request.nombre(), request.direccion(), request.latitud(),
				request.longitud(), request.capacidad());
		return EstacionResponse.desde(estacionRepository.save(estacion));
	}

	@Transactional(readOnly = true)
	public List<EstacionResponse> listar() {
		return estacionRepository.findAll().stream()
				.map(EstacionResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public EstacionResponse obtenerPorId(Long id) {
		return EstacionResponse.desde(buscar(id));
	}

	@Transactional
	public void desactivar(Long id) {
		buscar(id).desactivar();
	}

	Estacion buscar(Long id) {
		return estacionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "estacion no encontrada"));
	}
}
