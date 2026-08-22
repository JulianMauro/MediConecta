import { useEffect, useState } from 'react';
import { almacenesApi } from '../../api/almacenes';
import { ApiError } from '../../api/client';
import { Modal } from '../ui/Modal';
import { Field } from '../ui/Field';
import type { AlmacenResponse } from '../../types/dto';

const FORM_VACIO = { nombre: '', direccion: '', capacidad: 50 };

interface Props {
	onCambio?: () => void;
}

export function AlmacenesSection({ onCambio }: Props) {
	const [almacenes, setAlmacenes] = useState<AlmacenResponse[]>([]);
	const [modalAbierto, setModalAbierto] = useState(false);
	const [form, setForm] = useState(FORM_VACIO);
	const [error, setError] = useState<string | null>(null);
	const [guardando, setGuardando] = useState(false);

	function recargar() {
		almacenesApi.listar().then(setAlmacenes);
	}

	useEffect(recargar, []);

	function abrirCrear() {
		setForm(FORM_VACIO);
		setError(null);
		setModalAbierto(true);
	}

	async function crear() {
		setError(null);
		setGuardando(true);
		try {
			await almacenesApi.crear(form);
			setModalAbierto(false);
			recargar();
			onCambio?.();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo crear el almacén');
		} finally {
			setGuardando(false);
		}
	}

	return (
		<section className="tarjeta">
			<div className="tarjeta-encabezado">
				<div>
					<h2>Almacenes</h2>
					<p className="ayuda">Dónde se guardan y reparan las bicis que no están en circulación.</p>
				</div>
				<button onClick={abrirCrear}>+ Nuevo almacén</button>
			</div>

			<ul className="lista-simple">
				{almacenes.map((a) => (
					<li key={a.id}>
						<strong>{a.nombre}</strong> — {a.direccion} <span className="pill">{a.capacidad} lugares</span>
					</li>
				))}
				{almacenes.length === 0 && <li className="ayuda">Todavía no creaste ningún almacén.</li>}
			</ul>

			{modalAbierto && (
				<Modal
					title="Nuevo almacén"
					description="Depósito administrado solo por admins: acá entran las bicis nuevas y las que se retiran de circulación para reparar."
					onClose={() => setModalAbierto(false)}
					footer={
						<>
							<button className="boton-secundario" onClick={() => setModalAbierto(false)}>
								Cancelar
							</button>
							<button onClick={crear} disabled={!form.nombre || !form.direccion || guardando}>
								{guardando ? 'Creando...' : 'Crear almacén'}
							</button>
						</>
					}
				>
					<Field label="Nombre" required helpText="Cómo se va a identificar este almacén.">
						<input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} autoFocus />
					</Field>
					<Field label="Dirección" required helpText="Ubicación física del depósito.">
						<input value={form.direccion} onChange={(e) => setForm({ ...form, direccion: e.target.value })} />
					</Field>
					<Field label="Capacidad" required helpText="Cantidad máxima de bicis que entran en este almacén.">
						<input
							type="number"
							min={1}
							value={form.capacidad}
							onChange={(e) => setForm({ ...form, capacidad: Number(e.target.value) })}
						/>
					</Field>
					{error && <p className="error">{error}</p>}
				</Modal>
			)}
		</section>
	);
}
