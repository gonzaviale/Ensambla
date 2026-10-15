# Pantallas de la red proactiva (rama `feat/red-proactiva-suscripciones`)

Guía de trabajo y mapa de pantallas. Parte de [`diferencial-red-proactiva.md`](diferencial-red-proactiva.md) y del
[backlog por MVP](backlog-por-mvp.md). **Todo se simula en el navegador**: no hay API nueva ni backend (los datos son de
demostración y viven en `localStorage`). La app existente (cuaderno, sistemas, stock, reportes) no se toca.

## La idea en una línea

> «Llegó la lista de tu proveedor. Ensambla te muestra qué precios revisar, te avisa lo que importa y vos solo confirmás.»

Lo que la hace disruptiva (y lo que cada pantalla tiene que dejar ver):

1. **Proactiva:** no hay que buscar un reporte; la app avisa (precio en riesgo, lista nueva, stock, ofertas, comprás caro,
   mejor proveedor, cambio raro) con **una línea, el porqué y un toque**.
2. **Valor medible:** cada pantalla muestra plata y tiempo (`useValor`, «protegés $X por mes»), no sólo datos.
3. **Red neutral:** la comparación entre proveedores y con la zona sólo existe porque hay muchos comercios; el comercio
   nunca ve datos de otro (agregados y anónimos, mínimo 5 comercios).
4. **Sencillez:** lenguaje cotidiano, ninguna acción se aplica sin confirmar.
5. **Se paga solo:** los planes se justifican con el valor ya generado (`Mi plan`).

## Mapa de pantallas

| Ruta                      | Pantalla                                                                | Archivo                                         | Backlog                    |
| ------------------------- | ----------------------------------------------------------------------- | ----------------------------------------------- | -------------------------- |
| `/hoy`                    | Hoy: avisos, valor, pedido, resumen del día, ajustes de avisos          | `pages/Hoy/HoyPage.tsx`                         | H-16 (indicadores), doc §2 |
| `/listas`                 | Listas de proveedores: cargar y ver historial                           | `pages/Listas/ListasPage.tsx`                   | doc §1, tarea de lista     |
| `/listas/$listaId`        | Revisar una lista: qué cambió → dudas → precios → confirmar             | `pages/Listas/ListaPage.tsx`                    | doc §1, §2                 |
| `/mostrador`              | Cambios para el mostrador: marcar trasladados, etiquetas                | `pages/Listas/MostradorPage.tsx`                | resumen de la propuesta    |
| `/precios`                | Mis precios: margen, reglas, historial y trazabilidad de costos         | `pages/Precios/PreciosPage.tsx`                 | reglas de precio           |
| `/red`                    | Mi red: ofertas, comparar proveedores, ¿comprás caro?, catálogo maestro | `pages/Red/RedPage.tsx`                         | doc §1, §4, §5             |
| `/red/mayorista`          | Vista del mayorista (demostración): subir lista y publicar oferta       | `pages/Red/MayoristaPage.tsx`                   | doc §5                     |
| `/plan`                   | Mi plan: planes, prueba de 7 días, pago simulado, cancelar              | `pages/Plan/PlanPage.tsx`                       | H-20 · T-066 a T-070       |
| `/cuenta`                 | Mi cuenta: datos del negocio, ayuda, modo demostración                  | `pages/Cuenta/CuentaPage.tsx`                   | H-10 · T-041               |
| `/bienvenida`             | Bienvenida: el relato prende los bloques + recorrido                    | `pages/Bienvenida/BienvenidaPage.tsx`           | H-17 · T-057, doc §3       |
| `/olvide-contrasena`      | Pedir el enlace para recuperar la contraseña                            | `pages/RecuperarClave/OlvideClavePage.tsx`      | H-10 · T-039/T-040         |
| `/restablecer-contrasena` | Elegir una contraseña nueva                                             | `pages/RecuperarClave/RestablecerClavePage.tsx` | H-10 · T-040               |
| `/`                       | Landing actualizada con la propuesta nueva y los planes                 | `pages/Landing/LandingPage.tsx`                 | —                          |

Después de iniciar sesión se llega a `/hoy`; después de registrarse, a `/bienvenida`.

## La base compartida (ya hecha, no se modifica salvo que se diga)

| Capa        | Archivo                        | Qué hay                                                                                                      |
| ----------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| Tipos       | `types/red.ts`                 | Todo el dominio: `ProductoRed`, `ListaProveedor`, `ItemLista`, `Alerta`, `Suscripcion`…                      |
| Cálculos    | `lib/red/calculos.ts`          | `margenSobreCosto`, `precioSegunRegla`, `proponerCambios`, `resumenDeLista`, `diasDeStock`…                  |
| Avisos      | `lib/red/alertas.ts`           | `derivarAlertas` / `alertasVisibles` (los avisos se **derivan**; al aplicar un precio desaparecen solos)     |
| Planes      | `lib/red/planes.ts`            | `PLANES`, `planEfectivo`, `planIncluye`, `precioPorMes`, `importeACobrar`, `NOMBRE_FUNCION`                  |
| Valor       | `lib/red/valor.ts`             | `resumenValor`, `listasDelMes`, supuestos de tiempo                                                          |
| Formato     | `lib/red/formato.ts`           | `pesos`, `costo`, `porcentaje`, `plural`, `fechaCorta`, `nombreDelDia`                                       |
| Semilla     | `lib/red/semilla.ts`           | Estado inicial: Almacén Don Carlos, 3 proveedores, 17 productos, 2 listas por revisar, prueba en curso       |
| Acciones    | `lib/red/acciones.ts`          | Transiciones puras (aplicar precio/lista, confirmar ítem, suscribirse, cancelar…)                            |
| Store       | `stores/red.ts`                | Zustand + `localStorage`; `reiniciarRed()` para pruebas                                                      |
| Hooks       | `hooks/red/useRed.ts`          | **Lo único que la UI importa**: `useProductos`, `useAlertas`, `usePlanActual`, `useValor`, `useAccionesRed`… |
| Planes (UI) | `components/Plan/PlanGate.tsx` | `RequierePlan`, `CandadoPlan`, `EtiquetaPlan`; `BannerPlan` ya está en el layout                             |

Hooks de lectura: `useNegocio`, `useProductos`, `useProducto(id)`, `useProveedores`, `useProveedor(id)`, `useListas`,
`useLista(id)`, `useResumenLista(id)`, `useCambios`, `useReglas`, `useUmbrales`, `useOfertas`, `useZona`, `useBarrio`,
`usePedido`, `useSuscripcion`, `usePlanActual()`, `useAlertas()` → `{ activas, bloqueadas, ignoradas }`, `useValor()`.
Acciones (`useAccionesRed()`): `aplicarPrecio`, `confirmarItem`, `ignorarItem`, `aplicarLista`, `descartarLista`,
`subirLista`, `publicarOferta`, `marcarTrasladado`, `marcarTodosTrasladados`, `resolverAlerta`, `ignorarAlerta`,
`restaurarAlertas`, `guardarReglas`, `guardarUmbrales`, `agregarAlPedido`, `quitarDelPedido`, `vaciarPedido`,
`guardarNegocio`, `iniciarPrueba`, `suscribirse`, `cancelarSuscripcion`, `reactivarSuscripcion`,
`simularFinDePrueba`, `reiniciarDemo`.

## Planes (hipótesis a validar; precios ilustrativos)

| Plan           | Precio/mes | Incluye                                                                                                  |
| -------------- | ---------- | -------------------------------------------------------------------------------------------------------- |
| Gratis         | $ 0        | 2 listas por mes, 40 productos, avisos de precio en riesgo, lista nueva y cambio raro                    |
| Comercio       | $ 9.900    | Listas sin límite, stock por agotarse y pedido sugerido, resumen del día, etiquetas, historial de costos |
| Comercio + Red | $ 17.900   | Todo lo anterior + comparar proveedores, comprás caro (zona), ofertas, 3 personas con acceso             |

Prueba de 7 días del plan más completo (una sola vez). Anual: se pagan 10 meses de 12. Al terminar la prueba o cancelar,
se vuelve al plan Gratis **sin perder datos**: sólo se bloquean funciones (con candado y la invitación a mejorar).

## Reglas de UI (obligatorias)

- Manual de marca y glosario: [`marca.md`](marca.md). Voseo rioplatense, sin jerga («tabla», «columna», «fila», «lista»).
- Texto de lectura 18 px, controles de 48 px, flujo completo a 360 px sin scroll horizontal, foco visible.
- **Una sola acción principal por pantalla** (variante `solid`); apoyo en `primary`/`secondary`; terciarias `ghost`.
- Nada de color como única señal: ícono + texto. Íconos de `@phosphor-icons/react` con sufijo `Icon`.
- Toda pantalla tiene estado vacío, de carga y de error cuando corresponda (`EmptyState`, `Skeleton`, `ErrorState`).
- Acciones destructivas: `ConfirmDialog` que nombra qué se pierde, o «Deshacer» con `Toast`.
- Primitivos en `components/ui` (`Button`, `Panel`, `Badge`, `Sheet`, `Menu`, `Stepper`, `StickyActionBar`, `InputField`,
  `SelectField`, `TextareaField`, `PageHeading`…). Un `<h1>` por pantalla (`PageHeading`).
- Capas de ESLint: `routes > pages > layouts > components > hooks > stores > lib/types`. La UI **sólo** importa hooks de
  `@/hooks/red/useRed`, nunca `@/stores/red` ni `@/lib/red/acciones` directamente (sí puede importar helpers puros de
  `@/lib/red/*` como `pesos`, `porcentaje`, `margenSobreCosto`, `PLANES`).
- Cada pantalla pone su título con `PageHeading`. Cada pantalla enlaza a las otras con `Link` tipado (todas las rutas existen).

## Pruebas

- Vitest + Testing Library + `vitest-axe`. Mirá `src/test/routes/stock.test.tsx` y `src/test/routes/navegacion.test.tsx`.
- `renderApp('/ruta')` + `setupUsuarioConSesion()` (de `@/test/testHelpers`). El store de la red se reinicia solo entre tests.
- Una prueba de ruta por pantalla: que se vea, que la acción principal haga lo esperado y `axe` sin violaciones.
