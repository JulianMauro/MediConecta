import { useEffect, useState } from 'react';
import { viajesApi } from '../../api/viajes';
import type { ViajeResponse } from '../../types/dto';

export function ViajesSection() {
	const [viajes, setViajes] = useState<ViajeResponse[]>([]);

	useEffect(() => {
		viajesApi.listarPropios().then(setViajes);
	}, []);

	return (
		<section className="tarjeta">
			<h2>Historial de viajes</h2>
			<table className="tabla">
				<caption>Historial de tus viajes</caption>
				<thead>
					<tr>
						<th scope="col">Bici</th>
						<th scope="col">Inicio</th>
						<th scope="col">Duración</th>
						<th scope="col">Excedió</th>
					</tr>
				</thead>
				<tbody>
					{viajes.map((v) => (
						<tr key={v.id}>
							<td>{v.codigoBicicleta}</td>
							<td>{new Date(v.fechaInicio).toLocaleString()}</td>
							<td>{v.fechaFin ? `${v.duracionMinutos} min` : 'en curso'}</td>
							<td className={v.excedioTiempo ? 'texto-alerta' : ''}>{v.excedioTiempo ? 'sí' : 'no'}</td>
						</tr>
					))}
				</tbody>
			</table>
		</section>
	);
}
