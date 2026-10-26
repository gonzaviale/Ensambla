# ENS-244 — Home y planes sobre el dev actual

Base: `a9c2ed3f` de `dev`. Rama local: `feat/ens-244-home-planes-v2`.

El PR #54 fue cerrado sin merge el 26/10/2026. Su implementación no está en dev. Esta entrega
retoma el alcance ya aprobado y reutiliza las piezas compatibles, adaptadas al estado actual
del proyecto y a las reglas de `backend/ARQUITECTURA.md`.

## Alcance conservado

- Home con métricas por cuaderno, objetivos mínimos o máximos y metas locales separadas por
  usuario y cuaderno. Se excluyen filas de ejemplo y el almacén de demostración.
- Ventas registradas = cantidad por precio. No se suman pedidos a ventas ni se promete ganancia
  o cobros. Pedidos pendientes, stock bajo, clientes y filas se muestran según las tablas presentes.
- La bienvenida sigue disponible cuando no hay tablas; se conserva la pantalla Mis cuadernos,
  las fichas de creación actuales, la navegación móvil y los cambios recientes de Sami.
- La home reutiliza `FranjaDeSami` con los avisos autorizados del cuaderno seleccionado y navega
  hacia la pantalla actual `/avisos`, respetando el trabajo de ENS-245.
- Planes pública, catálogo global desde el servidor y datos de uso/suscripción privados.
- Tarjetas con formato consistente, bordes marcados y reportes filtrados exclusivos de Premium.
  No se cambian las cuotas ni el precio real del catálogo.
- Solo modalidad mensual, de acuerdo con la decisión previa de Brian. No se inventa tarifa anual.
- Se reducen las invitaciones a Premium; se conservan los mensajes de límite y un enlace discreto
  cuando efectivamente hay contenido bloqueado. Se preserva la variante compacta usada en Mi Zona.

## Backend actualizado

`GET /api/planes/catalogo` devuelve únicamente datos comerciales. No contiene identificadores
de negocio/usuario, consumo, suscripción, historial de prueba ni renovación. Se mantiene el acceso
por sesión y CSRF de las otras rutas, sin abrir un prefijo completo de seguridad.

El caso `ListarCatalogoPlanes` vive junto a sus DTOs en
`domain/useCase/plan/listarCatalogoPlanes`, sin sufijo UseCase ni dependencias de Spring.
Utiliza el puerto `PlanRepository.obtener`, `TransaccionPort` y fábricas `de()` de los DTOs.
No crea servicios de dominio ni mappers de dominio, ni agrega UUID sin tipar, H2 o migraciones.
El bean se registra en `PlanUseCaseConfig` y la traducción web sigue en `PlanPresentationMapper`.

## Validación realizada el 26/10/2026

- Pruebas por capa del catálogo y seguridad real con Testcontainers/PostgreSQL.
- `mvnw.cmd clean verify`, incluido ArchitectureTest y cobertura de líneas/ramas por clase.
- Frontend: lint, TypeScript, suite completa y build, con casos de metas, almacenamiento bloqueado,
  identidad, ejemplos, navegación actual de sugerencias, planes públicos y datos privados.
- Navegador sobre la compilación de producción con backend y una base local de pruebas separada,
  escritorio y 360 px; persistencia de metas, teclado, ausencia de desbordamiento y errores JS.

| Comprobación | Resultado |
| --- | --- |
| Backend `mvnw.cmd clean verify` | 2.404 pruebas aprobadas, 2 de Bedrock externo omitidas, 0 fallos |
| Arquitectura y cobertura | 7 pruebas de arquitectura aprobadas; controles de cobertura aprobados en 709 clases |
| Frontend: suite completa | 607 pruebas aprobadas en 53 archivos; 2 omisiones preexistentes, 0 fallos |
| Frontend: lint, TypeScript y build | Aprobados |
| Evaluador de cuadernos | 39 pruebas aprobadas |
| Navegador con API y PostgreSQL reales | Aprobado en escritorio y 360 px; sin errores JavaScript |

El recorrido real verificó catálogo público sin consumo ni suscripción, rutas privadas protegidas,
ventas de $10.000, un pedido pendiente y un producto con stock bajo. También verificó exclusión
de ejemplos, meta de ventas de $15.000, persistencia al recargar, recuperación del foco y una meta
máxima de stock alcanzada. Se abrió la prueba Premium y se cerró sin activarla.

Se usó la base local separada `ensambla_ens244_v2`, con datos sintéticos y proveedores de IA
simulados. No se realizaron cobros ni llamadas reales a IA. El navegador probó la compilación de
producción servida en `http://127.0.0.1:5173`, con backend local en el puerto 8080.

Evidencias: [resultado JSON](qa/ens244-2026-10-26/resultado.json),
[inicio en escritorio](qa/ens244-2026-10-26/home-principal.png),
[inicio en celular](qa/ens244-2026-10-26/home-mobile.png),
[planes públicos en escritorio](qa/ens244-2026-10-26/planes-publicos-desktop.png),
[planes públicos en celular](qa/ens244-2026-10-26/planes-publicos-mobile.png) y
[planes de cuenta en celular](qa/ens244-2026-10-26/planes-cuenta-mobile.png).
El recorrido reproducible está en `scripts/qa_ens244_local.py`.

La primera ejecución incremental encontró clases/recursos obsoletos en `target` de la arquitectura
anterior. La validación de entrega se completó con compilación limpia, sin modificar controles de
arquitectura ni omitir pruebas para resolver esos restos.

## Límites

Las metas se guardan solo en el navegador. No envían notificaciones con la aplicación cerrada
ni se sincronizan entre dispositivos. No se agregan selector opcional de períodos, pagos nuevos,
descarga de Excel o estudio de mercado. No se reabre ni modifica el PR anterior cerrado y no se
modifica el estado de Jira.

La publicación autorizada se organiza en cinco commits temáticos y un nuevo PR hacia `dev`,
sin merge ni merge automático.
