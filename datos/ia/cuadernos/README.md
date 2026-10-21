# Set privado de cuadernos — ENS-127

Esta carpeta está preparada y vacía: todavía no existe el set real de fotos.

- `fotos/`: páginas reales anonimizadas, con consentimiento. Usar nombres sin datos personales,
  por ejemplo `pagina-001.jpg`, `pagina-002.png`.
- `esperados/`: un JSON revisado por una persona por cada foto, con el mismo nombre base:
  `pagina-001.json`, `pagina-002.json`. Describe el tipo de registro, bloque, campos y renglones
  que efectivamente se ven en esa página.
- `reportes/`: resultados de la evaluación de modelos y versiones del prompt.

El formato de la lectura está en
[`esquema-lectura-cuaderno.json`](../../../backend/src/main/resources/ia/esquema-lectura-cuaderno.json).
El JSON simulado del backend sirve como ejemplo de formato; no es un resultado esperado de una foto real.

Las imágenes, sus respuestas esperadas, el manifiesto privado y los reportes están excluidos de Git.
No publicar fotos ni anotaciones en el repositorio. El set definitivo de ENS-127 necesita al menos
30 fotos reales de distintos tipos de registro y calidad, además de almacenamiento privado versionado.
La evaluación real y la elección del modelo se harán cuando ese material esté disponible.

## Manifest privado

Copiar `manifest.example.json` a `manifest.json`; completar la versión del set y confirmar
consentimiento/anonimización sólo cuando se hayan obtenido y revisado.
Agregar un caso por foto:

```json
{
  "id": "pagina-001",
  "foto": "fotos/pagina-001.jpg",
  "esperado": "esperados/pagina-001.json"
}
```

Cada anotación describe una foto, con `pagina: 1` y renglones únicos; los valores son texto
normalizado o null. Usar el esquema enlazado, incluidas confianza y advertencias.
La confianza humana no entra al cálculo de exactitud: se comparan valores, campos/tipos y clasificación.
No copiar los valores del ejemplo simulado como si fueran anotaciones reales.

Desde la raíz: `python scripts/evaluar_cuadernos.py`.
Mientras no haya fotos, el comando devuelve instrucciones y termina sin informar precisión.
Más detalles en [lectura-cuaderno-ia.md](../../../docs/lectura-cuaderno-ia.md).

## Columnas nuevas y casos borde (ENS-192 / T-130)

Para medir si la lectura detecta las columnas que el bloque destino no tiene (ENS-188), un caso del manifest puede
llevar el **bloque existente al que irían las filas** (`destino`) y su anotación, las **columnas que la foto trae y el
bloque no tiene** (`camposNuevos`). Con `destino`, la IA ve sólo ese bloque, igual que al subir las fotos a un bloque
elegido, y después se aplican las mismas reglas de cruce que la lectura real (nombre igual, sinónimos, sin duplicar).

```json
{
  "id": "pagina-014",
  "foto": "fotos/pagina-014.jpg",
  "esperado": "esperados/pagina-014.json",
  "destino": {
    "nombre": "Clientes",
    "campos": [
      { "nombre": "Nombre", "tipo": "TEXTO" },
      { "nombre": "Celular", "tipo": "TEXTO" }
    ]
  }
}
```

y en `esperados/pagina-014.json`, además de lo habitual: `"camposNuevos": [{ "nombre": "Dirección", "tipo": "TEXTO" }]`
(`tipo` es opcional: sin él, cualquier tipo cuenta como acierto de tipo). Un caso con `destino` **tiene que** declarar
`camposNuevos`, aunque sea vacío (`[]`: la foto no trae nada que el bloque no tenga). Los nombres se comparan sin
tildes, mayúsculas ni puntuación («¿Pagó?» = «pago»); los tipos son `TEXTO`, `NUMERO`, `FECHA`, `OPCION` o `SI_NO`.

Qué fotos sumar (cada una con su resultado esperado):

| Caso | `destino` | `camposNuevos` esperado |
|---|---|---|
| Columna que el bloque no tiene (por ejemplo «Teléfono») | sin ese campo | esa columna, con su tipo |
| Nombre parecido a un campo existente («Tel» / «Celular» / «Teléfono») | con el campo | vacío: no se duplica |
| Columna sin datos (sólo el encabezado) | cualquiera | vacío: no se propone |
| Todas las columnas ya están en el bloque | con todos | vacío |
| Varias columnas nuevas a la vez | con pocos campos | todas |
| Foto que no es de ese bloque | otro bloque | el lote se rechaza («no es compatible»): se cuenta como error de extracción |

El reporte suma `columnasNuevas` (y el mismo dato por caso): `precisionPct` (de las detectadas, cuántas eran nuevas
de verdad: un **falso positivo** es, por ejemplo, duplicar un campo existente), `recallPct` (de las esperadas, cuántas
se detectaron), `tipoCorrectoPct` y `casosExactosPct` (casos en que lo detectado coincide exactamente con lo
esperado). Con `--meta`, estas métricas también tienen que alcanzarla (si una no tiene base, por ejemplo no hay
columnas nuevas esperadas, no se exige).

**Muestra sintética para verificar el circuito:** `scripts/muestras/columnas-nuevas/` (cuatro casos con el lector
simulado; no demuestra precisión real):

```powershell
python scripts/evaluar_cuadernos.py --set scripts/muestras/columnas-nuevas --permitir-simulado
```

## Corrida real con un modelo de Anthropic (sólo para la prueba)

El backend ya trae un cliente real (`ClienteModeloVisionAnthropic`, `ensambla.ia.proveedor=anthropic`). Para
probarlo con fotos de verdad:

1. **Clave de API.** Una suscripción Claude Pro o Claude Code **no incluye** acceso a la API: hace falta una clave de
   la consola de desarrolladores (console.anthropic.com → API keys), con saldo cargado. Se factura aparte, por
   uso. Para unas pocas páginas alcanza con un saldo mínimo.
2. **Sólo en el entorno, nunca en un archivo ni en Git.** En PowerShell, en la misma ventana donde se corre:
   `$env:ENSAMBLA_IA_API_KEY = "<tu clave>"`. Se pierde al cerrar la ventana.
3. **Fotos.** Páginas sin datos personales de terceros (o anonimizadas): se envían al servicio de Anthropic. Una por
   caso en `fotos/`, con su anotación en `esperados/` y su entrada en `manifest.json` (ver arriba).
4. **Comando** (con menos de 30 fotos hace falta `--permitir-simulado`, que sólo habilita sets chicos: el reporte
   igual marca `evidenciaReal: false`):

```powershell
python scripts/evaluar_cuadernos.py --proveedor anthropic --modelo <modelo> --permitir-simulado `
  --precio-entrada-millon <usd> --precio-salida-millon <usd>
```

   `<modelo>` es un identificador vigente de la consola. Las tarifas se toman de la página de precios de Anthropic el
   día de la prueba (sin ellas el costo queda desconocido, nunca en cero).
5. El reporte queda en `datos/ia/cuadernos/reportes/evaluacion.json` (fuera de Git).

Una prueba con pocas fotos **demuestra que el circuito funciona con un modelo real**, no la precisión del producto:
para eso siguen haciendo falta las 30+ fotos reales con consentimiento (ENS-127) y la decisión del equipo (ENS-129).
