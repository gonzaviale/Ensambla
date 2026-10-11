# Lectura de cuadernos — ENS-120 / H-29

Implementación técnica aprobada: [plan-ENS-120.md](plan-ENS-120.md).
Esta etapa permite desarrollar y probar con el cliente **simulado** existente.
Todavía no hay un proveedor real elegido ni un set real de evaluación.

## Procesamiento

El worker consulta la cola persistida en PostgreSQL, reclama un lote pendiente y lee sus archivos privados.
Las transacciones del reclamo y del consumo son breves; la llamada a IA ocurre fuera de ellas.
Estados: `PENDIENTE → PROCESANDO → LISTO` o `ERROR`.
Los fallos recuperables vuelven a PENDIENTE con una demora; datos/archivos inválidos y cuota agotada
quedan en ERROR. El máximo automático es tres intentos, configurable.

Cada reclamo usa un token y una vigencia. Tras un reinicio, se recuperan los vencidos; una respuesta
vieja no puede sobrescribir otro intento ni duplicar consumo.
Se procesa un lote por instancia y sólo se admite una llamada al proveedor en ejecución por instancia.
Si un cliente ignora la cancelación, no se siguen acumulando llamadas; los reclamos pueden recuperarse.
La API de consulta responde independientemente del worker.

La propuesta conserva filas, confianza, advertencias y tipos. Un bloque del mismo negocio se reutiliza
si su nombre y cada columna tienen una coincidencia única con tipo/opciones compatibles.
Se ignoran diferencias de mayúsculas y acentos. Ante varios candidatos, se propone un bloque nuevo
para revisión; un destino explícito incompatible produce ERROR.
El mapeo relaciona las claves leídas con los UUID de campos existentes.
Esta historia **no crea registros definitivos ni confirma bloques nuevos**: eso es ENS-121 / H-30 (ver [Revisión y confirmación](#revisión-y-confirmación-ens-121--h-30)).

## Revisión y confirmación (ENS-121 / H-30)

La persona revisa la propuesta con la foto al lado, la corrige y recién entonces se guarda. Nada se escribe
en proyectos, bloques, campos ni registros hasta confirmar.

**Pantalla** (`/cuaderno/{loteId}`): la lectura tiene su propia dirección, así que se puede recargar. En celular
la foto va arriba y los datos debajo; en computadora, lado a lado. Se puede corregir cualquier celda, quitar y
agregar filas, renombrar, cambiar de tipo, quitar y agregar columnas, y renombrar la tabla. Los datos con
confianza menor al umbral (o vacíos) se resaltan y «Siguiente duda» los recorre. Lo corregido se conserva en
`sessionStorage` por lote hasta confirmar o descartar.

**Destino** al guardar: un proyecto nuevo, una tabla nueva dentro de un proyecto existente o, si la IA reconoció
una tabla del negocio (`bloqueExistenteId` + `mapeoCampos`), sumar las filas a ella.

| Ruta | Resultado |
|---|---|
| `POST /api/lotes/{loteId}/confirmar` | 201 `{ proyectoId, bloqueId, registros }` |
| `GET /api/lotes/{loteId}/fotos/{fotoId}` | 200 con los bytes de la foto (`Cache-Control: private`) |
| `GET /api/registros/{id}/foto-origen` | 200 con los bytes de la foto de la que salió el registro |

`POST …/confirmar` recibe el destino (`proyectoId` o `nombreProyecto`; `bloqueId` o `nombreBloque`), las `campos`
finales (`clave`, `campoId` si ya existe, `nombre`, `tipo`, `opciones`) y las `filas` (`pagina` desde 1 o `null` si
se agregó a mano, y `valores` por clave **ya tipados**: número, sí/no o texto). Se valida igual que una carga manual
(`Registro.validar`) y los errores vuelven ubicados: `filas[2].monto`, `campos[0].nombre`.

- Todo ocurre en **una transacción**: si algo falla no queda proyecto, tabla, campos ni registros a medias.
- El lote debe estar `LISTO` y pasa a `CONFIRMADO` con `confirmado_en` y `bloque_destino_id` mediante una
  sentencia condicional, así dos confirmaciones simultáneas no duplican datos (la segunda responde 400).
- Cada registro guarda `foto_origen_id` según su página; las filas agregadas a mano quedan sin foto.
- Máximo 500 filas por confirmación.
- Las fotos son privadas: se sirven con la sesión y sólo al negocio dueño (otro negocio recibe 404). Reemplazan a
  la «URL firmada» de Jira porque el almacenamiento es local; si se pasa a un almacenamiento externo, este es
  el único punto que cambia.
- `RegistroResponse` suma `fotoOrigenId` para saber si mostrar «Ver en el cuaderno».

## Contrato para integrar ENS-124 / T-090

La tarea de subida debe guardar archivos privados y crear, en su transacción, el lote PENDIENTE y sus fotos.
No se agrega otra ruta de subida en esta historia.

- Negocio y bloque de destino se validan desde la sesión en la subida.
- `foto_cuaderno.posicion` establece el orden; el lector recibe números consecutivos desde 1.
- Los archivos llegan comprimidos y rotados, como define `PaginaCuaderno`.
- El adaptador local admite claves `<uuid-negocio>/<archivo>`, relativas a una raíz privada.
  Rechaza rutas que terminen fuera del directorio de ese negocio y archivos vacíos o mayores a 20 MiB.
- Raíz de desarrollo: `datos/ia/cuadernos/fotos/`, excluida de Git (salvo `.gitkeep`), fuera de cualquier ruta pública.
  Se puede configurar una ruta absoluta.
- Otro almacenamiento puede implementar `ArchivoCuadernoPort`; la cola y el dominio no necesitan conocer su SDK.
- Coordinar estas consultas con ENS-124 si esa tarea agrega rutas equivalentes.

## Consumo y configuración

El día del límite es **UTC**. Antes de la llamada se reservan páginas en una operación atómica por
negocio y fecha. Una reserva rechazada no llama al proveedor.
Al finalizar, las páginas reservadas pasan a procesadas. Un intento interrumpido después de reservar
también cuenta páginas: evita reintentar indefinidamente sin límite.
Conserva el día de la reserva aunque termine después de medianoche.
Una falla anterior a reservar no consume páginas.

Las lecturas exitosas guardan tokens, duración y costo estimado. La simulación tiene costo cero.
Sin tarifas o sin respuesta del proveedor, el costo permanece desconocido.
Si `consumoCompleto=false`, el total de tokens/costo puede ser parcial; no representa una factura.

| Propiedad | Valor inicial | Uso |
|---|---|---|
| `ensambla.ia.procesamiento.habilitado` | `true` | Activar worker; variable `ENSAMBLA_IA_WORKER` |
| `ensambla.ia.procesamiento.intervalo` | `2000` ms | Demora entre ciclos |
| `ensambla.ia.timeout` | `60s` | Plazo de la llamada |
| `ensambla.ia.procesamiento.vigencia-reclamo` | `120s` | Debe superar el timeout |
| `ensambla.ia.procesamiento.demora-reintento` | `30s` | Demora no negativa |
| `ensambla.ia.procesamiento.max-intentos` | `3` | Entre 1 y 100 |
| `ensambla.ia.max-paginas-por-lectura` | `10` | Máximo por lote |
| `ensambla.ia.procesamiento.limite-diario` | `100` | Páginas por negocio; variable `ENSAMBLA_IA_LIMITE_DIARIO` |
| `ensambla.ia.procesamiento.archivos-raiz` | `../datos/ia/cuadernos/fotos` | Ruta relativa al directorio backend |
| `ensambla.ia.procesamiento.precio-entrada-millon` | desconocido | USD por millón de tokens de entrada |
| `ensambla.ia.procesamiento.precio-salida-millon` | desconocido | USD por millón de tokens de salida |

Con `proveedor=simulado`, los resultados son datos de demostración del JSON del proyecto.
Activar el proveedor real requiere implementar/registrar su cliente después de la decisión del equipo.

## API privada

| Ruta | Resultado |
|---|---|
| `GET /api/lotes/{loteId}/lectura` | 200: loteId, estado, motivoError, intentos, propuesta |
| `POST /api/lotes/{loteId}/reintentar` | 202 sin body: reencola ERROR propio; inicia una nueva serie de intentos |
| `GET /api/ia/consumo` | 200: fecha, limite, paginas, reservadas, disponibles, tokensEntrada, tokensSalida, costoEstimadoUsd, consumoCompleto |

Reintentar exige CSRF y vuelve a verificar disponibilidad. Si se pierde disponibilidad entre el pedido
y el worker, la reserva vuelve a rechazar el lote sin invocar IA.
Todos los recursos ajenos responden 404; sin sesión, 401; escritura sin CSRF, 403.
El negocio nunca se toma de la URL, body o query.
Los errores de regla (estado incorrecto o límite) responden 400 con mensaje claro.

La propuesta HTTP contiene tipoRegistro, nombre, bloqueExistenteId, campos, filas, advertencias,
mapeoCampos y consumo. Cada dato conserva `valor` nullable y `confianza`.
La consulta no entrega claves de archivos, bytes ni credenciales.

## Evaluación

Fotos y anotaciones: [carpeta de carga](../datos/ia/cuadernos/README.md).
Un comando desde la raíz:

```powershell
python scripts/evaluar_cuadernos.py
```

El script verifica el set y compila la herramienta en `backend/target/evaluacion-cli`, usando el mismo
lector/prompt/validación que el worker. No abre servidor ni conecta a la base.
También admite `--jar ruta/al/backend.jar` para reutilizar un artefacto de esta rama.
Proveedor/modelo se toman de las variables IA o de `--proveedor` / `--modelo`.
Las claves permanecen en variables de entorno o en la configuración del cliente; no se pasan como argumentos.

Para comprobar el circuito con una muestra sintética externa al set real:
`python scripts/evaluar_cuadernos.py --set ruta/a/muestra --permitir-simulado`.
La carpeta real preparada para el usuario permanece vacía.

Reporte por tipo de registro: clasificación, campos/tipos, datos, errores, latencia, tokens y costo
cuando hay tarifas. Filas/celdas adicionales se penalizan; `null` se compara sin inventar valores.
Se guardan hashes de fotos, anotaciones y manifest para reproducibilidad.
Opcionales: `--meta porcentaje`, `--precio-entrada-millon tarifa`, `--precio-salida-millon tarifa`
y `--reporte ruta.json`. La meta evalúa clasificación, campos y datos de cada categoría presente.
El costo por página es una estimación; fallos sin consumo dejan su costo desconocido.
El tiempo total del comando puede limitarse con `--timeout segundos` (900 por defecto).

Un set vacío falla con instrucciones y sin métricas. Un set real requiere al menos 30 fotos únicas
y consentimiento/anonimización declarados en el manifest.
Una ejecución simulada o menor a 30 casos nunca marca evidencia real ni cumplimiento de meta.
Para comparar 2–3 modelos y OCR, ejecutar cada adaptador sobre la misma versión del set y guardar
un reporte distinto; el adaptador OCR todavía depende de la decisión del spike.

## Estado de los criterios de Jira

| Subtarea | Estado de esta entrega |
|---|---|
| ENS-127 | Carpeta/contrato preparados; faltan las 30+ fotos reales |
| ENS-128 | Circuito de medición preparado; falta ejecutar modelos/OCR y elegir proveedor |
| ENS-129 | [Informe pendiente](comparacion-modelos-ia.md); faltan resultados y revisión del equipo |
| ENS-130 | Extracción intercambiable y validación probadas con simulado; falta cliente real elegido |
| ENS-131 | Cola/estados/reintentos/timeout/recuperación implementados; falta integrar subida ENS-124 |
| ENS-132 | Propuesta/mapeo tipado implementados; falta medir con fotos reales |
| ENS-133 | Reserva, límites y consulta implementados; tarifas reales pendientes |
| ENS-134 | Herramienta/pruebas preparadas; precisión real y meta acordada pendientes |
| ENS-121 | Revisión, edición, confirmación y foto de origen implementadas; falta probarlo con fotos reales |

Estos pendientes fueron acordados explícitamente con el usuario. Esta entrega no demuestra la
precisión real del producto ni declara completos los criterios que requieren ese material.

## Validación de esta entrega

`mvnw clean verify`: pruebas unitarias, seguridad HTTP y PostgreSQL real con migraciones V1–V4;
JaCoCo exige 100 % de líneas y ramas por clase.
Incluye reservas/reclamos concurrentes, finalización idempotente, respuestas tardías y cambio de día.
`python -m unittest discover -s scripts/tests -v`: reglas de medición con datos sintéticos.
Ver [backend/TESTING.md](../backend/TESTING.md) para reproducir los controles.
