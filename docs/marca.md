# Guía de marca (resumen para desarrollo)

Fuente visual vigente: *Ensambla_Mockup*, provisto como carpeta y ZIP en el escritorio. Los tokens están implementados en
[`frontend/src/styles/index.css`](../frontend/src/styles/index.css) (Tailwind 4, bloque `@theme`).

**Concepto:** *Tu negocio, un bloque a la vez.* Modular, cálida y reconocible. Tono profesional, claro y cordial, sin jerga
técnica (ver [guia-de-lenguaje.md](guia-de-lenguaje.md)).

## Color

| Nombre | Hex | Token Tailwind | Uso |
|---|---|---|---|
| Naranja | `#EE8000` | `accent` | Acción destacada, énfasis |
| Amarillo | `#EBCE32` | `yellow` | Piezas, etiquetas y acentos |
| Celeste | `#4EB2D4` | `blue` | Piezas, cabeceras y superficies claras |
| Petróleo | `#00506A` | `petrol` | Navegación, títulos y botones principales de trabajo |
| Coral | `#F85F5A` | `coral` | Piezas y acentos |
| Pizarra | `#263443` | `fg` | Texto |
| Papel | `#FFFDFA` | `canvas` | Fondo general |
| Crema | `#FFF7EB` | `cream` | Superficies cálidas |
| Salvia | `#F1F5ED` | `sage` | Taller e ilustraciones |

Los cinco colores del mockup conviven en el isotipo y la franja del encabezado. Petróleo organiza la interfaz;
naranja destaca la acción de entrada. `sand` y `amber` quedan disponibles para estados existentes.

**Criterio de color sólido (2026-11-19).** La referencia es Mi zona: superficies de color sólido con letra blanca. Se
implementa en `frontend/src/styles/sistema/solidos.css` y el color de cada concepto vive en
`frontend/src/lib/sistema/tonos.ts` (Ventas petróleo, Ganancia celeste, Costo de mercadería coral, Otros gastos naranja,
Fiado coral, Efectivo celeste, Transferencia naranja, Tarjeta coral).

- **R1.** En cada pantalla se ven al menos tres de los cuatro colores sólidos (naranja, celeste, coral, petróleo).
- **R2.** Petróleo: como máximo una superficie grande por pantalla (la destacada); después, solo detalles chicos.
- **R3.** Todo fondo sólido lleva letra blanca, en negrita (700) y de 16 px o más; las cifras, de 24 px o más.
- **R4.** El amarillo nunca es fondo: solo la estrella de favorito, puntos o líneas finas.
- **R5.** El color lo decide el concepto, no la posición: el mismo concepto lleva el mismo color en todas las pantallas.
- **R6.** Las tarjetas normales son blancas con borde fino, sin bordes gruesos ni rayas de color.
- **R7.** Un solo botón principal naranja por pantalla o ventana; los secundarios son blancos con borde del color del
  contexto. Pestañas, selectores de modo y filtros activos son selección, no acción.

**Contraste (accesibilidad, T-056):** los botones petróleo llevan texto blanco. Sobre las superficies sólidas el texto
va en blanco (R3): el blanco sobre naranja, coral y celeste queda debajo de 4,5:1; se compensa con negrita y tamaño;
decisión del equipo del 2026-11-19. Para texto naranja sobre claro usá `accent-700`/`accent-800`. Texto secundario `soft` (`#526268`) y
terciario `muted` (`#5F6E73`) se usan sobre fondos claros. Se mantiene el foco visible en petróleo.

## Tipografía

- **Plus Jakarta Sans** (principal; pesos 400–800), autoalojada con `@fontsource-variable/plus-jakarta-sans` (sin CDN).
- Escala por rol, definida en `@theme` (`frontend/src/styles/index.css`); no se usan tamaños sueltos (`text-[13px]`):

| Rol | Token | Tamaño | Uso |
|---|---|---|---|
| Caption | `text-caption` | 14 px | Metadatos, chips (mínimo absoluto) |
| Hint | `text-hint` | 15 px | Ayudas y texto secundario |
| Body | `text-body` | 18 px | Lectura y campos de formulario |
| Lead | `text-lead` | 20 px | Bajada de pantalla |
| H3 / H2 / H1 | `text-h3` `text-h2` `text-h1` | 22 / 28 / 34 px | Títulos |
| Display | `text-display` | 44 px | Sólo Landing |

- Etiquetas de campo: 16 px en semibold; ayudas: 15 px.
- La Landing y las composiciones de bienvenida tienen CSS acotado para reproducir el mockup; la paleta proviene de los tokens comunes. Los textos informativos conservan un mínimo de 14 px; los detalles de las ilustraciones decorativas pueden ser menores.

## Logo

- **Isotipo:** cinco bloques redondeados: naranja alto a la izquierda, amarillo y celeste a la derecha,
  petróleo abajo a la izquierda y coral abajo a la derecha. Componente `Isotipo` en
  `components/ui/Logo.tsx` y `public/favicon.svg`.
- **Logotipo:** "Ensambla" en sans bold, color Petróleo.
- **Sami:** recursos originales transparentes del mockup en `public/mockup/`, usados como apoyo visual; no representan una conversación o servicio adicional.
- A tamaños chicos (≤ 48 px) usar sólo el isotipo. Sin sombras, brillos ni 3D.

## Interfaz

- Pensada para emprendedores adultos y mayores: **texto de lectura 18 px, controles de 48 px** (variante `compact` de 40 px, con área
  de toque de 44 px, sólo en celdas de tabla de escritorio) y flujo completo a 360 px (T-033, T-035, T-056).
- **Jerarquía de acciones:** `solid` naranja para la acción destacada (una por pantalla o ventana, R7); `primary` petróleo para trabajar con sistemas y plantillas; `secondary` blanco con borde del color del contexto; `ghost` como texto.
- **Superficies sólidas (R1 a R6):** los indicadores y la tarjeta destacada de cada pantalla van en color sólido con letra blanca, según el concepto que muestran; una sola superficie petróleo grande por pantalla.
- Tarjetas blancas con radio `lg` (20 px), sombras suaves y bordes finos; fondo Papel. Chips con radio completo y superficies de apoyo Crema, Celeste y Salvia.
- Foco visible en todo: anillo de 3 px `petrol` con separación de 2 px. Bordes de campos con contraste ≥ 3 : 1.
- Nada de color como única señal: los avisos de duda o error llevan ícono y texto.
- Toda pantalla tiene estado vacío, de carga (con `Skeleton` de alto fijo) y de error con reintentar (T-034).
- Acciones destructivas: **Deshacer** donde es reversible (quitar una fila en la revisión); confirmación que nombra el elemento y lo que se
  pierde (con conteo) para borrar un sistema, tabla, columna o fila guardada.
- Primitivos en `components/ui`: `Button`, campos (`InputField`, `SelectField`, `TextareaField`), `Sheet` (hoja desde abajo en celular,
  panel lateral desde `lg`), `Menu`, `ConfirmDialog`, `Toast`, `Stepper`, `StickyActionBar`, `Badge`, `Skeleton`, `Panel`, `PageHeading`.

## Glosario de la interfaz

Un término por concepto. El código y la API conservan los nombres técnicos (`proyecto`, `bloque`, `campo`, `registro`); las rutas siguen en `/proyectos`.

| Antes | Ahora |
|---|---|
| Proyecto | **Sistema** (plural: «Mis sistemas») |
| Bloque | **Tabla** |
| Dato / campo (columna) | **Columna** |
| Registro | **Fila** |
| Modo construir / usar | **Armar** / **Cargar y ver** |
| Siguiente duda | **Ir al próximo dato para revisar** |
| Pág. | **Hoja** |
| Lote / lectura IA | «tu cuaderno» / «lo que leímos» |
