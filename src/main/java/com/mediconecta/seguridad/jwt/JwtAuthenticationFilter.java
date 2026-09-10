package com.mediconecta.seguridad.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.seguridad.UserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * Se ejecuta una vez por request, antes de que llegue al controller.
 *
 * Flujo:
 *   1. Lee el header "Authorization: Bearer <token>"
 *   2. Si no hay token, deja pasar (los endpoints publicos no lo necesitan)
 *   3. Si hay token, lo valida y carga el usuario de la base
 *   4. Lo guarda en el SecurityContext para que el controller sepa quien es
 *
 * Por que se consulta la base y no se usan los claims del token: el token es
 * inmutable hasta que vence, asi que si sus datos fueran la unica fuente, dar de
 * baja a un usuario o degradar un ADMIN no tendria efecto hasta la expiracion —
 * el usuario seguiria operando con los permisos viejos. El token sigue siendo la
 * prueba de identidad (la firma), pero el ESTADO (activo, rol) sale de la base.
 *
 * Cuesta una consulta por clave primaria por request. A esta escala es
 * despreciable frente a lo que evita.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtTokenProvider tokenProvider;
	private final UsuarioRepository usuarioRepository;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UsuarioRepository usuarioRepository) {
		this.tokenProvider = tokenProvider;
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			String token = getTokenDelRequest(request);

			if (StringUtils.hasText(token) && tokenProvider.validarToken(token)
					&& !tokenProvider.esRefreshToken(token)) {

				/*
				 * Si el usuario no existe o esta dado de baja, no se autentica: el request
				 * sigue como anonimo y Spring Security responde 401. Un token todavia
				 * vigente de una cuenta desactivada deja de servir en el acto.
				 */
				Usuario usuario = usuarioRepository.findById(tokenProvider.getUsuarioIdDelToken(token))
						.filter(Usuario::isActivo)
						.orElse(null);

				if (usuario != null) {
					// El rol sale de la base, no del claim: un cambio de rol aplica en el request siguiente.
					UserPrincipal principal = UserPrincipal.desde(usuario);

					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
							principal, null, principal.getAuthorities());
					auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			}
		} catch (Exception ex) {
			log.error("no se pudo autenticar el request", ex);
		}

		filterChain.doFilter(request, response);
	}

	private String getTokenDelRequest(HttpServletRequest request) {
		String header = request.getHeader("Authorization");

		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}
}
