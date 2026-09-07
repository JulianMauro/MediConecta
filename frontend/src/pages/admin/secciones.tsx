import { useOutletContext } from 'react-router-dom';
import type { AdminContexto } from '../AdminPage';
import { EstacionesSection } from '../../components/admin/EstacionesSection';
import { AlmacenesSection } from '../../components/admin/AlmacenesSection';
import { BicicletasSection } from '../../components/admin/BicicletasSection';
import { MembresiasSection } from '../../components/admin/MembresiasSection';
import { MovimientosSection } from '../../components/admin/MovimientosSection';

/**
 * Adaptadores entre las rutas del panel y las secciones, que no conocen el router.
 * Cada uno toma del contexto solo lo que su seccion necesita.
 */

export function AdminEstaciones() {
	const { notificarCambio } = useOutletContext<AdminContexto>();
	return <EstacionesSection onCambio={notificarCambio} />;
}

export function AdminAlmacenes() {
	const { notificarCambio } = useOutletContext<AdminContexto>();
	return <AlmacenesSection onCambio={notificarCambio} />;
}

export function AdminFlota() {
	const { refrescarSenal } = useOutletContext<AdminContexto>();
	return <BicicletasSection refrescarSenal={refrescarSenal} />;
}

export function AdminMovimientos() {
	const { refrescarSenal, notificarCambio } = useOutletContext<AdminContexto>();
	return <MovimientosSection refrescarSenal={refrescarSenal} onMovido={notificarCambio} />;
}

export function AdminMembresias() {
	return <MembresiasSection />;
}
