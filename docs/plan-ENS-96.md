# ENS-96 · Prueba premium de siete días

Alcance aprobado por Brian el 19 de octubre de 2026: una prueba por negocio,
siete días exactos y diez páginas de IA **en total durante la prueba**.
Desarrollo y validación inicial en local, en `feat/ens-21-planes-suscripciones`.
Posteriormente el usuario autorizó commits explicativos y publicación de la rama, sin cambios en Jira.

## Comportamiento

- `POST /api/planes/prueba` inicia la prueba del negocio del usuario autenticado.
  Requiere sesión y `X-CSRF-TOKEN`; no recibe negocio, plan, fechas ni cuotas del cliente.
- La respuesta contiene `inicioEn`, `finEn`, `paginasIaUtilizadas` y `limitePaginasIa=10`, con
  `Cache-Control: no-store`. La pantalla que usará esta operación corresponde a ENS-97.
- Una fila permanente en `prueba_premium`, con clave primaria `negocio_id`, conserva el período
  original y el consumo aunque la suscripción cambie a gratuito o a premium pago.
  Repetir el inicio devuelve 403 `PRUEBA_UTILIZADA` y no extiende el plazo ni reinicia páginas.
  Si ya tiene premium vigente, devuelve 403 `PREMIUM_ACTIVO`.
- El período incluye su inicio y excluye su fin: dura 168 horas. El reloj del servidor vive en los
  casos de uso; los servicios reciben el instante. En `PRUEBA` se habilitan sistemas y registros sin
  límite comercial, recetas/costos, reportes filtrados y XLSX. Se reemplaza la cuota mensual premium
  por la cuota total de prueba; agotar IA no cierra las demás funciones antes del vencimiento.
- Las páginas anteriores al inicio no se descuentan de la prueba, incluso si son del mismo día.
  El contador no se renueva por día ni por cambio de mes. Se conserva además el límite diario técnico.
- Lecturas inmediatas y lotes comparten el contador de prueba. Se registra el intento antes de llamar
  al proveedor: la vía inmediata al admitirse y los lotes al obtener una reserva válida. Errores,
  timeouts y reintentos admitidos consumen páginas; finalizar o recuperar un lote no vuelve a cobrarlas.
  Una reserva rechazada o duplicada no consume páginas. Validar una subida/reintento sólo comprueba
  disponibilidad; la reserva posterior vuelve a validarla antes de admitir el intento.
- Validación, consumo y reserva diaria comparten transacción y bloqueo de la fila del negocio.
  Dos solicitudes simultáneas no pueden superar diez páginas. El rechazo es 403 `LIMITE_IA_PRUEBA`.
  El alta de prueba y la modificación de suscripción también son una única transacción.
- Al llegar a `finEn`, las operaciones protegidas ya evalúan los límites gratuitos aunque el job no
  haya ejecutado. `VencimientoPruebasWorker` persiste el gratuito en lotes de hasta cien candidatos,
  con comprobación del estado después de obtener el mismo bloqueo por negocio. Es repetible y
  no degrada una suscripción que ya cambió a `ACTIVA` por un pago.
- El job está habilitado por defecto, demora inicial de diez segundos y ejecución cada minuto.
  Configuración: `ENSAMBLA_PRUEBAS_WORKER`, `ENSAMBLA_PRUEBAS_INTERVALO` y
  `ENSAMBLA_PRUEBAS_DEMORA_INICIAL` (milisegundos). Los perfiles de tests lo deshabilitan.
- Al vencer no se borran datos ni se bloquea su lectura/edición básica. Se rechazan nuevas altas
  que excedan el gratuito y las funciones premium. La cuota mensual gratuita sigue contando todo
  intento registrado ese mes, incluidos los realizados durante la prueba; no se regalan otras diez
  páginas al vencer. El contador de prueba permanece para impedir una segunda activación.

## Esquema

V14 agrega `prueba_premium` sin modificar V1–V13. Su PK es además FK del negocio con borrado
en cascada. La base exige siete días de duración, contador entre cero y diez y un período de
siete días no nulo cuando la suscripción está en `PRUEBA`. El índice parcial de vencimientos
permite buscar sólo las suscripciones en prueba, ordenadas por fin y negocio.

`inicio_en` es también la fecha de creación de este historial inmutable; se conserva junto con
`fin_en` al actualizar el contador. No hay activación automática ni cambios en datos o consumo previo.
La migración se prueba en PostgreSQL temporal; no se aplica desde esta tarea a la base habitual.

## Validación

Las pruebas cubren inicio simultáneo, repetición, siete días exactos, cruce de mes, consumo anterior,
concurrencia entre vías, reservas fallidas y duplicadas, finalización posterior al vencimiento,
premium activado durante una prueba, conservación de datos, rollback, restricciones del esquema,
migración V13→V14, sesión y CSRF. No se llama a un proveedor de IA real.

Verificación final: `backend/mvnw.cmd -B clean verify` aprobada con **1.489 pruebas** en 233 clases
de tests, cero fallos, cero errores y cero omitidas. JaCoCo comprobó 444 clases con **4.277 líneas y
1.014 ramas cubiertas**, sin líneas ni ramas pendientes: 100 % por clase.

El evaluador de cuadernos aprobó sus **39 pruebas sin omitidas**, incluida la ejecución del jar
recién compilado (`ENSAMBLA_EVAL_JAR`), sin llamadas a IA real. `git diff --check` sin errores.
Evidencia local ignorada: `test-ens96-verify-final.log` y `test-ens96-evaluador.log`.

No se modificó el frontend ni se aplicó V14 desde esta tarea a la base habitual. El registro y
vencimiento se verificaron con PostgreSQL aislado de Testcontainers y seguridad HTTP real mediante MockMvc.
El futuro flujo de pagos de ENS-98 debe usar el mismo bloqueo por negocio al cambiar la suscripción.

Inventario de esta subtarea: **32 archivos creados y 16 modificados**, detallados con enlaces en
[archivos-ENS-96.md](archivos-ENS-96.md), separado del trabajo previo de ENS-94/95.
