# Guía de marca (resumen para desarrollo)

Fuente: *Manual de Marca de Ensambla*. Los tokens ya están implementados en
[`frontend/src/styles/index.css`](../frontend/src/styles/index.css) (Tailwind 4, bloque `@theme`).

**Concepto:** *Tu sistema, un bloque a la vez.* Modular, cálida y reconocible. Tono cercano, claro y sin jerga técnica.

## Color

| Nombre | Hex | Token Tailwind | Uso |
|---|---|---|---|
| Naranja | `#F97316` | `accent` | Acción principal, énfasis |
| Coral | `#FB5852` | `coral` | Movimiento, cercanía, acentos |
| Arena | `#E6C9A8` | `sand` | Calidez, superficies secundarias |
| Pizarra | `#263443` | `fg` | Estructura, texto, fondos oscuros |
| Marfil | `#FFF8F1` | `canvas` | Fondo claro |
| Ámbar | `#F59E0B` | `amber` | Color de apoyo (acento del isotipo) |

Reglas: tonos cálidos para la acción, Pizarra para la estructura, Ámbar como apoyo.

**Contraste (accesibilidad, T-056):** el texto sobre naranja va en **Pizarra** (4,5 : 1); el blanco sobre `#F97316`
no cumple AA (2,8 : 1). Para texto o bordes de acción sobre fondo claro usá `accent-700`/`accent-800`.
Texto secundario `soft` (`#3E4B59`) y terciario `muted` (`#5A6570`) cumplen AA sobre Marfil.

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

## Logo

- **Isotipo:** cinco bloques redondeados que sugieren una "E": naranja (alto, a la izquierda), coral y arena
  (arriba a la derecha), pizarra (abajo a la izquierda) y ámbar (abajo a la derecha). Componente `Isotipo` en
  `components/ui/Logo.tsx` y `public/favicon.svg`. **Es una aproximación**: reemplazar por el SVG oficial del kit de marca.
- **Logotipo:** "Ensambla" en sans bold, color Pizarra. Versiones: color sobre Marfil, monocromo Pizarra y negativo blanco sobre Pizarra.
- A tamaños chicos (≤ 48 px) usar sólo el isotipo. Sin sombras, brillos ni 3D.

## Interfaz

- Pensada para emprendedores adultos y mayores: **texto de lectura 18 px, controles de 48 px** (variante `compact` de 40 px, con área
  de toque de 44 px, sólo en celdas de tabla de escritorio) y flujo completo a 360 px (T-033, T-035, T-056).
- **Una sola acción principal por pantalla**, en naranja sólido; las de apoyo van en contorno y las terciarias como texto.
- Tarjetas blancas con radio único (`lg`, 16 px), sombra suave en dos niveles (`shadow-sm` en reposo, `shadow-md` al elevar) y pocos bordes;
  fondo Marfil. Chips con radio completo. Coral, Ámbar y Arena se usan en estados, vacíos y el isotipo.
- Foco visible en todo: anillo de 3 px `accent-700` con separación de 2 px. Bordes de campos con contraste ≥ 3 : 1.
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
