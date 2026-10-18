# Adaptación visual de Ensambla al mockup

Referencia: distribución `Ensambla_Mockup/dist` entregada por el equipo. La implementación se realiza en `frontend/` sobre `origin/dev`, en la rama `feat/ens-171-ui-mockup`, siguiendo `.github/CONTRIBUTING.md`.

## Identidad visual

La fuente es **Plus Jakarta Sans**, disponible localmente mediante `@fontsource-variable/plus-jakarta-sans`. Los colores y superficies compartidos se centralizan en `frontend/src/styles/index.css`:

| Uso | Referencia |
| --- | --- |
| Fondo de papel | `#fffdfa` |
| Estructura, navegación y acciones de apoyo | Petróleo `#00506a` |
| Acción principal | Naranja `#ee8000` |
| Piezas y acentos | Azul `#4eb2d4`, amarillo `#ebce32`, coral `#f85f5a` |
| Superficies de apoyo | Crema, salvia y tintes de azul/petróleo |

Se incorporan bordes suaves, radios amplios, sombras discretas, bloques de color e ilustraciones de Sami. Los archivos de referencia utilizados como imágenes están en `frontend/public/mockup/`; no requieren servicios de imágenes externos. El isotipo conserva sus cinco piezas y utiliza la paleta del mockup.

## Mapa de pantallas

| Pantalla real | Adaptación visual | Comportamiento conservado |
| --- | --- | --- |
| Landing `/` | Paleta y superficies compartidas con la aplicación; encabezado de marca | Secciones, enlaces y comportamiento existente de la landing |
| Acceso `/login` y registro `/registro` | Composición de marca con Sami y formulario; en móvil el formulario aparece primero | Sesión del servidor, validación, errores por campo, estados de envío y redirección segura |
| Inicio `/proyectos` | Bienvenida, tres caminos de creación, ilustración de piezas, tarjetas de sistemas y plantillas | Listado real, búsqueda, crear en blanco, copiar plantilla, renombrar y eliminar |
| Armar `/proyectos/:id/construir` | Biblioteca de tablas, tablero de puntos, paneles y columnas con los tonos del mockup | CRUD de tablas y campos, fórmulas, conexiones, orden y confirmaciones |
| Cargar y ver `/proyectos/:id/usar` | Selector de tablas, pestañas de datos, formulario, tabla y ficha | Validación dinámica, valores iniciales, relaciones, guardar, editar, consultar y paginar |
| Subir `/cuaderno` | Zona de selección salvia, progreso, consejos y miniaturas | Cámara/galería, selección opcional de sistema y tabla, rotación, eliminación, límite de hojas y envío real |
| Revisar `/cuaderno/:loteId` | Encabezado azul, foto en papel, tabla petróleo y tarjetas móviles | Consulta de lectura, reintento, borrador local, correcciones, conexiones, destino y confirmación real |
| Stock `/proyectos/:id/stock` | Paneles de movimientos y existencias | Selección de tabla, movimientos, consultas y cálculos existentes |
| Reportes `/proyectos/:id/reportes` | Panel de filtros, totales de color y tabla | Filtros, consultas, estados de carga/error y descarga XLSX |

La ilustración del inicio está marcada como decorativa y usa piezas de ejemplo; no representa registros del negocio.

## Límites de la adaptación

El mockup contiene demostraciones que no tienen una funcionalidad equivalente disponible en el producto actual. No se trasladan su lectura simulada de imágenes, sus datos precargados como información de un negocio, el chat de Sami ni los recorridos demostrativos de mapa/proveedores o alertas. Las imágenes de Sami son apoyo visual. Tampoco se reemplazan los hooks, contratos HTTP, permisos, almacenamiento de sesión o reglas del backend por el JavaScript del prototipo.

Login, registro, stock y reportes extienden la identidad del mockup a flujos reales; no se presentan como una copia exacta de una pantalla que el prototipo no incluye.

## Responsive y accesibilidad

- Navegación inferior en móvil; navegación de escritorio a partir de su breakpoint. El modo del sistema usa dos columnas en pantallas pequeñas.
- Formularios y paneles apilados en móvil; tabla de revisión y foto en paralelo cuando hay espacio. Los datos se muestran como tarjetas en el celular.
- Las tablas anchas tienen desplazamiento dentro de su propio contenedor.
- Se conservan etiquetas de campos, mensajes de error, estados anunciados, foco visible y diálogos con cierre por Escape y retorno del foco.
- Las imágenes decorativas usan `alt=""` y las fotos cargadas conservan su descripción.
- Se respeta `prefers-reduced-motion`.

Las capturas y la comparación visual deben incluir **320, 375/390, 768, 1024 y 1440 px**. Revisar también zoom del navegador al 200 %, nombres largos, teclado virtual, orientación horizontal y áreas seguras del celular. Confirmar contraste AA sobre la superficie efectiva, incluidos los estados hover/activo y los textos secundarios.

Contrastes calculados a partir de los valores sRGB de los tokens, aplicando luminancia relativa:

| Combinación | Relación |
| --- | --- |
| Texto secundario `#5f6e73` sobre papel `#fffdfa` | 5,22:1 |
| Texto secundario sobre crema `#fff7eb` | 4,98:1 |
| Texto secundario sobre salvia `#f1f5ed` | 4,80:1 |
| Botón de apoyo: blanco sobre petróleo `#00506a` | 8,91:1 |
| Acción naranja: pizarra `#263443` sobre `#ee8000` | 4,67:1 |
| Acción naranja hover/activo: pizarra sobre `#f29122` | 5,35:1 |

Estas combinaciones superan 4,5:1. El cálculo no sustituye la revisión visual de superposiciones, transparencias o superficies distintas. El texto informativo del inicio conserva un mínimo de 14 px; los tamaños menores quedan en las ilustraciones decorativas.

## Validación automatizada realizada

La suite completa del frontend pasó: **373 pruebas en 30 archivos**, con `npm test -- --maxWorkers=2`. También pasaron ESLint global y el build de producción (incluye TypeScript).

El siguiente conjunto focalizado pasó: **52 pruebas**.

```sh
cd frontend
npx vitest run src/test/routes/cuaderno.test.tsx src/test/routes/revisarCuaderno.test.tsx src/test/api/auth.test.ts
npx vitest run src/test/routes/sesion.test.tsx
npx eslint src/components/AuthForm/AuthCard.tsx src/components/Cuaderno src/pages/Cuaderno
```

Cobertura de ese conjunto: 14 pruebas de subida, 24 de revisión, 6 de API de autenticación y 8 de sesión. Incluye selección de destino, cambio de sistema, relaciones entre tablas, borrador, correcciones, reintentos y guardado. ESLint pasó en esos componentes. Estas pruebas usan los handlers de prueba del proyecto; no acreditan por sí solas integración contra un servidor productivo ni una prueba de usabilidad con personas.

La revisión en navegador comprobó la landing a 320 px, el inicio y el cuaderno a 360 px, y el ingreso, la creación desde plantilla, el guardado y consulta de una fila, el constructor y stock en escritorio. Se usó el modo de simulación existente de Ensambla para esta comprobación local; el modo de producción conserva su configuración de API real. Tras corregir el contraste del modo activo al pasar el mouse, se repitieron las suites de navegación y accesibilidad: **16/16 pruebas**.

## Prueba de uso pendiente: cinco personas

La validación de ENS-187 requiere al menos cinco personas del público objetivo. Todavía debe realizarse; este cambio visual no la acredita ni cierra automáticamente las tareas de Jira.

Proponer a cada participante, sin asistirlo, el recorrido **foto → revisar → consultar**:

1. Ingresar a su espacio y encontrar la carga del cuaderno.
2. Seleccionar una foto legible y enviarla; si tiene tablas existentes, comprender la elección del destino.
3. Reconocer los datos dudosos, corregir uno y comparar con la foto.
4. Elegir dónde guardar y confirmar.
5. Encontrar lo guardado, consultar una fila y reconocer su origen.

Registrar dispositivo, duración, errores, dudas, intervenciones y resultado. Ejecutar al menos una sesión con teclado y una en celular. El criterio previsto es que **al menos cuatro de cinco** completen el recorrido sin asistencia. Consolidar hallazgos, priorizarlos y registrar correcciones antes de considerar terminada la prueba.

| Participante | Dispositivo | Completó sin ayuda | Dudas/errores | Duración | Corrección propuesta |
| --- | --- | --- | --- | --- | --- |
| 1 | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
| 2 | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
| 3 | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
| 4 | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
| 5 | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |

## Traer datos de una foto: revisión en tarjetas y campos nuevos (ENS-172)

El paso «Revisar» de **Traer datos de una foto** (`ModalImportar`) muestra un **registro por tarjeta**, no una tabla:
la foto queda arriba en el celular y al costado (fija) en la computadora. La lectura sigue siendo **simulada**
(`POST /importaciones/lectura`); el contrato que usa la pantalla es el que tendrá la lectura real.

- **Tarjetas (ENS-190):** cada dato se edita en la tarjeta y cada registro tiene «Quitar» (con «Deshacer»). Un dato
  dudoso se distingue por el borde punteado, el ícono y el texto «Revisá este dato: …», no sólo por el color; se
  resuelve corrigiéndolo o con «Está bien». Arriba se cuenta cuántos quedan por revisar.
- **Campos nuevos detectados (ENS-191):** si el bloque destino ya existe y la foto trae una columna que no tiene, una
  tarjeta al inicio la propone con su tipo sugerido y ejemplos de lo que se leyó. Se puede **aceptar**, **renombrar**,
  **cambiar el tipo** o **descartar** (y volver a sumar). Hasta decidir cada una no se puede incorporar; el nombre no
  puede estar vacío ni repetir un campo del bloque. Los aceptados aparecen en las tarjetas con la etiqueta «Campo
  nuevo» y se crean **junto con los registros, en una sola operación**: se ensaya sobre una copia y, si algo falla,
  no se guarda nada. Una columna descartada no se crea y sus valores se ignoran. Los registros que el bloque ya tenía
  no se modifican (el campo nuevo queda vacío en ellos).

Contrato de la lectura (`types/importacion.ts`): además de `filas` y `descripcion`, devuelve `camposNuevos` (campos
con `key`, `label` y `type`; sus valores vienen en cada fila por esa clave) y `dudas` (`fila`, `campo`, `motivo`).
El pedido suma `destinoExistente`: sólo con un bloque que ya está en el sistema se detectan columnas nuevas. La
lógica pura vive en `lib/sistema/revision.ts`; el backend real ya devuelve `camposNuevos` (ENS-188) y confirma
columnas nuevas y registros en una transacción (ENS-189).

