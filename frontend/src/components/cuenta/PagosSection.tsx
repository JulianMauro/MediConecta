import type { PagoResponse } from '../../types/dto';

interface Props {
	pagos: PagoResponse[];
	error: string | null;
	confirmar: (id: number) => void;
	rechazar: (id: number) => void;
}

export function PagosSection({ pagos, error, confirmar, rechazar }: Props) {
	return (
		<section className="tarjeta">
			<h2>Pagos</h2>
			{error && <p className="error">{error}</p>}
			<table className="tabla">
				<caption>Tus pagos, pendientes y confirmados</caption>
				<thead>
					<tr>
						<th scope="col">Concepto</th>
						<th scope="col">Monto</th>
						<th scope="col">Estado</th>
						<th scope="col">Fecha</th>
						<th scope="col"><span className="solo-lectores">Acciones</span></th>
					</tr>
				</thead>
				<tbody>
					{pagos.map((p) => (
						<tr key={p.id}>
							<td>{p.concepto}</td>
							<td>${p.monto}</td>
							<td>{p.estado}</td>
							<td>{new Date(p.fechaCreacion).toLocaleString()}</td>
							<td>
								{p.estado === 'PENDIENTE' && (
									<>
										<button className="boton-chico" onClick={() => confirmar(p.id)}>
											Pagar
										</button>
										<button className="boton-chico boton-secundario" onClick={() => rechazar(p.id)}>
											Rechazar
										</button>
									</>
								)}
							</td>
						</tr>
					))}
				</tbody>
			</table>
		</section>
	);
}
