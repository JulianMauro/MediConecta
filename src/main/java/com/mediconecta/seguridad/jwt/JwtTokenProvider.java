package com.mediconecta.seguridad.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


/**
 * Genera y valida los tokens JWT.
 *
 * Un JWT es un texto firmado que viaja en el header "Authorization" de cada request.
 * Como esta firmado con una clave secreta que solo conoce el servidor, si alguien
 * lo modifica la firma deja de coincidir y el token se rechaza. Eso permite validarlo
 * sin consultar la base de datos.
 */
@Component
public class JwtTokenProvider {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_ROL = "rol";
	private static final String CLAIM_TIPO = "tipo";
	private static final String TIPO_ACCESS = "access";
	private static final String TIPO_REFRESH = "refresh";

	private final SecretKey key;
	private final long accessExpirationMs;
	private final long refreshExpirationMs;

	public JwtTokenProvider(
			@Value("${app.jwt.secret}") String jwtSecret,
			@Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
			@Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
		this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		this.accessExpirationMs = accessExpirationMs;
		this.refreshExpirationMs = refreshExpirationMs;
	}

	/** Token corto que se manda en cada request. Lleva el id, el email y el rol. */
	public String generarAccessToken(Usuario usuario) {
		Date ahora = new Date();

		return Jwts.builder()
				.subject(usuario.getId().toString())
				.claim(CLAIM_EMAIL, usuario.getEmail())
				.claim(CLAIM_ROL, usuario.getRol().name())
				.claim(CLAIM_TIPO, TIPO_ACCESS)
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + accessExpirationMs))
				.signWith(key, Jwts.SIG.HS512)
				.compact();
	}

	/** Token largo que solo sirve para pedir un access token nuevo. Lleva el minimo de datos. */
	public String generarRefreshToken(Usuario usuario) {
		Date ahora = new Date();

		return Jwts.builder()
				.subject(usuario.getId().toString())
				.claim(CLAIM_TIPO, TIPO_REFRESH)
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + refreshExpirationMs))
				.signWith(key, Jwts.SIG.HS512)
				.compact();
	}

	/** Verifica la firma y la expiracion. No toca la base de datos. */
	public boolean validarToken(String token) {
		try {
			parsear(token);
			return true;
		} catch (ExpiredJwtException ex) {
			log.warn("token vencido");
		} catch (JwtException | IllegalArgumentException ex) {
			log.warn("token invalido: {}", ex.getMessage());
		}
		return false;
	}

	public boolean esRefreshToken(String token) {
		return TIPO_REFRESH.equals(parsear(token).get(CLAIM_TIPO, String.class));
	}

	public Long getUsuarioIdDelToken(String token) {
		return Long.valueOf(parsear(token).getSubject());
	}

	public String getEmailDelToken(String token) {
		return parsear(token).get(CLAIM_EMAIL, String.class);
	}

	public Rol getRolDelToken(String token) {
		return Rol.valueOf(parsear(token).get(CLAIM_ROL, String.class));
	}

	public long getAccessTokenSegundos() {
		return accessExpirationMs / 1000;
	}

	private Claims parsear(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
