package com.mediconecta.modulos.usuario.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.usuario.dto.UsuarioRequest;
import com.mediconecta.modulos.usuario.dto.UsuarioResponse;
import com.mediconecta.modulos.usuario.dto.UsuarioUpdateRequest;
import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.seguridad.UserPrincipal;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UsuarioResponse crear(UsuarioRequest request) {
		Usuario usuario = crearInterno(request.nombre(), request.apellido(), request.email(), request.password(),
				request.dni(), request.telefono(), request.rol());
		return UsuarioResponse.desde(usuario);
	}

	/** Alta publica (sin login): siempre entra como CLIENTE, nunca puede autoasignarse ADMIN. */
	@Transactional
	public Usuario registrarCliente(String nombre, String apellido, String email, String password, String dni,
			String telefono) {
		return crearInterno(nombre, apellido, email, password, dni, telefono, Rol.CLIENTE);
	}

	private Usuario crearInterno(String nombre, String apellido, String emailCrudo, String password, String dniCrudo,
			String telefonoCrudo, Rol rol) {
		String email = emailCrudo.trim().toLowerCase();

		if (usuarioRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya existe un usuario con ese email");
		}

		String dni = normalizar(dniCrudo);
		if (dni != null && usuarioRepository.existsByDni(dni)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya existe un usuario con ese dni");
		}

		// La password se hashea con BCrypt: en la base nunca queda el texto plano.
		Usuario usuario = new Usuario(
				nombre.trim(),
				apellido.trim(),
				email,
				passwordEncoder.encode(password),
				dni,
				normalizar(telefonoCrudo),
				rol);

		return usuarioRepository.save(usuario);
	}

	@Transactional(readOnly = true)
	public List<UsuarioResponse> listar() {
		return usuarioRepository.findAll().stream()
				.map(UsuarioResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public UsuarioResponse obtenerPorId(Long id) {
		return UsuarioResponse.desde(buscar(id));
	}

	/**
	 * Version con control de acceso: solo el propio usuario o un ADMIN.
	 *
	 * Sin esto, cualquier CLIENTE autenticado podia iterar ids y leer el email, el
	 * DNI, el telefono y el rol de todos los usuarios del sistema.
	 *
	 * El permiso se evalua ANTES de buscar la fila a proposito: si se buscara
	 * primero, un id inexistente daria 404 y uno ajeno 403, y esa diferencia sola
	 * alcanza para enumerar que ids existen.
	 */
	@Transactional(readOnly = true)
	public UsuarioResponse obtenerPorId(UserPrincipal solicitante, Long id) {
		boolean esElMismo = id.equals(solicitante.getId());
		if (!esElMismo && solicitante.getRol() != Rol.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no podes ver los datos de otro usuario");
		}
		return UsuarioResponse.desde(buscar(id));
	}

	@Transactional
	public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
		Usuario usuario = buscar(id);

		String dni = normalizar(request.dni());
		// Si cambio el dni, hay que verificar que no lo tenga otro usuario.
		if (dni != null && !dni.equals(usuario.getDni()) && usuarioRepository.existsByDni(dni)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya existe un usuario con ese dni");
		}

		usuario.actualizarDatos(
				request.nombre().trim(),
				request.apellido().trim(),
				dni,
				normalizar(request.telefono()),
				request.rol());

		// Al estar dentro de una transaccion, JPA detecta el cambio y persiste solo.
		return UsuarioResponse.desde(usuario);
	}

	@Transactional
	public void desactivar(Long id) {
		buscar(id).desactivar();
	}

	private Usuario buscar(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuario no encontrado"));
	}

	/** Deja en null los strings opcionales que vienen vacios. */
	private String normalizar(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim();
	}
}
