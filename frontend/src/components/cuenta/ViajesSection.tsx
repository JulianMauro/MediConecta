import { useEffect, useMemo, useState } from 'react';
import { viajesApi } from '../../api/viajes';
import type { ViajeResponse } from '../../types/dto';

export function ViajesSection() {
	const [viajes, setViajes] = useState<ViajeResponse[]>([]);

	useEffect(() => {
		viajesApi.listarPropios().then(setViajes);
	}, []);

	/*
	 * Mas reciente primero: el backend los devuelve en orden de insercion, que es
	 * al reves de como se lee un historial. Se copia con [...] antes de ordenar
	 * porque sort() muta, y viajes es estado del componente.
	 */
	const ordenados = useMemo(
		() => [...viajes].sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime()),
		[viajes],
	);

	return (
		<section id="viajes" className="tarjeta">
			<h2>Historial de viajes</h2>
			{/*
			 * La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta.
			 *
			 * tabla-apilable: ver PagosSection.
			 */}
			<div className="tabla-scroll">
				<table className="tabla tabla-apilable" role="table">
					<caption>Historial de tus viajes, del más reciente al más antiguo</caption>
					<thead role="rowgroup">
						<tr role="row">
							<th scope="col" role="columnheader">Bici</th>
							<th scope="col" role="columnheader">Inicio</th>
							<th scope="col" role="columnheader">Duración</th>
							<th scope="col" role="columnheader">Excedió</th>
						</tr>
					</thead>
					<tbody role="rowgroup">
						{ordenados.map((v) => (
							<tr role="row" key={v.id}>
								<td role="cell" data-label="Bici">{v.codigoBicicleta}</td>
								<td role="cell" data-label="Inicio">{new Date(v.fechaInicio).toLocaleString()}</td>
								<td role="cell" data-label="Duración">{v.fechaFin ? `${v.duracionMinutos} min` : 'en curso'}</td>
								<td role="cell" data-label="Excedió" className={v.excedioTiempo ? 'texto-alerta' : ''}>
									{v.excedioTiempo ? 'sí' : 'no'}
								</td>
							</tr>
						))}
						{viajes.length === 0 && (
							<tr role="row">
								<td role="cell" colSpan={4} className="tabla-vacia">
									Todavía no hiciste ningún viaje. Desbloqueá una bici desde Inicio para empezar el primero.
								</td>
							</tr>
						)}
					</tbody>
				</table>
			</div>
		</section>
	);
}
