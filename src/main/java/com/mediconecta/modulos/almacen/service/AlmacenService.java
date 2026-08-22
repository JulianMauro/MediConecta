package com.mediconecta.modulos.almacen.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.dto.AlmacenRequest;
import com.mediconecta.modulos.almacen.dto.AlmacenResponse;
import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.almacen.repository.AlmacenRepository;


@Service
public class AlmacenService {

	private final AlmacenRepository almacenRepository;

	public AlmacenService(AlmacenRepository almacenRepository) {
		this.almacenRepository = almacenRepository;
	}

	@Transactional
	public AlmacenResponse crear(AlmacenRequest request) {
		Almacen almacen = new Almacen(request.nombre(), request.direccion(), request.capacidad());
		return AlmacenResponse.desde(almacenRepository.save(almacen));
	}

	@Transactional(readOnly = true)
	public List<AlmacenResponse> listar() {
		return almacenRepository.findAll().stream()
				.map(AlmacenResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public AlmacenResponse obtenerPorId(Long id) {
		return AlmacenResponse.desde(buscar(id));
	}

	Almacen buscar(Long id) {
		return almacenRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "almacen no encontrado"));
	}
}
