import { useEffect, useRef } from 'react';
import { useGoogleMaps } from '../../hooks/useGoogleMaps';
import type { EstacionConDisponibilidad } from '../../hooks/useEstaciones';

interface Props {
	estaciones: EstacionConDisponibilidad[];
	/** Estacion resaltada; el mapa centra en ella cuando cambia. */
	seleccionadaId: number | null;
	onSeleccionar: (id: number) => void;
}

/** Centro por defecto: Obelisco. Solo se usa hasta que hay estaciones que encuadrar. */
const CENTRO_INICIAL = { lat: -34.6037, lng: -58.3816 };

/**
 * Envoltorio de React alrededor del mapa de Google.
 *
 * El punto del componente es aislar el unico pedazo imperativo de la app: Google
 * Maps no se describe, se ordena — hay que crear un objeto, mutarlo y destruirlo a
 * mano. Todo eso queda encerrado aca adentro, detras de props declarativas, para
 * que ninguna otra parte tenga que saber que existe un objeto mutable dando vueltas.
 *
 * De ahi la division en tres efectos, cada uno con su propia dependencia:
 *   1. crear el mapa      -> una sola vez
 *   2. sincronizar pines  -> cuando cambian las estaciones
 *   3. centrar            -> cuando cambia la seleccion
 * Si fuera un solo efecto, cambiar la seleccion recrearia el mapa entero y se
 * veria el parpadeo gris de la recarga.
 */
export function MapaEstaciones({ estaciones, seleccionadaId, onSeleccionar }: Props) {
	const estado = useGoogleMaps();
	const contenedor = useRef<HTMLDivElement>(null);
	const mapa = useRef<google.maps.Map | null>(null);
	// Los pines se guardan en un ref y no en estado: no se dibujan en el JSX, son
	// objetos que hay que poder recuperar para borrarlos del mapa.
	const pines = useRef<Map<number, google.maps.Marker>>(new Map());

	// 1. Crear el mapa.
	useEffect(() => {
		if (estado !== 'listo' || !contenedor.current || mapa.current) return;
		mapa.current = new google.maps.Map(contenedor.current, {
			center: CENTRO_INICIAL,
			zoom: 12,
			mapTypeControl: false,
			streetViewControl: false,
		});
	}, [estado]);

	// 2. Sincronizar los pines con las estaciones.
	useEffect(() => {
		const map = mapa.current;
		if (!map) return;

		// Se borran los pines de estaciones que ya no estan.
		const vigentes = new Set(estaciones.map((e) => e.id));
		for (const [id, pin] of pines.current) {
			if (!vigentes.has(id)) {
				pin.setMap(null);
				pines.current.delete(id);
			}
		}

		for (const estacion of estaciones) {
			// Sin coordenadas no hay nada que pintar: la estacion existe pero todavia
			// no fue geocodificada.
			if (estacion.latitud == null || estacion.longitud == null) continue;

			let pin = pines.current.get(estacion.id);
			if (!pin) {
				pin = new google.maps.Marker({ map, title: estacion.nombre });
				pin.addListener('click', () => onSeleccionar(estacion.id));
				pines.current.set(estacion.id, pin);
			}
			pin.setPosition({ lat: estacion.latitud, lng: estacion.longitud });
			pin.setLabel(String(estacion.bicisDisponibles));
		}

		// Encuadra todas las estaciones la primera vez que hay datos.
		const conCoordenadas = estaciones.filter((e) => e.latitud != null && e.longitud != null);
		if (conCoordenadas.length > 0) {
			const limites = new google.maps.LatLngBounds();
			for (const e of conCoordenadas) limites.extend({ lat: e.latitud!, lng: e.longitud! });
			map.fitBounds(limites, 48);
		}
	}, [estaciones, onSeleccionar]);

	// 3. Centrar en la estacion seleccionada.
	useEffect(() => {
		const map = mapa.current;
		if (!map || seleccionadaId == null) return;
		const estacion = estaciones.find((e) => e.id === seleccionadaId);
		if (estacion?.latitud == null || estacion.longitud == null) return;
		map.panTo({ lat: estacion.latitud, lng: estacion.longitud });
	}, [seleccionadaId, estaciones]);

	// Al desmontar, los pines se sacan del mapa: si no, quedan referenciados por el
	// objeto de Google y no los junta el recolector.
	useEffect(() => {
		const actuales = pines.current;
		return () => {
			for (const pin of actuales.values()) pin.setMap(null);
			actuales.clear();
		};
	}, []);

	if (estado === 'sin-clave') {
		return (
			<div className="mapa-vacio">
				<p className="aviso aviso-alerta">
					Falta configurar <code>VITE_GOOGLE_MAPS_API_KEY</code> en <code>frontend/.env</code> para ver el mapa.
				</p>
				<p className="ayuda">Mientras tanto, el listado de abajo funciona igual.</p>
			</div>
		);
	}

	if (estado === 'error') {
		return (
			<div className="mapa-vacio">
				<p className="aviso aviso-alerta">
					No se pudo cargar Google Maps. Revisá que la key sea válida y que el dominio esté permitido en Google Cloud.
				</p>
			</div>
		);
	}

	return (
		<div
			ref={contenedor}
			className="mapa"
			role="application"
			aria-label="Mapa de estaciones"
			aria-busy={estado === 'cargando'}
		/>
	);
}
