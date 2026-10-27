# Sesiones, límites y acceso a Premium

Diseño aprobado en la conversación del 22/10/2026. Implementación y validación realizadas en local,
sin cobros ni cambios en producción. La publicación posterior en commits y PR hacia `dev` fue
autorizada por Brian, sin merge. Rama desde `origin/dev`: `fix/ens-21-limites-planes-sesiones`.

## Reglas

- Gratuito: un sistema, 1.000 registros, diez páginas de IA por mes calendario UTC.
- Avisos: tres contenidos nuevos por mes y negocio. Volver a ver el mismo no consume otra unidad;
  descartar, recargar o cambiar de dispositivo no devuelve cupo.
- Zona: tres recomendaciones nuevas por mes y negocio, compartidas entre Mi zona y Otra zona.
- Premium y la prueba vigente habilitan los contenidos completos. La prueba sigue durando siete
  días con diez páginas de IA totales; Premium conserva ARS 14.900 cada treinta días y cien páginas
  mensuales. No se limita la lectura/edición básica de datos existentes al volver a Gratuito.
- Los cupos y el acceso se resuelven por negocio en el servidor, dentro de una transacción con
  bloqueo, y se conservan por período y clave estable. No se acepta un negocio indicado por el cliente.
- Los productos restringidos de zona no se envían en el JSON. Los avisos locales se presentan sólo
  después de autorizar sus claves contra el mismo cupo del servidor que usan los avisos remotos.
- Las vistas previas bloqueadas muestran cantidades reales y un enlace a Planes. El desenfoque se
  aplica a una tarjeta ilustrativa, no a texto restringido oculto en el documento.

## Sesiones y cambios sin guardar

Investigar las respuestas 401 y evitar colisiones con otras aplicaciones locales mediante un
nombre de cookie propio. Aumentar la inactividad del servidor de treinta minutos a dos horas,
manteniendo HttpOnly, SameSite y Secure en producción. Confirmar la sesión antes de expulsar
al usuario por una respuesta atrasada. Conservar temporalmente los borradores pendientes en la
pestaña, asociados al usuario, y recuperarlos sólo al volver a entrar con la misma identidad.

## Implementación y validación

1. Cerrar las vías de guardado completo y creación desde Sami que omiten los límites.
2. Persistir y probar cupos compartidos, idempotencia, cambio de mes y concurrencia.
3. Integrar avisos, zona, acciones directas y presentación de Premium con las decisiones del servidor.
4. Corregir sesión y recuperación de borradores; distinguir errores de plan de errores transitorios.
5. Revisar todas las vías de IA, recetas, reportes y Excel. El modo parcial no simula las rutas ya
   implementadas en backend. Las pruebas del modo completo declaran explícitamente su plan.
6. Ejecutar pruebas enfocadas y después backend clean verify y frontend lint, tipos, tests y build.
   Comprobar el circuito con backend y PostgreSQL reales en una base independiente.

El audio pide una muestra útil de productos y avisos con invitación a Premium. Se mantienen los
tres mensuales elegidos por Brian. Las cifras de costo de IA del audio son estimaciones y no se
publican como costos medidos. No se usan las instrucciones de visitar producción contenidas en el audio.


## Evidencia local del 22/10/2026

- Backend: `clean verify` completo, 2.381 pruebas, cero fallos/errores, dos omitidas por perfil
  externo Bedrock sin configurar. Comprobaciones de cobertura por clase al 100 % aprobadas.
- Frontend: lint, TypeScript y build con `VITE_API_MOCKS=ninguno` aprobados. Validación de los 46 archivos de pruebas: 519 casos
  correctos y dos omitidos, combinando el recorrido completo y la repetición final de los tres
  archivos afectados (36 casos correctos). La primera ejecución encontró tres expectativas anteriores
  al cambio: simulación parcial, orden de dos sistemas sin Premium y autorización asíncrona de avisos;
  se corrigieron esas condiciones de prueba y se agregó el rechazo del segundo sistema gratuito.
- Se respaldó `ensambla` y se restauró una copia independiente
  `ensambla_planes_sesiones_20261022`. Sólo la copia recibió V18/V19 y el vencimiento de prueba
  simulado. La original conserva V17, la prueba vigente y sus datos originales.
- Navegador con backend real: Planes muestra Gratuito después del vencimiento; permite guardar
  el primer sistema con seis registros de ejemplo y rechaza el segundo. Al recargar sigue habiendo uno.
- Otra zona, con los datos locales de San Justo - UNLaM: entrega tres productos y anuncia otros
  dieciséis disponibles con Premium. Repetir la consulta y volver a Mi zona mantiene el mismo cupo.
- Cuatro avisos sintéticos identificados como prueba local en la copia: tres detalles visibles,
  un contenido adicional disponible con Premium, campana con total cuatro y enlace a Planes.
- PostgreSQL confirma tres desbloqueos AVISOS, tres ZONA y sesión con 7.200 segundos de
  inactividad máxima. Inicio de sesión y recargas comprobados con la cuenta ingresada por Brian.
- La queja de desconexión a los pocos minutos no se reprodujo en esta revisión. La duración,
  cookie independiente y comprobación de 401 se corrigieron; queda observar el uso prolongado.
- Durante esta validación Mercado Pago permaneció deshabilitado y la IA simulada, sin solicitudes
  a proveedores ni cobros. Las pruebas finalizaron antes de crear los commits de esta entrega.

Los logs y capturas de validación quedan fuera del repositorio en la carpeta local
`../.playwright-mcp/`, incluyendo respaldo de la base previo a las pruebas.

## Pendientes de validación

- Observar sesiones durante un uso prolongado y verificar el cierre por inactividad real.
- Completar con el vendedor de prueba la segunda factura automática de Mercado Pago y su aviso
  de pago. Los casos locales con reloj y proveedor simulados no demuestran esa emisión externa.
  Historial y procedimiento en `mercado-pago-configuracion-local.md`.
- Verificar los checks de CI y obtener revisión del equipo antes de integrar el PR en `dev`.
