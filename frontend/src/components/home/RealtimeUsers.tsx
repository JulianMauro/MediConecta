import { useUsuariosActivos } from '../../hooks/useUsuariosActivos';
import { Odometer } from '../ui/Odometer';

/**
 * Cuántas personas están arriba de una bici ahora mismo en toda la red.
 * Se actualiza solo (polling): no hace falta refrescar la página para verlo cambiar.
 */
export function RealtimeUsers() {
	const { cantidad, error } = useUsuariosActivos();

	return (
		<div className="usuarios-activos" role="status">
			<span className="usuarios-activos-punto" aria-hidden="true" />
			{cantidad === null ? (
				<span className="usuarios-activos-texto">{error ?? 'Calculando…'}</span>
			) : (
				<span className="usuarios-activos-texto">
					<Odometer value={cantidad} /> {cantidad === 1 ? 'persona andando en bici ahora' : 'personas andando en bici ahora'}
				</span>
			)}
		</div>
	);
}
