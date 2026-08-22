import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../api/client';

export function RegisterPage() {
	const { register } = useAuth();
	const navigate = useNavigate();
	const [form, setForm] = useState({ nombre: '', apellido: '', email: '', password: '', dni: '', telefono: '' });
	const [error, setError] = useState<string | null>(null);
	const [enviando, setEnviando] = useState(false);

	function actualizar(campo: keyof typeof form, valor: string) {
		setForm((f) => ({ ...f, [campo]: valor }));
	}

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		setError(null);
		setEnviando(true);
		try {
			// El registro publico siempre crea rol CLIENTE: no hay forma de elegir rol desde el form.
			await register(form);
			navigate('/');
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo registrar');
		} finally {
			setEnviando(false);
		}
	}

	return (
		<div className="pantalla-centrada">
			<form className="tarjeta formulario" onSubmit={onSubmit}>
				<h1>Crear cuenta</h1>
				<label>
					Nombre
					<input value={form.nombre} onChange={(e) => actualizar('nombre', e.target.value)} required />
				</label>
				<label>
					Apellido
					<input value={form.apellido} onChange={(e) => actualizar('apellido', e.target.value)} required />
				</label>
				<label>
					Email
					<input type="email" value={form.email} onChange={(e) => actualizar('email', e.target.value)} required />
				</label>
				<label>
					Password
					<input
						type="password"
						minLength={8}
						value={form.password}
						onChange={(e) => actualizar('password', e.target.value)}
						required
					/>
				</label>
				<label>
					DNI (opcional)
					<input value={form.dni} onChange={(e) => actualizar('dni', e.target.value)} />
				</label>
				<label>
					Teléfono (opcional)
					<input value={form.telefono} onChange={(e) => actualizar('telefono', e.target.value)} />
				</label>
				{error && <p className="error">{error}</p>}
				<button type="submit" disabled={enviando}>
					{enviando ? 'Creando...' : 'Registrarme'}
				</button>
				<p>
					¿Ya tenés cuenta? <Link to="/login">Ingresá</Link>
				</p>
			</form>
		</div>
	);
}
