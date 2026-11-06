# Presentación del Laboratorio de decisiones

Demostración propuesta para el viernes 6 de noviembre de 2026.

## Qué se muestra y qué no

El recorrido integrado en `/laboratorio` es un prototipo navegable con datos ficticios. Sirve para validar la idea, el
flujo y la forma de explicar los conceptos económicos. Todavía no consulta un motor contable ni modifica registros.

Durante la reunión conviene decirlo al comenzar:

> Lo que vamos a mostrar es una propuesta funcional navegable. Las pantallas están integradas en Ensambla, pero los
> cálculos todavía usan un caso de demostración. Queremos validar el enfoque antes de construir el motor económico.

## Idea central

Ensambla evoluciona de un sistema que registra operaciones a uno que ayuda a comprender y ensayar decisiones:

```text
Registrar → interpretar → detectar → explicar → simular → decidir → medir
```

Los cálculos pertenecerían a algoritmos deterministas y comprobables. Sami no inventaría importes: traduciría el
fundamento, los datos utilizados, los supuestos y las limitaciones.

## Recorrido recomendado

### 1. Inicio: gemelo económico

Abrir **Laboratorio** desde la navegación principal y seleccionar **Ver el recorrido completo**.

Texto sugerido:

> El Laboratorio sería una representación económica del comercio. Permitiría probar una decisión sobre una copia de
> sus datos, sin modificar el cuaderno real.

### 2. Aviso económico

Mostrar el aviso «Vendiste más, pero tu margen bajó».

Señalar:

- La comparación entre ventas y resultado bruto.
- Los productos que explican la diferencia.
- El nivel de confianza y la cobertura de costos.
- Las acciones **Entender por qué** y **Probar una solución**.

Texto sugerido:

> Avisos deja de señalar solamente stock o precios. El motor económico detectaría contradicciones: por ejemplo, vender
> más pero obtener menos margen. El aviso resume la situación y el Laboratorio permite investigarla.

Elegir **Entender por qué**.

### 3. Fundamento contable

Recorrer el estado de resultados:

```text
Ventas registradas                         $1.200.000
- Costo estimado de lo vendido              $700.000
= Resultado bruto                            $500.000
- Gastos operativos                          $320.000
= Resultado operativo estimado               $180.000
```

Explicar:

- Las ventas fiadas forman parte del resultado, pero no equivalen a dinero disponible.
- El costo se estima con los datos registrados.
- La confianza depende de la cantidad y completitud de la información.
- Sami traduce el cálculo a lenguaje cotidiano.

No presentar este informe como contabilidad legal, liquidación impositiva ni reemplazo de un contador.

### 4. Definir una meta

Seleccionar **Convertirlo en una meta** y usar:

> Quiero obtener una ganancia de $500.000 por mes sin aumentar demasiado los precios.

Texto sugerido:

> La persona expresa un objetivo sin completar una planilla financiera. El sistema lo traduce a una meta medible y
> separa resultado, cobranzas y riesgo.

### 5. Comparar caminos

Mostrar las tres alternativas:

- Camino combinado.
- Aumentar volumen de ventas.
- Mejorar el margen por venta.

Texto sugerido:

> El sistema no ofrece una única respuesta. Expone alternativas, esfuerzo requerido, riesgo y confianza. Recomendamos
> el camino combinado porque no depende de una sola variable extrema.

### 6. Probar el escenario

Modificar visualmente ventas, margen y gastos. Señalar:

- Resultado proyectado.
- Punto de equilibrio.
- Margen de seguridad.
- Escenario pesimista, esperado y elegido.
- Supuesto de caída de demanda.

Texto sugerido:

> Una simulación no es una predicción garantizada. Cada resultado conserva los supuestos que lo producen. La persona
> puede discutirlos con Sami y probar qué pasa si se equivoca.

### 7. Cruzar con Mi zona

Abrir la pestaña **Mi zona** dentro del recorrido.

Mostrar:

- Precio propio.
- Promedio y rango agregado de la zona.
- Precio necesario para el margen objetivo.
- Alternativas comparadas.
- Aviso de privacidad y datos ficticios.

Texto sugerido:

> La información interna dice qué precio necesita el negocio para alcanzar un margen. Mi zona aporta una referencia
> externa agregada. Ninguna de las dos decide sola: se combinan para evaluar una alternativa razonable.

No afirmar que los datos actuales provienen de comercios reales ni que se actualizan en tiempo real.

### 8. Convertir la decisión en un plan

Elegir **Usar en mi plan**.

Mostrar las tres tareas semanales y aclarar:

- Nada se aplica automáticamente.
- Cada cambio requiere confirmación.
- El plan tiene un objetivo y una revisión periódica.

Texto sugerido:

> El simulador no termina en un gráfico. La alternativa elegida se transforma en acciones pequeñas, medibles y
> revisables.

### 9. Medir lo ocurrido

Elegir **Ver cómo se mediría**.

Comparar:

- Resultado proyectado: $500.000.
- Resultado observado: $462.000.
- Diferencia: $38.000.
- Supuesto que no se cumplió.

Texto sugerido:

> Ensambla cerraría el ciclo comparando lo esperado con lo ocurrido. No necesita afirmar que una inteligencia
> artificial aprendió sola: puede ajustar los próximos supuestos con datos observables y mantener toda la trazabilidad.

## Preguntas probables

### ¿Esto es inteligencia artificial?

La base debe ser un motor matemático determinista. Sami puede usar IA para conversar y traducir, pero todos los importes
deben proceder del motor económico y poder reproducirse.

### ¿Es un sistema contable?

Es un sistema de gestión con estado de resultados estimado, punto de equilibrio, márgenes y simulación. La primera
versión no incluye libro diario legal, impuestos ni balances certificados.

### ¿Cómo evita recomendaciones incorrectas?

Muestra datos utilizados, supuestos, cobertura, confianza y limitaciones. No ejecuta cambios sin confirmación.

### ¿De dónde salen los escenarios?

De los datos registrados y de variaciones explícitas de ventas, margen y gastos. Pesimista y optimista no son etiquetas
mágicas: cada uno conserva sus supuestos.

### ¿Cómo se integra con lo existente?

- **Cuadernos:** aportan ventas, inventario, costos, gastos y clientes.
- **Avisos:** detectan riesgos y oportunidades.
- **Laboratorio:** explica y simula.
- **Mi zona:** aporta referencias agregadas.
- **Sami:** traduce y guía.
- **Reportes:** podrían descargar el estado económico y el plan.

## Cierre sugerido

> Ensambla no reemplaza la decisión del comerciante. Organiza sus datos, traduce su situación económica y le permite
> ensayar alternativas antes de actuar. La innovación está en unir gestión, contabilidad comprensible, contexto local y
> simulación explicable en un mismo recorrido.
