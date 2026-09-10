import { useCallback, useMemo, useState } from 'react';
import { MapaEstaciones } from '../components/mapa/MapaEstaciones';
import { useEstaciones } from '../hooks/useEstaciones';
import { plural } from '../lib/plural';

/**
 * Mapa de la red.
 *
 * Reusa useEstaciones, el mismo hook que la pagina de Estaciones: ya trae las
 * estaciones activas con el recuento de bicis y anclajes libres. Duplicar la carga
 * en un hook propio hubiera significado mantener dos veces la misma logica y
 * arriesgarse a que las dos pantallas muestren numeros distintos.
 */
export function MapPage() {
	const { estaciones, cargando, error, recargar } = useEstaciones();
	const [seleccionadaId, setSeleccionadaId] = useState<number | null>(null);

	// useCallback porque la funcion es dependencia de un efecto dentro del mapa:
	// si cambiara de identidad en cada render, los pines se recrearian sin parar.
	const seleccionar = useCallback((id: number) => setSeleccionadaId(id), []);

	const sinCoordenadas = useMemo(
		() => estaciones.filter((e) => e.latitud == null || e.longitud == null),
		[estaciones],
	);

	const seleccionada = estaciones.find((e) => e.id === seleccionadaId) ?? null;

	return (
		<div className="contenido">
			<header className="encabezado-pagina">
				<h1>Mapa de la red</h1>
				<p className="bajada">
					Dónde está cada estación y cuántas bicis tiene ahora. Tocá un pin para ver el detalle.
				</p>
			</header>

			{error && <p className="aviso aviso-alerta">{error}</p>}

			<section className="tarjeta seccion-ancha">
				<MapaEstaciones
					estaciones={estaciones}
					seleccionadaId={seleccionadaId}
					onSeleccionar={seleccionar}
				/>
			</section>

			<div className="mapa-barra">
				<button type="button" className="boton-secundario" onClick={recargar}>
					Actualizar
				</button>
				<p className="ayuda" aria-live="polite">
					{cargando ? 'Cargando estaciones…' : `${plural(estaciones.length, 'estación', 'estaciones')} en la red`}
				</p>
			</div>

			{/*
			 * Una estacion sin latitud/longitud no se puede pintar. Se avisa en vez de
			 * ocultarla: si no, desde el mapa parece que la estacion no existe.
			 */}
			{sinCoordenadas.length > 0 && (
				<p className="aviso aviso-alerta">
					{plural(sinCoordenadas.length, 'estación', 'estaciones')} sin coordenadas, así que no aparecen en el mapa:{' '}
					{sinCoordenadas.map((e) => e.nombre).join(', ')}.
				</p>
			)}

			{seleccionada && (
				<section className="tarjeta">
					<h2>{seleccionada.nombre}</h2>
					<p className="estacion-direccion">{seleccionada.direccion}</p>
					<dl className="estacion-cifras">
						<div>
							<dt>Bicis para sacar</dt>
							<dd className="cifra">{seleccionada.bicisDisponibles}</dd>
						</div>
						<div>
							<dt>Lugares para devolver</dt>
							<dd className="cifra">{seleccionada.anclajesLibres}</dd>
						</div>
						<div>
							<dt>Fuera de servicio</dt>
							<dd className="cifra cifra-suave">{seleccionada.fueraServicio}</dd>
						</div>
					</dl>
				</section>
			)}
		</div>
	);
}
