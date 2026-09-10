# Desbloqueo de bicis por QR — plan de trabajo

Documento de arranque para implementar el escaneo de QR. Está escrito para que una
sesión nueva pueda tomarlo sin más contexto que el repo.

## Objetivo

Hoy el usuario desbloquea una bici **tipeando su código** (`BICI-001`) en el formulario
de la home. Queremos que además pueda **escanear un QR pegado en la bici**, que codifica
ese mismo código.

Dos entregables:

1. **Generadora de QR** para admins — permite fabricar los QR e imprimirlos. Sin esto no
   hay nada que escanear, así que va primero.
2. **Escáner** en el formulario de desbloqueo.

## Regla de oro: el backend NO se toca

El endpoint ya existe y ya hace todo:

```
POST /api/viajes/iniciar   { "codigoBicicleta": "BICI-001" }
```

`ViajeService.iniciar` valida cuenta bloqueada, viaje en curso, plan vigente, límite
diario, espera entre viajes y estado de la bici; después crea el `Viaje`, marca la bici
`EN_VIAJE`, libera el anclaje de origen e incrementa el contador de usuarios activos —
todo en una transacción.

**El QR es un método de captura del frontend, no un concepto del dominio.** El escáner
lee un string y ese string *ya es* el código de la bici: llena el mismo campo que hoy se
tipea a mano y llama al mismo endpoint. El backend nunca se entera de que hubo un QR.

Si en algún momento parece necesario tocar el backend, es señal de que el diseño se
desvió. No hay columnas nuevas, ni endpoints nuevos, ni DTOs nuevos.

## Decisiones ya tomadas (no reabrir)

| Decisión | Por qué |
|---|---|
| El QR va en la **bici**, no en el anclaje | El anclaje no tiene código global (solo `id` y un `numero` único por estación), así que esa variante pedía columna nueva, query nuevo y un resolver. Con el código de la bici el backend queda intacto. Además es como funciona Ecobici. |
| El QR codifica el **código pelado** (`BICI-001`), no una URL | Una URL en un sticker público habilita *quishing*: alguien pega una calcomanía con un dominio parecido y te manda a un login falso. Con el código pelado, un sticker adulterado como mucho desbloquea otra bici. El escaneo ocurre siempre dentro de la app. |
| Los QR **se generan, no se almacenan** | Un QR es la codificación de un string: guardarlo sería cachear una función pura. |
| Generación y lectura, **en el navegador** | Hacerlo en el backend pediría endpoint de imágenes, librería en Java y manejo de content-type, para una función pura de un string. |
| Librería de lectura: **`@zxing/browser`** | `BarcodeDetector` es nativo y sin dependencias, pero **no existe en Safari iOS ni en Firefox**. El uso real es un teléfono en la calle, así que no sirve. |
| El input manual **se queda** | Es el fallback cuando la cámara falla, el permiso está denegado o el sticker está rayado. No es solo para testear. |

## Patrones del frontend a seguir

`patrones.md` documenta el backend; esto es lo que hace falta acá.

**Página → hook → componentes de presentación.** La página arma el layout y el estado de
UI; un hook trae los datos; los componentes reciben props y no consultan la API. Antes de
escribir un hook nuevo, fijate si ya existe uno que sirva: `MapPage` reusa `useEstaciones`
justamente para no tener dos fuentes de verdad.

**Envoltorio imperativo aislado.** Cuando hay que manejar un objeto mutable ajeno a React
(la cámara, un canvas, el mapa de Google), va encerrado en un componente detrás de props
declarativas. Ver `components/mapa/MapaEstaciones.tsx` como precedente:

- el objeto vive en `useRef`, no en `useState` (no se dibuja en el JSX, y en estado
  dispararía renders inútiles);
- **un efecto por responsabilidad**, cada uno con su propia dependencia — si va todo en
  un efecto, cualquier cambio de props recrea el objeto entero;
- un efecto de limpieza que libera el recurso al desmontar.

Y `hooks/useGoogleMaps.ts` para cargar algo una sola vez: la promesa vive a nivel de
módulo, porque React en modo estricto monta cada componente dos veces.

**Registrar una sección de admin son tres lugares** (seguir tal cual):

1. `pages/AdminPage.tsx` → una entrada más en el array `SECCIONES` (`to`, `label`, `desc`)
2. `pages/admin/secciones.tsx` → un adaptador que toma del `useOutletContext` solo lo que
   la sección necesita (las secciones no conocen el router)
3. `App.tsx` → `<Route path="qr" element={<AdminQr />} />` dentro de `/admin`

La ruta `/admin` ya está envuelta en `<ProtectedRoute rolRequerido="ADMIN">`, así que la
restricción por rol sale gratis: **no agregar chequeos de rol propios**.

**Estilos.** Todo sale de los tokens de `styles/tokens.css`; nada de colores literales ni
de estilos inline. Clases que ya existen y hay que reusar: `.contenido`, `.tarjeta`,
`.campo`, `.fila`, `.ayuda`, `.aviso aviso-alerta`, `.boton-secundario`, `.tabla-scroll`.
Cualquier bloque que quede directamente sobre el fondo de la página necesita superficie
propia: el fondo es un patrón de mucho contraste y el texto encima es ilegible.

**Idioma y comentarios.** Nombres en español, como el resto del repo. Los comentarios
explican **por qué**, no qué hace la línea de al lado.

## Etapa 1 — Generadora de QR (admin)

1. `npm install qrcode` y `npm install -D @types/qrcode` en `frontend/`.
2. `components/admin/QrSection.tsx`:
   - campo manual para tipear un código (sirve para bicis que todavía no existen);
   - selector alimentado por `bicicletasApi.listar()` (`BicicletaResponse` ya trae
     `codigo`), para evitar errores de tipeo;
   - el QR se dibuja en un `<canvas>` con `qrcode`;
   - botón para imprimir una plancha con todas las bicis — es lo que hace útil la
     página: generás la hoja, la imprimís y tenés los stickers.
3. Registrarla en los tres lugares del panel (ver arriba).

Con esto ya se puede generar un QR real y escanearlo con la cámara del teléfono para
comprobar que devuelve `BICI-001`.

## Etapa 2 — Escáner en el desbloqueo

1. `npm install @zxing/browser`.
2. `components/home/QrScanner.tsx` — abre la cámara, lee, devuelve el string por callback.
   **Tiene que apagar el stream en la limpieza del efecto**: si no, la cámara del teléfono
   queda prendida después de cerrar el escáner.
3. Botón "Escanear" en `components/home/UnlockForm.tsx`, al lado del input. Al leer, llena
   el mismo estado `codigo` que ya existe y envía. El resto del formulario no cambia.
4. Permiso denegado o cámara no disponible: mensaje claro, no un error mudo.

## Cómo verificar

```bash
cd frontend && npx tsc --noEmit -p tsconfig.app.json
cd frontend && npx oxlint src
```

`oxlint` ya tiene warnings preexistentes en otros archivos (`set-state-in-effect`,
`purity`); lo que importa es no agregar nuevos.

Para ver la app: `docker compose up -d postgres` y levantar el backend y el front. Las
rutas `/` y `/admin` son protegidas, así que hace falta sesión iniciada para verlas.

## Trampas conocidas

- **`getUserMedia` exige HTTPS o `localhost`.** Desde el celular apuntando a la IP de la
  máquina por HTTP la cámara no arranca. Se resuelve con `vite --host` más un túnel
  HTTPS, o probando en el navegador de escritorio.
- **La cámara queda prendida** si el efecto no la detiene al desmontar.
- **`tsconfig.app.json` tiene un array `types` acotado.** Eso solo afecta a paquetes de
  tipos **globales** (como `google.maps`, que hubo que agregar a mano). `@types/qrcode`
  declara un módulo y se resuelve por el `import`: no hay que tocar nada.

## Lo que NO hay que hacer

- Tocar el backend.
- Guardar los QR generados.
- Codificar una URL en el QR.
- Sacar el input manual de código.
- Agregar chequeos de rol propios en la sección de admin.
- Escribir un hook nuevo para datos que ya trae uno existente.
