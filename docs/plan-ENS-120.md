# ENS-120 · Lectura del cuaderno con IA

Fecha: 2026-10-10. Rama: `feat/ens-120-lectura-cuaderno-ia`.
Base: `dev`, actualizada con `git pull --ff-only origin dev`, commit `8a7027e`.
Fuente: https://gonzaloleonelviale.atlassian.net/browse/ENS-120

## Alcance acordado y estado del diseño

El usuario pidió completar la historia y sus ocho subtareas, respetando Clean Architecture,
las skills de `.claude/skills`, pruebas con cobertura completa y PR hacia `dev`.
Confirmó que el set real no existe y que el proveedor/modelo aún no está decidido.
Autorizó preparar una carpeta vacía para cargar ese material después.

El usuario aprobó este diseño el 2026-10-10. La implementación sigue los pasos de abajo.
La implementación puede avanzar con el proveedor simulado existente. La recopilación de fotos,
la comparación real de modelos, su selección por el equipo y la meta de precisión se mantienen
pendientes: los tests con datos sintéticos no prueban esos criterios de Jira.

## Estado verificado de dev

- V1–V3 ya definen usuario, negocio, proyectos, bloques, campos, lotes, fotos y lecturas de IA.
- `LectorCuadernoPort` separa al dominio del proveedor.
- `ClienteModeloVision`, `PromptLecturaCuaderno`, JSON Schema y `LecturaIaJsonMapper` existen;
  hay un cliente simulado, sin conexión a un modelo real.
- `lectura_ia` ya puede guardar resultado/error, proveedor, modelo, versión del prompt,
  páginas, tokens, costo y duración por intento.
- Existen entidades y repositorios JPA básicos, pero no el worker, su gestión de estados,
  los límites diarios ni la consulta HTTP del procesamiento/consumo.
- `NegocioService` resuelve el negocio del usuario autenticado. Se reutiliza esa regla.
- ENS-124 / T-090 corresponde a otro trabajo: `POST /uploads` guarda fotos privadas y crea
  el lote pendiente. Todavía no está implementado en esta base.
- El único `Clock` compartido ya se declara en `AuthUseCaseConfig`; no se agregará otro bean.

## Alternativas de procesamiento

1. **Cola persistida en PostgreSQL, recomendada.** Reutiliza `lote_subida`, permite recuperar
   lotes tras reiniciar y evita agregar un servicio externo. Las transacciones para reclamar
   trabajo y reservar consumo son breves; la llamada al proveedor ocurre fuera de ellas.
2. Cola sólo en memoria: implementación menor, pero puede perder trabajo y no coordina
   varias instancias. No cumple bien la recuperación requerida.
3. Broker externo: permite más escala, pero agrega infraestructura y operación que hoy no
   necesita el alcance del MVP. No se incorporará sin una necesidad concreta.

## Propuesta por subtarea

| Jira | Trabajo de esta etapa | Criterio que requiere material o decisión posterior |
|---|---|---|
| ENS-127 / T-093 | Carpeta vacía, instrucciones y contrato de anotación; datos fuera de Git | Reunir 30+ fotos consentidas/anonimizadas y versionarlas en bucket privado |
| ENS-128 / T-094 | Preparar ejecución reproducible y captura de precisión, costo y latencia por modelo | Ejecutar 2–3 modelos multimodales y OCR sobre el set real; decisión del equipo |
| ENS-129 / T-110 | Documento de comparación con estado explícito y criterios de selección | Resultados reales, condiciones del proveedor elegido y meta de precisión revisada por el equipo |
| ENS-130 / T-095 | Completar validación de la extracción y contrato intercambiable; probar con cliente simulado | Conectar y verificar el cliente del proveedor real cuando se elija |
| ENS-131 / T-096 | Worker persistente, reclamo exclusivo, estados, timeout, reintentos y recuperación | Integración con la subida y lectura de archivos privados de ENS-124 |
| ENS-132 / T-097 | Propuesta tipada y mapeo estable a bloques del propio negocio; conservar filas/confianza | Medición sobre fotos reales y posterior confirmación de H-30 |
| ENS-133 / T-098 | Reservar límite diario sin carreras, registrar intentos/consumo y permitir consulta | Tarifas reales del modelo para estimaciones de costo precisas |
| ENS-134 / T-099 | Un comando de evaluación, reporte por tipo de registro y control de meta configurada | Ejecutar sobre el set real; fijar la meta tras el spike |

## Extracción y propuesta

- Reutilizar el puerto y cliente existentes. Ningún controller o servicio de dominio
  dependerá de HTTP, SDK o tipos específicos de un proveedor.
- Validar esquema y reglas semánticas: tipos permitidos, claves únicas, campos de cada fila,
  referencias a páginas/renglones, opciones y confianza entre 0 y 1.
- Un valor ilegible conserva `null` y su confianza; no se inventan datos faltantes.
- Cargar el contexto de bloques/campos del negocio del lote; nunca aceptar ese negocio
  desde el body o la URL. Rechazar referencias cruzadas y consultar recursos ajenos como 404.
- Resolver mapeos de columnas en dominio, con tipos compatibles y criterio determinista.
  Un caso ambiguo queda como propuesta para revisión, sin anexar registros a otro bloque.
- Dos páginas del mismo cuaderno dentro de un lote deben conservar la misma propuesta
  de bloque y el vínculo al bloque existente cuando corresponda.
- H-29 persiste una propuesta; la aprobación y creación definitiva de registros pertenece
  a ENS-121 / H-30 y no se implementa como efecto automático del worker.

## Cola y estados

- El lote que crea `POST /uploads` queda `PENDIENTE`. El worker lo reclama en una transacción
  que evita que dos instancias procesen el mismo trabajo al mismo tiempo.
- Transiciones: `PENDIENTE → PROCESANDO → LISTO` o `ERROR`. Un error final siempre deja un motivo claro.
- Guardar cada intento y su resultado/error. El tiempo entra a los servicios desde use cases
  con el `Clock` existente, para poder probar timeout, expiración y cambios de día.
- Configurar concurrencia acotada, timeout y máximo de intentos. Reintentar sólo fallos
  recuperables; no reintentar automáticamente un lote inválido o que agotó el límite diario.
- Recuperar reclamos vencidos tras reiniciar y evitar que un resultado tardío sobrescriba
  el de otro intento. No mantener bloqueos de base durante la llamada a IA.
- El acceso a las imágenes se hace mediante un puerto de almacenamiento privado. La
  integración concreta dependerá del contrato de ENS-124; no se construirá una segunda API de subida.

## Consumo y API propuesta

- Reservar páginas antes de llamar al proveedor, con una operación atómica por negocio/día.
  Dos lotes concurrentes no deben saltarse el límite.
- Configurar el límite diario y registrar por intento páginas, tokens, duración y costo.
  El costo desconocido se informa como desconocido, no como cero; el simulado no consume
  un servicio de pago. Las tarifas se configuran después de elegir proveedor/modelo.
- `GET /api/lotes/{loteId}/lectura`: estado, motivo de error y propuesta disponible.
- `POST /api/lotes/{loteId}/reintentar`: reencolar un lote fallido del propio negocio;
  exige sesión y CSRF y vuelve a comprobar el límite.
- `GET /api/ia/consumo`: consumo, reserva, límite y disponibilidad del negocio actual.
- La consulta del lote no invoca IA. La API responde mientras el worker procesa en segundo plano.
- Coordinar estas rutas con ENS-124 antes de integrar si su contrato agrega consultas equivalentes.

## Carpeta de fotos y evaluación

Ubicación local: `datos/ia/cuadernos/`.

- `fotos/`: páginas reales; usar nombres anónimos como `pagina-001.jpg`.
- `esperados/`: anotaciones revisadas, con el mismo nombre base (`pagina-001.json`).
- `reportes/`: informes de ejecuciones.

El script verificará presencia, correspondencia de fotos/anotaciones y versión del set,
ejecutará el servicio de extracción configurado y comparará el resultado con las anotaciones.
Reportará datos correctos/esperados por tipo de registro, campos/tipos, fallos de esquema,
latencia y consumo. El costo sólo se calculará cuando exista una tarifa configurada.

Un set ausente o vacío debe producir un mensaje claro y terminar sin informar precisión
ni éxito. Una ejecución simulada se identifica como tal y no habilita el cierre del spike.
La meta de precisión se ingresa después del acuerdo del equipo; no se fijará una cifra inventada.

## Migraciones y validación

- Mantener V1–V3 sin modificaciones. Si reclamos, reintentos o reservas necesitan columnas
  o tablas, usar la siguiente versión libre de Flyway y actualizar `modelo-datos.md`.
- Tests por capa según `escribir-tests`, con 100% de líneas y ramas por clase y `clean verify`.
- Integración PostgreSQL: aislamiento, reclamo concurrente, reserva diaria, persistencia de
  resultados, rollback, reintentos y recuperación tras timeout.
- HTTP: 401 sin sesión, 403 sin CSRF, 404 para un lote ajeno, estados/errores legibles y
  consulta del consumo sin disparar trabajo ni filtrar datos de otro negocio.
- Evaluación: anotaciones inválidas, set vacío, cálculos por categoría y salidas reproducibles.
- Si hay cambios del cliente, mantener `api → hooks → components`, validar respuestas con
  zod y pasar lint, tipos, tests y build. No rehacer la interfaz de subida de ENS-119.

## Entrega Git

Commits pequeños por responsabilidad, en imperativo y minúscula, por ejemplo
`feat(ia): procesar lotes pendientes en segundo plano` y
`test(ia): verificar limites diarios con procesamiento concurrente`.
Antes del push, actualizar referencias y rebasar sobre `origin/dev`. Abrir PR hacia `dev`
con la plantilla real `.github/PULL_REQUEST_TEMPLATE.md`; no autoaprobar ni integrar el PR.
El PR y la documentación deben distinguir trabajo terminado de criterios diferidos por el usuario.

## Secuencia de implementación aprobada

1. Agregar puertos/modelos de procesamiento, propuesta tipada y política de reintentos/costo.
2. Crear V4 con reclamos y reservas diarias; implementar operaciones atómicas en PostgreSQL.
3. Conectar almacenamiento privado local, timeout y worker configurable al lector existente.
4. Exponer consulta/reintento/consumo con sesión, CSRF y aislamiento por negocio.
5. Preparar evaluación reproducible e informe de comparación pendiente; documentar ENS-124.
6. Probar reglas, concurrencia, recuperación, HTTP y evaluación; exigir cobertura completa.
7. Crear commits por responsabilidad, rebasar sobre dev, publicar rama y abrir PR hacia dev.
