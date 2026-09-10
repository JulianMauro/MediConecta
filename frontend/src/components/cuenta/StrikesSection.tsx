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
			{/* La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta. */}
			<div className="tabla-scroll">
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
						{strikes.length === 0 && (
							<tr>
								<td colSpan={3} className="tabla-vacia">
									No tenés penalizaciones. Se genera una cuando devolvés la bici fuera del tiempo que incluye tu plan.
								</td>
							</tr>
						)}
					</tbody>
				</table>
			</div>
		</section>
	);
}
