import type { StrikeResponse } from '../../types/dto';

interface Props {
	strikes: StrikeResponse[];
}

export function StrikesSection({ strikes }: Props) {
	return (
		<section id="strikes" className="tarjeta">
			<h2>Strikes</h2>
			{/*
			 * La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta.
			 *
			 * tabla-apilable: ver PagosSection.
			 */}
			<div className="tabla-scroll">
				<table className="tabla tabla-apilable" role="table">
					<caption>Penalizaciones registradas en tu cuenta</caption>
					<thead role="rowgroup">
						<tr role="row">
							<th scope="col" role="columnheader">Viaje</th>
							<th scope="col" role="columnheader">Estado</th>
							<th scope="col" role="columnheader">Fecha</th>
						</tr>
					</thead>
					<tbody role="rowgroup">
						{strikes.map((s) => (
							<tr role="row" key={s.id}>
								<td role="cell" data-label="Viaje">#{s.viajeId}</td>
								<td role="cell" data-label="Estado" className={s.estado === 'ACTIVO' ? 'texto-alerta' : ''}>
									{s.estado}
								</td>
								<td role="cell" data-label="Fecha">{new Date(s.fechaGeneracion).toLocaleString()}</td>
							</tr>
						))}
						{strikes.length === 0 && (
							<tr role="row">
								<td role="cell" colSpan={3} className="tabla-vacia">
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
