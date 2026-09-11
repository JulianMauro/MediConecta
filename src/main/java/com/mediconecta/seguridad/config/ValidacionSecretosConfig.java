package com.mediconecta.seguridad.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;


/**
 * Corta el arranque si la app sube a produccion con los valores de desarrollo.
 *
 * El riesgo concreto: application.properties define defaults despues de ":" para
 * que el proyecto arranque sin configurar nada. Eso es comodo en la maquina, pero
 * si el hosting no tiene seteada la variable de entorno, la app levanta igual — con
 * el JWT_SECRET publicado en el repositorio (cualquiera puede firmarse un token de
 * ADMIN) y con el admin en admin@mediconecta.com / admin1234.
 *
 * Fallar al arrancar es preferible a quedar abierto sin que nadie se entere.
 * Solo aplica con el perfil "prod" activo: en local no molesta.
 */
@Configuration
@Profile("prod")
public class ValidacionSecretosConfig {

	/** Los valores que estan escritos en el repositorio y por eso ya no son secretos. */
	private static final String JWT_SECRET_DE_DESARROLLO =
			"mediconecta-clave-secreta-de-desarrollo-cambiar-en-produccion-1234567890";
	private static final String ADMIN_PASSWORD_DE_DESARROLLO = "admin1234";
	private static final int JWT_SECRET_MIN_LARGO = 64;

	private final String jwtSecret;
	private final String adminPassword;
	private final List<String> corsAllowedOrigins;

	public ValidacionSecretosConfig(
			@Value("${app.jwt.secret}") String jwtSecret,
			@Value("${app.admin-inicial.password}") String adminPassword,
			@Value("${app.cors.allowed-origins}") List<String> corsAllowedOrigins) {
		this.jwtSecret = jwtSecret;
		this.adminPassword = adminPassword;
		this.corsAllowedOrigins = corsAllowedOrigins;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void validar() {
		List<String> problemas = new ArrayList<>();

		if (JWT_SECRET_DE_DESARROLLO.equals(jwtSecret)) {
			problemas.add("JWT_SECRET es el valor de desarrollo, que esta publicado en el repositorio. "
					+ "Generá uno nuevo: openssl rand -base64 64");
		}
		if (jwtSecret.length() < JWT_SECRET_MIN_LARGO) {
			problemas.add("JWT_SECRET tiene " + jwtSecret.length() + " caracteres; HS512 necesita al menos "
					+ JWT_SECRET_MIN_LARGO + ".");
		}
		if (ADMIN_PASSWORD_DE_DESARROLLO.equals(adminPassword)) {
			problemas.add("ADMIN_PASSWORD es 'admin1234', el default de desarrollo. Cambialo.");
		}
		if (corsAllowedOrigins.stream().anyMatch(o -> o.contains("localhost") || "*".equals(o.trim()))) {
			problemas.add("CORS_ALLOWED_ORIGINS incluye localhost o '*'. En produccion van solo los dominios reales "
					+ "del frontend. Actual: " + corsAllowedOrigins);
		}

		if (!problemas.isEmpty()) {
			throw new IllegalStateException(
					"La configuracion de seguridad no es apta para produccion:"
							+ problemas.stream().reduce("", (acc, p) -> acc + "\n  - " + p)
							+ "\n\nSeteá esas variables de entorno en el hosting y volvé a desplegar.");
		}
	}
}
