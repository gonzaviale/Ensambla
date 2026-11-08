# Objetivos y Sami Academy

Rama: `feat/objetivos-sami-academy`. Reorganiza el recorrido de 9 pantallas del «laboratorio de decisiones»
([diseño](superpowers/specs/2026-11-05-laboratorio-decisiones-contable-design.md),
[guion de presentación](presentacion-laboratorio-decisiones-2026-11-06.md)) en dos secciones. En pantalla ya no aparece la
palabra «laboratorio».

## Dónde quedó cada pantalla

| Antes (paso del recorrido) | Ahora |
|---|---|
| Inicio | **Objetivos**: cómo van tus objetivos (si no hay ninguno, «Definí tu primer objetivo») |
| Aviso económico | Se quitó de acá: pasa al inicio de la app (lo hace otra persona) |
| Fundamento contable | Sami Academy → «Cómo se forma tu resultado» |
| Definir meta | Objetivos → formulario «¿Qué querés lograr?» |
| Caminos posibles | Objetivos → ícono «Recomendación de Sami» dentro de cada tarjeta |
| Probar escenario | Sami Academy → «Probá un escenario», con los controles funcionando; se llega desde la tarjeta del objetivo |
| Mi zona | Pestaña «Cruce con Mi zona» dentro de Objetivos. **En evaluación**: puede quedar, moverse o borrarse |
| Plan de acción | Objetivos → «Ver plan de acción» de cada tarjeta (`/objetivos/:id`) |
| Seguimiento | Se fundió con el plan de acción: avance, resultado de hoy y lo que dice Sami sobre la tendencia |

## Rutas

- `/objetivos`: lista de objetivos y, en la otra pestaña, el cruce con Mi zona.
- `/objetivos/:objetivoId`: plan de acción y seguimiento de un objetivo.
- `/sami-academy?leccion=…&objetivo=…`: lecciones `resultado`, `metricas`, `recomendaciones`, `escenario` y `consejos`.
  Sin `leccion` (o con una inválida) abre la primera. `objetivo` elige con cuál se prueba el escenario.

## Sami Academy

1. **Cómo se forma tu resultado**: estado de resultados de gestión y traducción a palabras simples.
2. **Tus métricas, una por una**: qué es, cómo se calcula y cómo leer cada una.
3. **Cómo recomienda Sami**: qué datos usa, cómo arma los caminos, cuánto confía y qué nunca hace.
4. **Probá un escenario**: tres palancas (ventas, margen, gastos) sobre el objetivo elegido.
5. **Sugerencias para usar Ensambla**: hábitos que mejoran la confianza de las cuentas.

## Qué es real y qué es simulado

Todavía no hay backend para objetivos ni para el cálculo económico.

- Los objetivos se guardan en el navegador (`localStorage`, clave `ensambla.objetivos.v1`), no por cuenta.
- La demo cargada (`npm run dev:demo`) siembra una sola vez un objetivo de ejemplo con seis semanas de historia. Un objetivo
  nuevo arranca con una sola semana y Sami avisa que todavía no hay con qué comparar.
- Los importes salen de la base de ejemplo «Almacén de barrio» (resultado $180.000 por mes). Sami toma el primer importe
  que aparece en el texto del objetivo como meta; si no hay ninguno, usa $500.000.
- Los caminos y el escenario escalan un camino de referencia (+12 % de ventas, −$80.000 de gastos, +3 puntos de margen para
  cubrir $320.000) a la brecha de cada objetivo, y reparten esa brecha entre las tres palancas (40 % / 25 % / 35 %). Con las
  tres al valor recomendado el resultado llega a la meta. Es una estimación de demostración, y la pantalla lo dice.
- «Cruce con Mi zona» sigue con datos ficticios, como antes.

Código: `src/lib/objetivos.ts` (cuentas), `src/stores/objetivosStore.ts`, `src/pages/Objetivos/`, `src/pages/SamiAcademy/`.
