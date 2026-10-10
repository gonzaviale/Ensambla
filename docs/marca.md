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

- **Plus Jakarta Sans** (principal; pesos 400–800). Respaldo: **Lato**.
- Títulos grandes y en negrita; etiquetas pequeñas en mayúsculas y negrita.
- Cuerpo 15 px; texto secundario 13–14 px.

## Logo

- **Isotipo:** cinco bloques redondeados que sugieren una "E": naranja (alto, a la izquierda), coral y arena
  (arriba a la derecha), pizarra (abajo a la izquierda) y ámbar (abajo a la derecha). Componente `Isotipo` en
  `components/ui/Logo.tsx` y `public/favicon.svg`. **Es una aproximación**: reemplazar por el SVG oficial del kit de marca.
- **Logotipo:** "Ensambla" en sans bold, color Pizarra. Versiones: color sobre Marfil, monocromo Pizarra y negativo blanco sobre Pizarra.
- A tamaños chicos (≤ 48 px) usar sólo el isotipo. Sin sombras, brillos ni 3D.

## Interfaz

- Tarjetas blancas con esquinas muy redondeadas y sombra suave; fondo Marfil.
- Controles táctiles de al menos 44 px (ya aplicado en `index.css`).
- Layout responsive hasta 360 px (T-035). Nombres de secciones y botones consistentes en todo el core (T-033).
- Toda pantalla tiene estado vacío, de carga y de error (T-034).
