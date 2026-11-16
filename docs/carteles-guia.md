# Guía de carteles y promociones (H-62 · ENS-370 / T-271)

Cómo se arma un cartel, qué se probó y qué falta probar a mano. El diseño y las reglas de armonía están en
[carteles-diseno.md](carteles-diseno.md).

## Flujo para la persona

1. **Qué promocionar:** la pantalla «Carteles» (`/carteles`) lista lo que el motor de avisos ya encontró (productos
   caros que no se mueven y combos). Cada sugerencia trae precio anterior, precio nuevo, ahorro y porcentaje,
   calculados en el servidor.
2. **Textos:** `POST /api/carteles/textos` redacta titular, bajada y texto del posteo. Si la IA está apagada, falla o
   dice un número que no coincide con los datos, se usan los textos de reserva. Cuenta contra el cupo mensual de
   carteles con IA (10 Gratuito, 40 Premium; provisorio) sólo cuando la IA redactó.
3. **Tres opciones:** `POST /api/carteles/opciones` devuelve las tres composiciones (`BLOQUE`, `FRANJA`, `MARCO`) como
   dibujo SVG; la pantalla las muestra como imagen.
4. **Ajustar:** sólo se editan titular, bajada y formato. Colores, letras y márgenes no se tocan.
5. **Bajar o compartir:** `POST /api/carteles/exportar` baja PDF (A4, A5) o PNG (cuadrado, vertical); el texto del
   posteo se copia o se manda por WhatsApp.

## Qué se midió

`VariedadYArmoniaDeCartelesTest` arma **30 carteles**: 5 rubros con marcas distintas (almacén, panadería, kiosco,
ferretería, peluquería) × 3 variantes × 2 hojas (A4 y A5), y comprueba:

| Qué | Criterio |
|---|---|
| Legibilidad | todo texto con contraste ≥ 4,5 : 1 sobre lo que tiene debajo (WCAG) |
| Zona segura | ningún texto fuera del margen de impresión ni por debajo del tamaño mínimo (9 pt en hojas) |
| Variedad entre variantes | las tres composiciones de una misma marca son distintas |
| Variedad entre negocios | cinco marcas dan cinco carteles distintos con la misma variante |

Los colores del negocio que no alcanzan el contraste se ajustan solos (se oscurecen o aclaran) antes de dibujar.

## Qué falta probar a mano

- **Impresión real en A4 y A5:** el PDF tiene el tamaño exacto de la hoja y respeta el margen de impresión, pero no se
  pudo probar en una impresora. Imprimir un cartel de cada rubro y revisar que nada se corte y que se lea a un metro.
- **Tipografía de la marca en los archivos:** el PDF usa Helvetica y el PNG la sans del servidor; la letra elegida por el
  negocio se ve sólo en la vista previa. Incrustar las seis tipografías permitidas es el siguiente paso.
- **Fotos (T-268) y variante libre con IA (T-270):** todavía no están. Sin foto, las plantillas funcionan con
  tipografía, como pide el diseño.
