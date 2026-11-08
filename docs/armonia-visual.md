# Armonía visual

Rama: `feat/armonia-visual`. Solo estilos (`frontend/src/styles/sistema/armonia.css`, que carga al final): no cambia
funcionalidad ni componentes. Unifica cómo se pintan botones, avisos y tarjetas.

## Reglas

1. **Color en el borde, el ícono y la letra; fondo blanco.** Los íconos y Sami dentro de una tarjeta van sin borde:
   el único borde es el del contenedor. Las tarjetas ya no se rellenan de color. Las que antes eran
   macizas (inicio, plantillas, cuadernos, bloques del editor, Premium, resumen de Mi zona, banners de Sami) pasan a
   blanco con borde del color de su marca y título azul.
2. **Una sola familia de botones.**

   | Botón | Cuándo | Aspecto |
   |---|---|---|
   | `.btn.primary` (alias `.orange`) | la acción principal de la zona | naranja, letras blancas |
   | `.btn.secondary` (alias `.soft`) | acciones secundarias | sin fondo, borde y letras azules |
   | `.btn.azul` | secundaria que tiene que destacar | azul sólido, letras blancas |
   | `.btn.amarillo` | casos puntuales (Premium) | amarillo oscuro, letras blancas |
   | `.btn.ghost` | baja jerarquía | solo letras azules |
   | `.btn.danger` / `.btn.peligro` | borrar o confirmar algo destructivo | rojo (sin fondo / sólido) |

   Se quitaron las sombras duras de 3 px y los colores sueltos por pantalla (coral en importar, amarillo en el cierre de
   creación, etc.).
3. **Estados con tres colores.** Rojo = urgente o peligro, amarillo = atención, azul = información o normal.
   En los avisos el color lo da la **prioridad** (urgente, para hoy, para mirar) y no el tipo: antes un aviso de stock era
   siempre rojo aunque fuera «para mirar». El tipo (stock, precio…) queda como etiqueta de texto.

## Tokens (`:root` en `armonia.css`)

| Token | Valor | Uso |
|---|---|---|
| `--accion` / `--accion-hover` | `#bd5900` / `#a64d00` | fondo del botón principal |
| `--orange` | `= --accion` | **el único naranja de la interfaz**: botones, bordes, íconos y barras. El logo y la franja de colores de arriba conservan el de marca (`#ee8000`) |
| `--azul` | petróleo `#00506a` | texto, bordes y botones azules |
| `--urgente-borde` / `--urgente-texto` | `#f85f5a` / `#c42b2b` | rojo |
| `--aviso-borde` / `--aviso-texto` | `#ebce32` / `#7a5f00` | amarillo |
| `--info-borde` / `--azul-texto` | `#4eb2d4` / `#1f6f8f` | celeste |
| `--naranja-texto` | `#b45300` | letras naranja sobre blanco |
| `--muted` | `#5f6d72` | texto secundario (antes `#69777c`) |

Cada color tiene una versión `-texto` que llega a 4,5:1 sobre blanco. Los de marca (`#ee8000`, `#ebce32`, `#4eb2d4`)
se usan solo en bordes, íconos y detalles: como letra dan entre 2,4 y 2,8:1.

## Decisión a revisar: el naranja

El naranja de marca `#ee8000` con letras blancas da 2,7:1 (el mínimo es 4,5:1). Toda la interfaz usa `#bd5900`, el más
claro de su familia que llega a 4,5:1, y también el naranja de los bloques (`COLORS.orange` en `lib/sistema/catalogo.ts`).
Si se prefiere el naranja de marca, se cambia `--accion`; los botones quedan a 2,7:1.

## Planes

- «Plan actual» (antes «Tu plan hoy», «Tu plan» y «…, tu espacio actual») es un resumen chico: título, uso y una nota.
- La fecha hasta cuándo dura la prueba o el período pago va al pie de las tarjetas, chica, a la izquierda y en gris,
  con un asterisco.
- Premium tiene más presencia: columna más ancha, borde amarillo grueso, sombra, nombre y precio grandes y botón alto.

## Cómo se verificó

Se recorrieron inicio, mis cuadernos, avisos, objetivos, sami te enseña, plantillas, mi zona, planes, cuenta, taller y el
editor de un cuaderno, y en cada una un script midió el contraste de todos los textos contra su fondo real. Quedó sin
textos por debajo de 4,5:1 (3:1 en textos grandes) salvo el contador de filtros de avisos, que el script no puede
medir porque su fondo es una marca animada.

## Pendiente

- Las hojas anteriores conservan sus reglas de fondos de color; `armonia.css` las pisa. Se pueden borrar de a poco.
- Quedan sin revisar los modales internos del editor, el tutorial guiado y el chat flotante de Sami dentro del cuaderno.
- Los tonos de los bloques (`COLORS` y `TINTS` en `lib/sistema/catalogo.ts`) no se tocaron.
