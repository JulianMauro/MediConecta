# Patrones usados en el proyecto

## Estructura por módulo

Cada dominio en `modulos/<nombre>/` con subpaquetes `entity`, `repository`, `service`, `controller`, `dto`. Los 11 módulos ya tienen las cinco capas completas.

## Repository — Repository Pattern (Spring Data JPA)

Interfaces que extienden `JpaRepository<Entidad, Long>`. Sin implementación propia: Spring genera el CRUD. Se agregan solo los métodos derivados que ya tienen un consumidor concreto (ej. `ViajeRepository.findByUsuarioIdAndFechaFinIsNull`, `StrikeRepository.findByUsuarioIdAndEstado`).
Módulos: todos.

## Entity — Active Record liviano (sin setters)

Las entidades JPA no exponen setters genéricos: tienen un constructor protegido vacío (lo pide Hibernate), un constructor público con los datos obligatorios, y métodos de negocio nombrados por la acción (`desactivar()`, `finalizar(anclajeDestino)`, `saldar()`, `anclarEn(anclaje)`). Esto evita que cualquier capa deje la entidad en un estado inconsistente por fuera de sus propias reglas.
Módulos: todos.

## DTO — Record + factory `desde(entidad)`

Los DTO de salida son `record` con un método estático `desde(Entidad)` que arma el DTO a partir de la entidad. Los de entrada son `record` con anotaciones `jakarta.validation` (`@NotBlank`, `@Size`, etc.), nunca validan a mano.
Módulos: todos.

## Service — capa transaccional + `ResponseStatusException`

Los `@Service` reciben sus dependencias por constructor (sin `@Autowired` en campo), marcan cada método `@Transactional` (o `@Transactional(readOnly = true)` en lecturas), y lanzan `ResponseStatusException` con el `HttpStatus` correspondiente para los errores de negocio (409 en duplicados, 404 en no encontrado, 403 en propiedad ajena). No hay excepciones custom.
Módulos: todos.

## Orquestación cross-módulo: repository ajeno para leer/mutar, service ajeno para reusar lógica

Cuando un flujo necesita tocar la entidad de otro módulo, el service inyecta el `Repository` de ese módulo directamente si solo hace falta cargar/guardar (ej. `ViajeService` usa `BicicletaRepository`, `AnclajeRepository`). Solo se inyecta el `Service` de otro módulo cuando hay una regla de negocio real para reusar (ej. `ViajeService` usa `StrikeService.generarPorExceso`, que a su vez usa `PagoService.crear`).
Esto evita ciclos de beans: ningún service depende de otro que dependa de él. El orquestador de todo el flujo de "viaje" es `ViajeService`; `StrikeService` orquesta el "exceso de tiempo" (pago + notificación + evaluación de bloqueo) sin depender de `ViajeService`.
Módulos: `viaje`, `strike`, `pago`, `membresia`, `logistica`.

## "Punto único de alta" para entidades con invariantes de estado

`Pago` y `Notificacion` no se crean con `new` + `repository.save()` desde cualquier lado: `PagoService.crear(...)` y `NotificacionService.crear(...)` son el único método público de alta, usado por los módulos que las disparan (`strike`, `viaje`, `membresia`). Mantiene el estado inicial (`PENDIENTE`, `leida=false`) centralizado en un solo lugar.
Módulos: `pago`, `notificacion`.

## Baja lógica, no borrado físico

Ninguna entidad se elimina de la base: se desactiva (`Usuario.desactivar()`, `Estacion.desactivar()`, `Membresia.desactivar()`) o cambia de estado (`SuscripcionUsuario.cancelar()`). Mantiene trazabilidad e integridad referencial con viajes/pagos históricos.
Módulos: `usuario`, `estacion`, `membresia`, `flota` (vía `EstadoBicicleta`), `strike`.

## Enum de estado como texto (`@Enumerated(EnumType.STRING)`)

Todos los enums de estado se persisten como `STRING`, nunca `ORDINAL`, para que agregar un valor nuevo no corra el significado de los ya guardados.
Módulos: `usuario` (`Rol`), `estacion` (`EstadoDock`), `flota` (`EstadoBicicleta`), `membresia` (`TipoMembresia`, `EstadoSuscripcion`), `strike` (`EstadoStrike`), `pago` (`EstadoPago`, `ConceptoPago`), `notificacion` (`TipoNotificacion`).

## Ubicación polimórfica vía FK nullable (no herencia JPA)

Cuando una entidad puede estar en uno de dos lugares excluyentes, se modela con dos `@ManyToOne` nullable en vez de una tabla polimórfica o herencia. El invariante "exactamente uno de los dos" lo garantiza la capa de servicio, no una constraint de base.
Módulos: `flota` (`Bicicleta.anclaje` / `Bicicleta.almacen`), `logistica` (`MovimientoBicis.origenEstacion/origenAlmacen` y `destinoEstacion/destinoAlmacen`).

## Historial vía entidad separada, no flag + fecha

Cuando un cambio de estado necesita auditoría (quién, cuándo, por qué), es su propia entidad con FK a quien lo generó, en vez de un booleano suelto en la entidad principal.
Ejemplos: `BloqueoCuenta` (no un `boolean bloqueado` en `Usuario`), `MovimientoBicis` + `MovimientoBiciItem` (trazabilidad del traslado en lote).

## Rol dentro de Usuario, no entidad separada

`Administrador` no existe como entidad: es `Usuario` con `rol = ADMIN`. Autorización vía `@PreAuthorize("hasRole('ADMIN')")` en el controller, habilitado por `@EnableMethodSecurity` en `SecurityConfig`. Cuando el controller entero es de gestión administrativa (`AlmacenController`, `MovimientoBicisController`), la anotación va a nivel de clase en vez de repetirla en cada método.
Módulos: `usuario`, `seguridad`, `almacen`, `logistica`.

## Ownership check dentro del service, no en el controller

Los endpoints "propios" (`/api/pagos/{id}/confirmar`, `/api/notificaciones/{id}/leida`, `/api/suscripciones/{id}`) no filtran por usuario en la query: el controller pasa el `usuarioId` del principal autenticado y el service compara contra el dueño real de la fila, devolviendo 403 si no coincide (salvo ADMIN, que puede operar sobre cualquiera). Mantiene la regla de autorización junto a la lógica de negocio en vez de repartirla entre capas.
Módulos: `pago`, `notificacion`, `membresia`.
