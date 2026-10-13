import { useCallback, useMemo, useState } from 'react';
import { MapaEstaciones } from '../components/mapa/MapaEstaciones';
import { useEstaciones } from '../hooks/useEstaciones';
import { plural } from '../lib/plural';
import { IconoCerrar } from '../components/ui/Iconos';

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

			{/*
			 * En escritorio, mapa y panel lado a lado: el detalle de la estacion aparece
			 * al costado del pin que la abrio, no debajo del mapa y fuera de la vista. El
			 * panel suma la lista de la red, que tambien selecciona y centra el mapa
			 * (es la misma estructura que el mapa web de Citi Bike). En el celular todo
			 * va en una columna y el detalle es la hoja inferior.
			 */}
			<div className="mapa-layout">
				<section className="tarjeta seccion-ancha mapa-marco">
					<MapaEstaciones
						estaciones={estaciones}
						seleccionadaId={seleccionadaId}
						onSeleccionar={seleccionar}
					/>
				</section>

				<div className="mapa-panel">
					{/*
					 * En el celular el detalle sube como hoja inferior sobre el mapa, que es
					 * como lo muestran Citi Bike y BA Ecobici: tocás el pin y la respuesta
					 * aparece donde esta el pulgar, sin perder el mapa de vista.
					 */}
					{seleccionada ? (
						<section className="tarjeta mapa-detalle" aria-label={`Estación ${seleccionada.nombre}`}>
							<button
								type="button"
								className="modal-cerrar mapa-detalle-cerrar solo-movil"
								onClick={() => setSeleccionadaId(null)}
								aria-label="Cerrar el detalle de la estación"
							>
								<IconoCerrar />
							</button>
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
					) : (
						<p className="tarjeta ayuda mapa-sin-seleccion solo-escritorio">
							Elegí una estación en el mapa o en la lista para ver cuántas bicis y lugares libres tiene.
						</p>
					)}

					<section className="tarjeta mapa-lista" aria-labelledby="mapa-lista-titulo">
						<div className="mapa-barra">
							<h2 id="mapa-lista-titulo" className="solo-escritorio">
								Estaciones
							</h2>
							<p className="ayuda" aria-live="polite">
								{cargando ? 'Cargando estaciones…' : `${plural(estaciones.length, 'estación', 'estaciones')} en la red`}
							</p>
							<button type="button" className="boton-secundario boton-chico" onClick={recargar}>
								Actualizar
							</button>
						</div>

						{/*
						 * Una estacion sin latitud/longitud no se puede pintar. Se avisa en vez de
						 * ocultarla: si no, desde el mapa parece que la estacion no existe.
						 */}
						{sinCoordenadas.length > 0 && (
							<p className="aviso aviso-alerta">
								{plural(sinCoordenadas.length, 'estación', 'estaciones')} sin coordenadas, así que no aparecen en el
								mapa: {sinCoordenadas.map((e) => e.nombre).join(', ')}.
							</p>
						)}

						{/* En el celular la lista de la red es la pestaña Estaciones. */}
						<ul className="mapa-lista-items solo-escritorio">
							{estaciones.map((e) => (
								<li key={e.id}>
									<button
										type="button"
										className="mapa-lista-item"
										aria-pressed={e.id === seleccionadaId}
										onClick={() => seleccionar(e.id)}
									>
										<span className="mapa-lista-nombre">{e.nombre}</span>
										<span className="mapa-lista-cifras">
											{plural(e.bicisDisponibles, 'bici', 'bicis')} ·{' '}
											{plural(e.anclajesLibres, 'lugar libre', 'lugares libres')}
										</span>
									</button>
								</li>
							))}
						</ul>
					</section>
				</div>
			</div>
		</div>
	);
}
