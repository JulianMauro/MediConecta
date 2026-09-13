import { useCallback } from 'react';
import { SuscripcionSection } from '../components/cuenta/SuscripcionSection';
import { PagosSection } from '../components/cuenta/PagosSection';
import { StrikesSection } from '../components/cuenta/StrikesSection';
import { NotificacionesSection } from '../components/cuenta/NotificacionesSection';
import { ViajesSection } from '../components/cuenta/ViajesSection';
import { useSuscripcion } from '../hooks/useSuscripcion';
import { usePagos } from '../hooks/usePagos';
import { useStrikes } from '../hooks/useStrikes';

export function CuentaPage() {
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
			</div>
		</div>
	);
}
