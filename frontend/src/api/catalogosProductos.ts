import { z } from 'zod';
import { http } from './client';
import { codigoGtin, mismoGtin } from '@/lib/escaneo/codigo';
import type { CoincidenciaCatalogo, ResultadoCatalogos } from '@/types/escaneo';

const respuestaCatalogo = z.object({
  producto: z
    .object({
      codigo: z.string(),
      nombre: z.string(),
      marca: z.string(),
      presentacion: z.string(),
      imagen: z.string().nullish(),
    })
    .nullable(),
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
    const { producto } = await http.get(
      `/catalogos/${fuente}/${codigo}`,
      respuestaCatalogo,
      controlador.signal,
    );
    if (producto && !mismoGtin(producto.codigo, codigo)) throw new Error('Otro código');
    const imagen = producto?.imagen;
    resultado = {
      fuente,
      estado: producto ? 'encontrado' : 'no_encontrado',
      ...(producto
        ? {
            producto: {
              codigo: producto.codigo,
              nombre: producto.nombre,
              marca: producto.marca,
              presentacion: producto.presentacion,
              ...(imagen &&
              /^https:\/\/images\.open(?:food|beauty|products)facts\.org\//.test(imagen)
                ? { imagen }
                : {}),
            },
          }
        : {}),
    };
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
