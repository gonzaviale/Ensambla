# Ensambla: la interfaz es el mockup, en React

Referencia: `Ensambla_Mockup/dist`. La interfaz de `frontend/` es un port **calcado** de ese mockup: mismo CSS,
mismas clases y misma estructura de DOM. Cambia sólo el contenido (Propuesta de Tema v1.3: almacenes, kioscos y
minimercados), el motor de avisos y la persistencia (API simulada con MSW, porque todavía no hay backend).

## Cómo se mapea el mockup al stack

| Mockup | React |
| --- | --- |
| `styles.css`, `visual.css`, `alertas.css`, `onboarding.css` | `src/styles/sistema/`, copias fieles acotadas bajo `.mk-root` con `postcss-prefix-selector` (ver `vite.config.ts`) |
| `app.js` (estado en variables y `render()`) | Store de Zustand (`stores/sistemaStore.ts`) + componentes en `components/sistema/` |
| Vistas `home`, `templates`, `wizard`, `editor`, `map` | Rutas `/inicio`, `/plantillas`, `/taller`, `/sistemas/$sistemaId?modo=construir\|usar`, `/zona` |
| `core.js` | `lib/sistema/core.ts` (misma semántica de mutación en sitio) |
| `alertas.js` | `lib/sistema/alertas.ts` (motor) + `components/sistema/avisos/` |
| `onboarding.js` | `lib/sistema/tutorial.ts` (pasos y condiciones puras) + `components/sistema/tutorial/` |
| `localStorage` | MSW (`mocks/`) + TanStack Query; el store guarda con un `PUT` diferido (`useSincronizarSistemas`) |

Dentro de `.mk-root` no se usan clases de Tailwind ni `components/ui`. El *preflight* de Tailwind no se aplica ahí
(el mockup cuenta con los estilos por defecto del navegador); la landing y el acceso siguen igual.

## Pantallas

- **Inicio**: héroe con ejemplo, sistemas guardados, tres puertas, importar foto, cartel de «Mi zona» y plantillas.
- **Plantillas**: Almacén o kiosco, Depósito y Pedidos y clientes, con filtro, búsqueda y vista previa.
- **Taller con Sami**: conversación, propuesta editable (nombre, ancho, distribución, piezas, secciones, arrastre).
- **Editor**: Construir (biblioteca, lienzo, inspector, secciones, campos, deshacer) y Usar (registros, formulario, franja de Sami).
- **Avisos**: campana, panel, «Sami vio un cambio» y franja. Seis reglas: stock, precio frente a la zona, estancado, muy vendido, combo y consumo de la zona. Cada aviso dice por qué y ofrece Aplicar o Descartar.
- **Mi zona**: precios agregados, qué se vende cerca y otra zona. **Sin mapa**: datos agregados y anónimos, con un mínimo de 3 comercios por dato.
- **Tutorial**: 17 pasos que comprueban acciones reales, con bienvenida la primera vez. Sin «Reiniciar la demo» ni aviso al volver atrás.
- **Perfil**: cambia el nombre que se muestra y cierra la sesión.

## API simulada (MSW)

`GET/PUT/DELETE /api/sistemas`, `GET/PUT /api/preferencias`, `POST /api/sami/propuesta`,
`POST /api/importaciones/lectura`, `GET /api/zona/agregados`, `POST /api/zona/analisis`. Todas exigen sesión (y CSRF
al escribir). En el navegador la base de MSW persiste en `localStorage`; en los tests, sólo en memoria.

## Verificación

`npm run lint`, `npm run typecheck`, `npm test` (incluye `vitest-axe` en las pantallas principales) y
`npm run build`. Los datos de ejemplo del modo `completo` se recorren con `demo@ensambla.local`.

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

Con el backend (`VITE_API_MOCKS=parcial`) la foto viaja en un multipart a `POST /api/importaciones/lectura` y la
lee la IA configurada (`docs/lectura-cuaderno-ia.md`); sólo en el modo `completo`, o con «Probar con un
ejemplo», la lectura es simulada, y la pantalla lo dice.
