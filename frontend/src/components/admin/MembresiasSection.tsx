import { useEffect, useState } from 'react';
import { membresiasApi } from '../../api/membresias';
import { ApiError } from '../../api/client';
import { Modal } from '../ui/Modal';
import { Field } from '../ui/Field';
import type { MembresiaResponse, TipoMembresia } from '../../types/dto';

const FORM_VACIO = {
	nombre: '',
	tipo: 'MENSUAL' as TipoMembresia,
	tiempoPermitidoMinutos: 30,
	precio: 1000,
	tarifaMinutoExtra: 50,
	viajesPorDia: 3,
	tiempoEsperaMinutos: 30,
};

const DURACION_LABEL: Record<TipoMembresia, string> = {
	INDIVIDUAL: '1 día',
	SEMANAL: '7 días',
	MENSUAL: '30 días',
};

export function MembresiasSection() {
	const [membresias, setMembresias] = useState<MembresiaResponse[]>([]);
	const [modalAbierto, setModalAbierto] = useState(false);
	const [form, setForm] = useState(FORM_VACIO);
	const [error, setError] = useState<string | null>(null);
	const [guardando, setGuardando] = useState(false);

	function recargar() {
		membresiasApi.listar().then(setMembresias);
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
			await membresiasApi.crear(form);
			setModalAbierto(false);
			recargar();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo crear la membresía');
		} finally {
			setGuardando(false);
		}
	}

	async function desactivar(id: number) {
		await membresiasApi.desactivar(id);
		recargar();
	}

	return (
		<section className="tarjeta">
			<div className="tarjeta-encabezado">
				<div>
					<h2>Membresías</h2>
					<p className="ayuda">
						Todo plan hay que contratarlo y pagarlo antes de poder sacar una bici — no existe una tarifa
						automática. La duración depende del tipo: Individual dura 1 día, Semanal 7 días, Mensual 30 días.
					</p>
				</div>
				<button onClick={abrirCrear}>+ Nueva membresía</button>
			</div>

			{/* La tabla scrollea dentro de su caja: sin esto se desborda de la tarjeta. */}
			<div className="tabla-scroll">
				<table className="tabla">
					<caption>Planes de membresía configurados en el sistema</caption>
					<thead>
						<tr>
							<th scope="col">Nombre</th>
							<th scope="col">Tipo</th>
							<th scope="col">Dura</th>
							<th scope="col">Min./viaje</th>
							<th scope="col">Viajes/día</th>
							<th scope="col">Espera</th>
							<th scope="col">Precio</th>
							<th scope="col">$/min extra</th>
							<th scope="col">Activa</th>
							<th scope="col"><span className="solo-lectores">Acciones</span></th>
						</tr>
					</thead>
					<tbody>
						{membresias.map((m) => (
							<tr key={m.id}>
								<td>{m.nombre}</td>
								<td>{m.tipo}</td>
								<td>{m.duracionDias} día(s)</td>
								<td>{m.tiempoPermitidoMinutos}</td>
								<td>{m.viajesPorDia}</td>
								<td>{m.tiempoEsperaMinutos} min</td>
								<td>${m.precio}</td>
								<td>${m.tarifaMinutoExtra}</td>
								<td>{m.activa ? 'sí' : 'no'}</td>
								<td>
									{m.activa && (
										<button className="boton-chico" onClick={() => desactivar(m.id)}>
											Desactivar
										</button>
									)}
								</td>
							</tr>
						))}
						{membresias.length === 0 && (
							<tr>
								<td colSpan={10} className="ayuda">
									Todavía no hay membresías.
								</td>
							</tr>
						)}
					</tbody>
				</table>
			</div>

			{modalAbierto && (
				<Modal
					title="Nueva membresía"
					description="Un plan que el cliente contrata y paga por adelantado. Mientras esté vigente, define cuántos viajes puede hacer por día, cuánto dura cada viaje y cuánto tiene que esperar entre uno y el siguiente."
					onClose={() => setModalAbierto(false)}
					footer={
						<>
							<button className="boton-secundario" onClick={() => setModalAbierto(false)}>
								Cancelar
							</button>
							<button onClick={crear} disabled={!form.nombre || guardando}>
								{guardando ? 'Creando...' : 'Crear membresía'}
							</button>
						</>
					}
				>
					<Field label="Nombre" required helpText="Cómo lo va a ver el cliente al elegir un plan.">
						<input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} autoFocus />
					</Field>
					<Field label="Duración del plan" required helpText={`Una vez contratado y pagado, el plan queda vigente ${DURACION_LABEL[form.tipo]} desde el momento del pago.`}>
						<select value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value as TipoMembresia })}>
							<option value="INDIVIDUAL">Individual (1 día)</option>
							<option value="SEMANAL">Semanal (7 días)</option>
							<option value="MENSUAL">Mensual (30 días)</option>
						</select>
					</Field>
					<div className="formulario-2col">
						<Field label="Minutos por viaje" required helpText="Tiempo máximo de cada viaje antes de considerarse tiempo excedido.">
							<input
								type="number"
								min={1}
								value={form.tiempoPermitidoMinutos}
								onChange={(e) => setForm({ ...form, tiempoPermitidoMinutos: Number(e.target.value) })}
							/>
						</Field>
						<Field label="Viajes por día" required helpText="Cuántas veces por día puede sacar una bici mientras el plan esté vigente.">
							<input
								type="number"
								min={1}
								value={form.viajesPorDia}
								onChange={(e) => setForm({ ...form, viajesPorDia: Number(e.target.value) })}
							/>
						</Field>
					</div>
					<Field
						label="Espera entre viajes (minutos)"
						required
						helpText="Cuánto tiempo tiene que pasar desde que devuelve una bici hasta que puede sacar otra. Poné 0 si no querés exigir espera."
					>
						<input
							type="number"
							min={0}
							value={form.tiempoEsperaMinutos}
							onChange={(e) => setForm({ ...form, tiempoEsperaMinutos: Number(e.target.value) })}
						/>
					</Field>
					<div className="formulario-2col">
						<Field label="Precio" required helpText="Lo que paga el cliente al contratar el plan.">
							<input
								type="number"
								min={0}
								value={form.precio}
								onChange={(e) => setForm({ ...form, precio: Number(e.target.value) })}
							/>
						</Field>
						<Field label="$/min extra" required helpText="Cuánto se cobra por cada minuto que un viaje se pasa del límite.">
							<input
								type="number"
								min={0}
								value={form.tarifaMinutoExtra}
								onChange={(e) => setForm({ ...form, tarifaMinutoExtra: Number(e.target.value) })}
							/>
						</Field>
					</div>
					{error && <p className="error">{error}</p>}
				</Modal>
			)}
		</section>
	);
}
