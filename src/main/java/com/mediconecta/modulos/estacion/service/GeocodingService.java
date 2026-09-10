package com.mediconecta.modulos.estacion.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


/**
 * Traduce una direccion a coordenadas contra la Geocoding API de Google.
 *
 * Es deliberadamente "best effort": ante cualquier problema devuelve Optional.empty()
 * en vez de tirar excepcion. El motivo es que quien lo llama (el alta de estacion) no
 * puede romperse porque Google no conteste — una estacion sin coordenadas es una
 * estacion que existe y funciona, solo que todavia no se puede pintar en el mapa.
 * Quien necesita tratar el fallo como error (el re-geocodificado manual) decide eso
 * por su cuenta al ver el Optional vacio.
 *
 * Los casos que fallan quedan logueados con el status real de Google, porque una key
 * mal configurada y una direccion inexistente se arreglan de formas muy distintas.
 */
@Service
public class GeocodingService {

	private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
	private static final String URL = "https://maps.googleapis.com/maps/api/geocode/json";

	private final RestClient restClient;
	private final String apiKey;

	public GeocodingService(@Value("${app.google-maps.api-key:}") String apiKey) {
		/*
		 * El cliente se arma aca en vez de inyectar RestClient.Builder: ese bean no
		 * esta autoconfigurado en este proyecto y la app no levantaba.
		 *
		 * Los timeouts no son opcionales. Esta llamada ocurre DENTRO de la transaccion
		 * que da de alta la estacion: sin timeout, un Google que no responde deja la
		 * transaccion abierta y la conexion del pool tomada por tiempo indefinido.
		 */
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(3));
		factory.setReadTimeout(Duration.ofSeconds(5));

		this.restClient = RestClient.builder().requestFactory(factory).build();
		this.apiKey = apiKey;
	}

	/** Vacio si no hay key, si Google no encuentra la direccion, o si la llamada falla. */
	public Optional<Coordenadas> geocodificar(String direccion) {
		if (apiKey == null || apiKey.isBlank()) {
			log.debug("geocoding sin API key: la estacion se guarda sin coordenadas");
			return Optional.empty();
		}

		try {
			// Los {..} los codifica RestClient: nunca concatenar la direccion a mano,
			// que trae espacios, comas y acentos.
			// components=country:AR restringe la busqueda a Argentina del lado de Google.
			// Es mejor que pegarle ", Argentina" al texto: no depende de como este
			// escrita la direccion y no se puede contradecir con lo que el usuario tipeo.
			RespuestaGeocoding respuesta = restClient.get()
					.uri(URL + "?address={direccion}&components=country:AR&key={key}", direccion, apiKey)
					.retrieve()
					.body(RespuestaGeocoding.class);

			if (respuesta == null) {
				log.warn("geocoding: respuesta vacia para '{}'", direccion);
				return Optional.empty();
			}

			// El status se mira SIEMPRE antes de tocar results: ante ZERO_RESULTS la
			// lista viene vacia, y un results.get(0) directo seria un 500 cada vez que
			// alguien escribe mal una direccion.
			if (!"OK".equals(respuesta.status()) || respuesta.results() == null || respuesta.results().isEmpty()) {
				log.warn("geocoding sin resultado para '{}': status={}", direccion, respuesta.status());
				return Optional.empty();
			}

			Ubicacion ubicacion = respuesta.results().get(0).geometry().location();
			return Optional.of(new Coordenadas(ubicacion.lat(), ubicacion.lng()));

		} catch (RestClientException e) {
			// Red caida, timeout, 5xx de Google: no es problema del usuario que esta
			// dando de alta la estacion, asi que no se le traslada como error.
			log.warn("geocoding fallo para '{}': {}", direccion, e.getMessage());
			return Optional.empty();
		}
	}

	/* Forma minima de la respuesta de Google: solo lo que se usa. Jackson ignora el
	   resto de los campos, que son muchos. */

	record RespuestaGeocoding(String status, List<Resultado> results) {
	}

	record Resultado(Geometria geometry) {
	}

	record Geometria(Ubicacion location) {
	}

	record Ubicacion(double lat, double lng) {
	}
}
