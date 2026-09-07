import { Link } from 'react-router-dom';
import { ComparativaPlanes } from '../planes/ComparativaPlanes';
import type { useSuscripcion } from '../../hooks/useSuscripcion';
import type { SuscripcionResponse } from '../../types/dto';

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

	return (
		<section className="tarjeta">
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
						<caption>Historial de suscripciones</caption>
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
							{suscripciones.map((s) => (
								<tr key={s.id}>
									<th scope="row">#{s.membresiaId}</th>
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
		</section>
	);
}
