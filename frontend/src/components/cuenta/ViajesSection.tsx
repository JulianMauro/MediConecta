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
		<section className="tarjeta">
			<h2>Historial de viajes</h2>
			{/* La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta. */}
			<div className="tabla-scroll">
				<table className="tabla">
					<caption>Historial de tus viajes, del más reciente al más antiguo</caption>
					<thead>
						<tr>
							<th scope="col">Bici</th>
							<th scope="col">Inicio</th>
							<th scope="col">Duración</th>
							<th scope="col">Excedió</th>
						</tr>
					</thead>
					<tbody>
						{ordenados.map((v) => (
							<tr key={v.id}>
								<td>{v.codigoBicicleta}</td>
								<td>{new Date(v.fechaInicio).toLocaleString()}</td>
								<td>{v.fechaFin ? `${v.duracionMinutos} min` : 'en curso'}</td>
								<td className={v.excedioTiempo ? 'texto-alerta' : ''}>{v.excedioTiempo ? 'sí' : 'no'}</td>
							</tr>
						))}
						{viajes.length === 0 && (
							<tr>
								<td colSpan={4} className="tabla-vacia">
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
