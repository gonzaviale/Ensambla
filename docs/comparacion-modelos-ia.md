# Comparación y elección de IA — ENS-128 / ENS-129

**Estado: pendiente de fotos reales y decisión del equipo.** No se ejecutaron modelos reales ni OCR.
No hay métricas de precisión/costo/latencia reales ni un proveedor recomendado por esta entrega.
El usuario autorizó preparar la infraestructura y postergar estas decisiones.

## Evidencia que falta

1. Reunir 30+ páginas consentidas y anonimizadas, variadas por tipo y calidad, con anotaciones humanas.
   Versionar el set en almacenamiento privado; ver [carpeta y manifest](../datos/ia/cuadernos/README.md).
2. Seleccionar 2–3 candidatos multimodales y una alternativa OCR disponibles para el equipo.
   Conectar cada cliente al contrato de extracción sin cambiar dominio ni cola.
3. Correr [la evaluación](lectura-cuaderno-ia.md#evaluación) para cada candidato usando la misma
   versión del set/prompt. Registrar precisión por categoría, errores, latencias y tokens.
4. Incorporar tarifas verificadas para estimar costo por página. Registrar fecha de consulta,
   límites del proveedor, región, conservación de imágenes y condiciones de uso de datos.
5. Revisar resultados con el equipo y acordar proveedor/modelo, meta por categoría y condiciones
   que exigen revisión humana. Evaluar costos junto con los límites diarios del MVP.

## Criterios de decisión

La selección debe considerar clasificación de ventas/gastos/pedidos/stock/fiados, fidelidad de datos
y tipos, incertidumbre ante letra ilegible, costo por página, latencia y límites del servicio.
Se prioriza que una respuesta desconocida permanezca ilegible y requiera revisión.
El modelo seleccionado debe devolver el JSON validado o fallar de forma observable.

## Registro de la decisión

No completado. Cuando se ejecute el spike, adjuntar aquí las versiones/hashes del set, reportes de
cada candidato, enlaces a tarifas/condiciones verificadas, revisión del equipo y motivo de la elección.
La simulación sólo verifica el circuito y no sustituye esta evidencia.
