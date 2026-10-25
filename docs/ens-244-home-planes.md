# ENS-244 — Home y planes

Implementación local sobre `feat/ens-244-home-planes`, creada desde `dev` en `8765c65` e integrada con `origin/dev` en `718d7ce`
(ENS-243 y la nueva pantalla de cuenta) antes de publicar.
Diseño aprobado por Brian el 24/10/2026. La implementación se validó primero en local.
Luego Brian solicitó commits temáticos, publicación de la rama y PR a `dev`, sin merge ni despliegue.

## Comportamiento

- Cuando hay tablas, Inicio prioriza el resumen del sistema seleccionado. Sin tablas conserva
  la bienvenida y las tres maneras de empezar.
- Los indicadores dependen de las tablas presentes: ventas, pedidos pendientes, productos con
  stock bajo, clientes y filas. Las ventas son cantidad por precio; los importes de Pedidos no
  se suman a Ventas y el resultado no se llama ganancia ni cobros.
- Se excluyen filas marcadas como ejemplo y los sistemas de demostración `demoAlmacen`.
  Los importes incompletos no se presentan como ventas válidas.
- Cada métrica explica su objetivo y admite una meta: mínimo para ventas, clientes y filas;
  máximo para pendientes y productos con stock bajo. Incluye validación, progreso o distancia,
  edición, eliminación y recuperación al volver al sistema o recargar.
- Las metas son locales, con clave versionada por usuario y sistema. La aplicación actual
  resuelve un negocio por sesión; el sistema seleccionado pertenece a ese negocio. No se
  comparten entre cuentas o sistemas ni se promete sincronización entre dispositivos.
- La meta alcanzada se indica en la home; no genera correo ni notificaciones con la app cerrada.
- `/planes` es pública. Visitantes consultan únicamente la sesión opcional y el catálogo global,
  con acciones de registro. Con sesión permanecen el consumo, la prueba y la renovación.
- `GET /api/planes/catalogo` devuelve solo DTOs comerciales. No incluye identificadores,
  suscripción, consumo, historial de prueba ni pagos. Solo ese GET nuevo es público; las otras
  rutas de planes y sus escrituras mantienen sesión y CSRF.
- Las tarjetas comparten estructura, bordes y nombres de beneficios. Solo se muestra modalidad
  mensual, según la decisión de Brian. No se cambian tarifas, cuotas ni cobros.
- Se sustituye «Avisos por mes» por «Sugerencias», se elimina «Sin límite comercial» y se
  explicita la limitación gratuita de tres sugerencias al mes. Premium conserva la ausencia
  de tope mensual del servidor: no se inventa una cuota para aplicar literalmente «Limitados».
- Los reportes filtrados siguen siendo Premium, tanto en la comparación como en el control
  existente del backend. La home básica continúa disponible para Gratuito.
- Se retiran previews promocionales y enlaces constantes a Premium cuando no hay contenido
  bloqueado. Al alcanzar un límite se informa el cupo con un enlace discreto a opciones del plan.

## Validación

Backend: `mvnw.cmd clean verify`, 2.399 pruebas, cero fallos y errores; dos omitidas por falta
de configuración externa de Bedrock. Todos los controles de cobertura por clase aprobaron.
Se prueba el catálogo, su mapeo, el wiring, la respuesta HTTP y la seguridad con PostgreSQL real.
No hay cambios de esquema ni migraciones nuevas.

Frontend: lint, TypeScript y build de producción aprobados. La suite general y las revisiones
específicas cubren fórmulas, ejemplos, metas, separación de identidades, almacenamiento bloqueado,
planes públicos/privados, límites, prueba, renovación y accesibilidad comprobable en jsdom.
Los resultados finales de las suites se registran al pie de este documento.
Las 39 pruebas del evaluador de cuadernos también aprobaron; esto valida su infraestructura,
no la precisión de un proveedor real de IA.

QA con la compilación de producción servida localmente, backend real y una base aislada
`ensambla_ens244_local`, dentro del contenedor existente `ensambla-postgres`:

- Catálogo público sin solicitudes a sistemas, zona, uso o pagos. Rutas privadas devuelven 401.
- Registro e ingreso de una cuenta sintética; creación de un sistema a través de la API real.
- Ventas propias de $10.000; un pedido pendiente y un producto con stock bajo. Un pedido de
  $100.000 y una venta de ejemplo no contaminan el importe de ventas.
- Meta de ventas de $15.000, progreso de $10.000 y persistencia tras recarga.
- Meta máxima de stock bajo alcanzada, navegación con teclado y recuperación del foco.
- Modal de prueba abre y cierra con Escape, sin activar la prueba ni contratar pagos.
- Escritorio y 360 px, sin desbordamiento horizontal ni errores JavaScript. La navegación
  desde la landing a Planes también se verifica en celular.

Evidencia: [resultado](qa/ens244/resultado.json), [home](qa/ens244/home-principal.png),
[home en celular](qa/ens244/home-mobile.png), [planes públicos](qa/ens244/planes-publicos-desktop.png),
[planes en celular](qa/ens244/planes-publicos-mobile.png),
[planes con sesión](qa/ens244/planes-cuenta-mobile.png).
Reproducción: `python scripts/qa_ens244_local.py`, con frontend en `127.0.0.1:5173` y backend
apuntando a esa base aislada. El script agrega únicamente datos sintéticos de prueba.

## Dependencias y alcance

La tarjeta de sugerencias para la home depende de ENS-245. No se duplica su implementación ni
se declara integrada. El formato de TarjetaPlan queda disponible para su uso en Mi Zona.
No se implementa el selector opcional de períodos, ni la tarifa anual, ni preferencias de
notificaciones, Excel o estudio de mercado.

El cambio previo en `FotoCuadernoEntity.java` sigue conservado en el stash
`respaldo-previo-ens244-foto-cuaderno`, separado de esta rama.

## Resultado final del 24/10/2026

| Comprobación | Resultado |
|---|---|
| Backend `clean verify` | 2.397 aprobadas, 2 omitidas por Bedrock externo, 0 fallos/errores |
| Cobertura del backend | Todos los controles por clase aprobados; 761 clases analizadas |
| Evaluador de cuadernos | 39 aprobadas |
| Frontend completo final | 541 aprobadas, 2 omitidas preexistentes, 48 archivos aprobados |
| Revisión de planes/navegación/límites | 35 aprobadas |
| Revisión de métricas, demostración y avisos | 44 aprobadas |
| Lint y TypeScript | Aprobados |
| Build de producción | Aprobado |
| Navegador con build de producción y backend real | Aprobado: escritorio y 360 px, 0 errores JavaScript |

Las dos omisiones preexistentes del frontend corresponden a una sección de ejemplo comentada
en Inicio y a un antiguo acceso a Sami que ya no se presenta desde la home. No se omitieron
pruebas nuevas de ENS-244. Las corridas iniciales encontraron expectativas antiguas sobre
la home y los avisos promocionales, y un fixture de mapper incorrecto; se corrigieron y se
repitieron las suites completas. Los resultados de la tabla corresponden a las corridas finales.

La vista de producción local queda disponible en `http://127.0.0.1:5173/planes`, con API en
`http://localhost:8080` y la base de pruebas aislada indicada arriba. No se usó la base habitual
para los registros sintéticos y no se hicieron solicitudes a un proveedor de pagos.

## Validación antes de publicar

Se integró `origin/dev` en `718d7ce`, que agrega ENS-243 y la pantalla de cuenta, mediante rebase
sin conflictos. Se repitieron lint, TypeScript, build y la suite completa: **585 aprobadas,
2 omitidas preexistentes y 51 archivos aprobados**. El backend conserva exactamente el código
ya validado con `clean verify`; los nueve commits nuevos de dev solo cambian frontend.

El recorrido de `scripts/qa_ens244_local.py` también pasó nuevamente contra el build integrado,
backend y PostgreSQL locales reales. Las capturas y el JSON de evidencia corresponden a esa
última ejecución, con escritorio, 360 px, navegación por teclado y cero errores JavaScript.

La entrega se separa en cinco commits: catálogo del backend, planes públicos, métricas de Inicio,
pruebas de ENS-244 y documentación/evidencia. Se publica para revisión en un PR a `dev`,
sin realizar merge ni habilitar auto-merge.
