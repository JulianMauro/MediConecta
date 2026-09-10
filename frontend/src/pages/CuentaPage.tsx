import { useCallback } from 'react';
import { SuscripcionSection } from '../components/cuenta/SuscripcionSection';
import { PagosSection } from '../components/cuenta/PagosSection';
import { StrikesSection } from '../components/cuenta/StrikesSection';
import { NotificacionesSection } from '../components/cuenta/NotificacionesSection';
import { ViajesSection } from '../components/cuenta/ViajesSection';
import { useSuscripcion } from '../hooks/useSuscripcion';
import { usePagos } from '../hooks/usePagos';

export function CuentaPage() {
	const suscripcion = useSuscripcion();
	const pagos = usePagos();

	// Contratar una suscripción genera un pago pendiente: recargamos "Pagos" para que aparezca.
	const contratar = useCallback(
		async (membresiaId: number) => {
			await suscripcion.contratar(membresiaId);
			await pagos.recargar();
		},
		[suscripcion, pagos],
	);

	// Confirmar/rechazar un pago puede activar o frenar la suscripción: recargamos "Suscripción".
	const confirmarPago = useCallback(
		async (id: number) => {
			await pagos.confirmar(id);
			await suscripcion.recargar();
		},
		[pagos, suscripcion],
	);

	const rechazarPago = useCallback(
		async (id: number) => {
			await pagos.rechazar(id);
			await suscripcion.recargar();
		},
		[pagos, suscripcion],
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
				<StrikesSection />
				<NotificacionesSection />
				<ViajesSection />
			</div>
		</div>
	);
}
