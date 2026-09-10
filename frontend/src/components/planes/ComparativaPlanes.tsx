import type { MembresiaResponse } from '../../types/dto';

interface Props {
	membresias: MembresiaResponse[];
	/** Si no se pasa, la tabla es solo informativa (no muestra la fila de acciones). */
	onContratar?: (membresiaId: number) => void;
	contratando?: number | null;
	deshabilitado?: boolean;
}

/** Cada fila es un atributo comparable entre planes, no un plan. */
const ATRIBUTOS: { etiqueta: string; valor: (m: MembresiaResponse) => string }[] = [
	{ etiqueta: 'Precio', valor: (m) => `$${m.precio}` },
	{ etiqueta: 'Duración', valor: (m) => `${m.duracionDias} día${m.duracionDias === 1 ? '' : 's'}` },
	{ etiqueta: 'Minutos por viaje', valor: (m) => `${m.tiempoPermitidoMinutos} min` },
	{ etiqueta: 'Viajes por día', valor: (m) => `${m.viajesPorDia}` },
	{
		etiqueta: 'Espera entre viajes',
		valor: (m) => (m.tiempoEsperaMinutos > 0 ? `${m.tiempoEsperaMinutos} min` : 'Sin espera'),
	},
	{ etiqueta: 'Minuto extra', valor: (m) => `$${m.tarifaMinutoExtra}` },
];

/**
 * Comparativa de planes en tabla, no en grilla de tarjetas: alinea el mismo atributo
 * en la misma fila para todos los planes, que es lo que la persona necesita comparar.
 * Estructura tomada de la pagina de planes de Citi Bike.
 */
export function ComparativaPlanes({ membresias, onContratar, contratando, deshabilitado }: Props) {
	if (membresias.length === 0) {
		return <p className="ayuda">Todavía no hay planes disponibles para contratar — hablalo con un admin.</p>;
	}

	/*
	 * La tabla va dentro de una .tarjeta y no suelta sobre la pagina: el fondo del
	 * body es un patron de mucho contraste y el texto encima queda ilegible. Es la
	 * misma regla que ya siguen .encabezado-pagina y SuscripcionSection.
	 */
	return (
		<section className="tarjeta">
			<div className="tabla-scroll">
				<table className="tabla tabla-planes">
					<caption>
						Comparación de los {membresias.length} planes disponibles. Cada fila es una característica; cada columna, un plan.
					</caption>
					<thead>
						<tr>
							<th scope="col">
								<span className="solo-lectores">Característica</span>
							</th>
							{membresias.map((m) => (
								<th key={m.id} scope="col" className="plan-columna">
									{m.nombre}
									<span className="plan-precio-th">${m.precio}</span>
								</th>
							))}
						</tr>
					</thead>
					<tbody>
						{ATRIBUTOS.map((attr) => (
							<tr key={attr.etiqueta}>
								<th scope="row">{attr.etiqueta}</th>
								{membresias.map((m) => (
									<td key={m.id}>{attr.valor(m)}</td>
								))}
							</tr>
						))}
					</tbody>
					{onContratar && (
						<tfoot>
							<tr>
								<th scope="row">
									<span className="solo-lectores">Contratar</span>
								</th>
								{membresias.map((m) => (
									<td key={m.id}>
										<button
											type="button"
											onClick={() => onContratar(m.id)}
											disabled={deshabilitado || contratando === m.id}
										>
											{contratando === m.id ? 'Generando pago…' : `Contratar ${m.nombre}`}
										</button>
									</td>
								))}
							</tr>
						</tfoot>
					)}
				</table>
			</div>
		</section>
	);
}
