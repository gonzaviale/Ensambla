# ENS-21 · Planes, prueba premium y control por plan

Enfoque aprobado por el usuario el 19 de octubre de 2026. La implementación y validación
de ENS-94 a ENS-96 se completaron primero en local, sin commits.
Posteriormente el usuario autorizó commits explicativos con Conventional Commits y su publicación
en `feat/ens-21-planes-suscripciones`. Las notas de cada etapa conservan el alcance de esa validación local.

## Etapa actual: ENS-96

El usuario aprobó continuar ENS-96 con una única prueba por negocio de siete días exactos y diez
páginas de IA totales. Se incorporaron V14, el inicio privado desde sesión y CSRF, el contador
persistente compartido por lectura inmediata y cola, y el vencimiento automático sin pérdida de datos.
El detalle técnico y la validación están en [plan-ENS-96.md](plan-ENS-96.md).
La publicación autorizada corresponde a esta rama de funcionalidad. La pantalla pertenece a ENS-97
y los pagos a ENS-98; ambas subtareas siguen pendientes.
ENS-96 quedó validada con 1.489 pruebas del backend y 39 del evaluador sin fallos ni omitidas;
444 clases con el 100 % de líneas y ramas. V14 se verificó en PostgreSQL temporal sin aplicarla desde
esta tarea a la base habitual. [Archivos creados y modificados en ENS-96](archivos-ENS-96.md).

## ENS-95: resultado de la etapa anterior

ENS-94 está implementada y verificada. El usuario aprobó continuar ENS-95 con los límites propuestos:
gratuito de un sistema, 1.000 registros y diez páginas mensuales de IA; premium sin límite comercial
de sistemas/registros y con cien páginas mensuales, recetas/costos, reportes filtrados y XLSX.
La propuesta de prueba de siete días con diez páginas totales se implementará en ENS-96.
El detalle técnico y la validación de esta etapa están en [plan-ENS-95.md](plan-ENS-95.md).
ENS-95 quedó implementada y validada: 1.426 pruebas del backend sin fallos, errores ni omitidas,
430 clases con 100 % de líneas y ramas, y 39 pruebas del evaluador aprobadas sin omitidas.
Se mantiene el trabajo local y sin commits; V13 se validó en PostgreSQL temporal y no se aplicó
desde esta tarea a la base de desarrollo habitual. La siguiente subtarea es ENS-96.

## ENS-94: resultado de la etapa anterior

Implementación y verificación del backend completadas localmente. No se hicieron commits,
push ni cambios en Jira. La migración se validó en PostgreSQL aislado de Testcontainers;
no se aplicó desde esta tarea a la base de desarrollo de uso habitual.

Validación: `backend/mvnw.cmd -B clean verify` finalizó correctamente con 1.364 pruebas,
cero fallos, cero errores y cero omitidas. JaCoCo verificó 422 clases, 4.096 líneas y
964 ramas cubiertas, sin líneas ni ramas pendientes. Se probaron también las restricciones
de planes, suscripción única, aislamiento, borrado en cascada y reversión completa del registro.
El evaluador de cuadernos pasó sus 39 pruebas, incluida la ejecución del jar compilado, sin omitidas.

El alcance de esta etapa es la migración V12 de `plan` y `suscripcion`, sus modelos,
repositorios y mappers, y la asignación transaccional del gratuito durante el registro.
Se prueba la actualización de una base V11 con negocios existentes, el aislamiento,
las restricciones del esquema y el rollback del registro si falla la suscripción.

El catálogo inicial queda con `configurado=false`: precio premium, moneda, cuotas y funciones
siguen pendientes de definición comercial. Un límite nulo de un plan sin configurar no significa
ilimitado. La migración sólo fija el precio cero del gratuito. Los números de los fixtures de tests
no son valores comerciales. Las tablas y los controles de pagos, prueba y consumo mensual se
agregarán en sus etapas; ENS-94 no activa esas funciones ni cambia el consumo diario existente.

Pasos de implementación de ENS-94:

1. Crear V12 sin modificar las migraciones aplicadas, sembrar los dos planes y completar
   una suscripción gratuita activa por cada negocio existente.
2. Crear modelos, puertos y adaptadores de persistencia con consultas por código de plan
   o por negocio; incorporar el servicio de suscripción al registro transaccional existente.
3. Cubrir mapeos y servicios con tests unitarios; probar esquema, actualización V11→V12,
   aislamiento y rollback contra PostgreSQL de Testcontainers.
4. Ejecutar los tests afectados y `clean verify`; actualizar modelo de datos y README
   con los resultados comprobados y las funciones que corresponden a las siguientes subtareas.

## Base de trabajo

- `dev` actualizado mediante fast-forward a `89c35d5`, igual a `origin/dev`.
- Rama de trabajo: `feat/ens-21-planes-suscripciones`, creada desde ese estado.
- Se revisaron `backend/AGENTS.md`, las tres skills de `.claude/skills`, los README,
  `backend/TESTING.md`, el modelo de datos y las descripciones actuales de Jira.
- Se trabaja localmente. Commits sólo a pedido, según las reglas del repositorio.

## Alcance comprobado en Jira

Historia: [ENS-21](https://gonzaloleonelviale.atlassian.net/browse/ENS-21),
«H-20 · Planes, prueba premium y control por plan».

1. [ENS-94 / T-066](https://gonzaloleonelviale.atlassian.net/browse/ENS-94):
   persistir planes, suscripciones y límites. Cada negocio comienza con el gratuito.
2. [ENS-95 / T-067](https://gonzaloleonelviale.atlassian.net/browse/ENS-95):
   bloquear funciones premium y límites superados desde el backend con un error claro, 402/403.
3. [ENS-96 / T-068](https://gonzaloleonelviale.atlassian.net/browse/ENS-96):
   registrar inicio y fin de una prueba de siete días y ejecutar su vencimiento automático.
4. [ENS-97 / T-069](https://gonzaloleonelviale.atlassian.net/browse/ENS-97):
   comparar planes y precios, mostrar el estado actual e iniciar la prueba desde la aplicación.
5. [ENS-98 / T-070](https://gonzaloleonelviale.atlassian.net/browse/ENS-98):
   cobrar por una pasarela y recibir cambios de estado; un pago aprobado activa premium,
   uno rechazado no lo activa.

La historia originalmente indica premium sin límites. Brian aprobó ajustar ese alcance para la IA:
premium tendrá cien páginas mensuales, manteniendo sistemas y registros sin límite comercial.
El catálogo de proveedores conserva su condición gratuita en este MVP.

## Estado encontrado

No se encontraron módulos de planes, suscripciones ni integración de pagos en el código revisado.
Las migraciones llegan a V11; el siguiente cambio de esquema debe ser nuevo, sin editar V1–V11.
El dominio y las tablas usan español, por lo que se propone `plan` y `suscripcion`.

La IA ya reserva páginas de manera persistente y concurrente por negocio y día UTC;
el límite diario configurable tiene valor por defecto de 100. Eso no representa una cuota mensual
ni un plan comercial. El nuevo control debe integrarse con esa reserva y sus reintentos.

La confirmación de una foto puede crear un proyecto y varias filas. Además de las altas manuales
y las copias de plantillas, ese camino debe respetar los límites del plan.

## Enfoques considerados

- Recomendado: catálogo y suscripciones persistidos, con un servicio de dominio que resuelva
  capacidades y consumo. Permite auditar prueba, vencimiento y pagos por negocio.
- Alternativa: límites en configuración y suscripciones persistidas. Reduce esquema, pero cambiar
  el catálogo exige coordinar configuración y despliegue; dificulta consultar una única fuente de precios.
- Alternativa: controles independientes en cada endpoint. Tiene menos estructura inicial, pero duplica
  reglas y permite diferencias entre carga manual, plantillas y confirmación de fotos.

## Diseño propuesto

### Datos y reglas

Crear una migración nueva para el catálogo de planes, la suscripción por negocio, las fechas de prueba,
el historial necesario de pagos y el consumo mensual de IA con sus reservas. Asignar el gratuito a
negocios existentes y nuevos, sin borrar datos. Los nombres finales y constraints se definirán con
los modelos y las consultas, manteniendo el aislamiento por negocio de la sesión.

Resolver las capacidades en el servidor, con validaciones en los servicios compartidos de creación
y procesamiento. Los controles de cantidad y las reservas deben ser atómicos para impedir que dos
solicitudes simultáneas superen la cuota. Proponer 403 con un código de error de plan y mensaje claro.
Un recurso ajeno sigue respondiendo 404.

Proponer una única prueba por negocio. Usar `Clock` en los casos de uso para medir siete días exactos,
un proceso programado para persistir el vencimiento y una comprobación de vigencia en cada operación
protegida. Al vencer, conservar los datos y la consulta; bloquear nuevas altas que excedan el gratuito.
Una suscripción paga vigente no debe ser degradada por el vencimiento de una prueba anterior.

### API y pantalla

Exponer catálogo, suscripción actual con capacidades y consumo, inicio de prueba e inicio del pago.
El servidor decide el negocio, precio y plan; no acepta un estado premium enviado por el navegador.
Agregar una pantalla de planes dentro de la aplicación, siguiendo sus componentes y estilos actuales.
Los datos pasan por `api` y hooks; las respuestas se validan con zod.

### Pagos

Proponer Mercado Pago en ambiente de pruebas, pendiente de confirmar como pasarela.
Consultar documentación oficial vigente antes de implementar el contrato del proveedor.
Separar el puerto de pagos del adaptador externo y persistir la referencia de la operación.
Validar la notificación según el proveedor y corroborar el pago en su API, incluidos negocio,
referencia, importe, moneda y estado. Procesar notificaciones repetidas de forma idempotente.
La vuelta desde el checkout no activa premium. Un pago rechazado no revoca por sí solo un período
ya pagado y vigente. Deben definirse cobro recurrente y vencimiento de los períodos pagos.

## Orden de implementación y validación

1. ENS-94: migración, entidades y asignación del gratuito; tests de esquema y registro de negocios.
2. ENS-95: capacidades y cuotas en todas las rutas de creación; tests de aislamiento y concurrencia.
3. ENS-96: prueba y vencimiento con reloj controlado; probar el límite exacto de siete días,
   solicitudes repetidas y conservación de datos.
4. ENS-98: adaptador y estados de pago; probar aprobación, rechazo, duplicados, errores del proveedor
   y notificaciones que no coinciden con la operación. Validación externa en pruebas según acceso disponible.
5. ENS-97: página conectada, estado, consumo, prueba y pago; pruebas de interacción y revisión en navegador.
6. Ejecutar `backend/mvnw.cmd clean verify` con el 100 % de líneas y ramas por clase exigido,
   pruebas del evaluador de IA y lint, tipos, tests y build del frontend. Actualizar documentación.

## Decisiones comerciales pendientes

- Precio, moneda y condiciones del período mensual. Las cuotas y funciones ya fueron aprobadas.
- Confirmar Mercado Pago y si el cobro mensual será recurrente automático o por renovación manual.

No se fija precio ni moneda ni se declara completa la integración de pagos sin esas definiciones
y sin evidencia de sus pruebas correspondientes.
