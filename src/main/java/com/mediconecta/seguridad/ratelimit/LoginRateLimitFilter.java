package com.mediconecta.seguridad.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * Limita los intentos de login fallidos por IP.
 *
 * Sin esto, /api/auth/login se puede llamar infinitas veces: aunque las passwords
 * esten hasheadas con BCrypt, nada impide probar un diccionario contra una cuenta
 * conocida (por ejemplo la del admin) hasta acertar.
 *
 * Como funciona: el filtro deja pasar el request y despues mira el codigo de
 * respuesta. Un 401 cuenta como intento fallido; un 2xx limpia el contador.
 * Al llegar a {@code maxIntentos} fallos dentro de la ventana, la IP recibe 429
 * hasta que pase el bloqueo, sin siquiera tocar la base.
 *
 * Limitaciones asumidas (es una POC de un solo nodo):
 * - El contador vive en memoria: se reinicia si se reinicia la app, y no se
 *   comparte entre instancias. Para varios nodos haria falta Redis.
 * - Se cuenta por IP y no por email para no tener que leer (y bufferear) el body.
 *   Un atacante con muchas IPs lo esquiva; el caso comun de fuerza bruta no.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

	private static final String RUTA_LOGIN = "/api/auth/login";
	/** Arriba de esto se purgan las entradas vencidas, para que el mapa no crezca sin techo. */
	private static final int UMBRAL_LIMPIEZA = 1_000;

	private final Map<String, Intentos> porIp = new ConcurrentHashMap<>();
	private final int maxIntentos;
	private final Duration ventana;
	private final Duration bloqueo;

	public LoginRateLimitFilter(
			@Value("${app.seguridad.login.max-intentos:5}") int maxIntentos,
			@Value("${app.seguridad.login.ventana-minutos:15}") long ventanaMinutos,
			@Value("${app.seguridad.login.bloqueo-minutos:15}") long bloqueoMinutos) {
		this.maxIntentos = maxIntentos;
		this.ventana = Duration.ofMinutes(ventanaMinutos);
		this.bloqueo = Duration.ofMinutes(bloqueoMinutos);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// getRequestURI() y no getServletPath(): el segundo llega vacio segun como
		// este mapeado el dispatcher, y el filtro quedaria sin aplicarse nunca.
		String ruta = request.getRequestURI().substring(request.getContextPath().length());
		return !("POST".equalsIgnoreCase(request.getMethod()) && RUTA_LOGIN.equals(ruta));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String ip = ipDelCliente(request);
		Intentos intentos = porIp.get(ip);

		if (intentos != null && intentos.estaBloqueado(maxIntentos, bloqueo)) {
			long segundos = intentos.segundosRestantes(bloqueo);
			log.warn("login bloqueado por exceso de intentos: ip={} faltan={}s", ip, segundos);

			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader("Retry-After", String.valueOf(segundos));
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(
					"{\"error\":\"demasiados intentos fallidos, probá de nuevo en " + segundos + " segundos\"}");
			return;
		}

		filterChain.doFilter(request, response);

		int estado = response.getStatus();
		if (estado == HttpStatus.UNAUTHORIZED.value()) {
			registrarFallo(ip);
		} else if (estado < 400) {
			// Login correcto: se borra el historial de esa IP.
			porIp.remove(ip);
		}
	}

	private void registrarFallo(String ip) {
		if (porIp.size() > UMBRAL_LIMPIEZA) {
			purgarVencidos();
		}
		Intentos intentos = porIp.computeIfAbsent(ip, k -> new Intentos());
		int fallos = intentos.sumarFallo(ventana);

		if (fallos >= maxIntentos) {
			log.warn("ip bloqueada por {} intentos de login fallidos: {}", fallos, ip);
		}
	}

	private void purgarVencidos() {
		Instant corte = Instant.now().minus(ventana.plus(bloqueo));
		porIp.entrySet().removeIf(e -> e.getValue().ultimoIntento().isBefore(corte));
	}

	/**
	 * Detras de un proxy o load balancer (Render, Fly, Nginx) la IP real viene en
	 * X-Forwarded-For; getRemoteAddr() devolveria siempre la del proxy y bloquearia
	 * a todos los usuarios de una.
	 */
	private String ipDelCliente(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwarded)) {
			// El header es una lista "cliente, proxy1, proxy2": el primero es el origen.
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	/** Contador de fallos de una IP. Los metodos que mutan estado van sincronizados. */
	private static final class Intentos {

		private final AtomicInteger fallos = new AtomicInteger();
		private volatile Instant ultimoIntento = Instant.now();

		synchronized int sumarFallo(Duration ventana) {
			Instant ahora = Instant.now();
			// Si el ultimo fallo quedo fuera de la ventana, la racha arranca de cero.
			if (ultimoIntento.isBefore(ahora.minus(ventana))) {
				fallos.set(0);
			}
			ultimoIntento = ahora;
			return fallos.incrementAndGet();
		}

		boolean estaBloqueado(int maxIntentos, Duration bloqueo) {
			return fallos.get() >= maxIntentos && ultimoIntento.isAfter(Instant.now().minus(bloqueo));
		}

		long segundosRestantes(Duration bloqueo) {
			long restante = Duration.between(Instant.now(), ultimoIntento.plus(bloqueo)).getSeconds();
			return Math.max(restante, 1);
		}

		Instant ultimoIntento() {
			return ultimoIntento;
		}
	}
}
