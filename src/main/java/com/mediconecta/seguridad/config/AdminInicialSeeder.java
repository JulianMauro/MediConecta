package com.mediconecta.seguridad.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


/**
 * Crea el primer ADMIN al arrancar si no existe.
 *
 * Hace falta porque el alta de usuarios exige rol ADMIN: sin este usuario inicial
 * no habria forma de crear el primero (problema del huevo y la gallina).
 * Como la base es H2 en memoria, se recrea en cada arranque.
 */
@Component
public class AdminInicialSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInicialSeeder.class);

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public AdminInicialSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			@Value("${app.admin-inicial.email}") String email,
			@Value("${app.admin-inicial.password}") String password) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(String... args) {
		if (usuarioRepository.existsByEmail(email)) {
			return;
		}

		usuarioRepository.save(new Usuario(
				"Admin",
				"MediConecta",
				email,
				passwordEncoder.encode(password),
				null,
				null,
				Rol.ADMIN));

		log.info("usuario ADMIN inicial creado: {}", email);
	}
}
