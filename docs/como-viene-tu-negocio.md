# Cómo viene tu negocio (ENS-249)

La pantalla de Objetivos muestra cómo viene el negocio con lo que la persona anotó en sus cuadernos. Antes la cuenta se
hacía en el front (`negocioDe`) y el historial era de ejemplo. Ahora la hace el backend, con los mismos números que el
resumen en PDF.

## Contrato

`GET /api/objetivos/negocio`: requiere sesión. El negocio sale de la sesión y se suman todos sus sistemas.

```json
{
  "hayVentas": true,
  "vendiste": 56100,
  "mercaderia": 40500,
  "gastos": 8000,
  "queda": 7600,
  "teDeben": 17000,
  "semanas": [
    { "semana": "Hace 5 sem.", "queda": 0 },
    { "semana": "Hace 4 sem.", "queda": 0 },
    { "semana": "Hace 3 sem.", "queda": 0 },
    { "semana": "Hace 2 sem.", "queda": -8000 },
    { "semana": "Hace 1 sem.", "queda": 4100 },
    { "semana": "Hoy", "queda": 7600 }
  ]
}
```

Todos los montos van en pesos enteros, redondeados hacia el más cercano.

| Campo | Qué es |
|---|---|
| `hayVentas` | Si hubo alguna venta en los últimos 30 días. Si es `false`, los importes van en cero, `semanas` viene vacío y el front no muestra números. |
| `vendiste` | Lo que entró por las ventas con precio de los últimos 30 días, hoy incluido y en hora argentina. Es el mismo número que «Ingresos» del PDF. |
| `mercaderia` | Lo que costó lo vendido: la cantidad por el costo actual del producto. Un producto sin costo no suma. |
| `gastos` | Los gastos del período sin la categoría «Mercadería», que ya se descuenta al vender. |
| `queda` | `vendiste − mercaderia − gastos`. Puede ser negativo. |
| `teDeben` | Lo vendido a fiado en el período. Es el mismo número que «Fiado» del PDF. |
| `semanas` | Seis puntos, del más antiguo a «Hoy». Cada uno es el `queda` de los 30 días que terminan ese día: hoy, hace 7 días, hace 14 días, etc. |

## Dónde está

- Cálculo: `domain/model/objetivo/ComoVieneElNegocio` (reutiliza `ResumenNegocio`).
- Caso de uso: `domain/useCase/objetivo/obtenerComoVieneElNegocio/ObtenerComoVieneElNegocio`.
- Controller: `ComoVieneElNegocioController`. Es un controller propio para no tocar `ObjetivoController` (ENS-248).
- No necesita migración.

## Regla de ventas de 30 días por producto (para ENS-250)

`ComoVieneElNegocio.vendidasEn30Dias(datos, hoy)` devuelve, por cada producto del inventario de un sistema, las
unidades vendidas en los mismos 30 días que usa este endpoint. Un producto sin ventas vale 0, y las ventas de un
producto que ya no está en el inventario no se cuentan. Es la que tiene que usar `GET /api/objetivos/pasos` para
«vendidas en 30 días».
