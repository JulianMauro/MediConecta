package com.mediconecta.modulos.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.usuario.entity.Usuario;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/** Lo usa el login para buscar al usuario por su email. */
	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByDni(String dni);
}
