import { useEffect, useState } from 'react';
import { bicicletasApi } from '../../api/bicicletas';
import { almacenesApi } from '../../api/almacenes';
import { ApiError } from '../../api/client';
import { Modal } from '../ui/Modal';
import { Field } from '../ui/Field';
import type { AlmacenResponse, BicicletaResponse } from '../../types/dto';

interface Props {
	refrescarSenal?: number;
}

const ESTADO_LABEL: Record<BicicletaResponse['estado'], string> = {
	DISPONIBLE: 'Disponible',
	EN_VIAJE: 'En viaje',
	DESACTIVADA: 'Desactivada',
	EN_REPARACION: 'En reparación',
};

export function BicicletasSection({ refrescarSenal }: Props) {
	const [bicicletas, setBicicletas] = useState<BicicletaResponse[]>([]);
	const [almacenes, setAlmacenes] = useState<AlmacenResponse[]>([]);
	const [modalAbierto, setModalAbierto] = useState(false);
	const [codigo, setCodigo] = useState('');
	const [almacenId, setAlmacenId] = useState<number | ''>('');
	const [error, setError] = useState<string | null>(null);
	const [guardando, setGuardando] = useState(false);

	function recargar() {
		bicicletasApi.listar().then(setBicicletas);
		almacenesApi.listar().then(setAlmacenes);
	}

	useEffect(recargar, [refrescarSenal]);

	function abrirCrear() {
		setCodigo('');
		setAlmacenId(almacenes[0]?.id ?? '');
		setError(null);
		setModalAbierto(true);
	}

	async function crear() {
		if (almacenId === '') return;
		setError(null);
		setGuardando(true);
		try {
			await bicicletasApi.crear({ codigo: codigo.trim(), almacenId });
			setModalAbierto(false);
			recargar();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo crear la bicicleta');
		} finally {
			setGuardando(false);
		}
	}

	return (
		<section className="tarjeta">
			<div className="tarjeta-encabezado">
				<div>
					<h2>Bicicletas</h2>
					<p className="ayuda">Se dan de alta en un almacén; para ponerlas en circulación usá "Mover bicis".</p>
				</div>
				<button onClick={abrirCrear} disabled={almacenes.length === 0}>
					+ Nueva bicicleta
				</button>
			</div>
			{almacenes.length === 0 && <p className="ayuda">Creá un almacén primero: toda bici nueva necesita uno.</p>}

			{/* La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta. */}
			<div className="tabla-scroll">
				<table className="tabla">
					<caption>Bicicletas de la flota, con su estado y ubicación actual</caption>
					<thead>
						<tr>
							<th scope="col">Código</th>
							<th scope="col">Estado</th>
							<th scope="col">Ubicación</th>
						</tr>
					</thead>
					<tbody>
						{bicicletas.map((b) => (
							<tr key={b.id}>
								<td>{b.codigo}</td>
								<td>
									<span className={`pill pill-${b.estado.toLowerCase()}`}>{ESTADO_LABEL[b.estado]}</span>
								</td>
								<td>{b.anclajeId ? `Anclaje #${b.anclajeId}` : b.almacenId ? `Almacén #${b.almacenId}` : '-'}</td>
							</tr>
						))}
						{bicicletas.length === 0 && (
							<tr>
								<td colSpan={3} className="ayuda">
									Todavía no hay bicicletas.
								</td>
							</tr>
						)}
					</tbody>
				</table>
			</div>

			{modalAbierto && (
				<Modal
					title="Nueva bicicleta"
					description='Toda bici nace "Desactivada" dentro de un almacén. Para que un cliente la pueda usar, después hay que moverla a un anclaje libre desde "Mover bicis".'
					onClose={() => setModalAbierto(false)}
					footer={
						<>
							<button className="boton-secundario" onClick={() => setModalAbierto(false)}>
								Cancelar
							</button>
							<button onClick={crear} disabled={!codigo || almacenId === '' || guardando}>
								{guardando ? 'Creando...' : 'Crear bicicleta'}
							</button>
						</>
					}
				>
					<Field label="Código" required helpText="El identificador fijo que el cliente va a escanear o tipear para desbloquearla.">
						<input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="ej: BICI-001" autoFocus />
					</Field>
					<Field label="Almacén" required helpText="Dónde queda guardada la bici hasta que un admin la despliegue.">
						<select value={almacenId} onChange={(e) => setAlmacenId(e.target.value ? Number(e.target.value) : '')}>
							<option value="">Elegí un almacén</option>
							{almacenes.map((a) => (
								<option key={a.id} value={a.id}>
									{a.nombre}
								</option>
							))}
						</select>
					</Field>
					{error && <p className="error">{error}</p>}
				</Modal>
			)}
		</section>
	);
}
