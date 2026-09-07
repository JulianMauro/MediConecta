import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Nav } from './components/Nav';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { HomePage } from './pages/HomePage';
import { CuentaPage } from './pages/CuentaPage';
import { PlanesPage } from './pages/PlanesPage';
import { EstacionesPage } from './pages/EstacionesPage';
import { AdminPage } from './pages/AdminPage';
import {
	AdminAlmacenes,
	AdminEstaciones,
	AdminFlota,
	AdminMembresias,
	AdminMovimientos,
} from './pages/admin/secciones';

export default function App() {
	return (
		<BrowserRouter>
			<AuthProvider>
				{/* Primer elemento tabulable de la pagina: salta la navegacion entera. */}
				<a className="salto-contenido" href="#contenido-principal">
					Saltar al contenido
				</a>
				<Nav />
				<main id="contenido-principal" tabIndex={-1}>
					<Routes>
						<Route path="/login" element={<LoginPage />} />
						<Route path="/register" element={<RegisterPage />} />
						<Route
							path="/"
							element={
								<ProtectedRoute>
									<HomePage />
								</ProtectedRoute>
							}
						/>
						<Route
							path="/estaciones"
							element={
								<ProtectedRoute>
									<EstacionesPage />
								</ProtectedRoute>
							}
						/>
						<Route
							path="/planes"
							element={
								<ProtectedRoute>
									<PlanesPage />
								</ProtectedRoute>
							}
						/>
						<Route
							path="/cuenta"
							element={
								<ProtectedRoute>
									<CuentaPage />
								</ProtectedRoute>
							}
						/>
						<Route
							path="/admin"
							element={
								<ProtectedRoute rolRequerido="ADMIN">
									<AdminPage />
								</ProtectedRoute>
							}
						>
							<Route index element={<Navigate to="estaciones" replace />} />
							<Route path="estaciones" element={<AdminEstaciones />} />
							<Route path="almacenes" element={<AdminAlmacenes />} />
							<Route path="flota" element={<AdminFlota />} />
							<Route path="movimientos" element={<AdminMovimientos />} />
							<Route path="membresias" element={<AdminMembresias />} />
						</Route>
						<Route path="*" element={<Navigate to="/" replace />} />
					</Routes>
				</main>
			</AuthProvider>
		</BrowserRouter>
	);
}
