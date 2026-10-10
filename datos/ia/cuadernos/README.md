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
