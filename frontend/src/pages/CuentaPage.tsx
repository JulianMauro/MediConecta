import { SuscripcionSection } from '../components/cuenta/SuscripcionSection';
import { PagosSection } from '../components/cuenta/PagosSection';
import { StrikesSection } from '../components/cuenta/StrikesSection';
import { NotificacionesSection } from '../components/cuenta/NotificacionesSection';
import { ViajesSection } from '../components/cuenta/ViajesSection';

export function CuentaPage() {
	return (
		<div className="contenido">
			<h1>Mi cuenta</h1>
			<div className="grilla">
				<SuscripcionSection />
				<PagosSection />
				<StrikesSection />
				<NotificacionesSection />
				<ViajesSection />
			</div>
		</div>
	);
}
