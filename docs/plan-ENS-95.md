# ENS-95 · Control de funciones y cuotas por plan

Alcance aprobado por Brian el 19 de octubre de 2026. Desarrollo y validación inicial en local,
en `feat/ens-21-planes-suscripciones`. Posteriormente autorizó commits explicativos y publicación
de la rama. No se realizaron cambios en Jira.

## Capacidades aprobadas

| Capacidad | Gratuito | Premium |
|---|---:|---:|
| Sistemas/proyectos por negocio | 1 | Sin límite comercial |
| Registros guardados en total por negocio | 1.000 | Sin límite comercial |
| Páginas/intentos de IA por mes calendario UTC | 10 | 100 |
| Carga manual, edición, búsqueda y stock básico | Sí | Sí |
| Recetas y cálculo de costos | No | Sí |
| Reportes filtrados y descarga XLSX | No | Sí |

La cuota cuenta todos los registros persistidos del negocio, sin multiplicarla por bloque o sistema.
Precio y moneda siguen pendientes: las capacidades pueden aplicarse sin inventar condiciones de cobro.
La prueba de siete días y su cuota de diez páginas para toda la prueba pertenecen a ENS-96.
No se activan pruebas ni se cobran suscripciones en esta etapa.

## Implementación

- V13 configura las capacidades del catálogo y agrega `limites_configurados`, independiente de
  `configurado` (precio y moneda). V12 y las migraciones anteriores se conservan.
- `ControlPlanService` resuelve la suscripción en el servidor. Una suscripción activa exige inicio
  alcanzado y fin no vencido; las futuras, vencidas o canceladas se evalúan como gratuito.
  El estado `PRUEBA` también usa el gratuito hasta implementar ENS-96.
- Un plan sin capacidades configuradas falla de forma cerrada. El cliente no decide el negocio,
  el plan, la fecha ni los límites.
- Las altas de sistemas en blanco, desde plantilla y desde confirmación de fotos pasan por el mismo
  servicio. Las altas manuales y múltiples de registros comprueban toda la cantidad antes de guardar.
- Un bloqueo de la fila del negocio permanece hasta terminar la transacción de validación y alta
  o reserva. Así dos solicitudes simultáneas no autorizan una misma disponibilidad.
- La IA conserva el contador diario persistente. El mes suma `paginas + reservadas` entre su primer
  día inclusive y el primer día del siguiente mes exclusive, en UTC, siempre filtrando por negocio.
  El índice primario existente `(negocio_id, fecha)` permite esta consulta.
- La validación del mes y la reserva diaria del worker comparten una transacción mediante
  `ReservaIaService`. La llamada externa se realiza después, fuera del bloqueo. Finalizar o recuperar
  un lote conserva la fecha de su reserva; las reservas rechazadas no gastan cuota.
- Subir un lote comprueba su cantidad de fotos y un reintento exige disponibilidad. No se promete
  capacidad a todos los trabajos pendientes: el worker vuelve a validar al reservar.
- La lectura inmediata de `POST /api/importaciones/lectura` toma el usuario de la sesión y registra
  un intento antes de salir al proveedor, usando la misma cuota mensual y el límite diario técnico.
  Un error o timeout también puede haber consumido IA y conserva el cargo del intento.
- `paginas_inmediatas` permite identificar ese consumo dentro del total diario. Como la vía inmediata
  todavía no persiste el detalle completo de tokens/costo, la consulta diaria lo marca incompleto
  y devuelve costo desconocido en lugar de presentarlo como cero.
- Recetas/costos, reportes filtrados y exportación XLSX validan su función luego de comprobar que
  el recurso pertenece al negocio. Un recurso ajeno sigue respondiendo 404.
- Una restricción de plan devuelve HTTP 403, mensaje para la persona y un `codigo` estable:
  `LIMITE_SISTEMAS`, `LIMITE_REGISTROS`, `LIMITE_IA_MENSUAL`, `LIMITE_IA_DIARIO`,
  `FUNCION_PREMIUM` o `PLAN_NO_CONFIGURADO`. Las respuestas no se cachean.

## Conservación de datos

No se borran sistemas, filas ni recetas por superar una cuota o por terminar la vigencia del plan.
Las lecturas básicas, ediciones y borrados de datos continúan disponibles. Las nuevas altas que
no entren en el gratuito y las operaciones premium se bloquean. Borrar datos libera capacidad de
sistemas/registros; el historial de intentos de IA se conserva y la disponibilidad se renueva en
el siguiente mes UTC.

## Validación

Las pruebas de integración usan PostgreSQL temporal de Testcontainers; no necesitan aplicar la
migración a la base local habitual ni llamar a un proveedor de IA.

- Altas simultáneas de sistemas y registros en bloques distintos, y rechazo completo de lotes de filas.
- Concurrencia entre lectura inmediata y cola, reservas de días anteriores, renovación del mes y
  finalización de una reserva después del cambio de mes.
- Lectura y edición de datos por encima de la cuota, acceso premium y vencimiento comprobado al consultar.
- HTTP real con sesión y CSRF: funciones premium, cuotas, lectura inmediata, reintentos, 401 y 404.
- Actualización V11→V12 y V12→V13, conservación de datos, consumo y precio previamente definido.
- Suite completa `mvnw.cmd -B clean verify` aprobada: 1.426 pruebas en 221 clases de tests,
  cero fallos, cero errores y cero omitidas. JaCoCo verificó 430 clases con 4.183 líneas y
  992 ramas cubiertas, sin líneas ni ramas pendientes (100 % por clase).
- Evaluador de cuadernos: 39 pruebas aprobadas, incluida la ejecución del jar compilado, sin omitidas.
- Se reinició Docker al detectarse que estaba detenido; la pasada final se ejecutó con PostgreSQL
  temporal disponible. Desde esta tarea no se aplicó V13 a la base local de uso habitual.
- No hay cambios de frontend en ENS-95: la pantalla y los avisos de upgrade pertenecen a ENS-97.
