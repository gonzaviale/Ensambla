# Ensambla — «Se ensambla solo» (brief de trabajo)

Rama `feat/ensambla-se-ensambla-solo` (desde `origin/dev`). Documento único para quien construye pantallas.
Si algo no está acá, **decidí con sentido común y avisalo en el informe**; no explores el repo sin necesidad.

## La idea

> **Contá tu negocio. Ensambla lo arma.** No cambiás de hábito: contás qué hacés, Ensambla arma tu sistema con piezas
> (bloques) y cada pieza **desbloquea una respuesta** («te quedaron $ 182.000», «quién te debe»…). El valor crece con cada bloque.

Qué corrige respecto de la presentación: **público chico** (ahora cualquier negocio que anota a mano; rubros iniciales
_Comercio de barrio_ y _Gastronomía y viandas_, el catálogo crece), **complejidad** (la persona **nunca** arma columnas, filas ni
cálculos; tablas sólo en «Ver todo») y **disrupción/valor** (se arma solo + respuestas con plata). La foto del cuaderno es un
atajo para traer historia, **no** el centro.

Principios de UI: máx. **3 datos grandes por pantalla**, **1 acción principal**, lenguaje cotidiano sin jerga (glosario en
`docs/marca.md`), tablas sólo detrás de «Ver todo», lo simulado está **rotulado**.

## Lo que ya está hecho (no lo reescribas)

| Qué                                                                                                                                                                           | Dónde                                                                                                         |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Tipos del dominio nuevo                                                                                                                                                       | `src/types/negocio.ts`                                                                                        |
| Catálogo de piezas (`PIEZAS`, `pieza`, `nombreDePieza`, `piezaDeNombre`, `RUBROS`, `NOMBRE_RUBRO`)                                                                            | `src/lib/catalogo/piezas.ts`                                                                                  |
| Ensamblador (`ensamblar(relato, rubro?)` → `{ rubro, piezas: {id, nombre, motivo}[] }`, `detectarRubro`)                                                                      | `src/lib/ensamblador/index.ts`                                                                                |
| Respuestas: definiciones (`RESPUESTAS`, `definicion`, `faltan`, `respuestasQueDesbloquea`) y cálculos puros                                                                   | `src/lib/respuestas/{definiciones,calculos}.ts`                                                               |
| «Contá qué pasó» (`entenderRelato(texto)` → `{ filas, sinEntender }`, `leerImporte`)                                                                                          | `src/lib/relato/parser.ts`                                                                                    |
| Planes (`PLANES`, `ORDEN_PLANES`, `NOMBRE_FUNCION`, `planIncluye`, `planMinimoPara`, `precioPorMes`, `importeACobrar`, `planEfectivo`, `DIAS_DE_PRUEBA`) y transiciones puras | `src/lib/planes/{planes,suscripcion}.ts`                                                                      |
| Formato (`pesos`, `costo`, `porcentaje`, `plural`, `nombreDelDia`, `fechaLarga`, `fechaCorta`)                                                                                | `src/lib/formato.ts`                                                                                          |
| Suscripción simulada (Zustand + localStorage)                                                                                                                                 | `src/stores/suscripcion.ts`                                                                                   |
| **Hooks que usa la UI**                                                                                                                                                       | `src/hooks/negocio/*` (abajo)                                                                                 |
| Navegación (barra superior, inferior, `secciones.ts`), `AppLayout`, `BannerPlan`, `PlanGate` (`RequierePlan`, `CandadoPlan`, `EtiquetaPlan`)                                  | `src/components/{Navbar,Plan}`, `src/layouts`                                                                 |
| Rutas (archivos en `src/routes`) y **páginas stub** a reemplazar                                                                                                              | `src/pages/*`                                                                                                 |
| Recorrido guiado (tarjetas ancladas por `data-recorrido`), store y botón; tooltip `InfoTip`                                                                                   | `src/components/Recorrido`, `src/stores/recorrido.ts`, `src/hooks/recorrido`, `src/components/ui/InfoTip.tsx` |

### Hooks (`@/hooks/negocio/…`)

- `useMiNegocio()` → `{ cargando, error, sistema, sistemaId, hayNegocio, presentes, piezasPresentes }`. `presentes` es un
  `Map<PiezaId, { pieza, bloque, campo(rol) }>`: la pieza del catálogo que tiene el sistema, su tabla (`bloque`, con `.id`,
  `.nombre`, `.campos`) y `campo('monto')` → la columna por rol. «Mi negocio» = el sistema más reciente.
- `useRespuestas()` → `{ cargando, hayNegocio, sistemaId, piezasPresentes, vistas }`. Cada vista es
  `{ definicion: {id, pregunta, requiere, ayuda, basica}, resultado }` y `resultado.estado` es:
  `'lista'` (`titular`, `detalle`, `tendencia?`, `items?`) · `'bloqueada'` (`faltan: PiezaId[]`) · `'sin-datos'` · `'plan'`.
- `useArmarSistema()` → mutación `mutateAsync({ nombre, piezas: PiezaId[], rubro })` que crea el sistema y sus tablas.
  `useSumarPieza(proyectoId)` → `mutateAsync({ id: PiezaId, rubro? })` suma una pieza a un sistema existente.
- `usePlanActual()` → `{ plan, estado, enPrueba, diasDePrueba, definicion, suscripcion, incluye(funcion) }`.
  `useAccionesPlan()` → `iniciarPrueba, suscribirse, cancelarSuscripcion, reactivarSuscripcion, simularFinDePrueba, reiniciar`.
- Datos existentes de siempre: `@/hooks/proyectos/*`, `@/hooks/registros/*` (`useRegistros`, `useGuardarRegistro`…),
  `@/hooks/reportes/*` (`useReporte`), `@/hooks/lotes/*` (cuaderno con IA). La UI **no** importa `@/api/*` ni `@/mocks/*`.
- Cada agente puede **agregar** archivos nuevos en `src/hooks/negocio/` (con nombre propio); no editar los existentes.

### Qué mapea de la otra rama (si traés código de `feat/red-proactiva-suscripciones`)

| Antes                                                     | Ahora                                                                                                      |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `@/hooks/red/useRed` (`usePlanActual`, `useAccionesRed`)  | `@/hooks/negocio/usePlan` (`usePlanActual`, `useAccionesPlan`)                                             |
| `@/lib/red/planes`                                        | `@/lib/planes/planes` (planes `gratis` · `negocio` · `negocio-plus`; funciones nuevas en `NOMBRE_FUNCION`) |
| `@/lib/red/formato`                                       | `@/lib/formato`                                                                                            |
| `@/types/red`                                             | `@/types/negocio`                                                                                          |
| `@/stores/red`, `useValor`, avisos, listas, ofertas, zona | **no existen**: se reemplazan por `useRespuestas` / `useMiNegocio`                                         |

## Planes (simulados, precios ilustrativos)

| Plan                                 | Incluye                                                                                   |
| ------------------------------------ | ----------------------------------------------------------------------------------------- |
| Gratis                               | 3 bloques, respuestas básicas (`basica: true`), 2 cargas de historia al mes               |
| Negocio (`negocio`, $ 9.900)         | Bloques ilimitados, todas las respuestas, comparativas, historial, descarga XLSX          |
| Negocio + (`negocio-plus`, $ 17.900) | Lo anterior + varias personas, más de un negocio, resumen semanal por WhatsApp (simulado) |

Prueba de 7 días del plan más completo (una sola vez). Anual: 10 meses de 12. Al volver al plan Gratis **no se pierde nada**:
sólo se bloquean funciones (candado `CandadoPlan`/`RequierePlan` con invitación a mejorar, nunca una pared).

## Anclas del recorrido (contrato entre agentes)

Cada ancla es `data-recorrido="<nombre>"` en **un** elemento visible de su pantalla. El recorrido (`lib/recorrido/pasos.ts`) las usa.
Si un ancla no está, el recorrido muestra la tarjeta centrada (no falla).

| Pantalla               | Anclas                                                        | Quién las pone |
| ---------------------- | ------------------------------------------------------------- | -------------- |
| `/inicio`              | `inicio-respuestas`, `inicio-desbloquear`, `inicio-checklist` | A              |
| `/subir`               | `subir-contar`, `subir-historia`                              | B              |
| `/bloques`             | `bloques-tarjetas`, `bloques-sumar`                           | C              |
| `/respuestas`          | `respuestas-lista`                                            | A              |
| `/plan`                | `plan-planes`                                                 | F              |
| `/cuenta`              | `cuenta-demo`                                                 | D2             |
| barra superior / «Más» | `nav-recorrido`                                               | ya está        |

## Reglas de trabajo (para cuidar tokens)

1. **Leé sólo este brief y los archivos que nombra tu tarea.** Sin exploración libre del repo ni de la otra rama (si hace falta
   algo de ella: `git show feat/red-proactiva-suscripciones:<ruta>`).
2. **Sólo editá tus archivos** (listados en tu tarea). Otros 6 agentes trabajan en paralelo en otras carpetas. Si te falta algo
   de la base, resolvelo dentro de tus archivos y anotalo en el informe.
3. Verificación **sólo de lo tuyo**: `npx vitest run <tus tests>`, `npx eslint <tus carpetas>`,
   `npx tsc -p tsconfig.app.json --noEmit --incremental false` (ignorá errores de archivos que no son tuyos).
   **No** corras la suite completa (la corre el orquestador) ni formatees `src/` entero; `npx prettier --write` sólo en tus archivos.
4. **Máx. 2 intentos** para arreglar un mismo error; si no sale, reportalo y seguí.
5. Sin servidores de desarrollo, sin capturas, sin commits, sin dependencias nuevas.
6. Informe final de **≤ 15 líneas**: archivos, decisiones de UX, límites y pendientes.

## Reglas de UI y capas

- Marca: `docs/marca.md`. Voseo rioplatense, sin jerga («tabla», «columna», «fila»; en esta propuesta «bloque» y «respuesta»).
- Texto de lectura 18 px, controles de 48 px, 360 px sin scroll horizontal, foco visible, un `<h1>` por pantalla (`PageHeading`).
- Una acción principal por pantalla (`variant="solid"`); apoyo `primary`/`secondary`; `ghost` para terciarias.
- Nada de color como única señal. Íconos de `@phosphor-icons/react` con sufijo `Icon`.
- Vacío / cargando / error en cada pantalla (`EmptyState`, `Skeleton`/`PageSpinner`, `ErrorState`). Destructivo → `ConfirmDialog` o «Deshacer».
- Capas de ESLint: `routes > pages > layouts > components > hooks > api/stores > types, lib`. La UI importa **hooks**, no `api` ni `mocks`.
- `InfoTip` sólo en lo difícil (ganancia, mes anterior, qué es un bloque, qué desbloquea, dudas de la lectura).

## Tests

Vitest + Testing Library + `vitest-axe`. Mirá `src/test/routes/stock.test.tsx` para el estilo. `renderApp('/ruta')`,
`setupUsuarioConSesion()` y `crearProyecto`/`crearBloque`/`crearCampo` de `@/api/*` para sembrar datos del motor simulado (MSW).
Cada pantalla: que se vea, que la acción principal haga lo esperado y `axe` sin violaciones. La suscripción y el recorrido se
reinician solos entre tests (`src/test/setup.ts`).
