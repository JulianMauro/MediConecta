import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ComparativaPlanes } from '../planes/ComparativaPlanes';
import type { useSuscripcion } from '../../hooks/useSuscripcion';
import type { SuscripcionResponse } from '../../types/dto';

/*
 * El historial se pagina en el cliente: el backend devuelve la lista completa y
 * son pocas filas por usuario. Si algun dia crece, esto se cambia por un endpoint
 * paginado sin tocar el resto del componente.
 */
const VISIBLES_INICIAL = 5;
const INCREMENTO = 10;

const ESTADO_LABEL: Record<SuscripcionResponse['estado'], string> = {
	ACTIVA: 'Activo',
	VENCIDA: 'Vencido',
	CANCELADA: 'Cancelado',
};

type Props = Pick<
	ReturnType<typeof useSuscripcion>,
	'membresias' | 'suscripciones' | 'planVigente' | 'mensaje' | 'error' | 'contratando' | 'contratar' | 'cancelar'
>;

export function SuscripcionSection({
	membresias,
	suscripciones,
	planVigente,
	mensaje,
	error,
	contratando,
	contratar,
	cancelar,
}: Props) {
	const [visibles, setVisibles] = useState(VISIBLES_INICIAL);

	/*
	 * Mas reciente primero. El backend las devuelve en orden de insercion, que es
	 * justo al reves de lo que se quiere leer: lo primero que interesa es la ultima
	 * suscripcion, no la primera que se contrato.
	 *
	 * Se copia con [...] antes de ordenar porque sort() muta el array, y este llega
	 * por props desde el hook: mutarlo seria tocar el estado de otro componente.
	 */
	const ordenadas = useMemo(
		() => [...suscripciones].sort((a, b) => new Date(b.fechaInicio).getTime() - new Date(a.fechaInicio).getTime()),
		[suscripciones],
	);

	/*
	 * membresiaId -> nombre. La suscripcion solo guarda el id, y '#3' no le dice
	 * nada a nadie. Se resuelve con la lista de membresias que ya llega por props,
	 * que incluye tambien las desactivadas: un plan dado de baja tiene que seguir
	 * mostrando su nombre en el historial de quien lo contrato.
	 */
	const nombrePlan = useMemo(() => {
		const porId = new Map(membresias.map((m) => [m.id, m.nombre]));
		// Si el plan no esta en la lista, el id es mejor que una celda vacia.
		return (id: number) => porId.get(id) ?? `Plan #${id}`;
	}, [membresias]);

	const enPantalla = ordenadas.slice(0, visibles);
	const restantes = ordenadas.length - enPantalla.length;

	/*
	 * seccion-ancha: ocupa las dos columnas de la grilla. Adentro va la comparativa
	 * de planes, que es una tabla de varias columnas con un ancho minimo; en media
	 * grilla no entra y aparece un scroll horizontal dentro de la tarjeta.
	 */
	return (
		<section className="tarjeta seccion-ancha">
			<h2>Suscripción</h2>
			<p className="ayuda">
				Sin un plan vigente no podés sacar una bici. Un plan te da minutos por viaje, una cantidad de viajes por día, y
				una espera mínima entre viaje y viaje — todo eso hasta que el plan vence.
			</p>

			<div aria-live="polite">
				{mensaje && <p className="aviso aviso-ok">{mensaje}</p>}
				{error && <p className="aviso aviso-alerta">{error}</p>}
			</div>

			{planVigente ? (
				<p className="aviso aviso-ok">
					Tenés un plan vigente hasta {planVigente.fechaFin ? new Date(planVigente.fechaFin).toLocaleString() : '-'}.
					Cancelalo si querés contratar otro distinto.
				</p>
			) : (
				<>
					<ComparativaPlanes membresias={membresias} onContratar={contratar} contratando={contratando} />
					<p className="ayuda">
						¿Querés verlos con más detalle? Mirá <Link to="/planes">planes y precios</Link>.
					</p>
				</>
			)}

			{suscripciones.length > 0 && (
				<div className="tabla-scroll">
					<table className="tabla">
						<caption>
							Historial de suscripciones — {enPantalla.length} de {ordenadas.length}, de la más reciente a la
							más vieja
						</caption>
						<thead>
							<tr>
								<th scope="col">Plan</th>
								<th scope="col">Estado</th>
								<th scope="col">Desde</th>
								<th scope="col">Hasta</th>
								<th scope="col">
									<span className="solo-lectores">Acciones</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{enPantalla.map((s) => (
								<tr key={s.id}>
									<th scope="row">{nombrePlan(s.membresiaId)}</th>
									<td>{ESTADO_LABEL[s.estado]}</td>
									<td>{new Date(s.fechaInicio).toLocaleDateString()}</td>
									<td>{s.fechaFin ? new Date(s.fechaFin).toLocaleString() : '-'}</td>
									<td>
										{s.estado === 'ACTIVA' && (
											<button type="button" className="boton-secundario boton-chico" onClick={() => cancelar(s.id)}>
												Cancelar
											</button>
										)}
									</td>
								</tr>
							))}
						</tbody>
					</table>
				</div>
			)}

			{restantes > 0 && (
				<button
					type="button"
					className="boton-secundario"
					onClick={() => setVisibles((n) => n + INCREMENTO)}
				>
					Mostrar {Math.min(INCREMENTO, restantes)} más
				</button>
			)}
		</section>
	);
}
