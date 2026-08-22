package com.mediconecta.modulos.usuario.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.usuario.dto.BloqueoCuentaResponse;
import com.mediconecta.modulos.usuario.entity.BloqueoCuenta;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.BloqueoCuentaRepository;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


@Service
public class BloqueoCuentaService {

	private final BloqueoCuentaRepository bloqueoCuentaRepository;
	private final UsuarioRepository usuarioRepository;

	public BloqueoCuentaService(BloqueoCuentaRepository bloqueoCuentaRepository, UsuarioRepository usuarioRepository) {
		this.bloqueoCuentaRepository = bloqueoCuentaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public boolean estaBloqueado(Long usuarioId) {
		return bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId).isPresent();
	}

	@Transactional(readOnly = true)
	public BloqueoCuentaResponse obtenerActivo(Long usuarioId) {
		BloqueoCuenta bloqueo = bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "el usuario no tiene un bloqueo activo"));
		return BloqueoCuentaResponse.desde(bloqueo);
	}

	/** Levanta el bloqueo activo del usuario. Solo lo puede hacer un admin. */
	@Transactional
	public BloqueoCuentaResponse desbloquear(Long usuarioId, Long adminId) {
		BloqueoCuenta bloqueo = bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "el usuario no tiene un bloqueo activo"));

		Usuario admin = usuarioRepository.findById(adminId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));

		bloqueo.desbloquear(admin);
		return BloqueoCuentaResponse.desde(bloqueo);
	}
}
