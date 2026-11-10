import { z } from 'zod';
import { codigoGtin, mismoGtin } from '@/lib/escaneo/codigo';
import type { CoincidenciaCatalogo, ResultadoCatalogos } from '@/types/escaneo';

const productoSepa = z.object({
  codigo: z.string(),
  nombre: z.string(),
  marca: z.string(),
  presentacion: z.string(),
});
const respuestaSepa = z.object({ producto: productoSepa.nullable() });
const respuestaOff = z.object({
  product: z
    .object({
      code: z.string(),
      product_name: z.string().optional(),
      product_name_es: z.string().optional(),
      brands: z.string().optional(),
      quantity: z.string().optional(),
      image_front_url: z.string().optional(),
    })
    .optional(),
});
const cache = new Map<string, { hasta: number; fuente: CoincidenciaCatalogo }>();

async function consultarFuente(
  fuente: CoincidenciaCatalogo['fuente'],
  codigo: string,
  signal: AbortSignal,
): Promise<CoincidenciaCatalogo> {
  const clave = `${fuente}:${codigo.padStart(14, '0')}`;
  const previa = cache.get(clave);
  if (previa && previa.hasta > Date.now()) return previa.fuente;
  let resultado: CoincidenciaCatalogo;
  const controlador = new AbortController();
  const alCancelar = () => controlador.abort();
  signal.addEventListener('abort', alCancelar, { once: true });
  const plazo = setTimeout(() => controlador.abort(), 8_000);
  try {
    if (signal.aborted) throw new DOMException('Consulta cancelada', 'AbortError');
    const res = await fetch(`/catalogos/${fuente}/${codigo}`, {
      signal: controlador.signal,
      credentials: 'omit',
    });
    if (res.status === 404) resultado = { fuente, estado: 'no_encontrado' };
    else {
      if (!res.ok) throw new Error('Fuente no disponible');
      const datos: unknown = await res.json();
      if (fuente === 'sepa') {
        const { producto } = respuestaSepa.parse(datos);
        if (producto && !mismoGtin(producto.codigo, codigo)) throw new Error('Otro código');
        resultado = {
          fuente,
          estado: producto ? 'encontrado' : 'no_encontrado',
          ...(producto ? { producto } : {}),
        };
      } else {
        const { product: p } = respuestaOff.parse(datos);
        if (p && !mismoGtin(p.code, codigo)) throw new Error('Otro código');
        const imagen = p?.image_front_url;
        resultado = {
          fuente,
          estado: p ? 'encontrado' : 'no_encontrado',
          ...(p
            ? {
                producto: {
                  codigo,
                  nombre: p.product_name_es?.trim() || p.product_name?.trim() || '',
                  marca: p.brands ?? '',
                  presentacion: p.quantity ?? '',
                  ...(imagen &&
                  /^https:\/\/images\.open(?:food|beauty|products)facts\.org\//.test(imagen)
                    ? { imagen }
                    : {}),
                },
              }
            : {}),
        };
      }
    }
  } catch {
    if (signal.aborted) throw new DOMException('Consulta cancelada', 'AbortError');
    return { fuente, estado: 'no_disponible' };
  } finally {
    clearTimeout(plazo);
    signal.removeEventListener('abort', alCancelar);
  }
  if (cache.size >= 1000) cache.delete(cache.keys().next().value ?? '');
  cache.set(clave, { hasta: Date.now() + 15 * 60_000, fuente: resultado });
  return resultado;
}

/** Cada base conserva su respuesta: una caída no se presenta como producto inexistente. */
export async function consultarCatalogos(
  entrada: string,
  signal: AbortSignal,
): Promise<ResultadoCatalogos> {
  const codigo = codigoGtin(entrada);
  const fuentes = await Promise.all([
    consultarFuente('sepa', codigo, signal),
    consultarFuente('openfoodfacts', codigo, signal),
  ]);
  return { codigo, fuentes };
}
