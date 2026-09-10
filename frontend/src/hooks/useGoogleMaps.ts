import { useEffect, useState } from 'react';

/**
 * Carga el SDK de Google Maps una sola vez para toda la app.
 *
 * Por que no se pone el <script> en index.html: ahi se descargaria en cada visita
 * a cualquier pagina, cuando en realidad solo lo necesita el mapa. Y por que no se
 * carga dentro del componente sin mas: si hubiera dos mapas en pantalla (o React
 * remontara el componente, cosa que en modo estricto pasa siempre) se inyectaria
 * el script dos veces y Google avisa por consola.
 *
 * La promesa vive a nivel de modulo justamente para eso: la primera llamada crea
 * el <script>, las demas se cuelgan de la misma promesa.
 */
let promesa: Promise<void> | null = null;

/** La key del front es publica por definicion: viaja en la URL del script y
 *  cualquiera puede leerla. Lo que la protege es la restriccion por dominio
 *  (HTTP referrer) que se configura en Google Cloud, no el secreto. */
const CLAVE = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;

function cargar(): Promise<void> {
	if (promesa) return promesa;

	promesa = new Promise((resolve, reject) => {
		if (!CLAVE) {
			reject(new Error('falta VITE_GOOGLE_MAPS_API_KEY'));
			return;
		}
		const script = document.createElement('script');
		// loading=async es lo que Google pide desde 2023; sin eso avisa por consola.
		script.src = `https://maps.googleapis.com/maps/api/js?key=${CLAVE}&libraries=marker&loading=async&language=es&region=AR`;
		script.async = true;
		script.onload = () => resolve();
		script.onerror = () => reject(new Error('no se pudo cargar Google Maps'));
		document.head.appendChild(script);
	});

	return promesa;
}

export type EstadoMapa = 'sin-clave' | 'cargando' | 'listo' | 'error';

/** Estado de carga del SDK. El componente del mapa lo usa para no tocar
 *  window.google antes de tiempo. */
export function useGoogleMaps(): EstadoMapa {
	const [estado, setEstado] = useState<EstadoMapa>(CLAVE ? 'cargando' : 'sin-clave');

	useEffect(() => {
		if (!CLAVE) return;
		let vigente = true;
		cargar().then(
			() => vigente && setEstado('listo'),
			() => vigente && setEstado('error'),
		);
		// Si el componente se desmonta mientras carga, no se toca su estado.
		return () => {
			vigente = false;
		};
	}, []);

	return estado;
}
