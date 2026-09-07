import { Link } from 'react-router-dom';
import { ComparativaPlanes } from '../components/planes/ComparativaPlanes';
import { useSuscripcion } from '../hooks/useSuscripcion';

export function PlanesPage() {
	const { membresias, planVigente, cargando, mensaje, error, contratando, contratar } = useSuscripcion();

	return (
		<div className="contenido">
			<header className="encabezado-pagina">
				<h1>Planes y precios</h1>
				<p className="bajada">
					Todos los planes te dejan sacar una bici de cualquier estación y devolverla en cualquier otra. Cambia cuántos
					minutos tenés por viaje, cuántos viajes por día, y qué pagás si te pasás del tiempo.
				</p>
			</header>

			<div aria-live="polite">
				{mensaje && <p className="aviso aviso-ok">{mensaje}</p>}
				{error && <p className="aviso aviso-alerta">{error}</p>}
			</div>

			{planVigente && (
				<p className="aviso aviso-ok">
					Ya tenés un plan vigente. Para cambiarlo, cancelá el actual desde <Link to="/cuenta">Mi cuenta</Link>.
				</p>
			)}

			{cargando ? (
				<p className="centrado">Cargando planes…</p>
			) : (
				<ComparativaPlanes
					membresias={membresias}
					onContratar={contratar}
					contratando={contratando}
					deshabilitado={Boolean(planVigente)}
				/>
			)}

			<section className="tarjeta bloque-explicativo">
				<h2>Cómo funciona</h2>
				<ol className="pasos">
					<li>
						<strong>Contratá un plan.</strong> Se genera un pago pendiente que confirmás desde Mi cuenta.
					</li>
					<li>
						<strong>Desbloqueá una bici.</strong> Con el código de la bici, desde Inicio.
					</li>
					<li>
						<strong>Devolvela en cualquier anclaje libre.</strong> Si te pasaste del tiempo incluido, se genera un
						cargo por minuto extra.
					</li>
				</ol>
			</section>
		</div>
	);
}
