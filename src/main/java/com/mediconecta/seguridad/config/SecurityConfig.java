package com.mediconecta.seguridad.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mediconecta.seguridad.UserDetailsServiceImpl;
import com.mediconecta.seguridad.jwt.JwtAuthenticationFilter;
import com.mediconecta.seguridad.ratelimit.LoginRateLimitFilter;

import jakarta.servlet.DispatcherType;


/**
 * Configuracion central de Spring Security.
 *
 * @EnableMethodSecurity habilita las anotaciones @PreAuthorize de los controllers,
 * que son las que aplican el control por rol endpoint por endpoint.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final LoginRateLimitFilter loginRateLimitFilter;
	private final UserDetailsServiceImpl userDetailsService;
	private final List<String> corsAllowedOrigins;
	/** Solo el perfil "h2" prende la consola; con PostgreSQL siempre esta apagada. */
	private final boolean h2ConsoleHabilitada;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			LoginRateLimitFilter loginRateLimitFilter,
			UserDetailsServiceImpl userDetailsService,
			@Value("${app.cors.allowed-origins}") List<String> corsAllowedOrigins,
			@Value("${spring.h2.console.enabled:false}") boolean h2ConsoleHabilitada) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.loginRateLimitFilter = loginRateLimitFilter;
		this.userDetailsService = userDetailsService;
		this.corsAllowedOrigins = corsAllowedOrigins;
		this.h2ConsoleHabilitada = h2ConsoleHabilitada;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				// Sin CSRF: el sistema es stateless y no usa cookies de sesion.
				.csrf(csrf -> csrf.disable())
				// Sin sesiones en el servidor: la identidad viaja en el token de cada request.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(this::configurarHeaders)
				// Sin token (o con uno invalido) devuelve 401, no 403.
				// 403 queda reservado para "estas identificado pero tu rol no alcanza".
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> {
					// Spring reenvia los errores a /error: si no se permite ese dispatch,
					// el 401 o el 404 original se pierde y llega un 403 al cliente.
					auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
							// Endpoints publicos: no requieren token.
							.requestMatchers(
									"/api/auth/login",
									"/api/auth/register",
									"/api/auth/refresh",
									"/api/ping",
									"/actuator/health")
							.permitAll();

					// La consola H2 se abre SOLO con el perfil h2. Si esta regla fuera
					// incondicional y alguien prendiera la consola en un ambiente real,
					// quedaria una consola SQL sin autenticar contra la base productiva.
					if (h2ConsoleHabilitada) {
						auth.requestMatchers("/h2-console/**").permitAll();
					}

					// Todo lo demas exige un access token valido.
					auth.anyRequest().authenticated();
				})
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				// Antes que el de JWT: frena la fuerza bruta sin llegar a consultar la base.
				.addFilterBefore(loginRateLimitFilter, JwtAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Headers de respuesta que endurecen el navegador del cliente.
	 * Los valores dependen del perfil: la consola H2 es HTML y necesita permisos
	 * que una API JSON no tiene por que dar.
	 */
	private void configurarHeaders(HeadersConfigurer<HttpSecurity> headers) {
		headers
				// HSTS: una vez visitado por HTTPS, el navegador no vuelve a intentar HTTP.
				// Solo tiene efecto sobre HTTPS, asi que en local no cambia nada.
				.httpStrictTransportSecurity(hsts -> hsts
						.includeSubDomains(true)
						.maxAgeInSeconds(31_536_000))
				// No filtrar la URL completa (que puede llevar ids) a sitios de terceros.
				.referrerPolicy(ref -> ref
						.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

		if (h2ConsoleHabilitada) {
			// La consola de H2 se renderiza dentro de un iframe del mismo origen.
			headers.frameOptions(frame -> frame.sameOrigin());
		} else {
			// La API no se embebe en ningun lado: nadie deberia poder meterla en un iframe.
			headers.frameOptions(frame -> frame.deny())
					// Es una API JSON: no carga scripts, estilos ni imagenes propias.
					.contentSecurityPolicy(csp -> csp
							.policyDirectives("default-src 'none'; frame-ancestors 'none'; sandbox"));
		}
	}

	/** BCrypt: algoritmo de hash pensado para passwords (lento a proposito). */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Es quien valida email + password en el login: busca el usuario con
	 * UserDetailsService y compara la password contra el hash BCrypt.
	 */
	@Bean
	public AuthenticationManager authenticationManager() {
		return new ProviderManager(authenticationProvider());
	}

	/** No se expone como @Bean a proposito: si lo fuera, Spring ignoraria el UserDetailsService. */
	private DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		// "*" con allowCredentials=true lo rechaza el propio navegador, y ademas
		// abriria la API a cualquier sitio. Mejor enterarse al arrancar.
		if (corsAllowedOrigins.stream().anyMatch(origen -> "*".equals(origen.trim()))) {
			throw new IllegalStateException(
					"app.cors.allowed-origins no puede ser '*': hay que listar los dominios del frontend. "
							+ "Para varios, separalos con coma.");
		}

		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(corsAllowedOrigins.stream().map(String::trim).toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		// Evita repetir el preflight en cada request durante una hora.
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
