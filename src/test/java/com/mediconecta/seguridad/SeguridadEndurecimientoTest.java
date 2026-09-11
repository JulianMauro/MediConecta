package com.mediconecta.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cubre el endurecimiento de seguridad: fuerza bruta en el login, headers de
 * respuesta y que los endpoints privados sigan pidiendo token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SeguridadEndurecimientoTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String LOGIN_INEXISTENTE = """
			{"email":"nadie@ejemplo.com","password":"passwordIncorrecta"}
			""";

	@Test
	void bloqueaElLoginTrasCincoIntentosFallidos() throws Exception {
		// Una IP propia para no chocar con el contador de los otros tests.
		String ip = "203.0.113.10";

		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login")
					.header("X-Forwarded-For", ip)
					.contentType(MediaType.APPLICATION_JSON)
					.content(LOGIN_INEXISTENTE))
					.andExpect(status().isUnauthorized());
		}

		// El sexto ya no llega a validar credenciales: lo corta el filtro.
		mockMvc.perform(post("/api/auth/login")
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN_INEXISTENTE))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}

	@Test
	void otraIpNoQuedaAfectadaPorElBloqueoAjeno() throws Exception {
		String ipAtacante = "203.0.113.20";
		for (int i = 0; i < 6; i++) {
			mockMvc.perform(post("/api/auth/login")
					.header("X-Forwarded-For", ipAtacante)
					.contentType(MediaType.APPLICATION_JSON)
					.content(LOGIN_INEXISTENTE));
		}

		// Un usuario legitimo desde otra IP tiene que poder seguir intentando.
		mockMvc.perform(post("/api/auth/login")
				.header("X-Forwarded-For", "203.0.113.99")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN_INEXISTENTE))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void losEndpointsPrivadosSiguenPidiendoToken() throws Exception {
		mockMvc.perform(get("/api/viajes"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/usuarios"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthEsPublicoPeroSinDetalle() throws Exception {
		// El healthcheck de Docker necesita el 200; el detalle (base, disco) no.
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}
}
