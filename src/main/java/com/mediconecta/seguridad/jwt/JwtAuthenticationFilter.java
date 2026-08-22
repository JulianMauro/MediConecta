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
 *   3. Si hay token, lo valida y arma el usuario autenticado con los datos del propio token
 *   4. Lo guarda en el SecurityContext para que el controller sepa quien es
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtTokenProvider tokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			String token = getTokenDelRequest(request);

			if (StringUtils.hasText(token) && tokenProvider.validarToken(token)
					&& !tokenProvider.esRefreshToken(token)) {

				// Todo sale del token: no hace falta ir a la base de datos.
				UserPrincipal principal = new UserPrincipal(
						tokenProvider.getUsuarioIdDelToken(token),
						tokenProvider.getEmailDelToken(token),
						null,
						tokenProvider.getRolDelToken(token),
						true);

				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
						principal, null, principal.getAuthorities());
				auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(auth);
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
