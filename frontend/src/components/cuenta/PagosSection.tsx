import { useEffect, useState } from 'react';
import { pagosApi } from '../../api/pagos';
import { ApiError } from '../../api/client';
import type { PagoResponse } from '../../types/dto';

export function PagosSection() {
	const [pagos, setPagos] = useState<PagoResponse[]>([]);
	const [error, setError] = useState<string | null>(null);

	function recargar() {
		pagosApi.listarPropios().then(setPagos);
	}

	useEffect(recargar, []);

	async function confirmar(id: number) {
		setError(null);
		try {
			// Simula la respuesta de la pasarela: en un sistema real esto lo dispara un webhook, no el usuario.
			await pagosApi.confirmar(id, { referenciaPasarela: `SIM-${Date.now()}` });
			recargar();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo confirmar el pago');
		}
	}

	async function rechazar(id: number) {
		setError(null);
		try {
			await pagosApi.rechazar(id);
			recargar();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo rechazar el pago');
		}
	}

	return (
		<section className="tarjeta">
			<h2>Pagos</h2>
			{error && <p className="error">{error}</p>}
			<table className="tabla">
				<thead>
					<tr>
						<th>Concepto</th>
						<th>Monto</th>
						<th>Estado</th>
						<th>Fecha</th>
						<th></th>
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
