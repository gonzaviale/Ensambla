# ENS-244: propuesta de Home y planes

Fecha: 24/10/2026. Estado: aprobado por Brian para implementación y pruebas locales.

## Base y alcance

- Historia: ENS-242, «Cambios de front: Sami, home y planes, avisos y Mi Zona».
- Subtarea: ENS-244, «[FE] Persona 2: Home y planes», asignada a Brian Klehr y en curso.
- Base sincronizada: `dev` y `origin/dev` en `8765c6508212beebab8109ac27eaa67b8451b72b`.
- Rama local: `feat/ens-244-home-planes`.
- Un cambio previo de dos líneas en blanco de `FotoCuadernoEntity.java` se conserva en el stash
  `respaldo-previo-ens244-foto-cuaderno`, asociado a la rama anterior.

Se trabaja sobre InicioPage, PlanesPage y TarjetaPlan, con componentes y hooks pequeños para
sus responsabilidades. La consulta pública del catálogo requiere un cambio acotado del backend.
No se modifica el esquema ni las condiciones comerciales.

## Alternativas consideradas

1. **Reutilizar sistemas y catálogo del servidor (recomendada).** Home con métricas derivadas de
   los sistemas ya cargados, tarjetas compartidas y una consulta pública solo del catálogo.
   Requiere un endpoint pequeño, pero mantiene precios y límites en una única fuente.
2. **Resolver todo solo en frontend.** Copiar el catálogo para visitantes permite evitar el endpoint,
   pero puede mostrar precios o límites distintos del servidor. Se descarta esa duplicación.
3. **Crear un panel analítico completo.** Métricas nuevas en servidor, períodos y notificaciones
   persistentes amplían el alcance de esta subtarea. Se conserva como trabajo independiente.

## Home

- Sin tablas armadas se conserva el flujo de bienvenida y las maneras de empezar.
- Con tablas, el resumen del negocio tiene prioridad visual; los cuadernos siguen accesibles y
  crear sistemas o elegir plantillas pasa a un segundo nivel.
- Un selector de sistema evita mezclar tablas de distintos sistemas sin explicar su procedencia.
- Se muestran solo indicadores respaldados por las tablas de ese sistema: ventas registradas,
  pedidos pendientes, productos con stock bajo, clientes y filas cargadas, según corresponda.
- Ventas registradas se calcula como cantidad por precio en tablas de ventas. Los importes de
  pedidos se mantienen separados: no se suman a ventas ni se presentan como ganancia o cobros.
- Cada indicador explica qué cuenta, de qué tabla sale y para qué sirve. Las filas de ejemplo no
  se mezclan con los datos reales; si solo hay ejemplos, se informa que faltan datos propios.
- Cada métrica admite una meta numérica opcional y una dirección explícita: alcanzar un mínimo
  para ventas, clientes o filas; bajar a un máximo para pendientes o productos con stock bajo.
  La tarjeta muestra progreso o distancia y un texto cuando se alcanza la meta.
- Las metas se conservan localmente por usuario, negocio, sistema y métrica. Se valida que sean
  números finitos no negativos; los conteos requieren enteros. No se reutilizan entre identidades.
- El aviso de meta alcanzada se muestra al consultar la home. No se promete monitoreo con la
  aplicación cerrada ni envío de correo, WhatsApp o notificaciones del dispositivo.
- Se omite el selector opcional semanal/quincenal/mensual en esta entrega: primero se muestra
  el estado actual y el acumulado registrado, identificados como tales.
- No se inventa una tarjeta de sugerencias. La integración visual de esa tarjeta depende de
  ENS-245 y se realiza cuando exista el componente acordado; no impide trabajar métricas y planes.

## Planes públicos y sesión

- `/planes` queda accesible para visitantes y usuarios autenticados, fuera de los dos guards que
  hoy obligan a iniciar sesión o redirigen a quienes ya la tienen.
- Visitantes ven comparación y enlaces a registro/ingreso; no se consultan sistemas, zona,
  consumo, suscripción ni renovación para construir la comparación pública.
- Con sesión se conserva el acceso al estado del plan, consumo, prueba y renovación, con las
  confirmaciones actuales. Las acciones privadas siguen requiriendo sesión y CSRF.
- Se agrega `GET /api/planes/catalogo`, que devuelve únicamente planes y condiciones comerciales
  globales, usando el repositorio existente y DTOs explícitos. No contiene identificadores de
  usuario/negocio, consumo, elegibilidad, prueba ni pagos.
- `GET /api/planes`, la prueba y las rutas de renovación conservan sus protecciones.
- El catálogo público tiene estados de carga, error y reintento; un error no se reemplaza por
  precios inventados. Las consultas privadas se mantienen separadas del catálogo global.
- Las tarjetas usan la misma estructura para ambos planes, bordes visibles, bloques alineados
  y botones consistentes, respetando la marca y los controles accesibles en celular.
- **Decisión del usuario:** mostrar solo Mensual hasta que se defina la tarifa anual. No aparece
  un precio anual calculado, un descuento supuesto ni una contratación anual.
- Se cambia «Avisos por mes» por «Sugerencias», se retira «Sin límite comercial» y se revisa
  «Todos los disponibles» con el texto «Limitados» solicitado en Jira, sin alterar cuotas reales
  ni presentar una cuota numérica que el servidor no define. Los recursos sin tope configurado
  se describen con texto que conserve esa condición; la cantidad de sugerencias gratuitas sigue explícita.
- Se mantienen reportes filtrados como función de Premium. La home básica y la búsqueda de
  filas existentes no se convierten en funciones pagas.
- Se reducen las invitaciones repetidas: navegación a Planes disponible y explicación discreta
  al alcanzar un límite o intentar una función exclusiva. No se muestran pedidos de suscripción
  al realizar acciones básicas admitidas por Gratuito.

## Dependencias y límites

ENS-243 corresponde a Sami y armado de tablas. ENS-245 corresponde a sugerencias, chat, Mi Zona
y configuración de zona. TarjetaPlan queda reutilizable para que ENS-245 adopte el mismo formato.
No se interviene en las conversaciones ni se contacta a otros miembros del equipo.

Quedan fuera de alcance guardar preferencias de notificaciones, descarga de Excel, estudio de
mercado y nuevos cobros. La persistencia local de una meta no se presenta como una notificación
persistente del servidor. No hay cambios en producción, push, PR ni commits en esta etapa.

## Validación de la implementación

- Home vacía, tablas vacías, datos propios frente a ejemplos y varios sistemas.
- Fórmulas de métricas, falta de tablas/campos, metas inválidas, progreso y separación por identidad.
- Planes sin sesión y con sesión, carga/error/reintento y ausencia de solicitudes privadas en visitantes.
- Comparación y nombres de beneficios; reportes filtrados exclusivos de Premium.
- Regresión de prueba y renovación, expiración de sesión, CSRF y rutas privadas.
- Endpoint público limitado al catálogo y tests por capa según los skills del repositorio.
- Frontend: lint, tipos, pruebas y build. Backend: `mvnw.cmd clean verify`, con cobertura requerida.
- Revisión visual en escritorio y 360 px, foco, teclado y ausencia de desplazamiento horizontal.

## Comprobaciones ya realizadas

Se leyó ENS-242 y ENS-244 en Jira, se actualizó dev por fast-forward, se verificó igualdad de hashes,
se creó la rama y se ejecutaron `npm run lint` y `npm run typecheck`, ambos sin errores.
Todavía no se modificó código ni se ejecutaron las pruebas completas de la futura implementación.
