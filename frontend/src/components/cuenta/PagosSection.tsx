import type { PagoResponse } from '../../types/dto';

interface Props {
	pagos: PagoResponse[];
	error: string | null;
	confirmar: (id: number) => void;
	rechazar: (id: number) => void;
}

export function PagosSection({ pagos, error, confirmar, rechazar }: Props) {
	return (
		<section id="pagos" className="tarjeta">
			<h2>Pagos</h2>
			{error && <p className="error">{error}</p>}
			{/*
			 * La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta.
			 *
			 * tabla-apilable: en el celular cada fila pasa a ser un bloque con sus datos
			 * uno debajo del otro, rotulados con data-label. Los role explicitos no estan
			 * de adorno: Safari deja de anunciar como tabla una <table> a la que el CSS le
			 * cambia el display, y con ellos VoiceOver la sigue leyendo como tal.
			 */}
			<div className="tabla-scroll">
				<table className="tabla tabla-apilable" role="table">
					<caption>Tus pagos, pendientes y confirmados</caption>
					<thead role="rowgroup">
						<tr role="row">
							<th scope="col" role="columnheader">Concepto</th>
							<th scope="col" role="columnheader">Monto</th>
							<th scope="col" role="columnheader">Estado</th>
							<th scope="col" role="columnheader">Fecha</th>
							<th scope="col" role="columnheader"><span className="solo-lectores">Acciones</span></th>
						</tr>
					</thead>
					<tbody role="rowgroup">
						{pagos.map((p) => (
							<tr role="row" key={p.id}>
								<td role="cell" data-label="Concepto">{p.concepto}</td>
								<td role="cell" data-label="Monto">${p.monto}</td>
								<td role="cell" data-label="Estado">{p.estado}</td>
								<td role="cell" data-label="Fecha">{new Date(p.fechaCreacion).toLocaleString()}</td>
								<td role="cell" className="tabla-acciones">
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
						{pagos.length === 0 && (
							<tr role="row">
								<td role="cell" colSpan={5} className="tabla-vacia">
									Todavía no tenés pagos. Se genera uno cuando contratás un plan, o cuando te pasás del tiempo incluido en un viaje.
								</td>
							</tr>
						)}
					</tbody>
				</table>
			</div>
		</section>
	);
}
