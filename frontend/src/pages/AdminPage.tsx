import { useState } from 'react';
import { EstacionesSection } from '../components/admin/EstacionesSection';
import { AlmacenesSection } from '../components/admin/AlmacenesSection';
import { BicicletasSection } from '../components/admin/BicicletasSection';
import { MembresiasSection } from '../components/admin/MembresiasSection';
import { MovimientosSection } from '../components/admin/MovimientosSection';

export function AdminPage() {
	// Se incrementa para forzar un refetch en las secciones que dependen de listas de otras
	// (bicicletas necesita almacenes, movimientos necesita estaciones/almacenes/bicicletas).
	const [refrescarSenal, setRefrescarSenal] = useState(0);
	const notificarCambio = () => setRefrescarSenal((n) => n + 1);

	return (
		<div className="contenido">
			<h1>Panel de administración</h1>
			<div className="grilla">
				<EstacionesSection onCambio={notificarCambio} />
				<AlmacenesSection onCambio={notificarCambio} />
				<BicicletasSection refrescarSenal={refrescarSenal} />
				<MembresiasSection />
				<MovimientosSection refrescarSenal={refrescarSenal} onMovido={notificarCambio} />
			</div>
		</div>
	);
}
