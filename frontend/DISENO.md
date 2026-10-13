# Sistema de diseño — PeopleBikes

## Paleta

Cuatro colores de marca. Todo lo demás se deriva de ellos.

| Token | Hex | Rol |
|---|---|---|
| `--marca-100` | `#F3E8FF` | Superficie de acento: fondos de sección, avisos, chips seleccionados, hover de filas |
| `--marca-500` | `#A855F7` | **Decorativo únicamente**: bordes, anillo de foco, barras, íconos grandes, gráficos |
| `--marca-800` | `#6B21A8` | Interactivo: fondo de botón primario, links, texto enfatizado de marca |
| `--tinta-900` | `#1F2937` | Texto principal y superficie oscura en modo oscuro |

## Contrastes medidos (WCAG 2.1)

| Combinación | Ratio | Veredicto |
|---|---|---|
| `#1F2937` sobre blanco | 14.68:1 | AAA — texto base |
| `#1F2937` sobre `#F3E8FF` | 12.44:1 | AAA — texto sobre superficie de acento |
| `#6B21A8` sobre blanco | 8.72:1 | AAA — links y texto de marca |
| blanco sobre `#6B21A8` | 8.72:1 | AAA — botón primario |
| `#6B21A8` sobre `#F3E8FF` | 7.39:1 | AAA — link dentro de aviso |
| **`#A855F7` sobre blanco** | **3.96:1** | **Falla AA para texto.** Solo bordes, íconos y foco (mínimo 3:1 para componentes, lo cumple) |
| **blanco sobre `#A855F7`** | **3.96:1** | **Falla AA.** Nunca botón primario con texto blanco |
| `#A855F7` sobre `#1F2937` | 3.71:1 | Solo UI, no texto |
| `#F3E8FF` sobre `#1F2937` | 12.44:1 | AAA — texto en modo oscuro |
| `#D8B4FE` sobre `#1F2937` | 8.30:1 | AAA — acento interactivo en modo oscuro |

**Regla dura: `#A855F7` nunca lleva ni recibe texto.** Cuando hace falta púrpura con texto encima, es `#6B21A8`.

## Derivados permitidos

Tintes de la misma familia, no colores nuevos:

- `--marca-50 #FAF5FF`, `--marca-200 #E9D5FF`, `--marca-300 #D8B4FE` (acento en modo oscuro), `--marca-600 #9333EA` (hover de botón primario)
- Neutros derivados de la tinta: `--tinta-700 #374151`, `--tinta-500 #4B5563`, `--tinta-400 #6B7280` (texto secundario, 4.83:1 sobre blanco — el mínimo aceptable)

## Colores funcionales

Fuera de la paleta de marca porque comunican estado, no identidad. Se usan **solo** para éxito y error, nunca decorativos.

| Rol | Texto | Fondo | Contraste |
|---|---|---|---|
| Éxito | `#166534` | `#DCFCE7` | 7.4:1 |
| Error | `#B91C1C` | `#FEE2E2` | 6.4:1 |
| Advertencia | `#854D0E` | `#FEF9C3` | 7.9:1 |

## Reglas de uso

1. **Nunca color solo.** Todo estado (anclaje libre/ocupado/fuera de servicio, viaje excedido, notificación no leída) lleva además texto o forma. WCAG 1.4.1.
2. **Nunca `opacity` para bajar contraste.** Se usa el token `--texto-suave`, que tiene ratio garantizado.
3. **Foco siempre visible**: anillo de 2px `--marca-500` con 2px de separación. Nunca `outline: none` sin reemplazo.
4. **Jerarquía por tamaño y peso, no por color.** El texto secundario baja de tamaño, no se aclara hasta desaparecer.
5. **Targets de 44×44px mínimo** en el modo bici y en la navegación. En la calle, con una mano.

## Tipografía

Base **17px** (punto medio entre los 16px de Citi Bike y los 18px de Bicing, que es sector público con obligación de accesibilidad). Escala:

| Nivel | Tamaño / peso | Uso |
|---|---|---|
| Display | 48px / 700 | Hero de la landing |
| H1 | 32px / 700 | Título de página |
| H2 | 22px / 600 | Título de tarjeta o sección |
| H3 | 17px / 600 | Subtítulo |
| Cuerpo | 17px / 400 | Texto general |
| Chico | 15px / 400 | Ayudas, metadatos de tabla |
| Micro | 13px / 500 | Chips y pills |
| Contador | 64px / 700 tabular | Cronómetro del viaje activo |

Todo en `rem` para que respete el tamaño de fuente del navegador.

## Referencias

Estructura y decisiones tomadas de Citi Bike (Lyft Core UI), Bicing Barcelona y Ecobici Buenos Aires — ver el análisis de benchmark. De Lyft se copia el esquema de tokens con par claro/oscuro y la escala de elevación; de Bicing, la tipografía grande de base; de los tres, el skip link.

## Estructura de la aplicación

| Ruta | Qué es |
|---|---|
| `/` | Inicio: desbloqueo de bici, o modo bici si hay viaje en curso |
| `/estaciones` | Disponibilidad de la red: bicis para sacar y lugares para devolver, por estación |
| `/planes` | Comparativa de membresías en tabla, con contratación |
| `/cuenta` | Suscripción, pagos, strikes, notificaciones e historial de viajes |
| `/admin/:seccion` | Panel con sidebar: estaciones, almacenes, flota, movimientos, membresías |

El panel de administración es una ruta por sección en vez de las cinco apiladas: solo se monta y consulta la que está visible.

### Capas

- `styles/tokens.css` — primitivos y semánticos. Ningún componente usa un color literal.
- `hooks/` — estado y acciones de un dominio (`useSuscripcion`, `useEstaciones`). La lógica no se duplica entre pantallas que muestran lo mismo de distinta forma.
- `api/` — un archivo por módulo del backend, sobre el `client.ts` que maneja token y refresh.
- `components/` — presentación, agrupada por dominio.

## Decisiones de accesibilidad implementadas

- **Skip link** como primer elemento tabulable, más landmark `<main>`.
- **Foco visible** único para todo lo enfocable, con anillo de `--foco` y separación de 2px.
- **Modal**: foco atrapado mientras está abierto, respeta el `autoFocus` del contenido si lo hay, y devuelve el foco al elemento que lo abrió. El disparador se captura en el render, no en el efecto: para cuando corren los efectos, un campo con `autoFocus` ya se llevó el foco.
- **Contador de viaje**: el número visual está oculto para lectores de pantalla y hay una región viva aparte que solo cambia cuando cambia el minuto. Anunciar un valor por segundo haría la página inusable.
- **Tablas** con `<caption>` y `scope` en cada cabecera.
- **Estados nunca solo por color**: los chips de anclaje suman símbolo, las notificaciones no leídas suman punto, y las cifras en cero de una estación llevan además un aviso en texto.
- **Targets de 44px** en botones, campos y navegación; 56px en el modo bici.

## Celular (hasta 720px)

La escena de calle de PRODUCT.md: de pie, con una mano y a veces con sol directo. No es el escritorio achicado; las decisiones salen de cómo lo resuelven las apps de bici compartida que ya existen (Citi Bike, BA Ecobici, Bicing, Lime). Todo vive dentro de `@media (max-width: 720px)` o en elementos que en escritorio tienen `display: none`: lo que se haga acá no toca el escritorio, y al revés (ver la sección siguiente).

| Pieza | Qué hace en el celular | Por qué |
|---|---|---|
| Barra de pestañas | Fija abajo, seis destinos con ícono y rótulo, respeta `safe-area-inset-bottom` | El pulgar llega abajo, no arriba. El rail de escritorio se oculta: nunca hay dos navegaciones a la vez |
| Barra superior | Marca y contador en vivo (versión corta, se recorta si no entra) y el chip de Admin | Antes ocupaba ~120px y dejaba "Tickets" y "Mi cuenta" fuera de la pantalla |
| Inicio | "Escanear el QR de la bici" es el botón principal; el código queda como alternativa debajo | El QR está pegado en el caño y la cámara ya está en la mano |
| Mapa | El detalle de la estación sube como hoja inferior, con su X para cerrarla | La respuesta aparece donde está el pulgar, sin perder el mapa de vista |
| Planes | Un plan debajo del otro con sus atributos y el botón al pie | La tabla pedía ~520px y obligaba a scrollear de costado |
| Tablas (`.tabla-apilable`) | Cada fila es un bloque "rótulo — valor" (`data-label`) | Sin scroll horizontal. Llevan `role` explícitos: Safari deja de anunciar como tabla una `<table>` con otro `display` |
| Mi cuenta | Atajos fijos a cada sección | Son seis tarjetas apiladas y la que se busca (casi siempre un pago pendiente) queda varias pantallas más abajo |
| Tickets | Al elegir un ticket, el chat se trae a la vista | En una sola columna el chat se abría varias pantallas más abajo |
| Modal | Hoja inferior a todo el ancho | Los botones quedan al alcance del pulgar y el teclado no la saca de la pantalla |

Reglas que se suman a las de arriba:

- **Campos a 16px como mínimo**: con menos, Safari del iPhone hace zoom al enfocarlos.
- **Ningún botón chico**: `.boton-chico` sube a 44px en el celular.
- **`:hover` solo con `(hover: hover)`**: en pantallas táctiles el hover quedaba "pegado" después de tocar un botón. El feedback del toque es `:active`.
- **Teclado correcto**: `autoComplete`, `inputMode`, `autoCapitalize` y `enterKeyHint` en los campos (código de bici en mayúsculas y sin autocorrector, DNI numérico, teléfono `tel`).
- Íconos SVG propios en `components/ui/Iconos.tsx`: grilla de 24px, trazo 1.75, siempre acompañados de texto.

## Escritorio (más de 720px)

Composición para pantalla ancha, dentro de `@media not all and (max-width: 720px)`. Cada decisión corrige algo que se veía mal:

| Pieza | Qué hace | Por qué |
|---|---|---|
| Encabezado de página | Ocupa el ancho del contenido; la bajada conserva la medida de 62ch | Con 62ch era un rótulo corto con un hueco de fondo al lado, y su borde no coincidía con el de nada |
| Inicio y login | La tarjeta centrada en la ventana | Pegada arriba dejaba tres cuartos de la pantalla vacíos |
| Estaciones | Resumen de la red en el encabezado; búsqueda, filtro, recarga y conteo en una sola barra; grilla de a tres con filas del mismo alto | El rótulo del buscador caía sobre el fondo de rayas, y en dos columnas las tarjetas quedaban estiradas |
| Mapa | Mapa a la izquierda; a la derecha el detalle de la estación y la lista de la red, que también selecciona y centra el mapa | El detalle aparecía debajo del mapa, fuera de la vista |
| Mi cuenta | Pagos a la fila entera; filas de tarjetas del mismo alto; "Ajustes" con apariencia y cerrar sesión | En media columna los botones de Pagar y Rechazar quedaban cortados |
| Panel de estilo | Con sesión ya no flota: está en Mi cuenta > Ajustes (en celular y escritorio). Sin sesión sigue flotando | Tapaba contenido, por ejemplo el botón de contratar el último plan |
| Tickets | El chat queda fijo al costado; al llegar un mensaje solo se mueve la caja del chat | Antes toda la página saltaba al abrir un ticket |

Correcciones que valen en todos los tamaños:

- **Hovers que se pisaban**: `.fila-clickeable`, `.ticket-item-boton` y `.modal-cerrar` tienen su propio hover con `:not(:disabled)`. Sin eso les ganaba el hover genérico de `button` y se pintaban con el violeta girado de tono, con el texto ilegible encima.
- **`.aviso` es bloque**, no flex: como flex, cada link quedaba como una columna aparte.
- **`.tabla-scroll` es `position: relative`**: el texto `.solo-lectores` de los encabezados escapaba del scroll y estiraba la página.
- **`.subpanel`** lleva borde fino completo en vez de la franja de 4px a la izquierda.
