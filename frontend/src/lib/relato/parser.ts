import { sinTildes } from '@/lib/catalogo/piezas';
import type { FilaRelato } from '@/types/negocio';

// «Contá qué pasó»: entiende frases simples («vendí 3 velas a 4500», «gasté 12000 en cera»,
// «le fié a Marta 3000») y las convierte en filas a confirmar. Es una simulación por reglas: en la
// versión real lo hace un modelo de IA con la misma entrada y salida.

/** «4.500», «4500», «4,5» → número. Los puntos son miles; la coma, decimales. */
export function leerImporte(texto: string): number | null {
  const limpio = texto.replace(/[^\d.,]/g, '');
  if (!limpio) return null;
  const sinMiles = /^\d{1,3}(\.\d{3})+(,\d+)?$/.test(limpio) ? limpio.replace(/\./g, '') : limpio;
  const valor = Number(sinMiles.replace(',', '.'));
  return Number.isFinite(valor) && valor > 0 ? valor : null;
}

const IMPORTE = String.raw`\$?\s*(\d[\d.,]*)`;

const VENTA = new RegExp(
  String.raw`^(?:vendi|vendimos|cobre|vendio)\s+(?:(\d+)\s+)?(.+?)\s+(?:a|por|en)\s+${IMPORTE}`,
);
const GASTO_EN = new RegExp(
  String.raw`^(?:gaste|gastamos|pague|compre|compramos)\s+${IMPORTE}\s+(?:en|de|por)\s+(.+)$`,
);
const GASTO_POR = new RegExp(
  String.raw`^(?:gaste|gastamos|pague|compre|compramos)\s+(.+?)\s+(?:a|por|en)\s+${IMPORTE}`,
);
const FIADO_A = new RegExp(String.raw`^(?:le\s+fie|fie|fiamos)\s+(?:a\s+)?(.+?)\s+${IMPORTE}`);
const FIADO_DEBE = new RegExp(String.raw`^(.+?)\s+(?:me\s+debe|debe)\s+${IMPORTE}`);

function capitalizar(texto: string) {
  const limpio = texto.trim();
  return limpio ? limpio[0]?.toUpperCase() + limpio.slice(1) : limpio;
}

/** Cada frase puede tener una venta, un gasto o un fiado; las que no se entienden se devuelven aparte. */
export function entenderRelato(texto: string): { filas: FilaRelato[]; sinEntender: string[] } {
  const frases = texto
    .split(/[\n;]+|\.\s+|\s+y\s+(?=(?:vend|gast|compr|pagu|le fi|cobr))/i)
    .map((frase) => frase.trim().replace(/[.!]+$/, ''))
    .filter(Boolean);

  const filas: FilaRelato[] = [];
  const sinEntender: string[] = [];

  for (const original of frases) {
    const frase = sinTildes(original);
    let hallada = false;

    const venta = VENTA.exec(frase);
    if (venta) {
      const monto = leerImporte(venta[3] ?? '');
      if (monto !== null) {
        filas.push({
          pieza: 'ventas',
          texto: original,
          valores: {
            cantidad: venta[1] ? Number(venta[1]) : 1,
            producto: capitalizar(venta[2] ?? ''),
            monto,
          },
        });
        hallada = true;
      }
    }
    if (!hallada) {
      const gastoEn = GASTO_EN.exec(frase);
      const gastoPor = gastoEn ? null : GASTO_POR.exec(frase);
      const monto = leerImporte(gastoEn?.[1] ?? gastoPor?.[2] ?? '');
      const concepto = gastoEn?.[2] ?? gastoPor?.[1];
      if (monto !== null && concepto) {
        filas.push({
          pieza: 'gastos',
          texto: original,
          valores: { concepto: capitalizar(concepto), monto },
        });
        hallada = true;
      }
    }
    if (!hallada) {
      const fiado = FIADO_A.exec(frase) ?? FIADO_DEBE.exec(frase);
      const monto = leerImporte(fiado?.[2] ?? '');
      if (fiado && monto !== null) {
        filas.push({
          pieza: 'fiados',
          texto: original,
          valores: { cliente: capitalizar(fiado[1] ?? ''), monto },
        });
        hallada = true;
      }
    }
    if (!hallada) sinEntender.push(original);
  }
  return { filas, sinEntender };
}
