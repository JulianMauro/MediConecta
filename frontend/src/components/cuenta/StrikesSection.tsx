import { useEffect, useState } from 'react';
import { strikesApi } from '../../api/strikes';
import type { StrikeResponse } from '../../types/dto';

export function StrikesSection() {
	const [strikes, setStrikes] = useState<StrikeResponse[]>([]);

	useEffect(() => {
		strikesApi.listarPropios().then(setStrikes);
	}, []);

	return (
		<section className="tarjeta">
			<h2>Strikes</h2>
			<table className="tabla">
				<caption>Penalizaciones registradas en tu cuenta</caption>
				<thead>
					<tr>
						<th scope="col">Viaje</th>
						<th scope="col">Estado</th>
						<th scope="col">Fecha</th>
					</tr>
				</thead>
				<tbody>
					{strikes.map((s) => (
						<tr key={s.id}>
							<td>#{s.viajeId}</td>
							<td className={s.estado === 'ACTIVO' ? 'texto-alerta' : ''}>{s.estado}</td>
							<td>{new Date(s.fechaGeneracion).toLocaleString()}</td>
						</tr>
					))}
				</tbody>
			</table>
		</section>
	);
}
