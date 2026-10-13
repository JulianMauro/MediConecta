import { useCallback } from 'react';
import { SuscripcionSection } from '../components/cuenta/SuscripcionSection';
import { PagosSection } from '../components/cuenta/PagosSection';
import { StrikesSection } from '../components/cuenta/StrikesSection';
import { NotificacionesSection } from '../components/cuenta/NotificacionesSection';
import { ViajesSection } from '../components/cuenta/ViajesSection';
import { useSuscripcion } from '../hooks/useSuscripcion';
import { usePagos } from '../hooks/usePagos';
import { useStrikes } from '../hooks/useStrikes';
import { EstiloOpciones } from '../components/ui/EstiloToggle';
import { useAuth } from '../context/AuthContext';

/**
 * Atajos a cada seccion, solo en el celular. Ahi "Mi cuenta" son cinco tarjetas
 * apiladas y la que se busca (casi siempre un pago pendiente) queda varias
 * pantallas mas abajo. En escritorio la grilla ya las muestra de a dos y no hacen falta.
 */
const ATAJOS = [
	{ id: 'suscripcion', label: 'Plan' },
	{ id: 'pagos', label: 'Pagos' },
	{ id: 'strikes', label: 'Strikes' },
	{ id: 'notificaciones', label: 'Avisos' },
	{ id: 'viajes', label: 'Viajes' },
	{ id: 'ajustes', label: 'Ajustes' },
];

export function CuentaPage() {
	const { usuario, logout } = useAuth();
	const suscripcion = useSuscripcion();
	const pagos = usePagos();
	const strikes = useStrikes();

	// Contratar una suscripción genera un pago pendiente: recargamos "Pagos" para que aparezca.
	const contratar = useCallback(
		async (membresiaId: number) => {
			await suscripcion.contratar(membresiaId);
			await pagos.recargar();
		},
		[suscripcion, pagos],
	);

	// Confirmar/rechazar un pago puede activar o frenar la suscripción y saldar un strike
	// de tiempo extra: recargamos "Suscripción" y los strikes (tabla y contador).
	const confirmarPago = useCallback(
		async (id: number) => {
			await pagos.confirmar(id);
			await Promise.all([suscripcion.recargar(), strikes.recargar()]);
		},
		[pagos, suscripcion, strikes],
	);

	const rechazarPago = useCallback(
		async (id: number) => {
			await pagos.rechazar(id);
			await Promise.all([suscripcion.recargar(), strikes.recargar()]);
		},
		[pagos, suscripcion, strikes],
	);

	return (
		<div className="contenido">
			{/* Igual que el resto de las paginas: el titulo va dentro de una superficie.
			    Suelto cae sobre el fondo decorativo, que es de altisimo contraste, y el
			    gradiente del h1 se vuelve ilegible letra por letra. */}
			<header className="encabezado-pagina">
				<h1>Mi cuenta</h1>
				<p className="bajada">Tu plan, tus pagos, tus penalizaciones y el historial de viajes.</p>
			</header>
			<nav className="cuenta-atajos solo-movil" aria-label="Secciones de Mi cuenta">
				{ATAJOS.map((a) => (
					<a key={a.id} href={`#${a.id}`}>
						{a.label}
					</a>
				))}
			</nav>
			<div className="grilla">
				<SuscripcionSection
					membresias={suscripcion.membresias}
					suscripciones={suscripcion.suscripciones}
					planVigente={suscripcion.planVigente}
					mensaje={suscripcion.mensaje}
					error={suscripcion.error}
					contratando={suscripcion.contratando}
					contratar={contratar}
					cancelar={suscripcion.cancelar}
				/>
				<PagosSection
					pagos={pagos.pagos}
					error={pagos.error}
					confirmar={confirmarPago}
					rechazar={rechazarPago}
				/>
				<StrikesSection strikes={strikes.strikes} />
				<NotificacionesSection strikes={strikes.strikes} />
				<ViajesSection />
				{/*
				 * El estilo de la app y la sesion. Con sesion iniciada el panel de estilo ya
				 * no flota sobre el contenido (tapaba botones, como el de contratar el
				 * ultimo plan): vive aca, en celular y en escritorio. Sin sesion (login,
				 * registro) sigue flotando. En escritorio comparte la ultima fila con el
				 * historial de viajes.
				 */}
				<section id="ajustes" className="tarjeta ajustes">
					<h2>Ajustes</h2>
					<h3>Apariencia</h3>
					<p className="ayuda">El fondo y el estilo de las tarjetas. Se recuerda en este teléfono.</p>
					<EstiloOpciones />
					<h3>Sesión</h3>
					{usuario && (
						<p className="ayuda">
							Entraste como{' '}
							<strong>
								{usuario.nombre} {usuario.apellido}
							</strong>{' '}
							({usuario.email}).
						</p>
					)}
					<button type="button" className="boton-secundario" onClick={logout}>
						Cerrar sesión
					</button>
				</section>
			</div>
		</div>
	);
}
