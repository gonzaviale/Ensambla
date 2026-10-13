import { delay, http, HttpResponse } from 'msw';
import type { Bloque, Campo } from '@/types/proyecto';
import {
  MAX_LINEAS_RECETA,
  MAX_UNIDAD_RECETA,
  type GuardarReceta,
  type Receta,
} from '@/types/receta';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';
import { bloqueDeLaSesion, etiquetaDe } from './registros';

// Mismo contrato que GET/PUT /api/registros/{id}/receta del backend: el costo se calcula al leer con el
// precio actual de cada insumo y una receta sin líneas se quita.
const MAX_DECIMALES = 3;
const CANTIDAD_MAXIMA = 99_999_999_999.999;
const INSUMO_ELIMINADO = 'Insumo eliminado';

const pathDe = (request: Request) => new URL(request.url).pathname;

function privado(request: Request, escritura = false) {
  return (escritura && exigirCsrf(request)) || exigirSesion(request);
}

/** La columna y su tabla, entre las de la persona con la sesión abierta. */
function campoDeLaSesion(campoId: string): { bloque: Bloque; campo: Campo } | undefined {
  for (const { usuarioId, proyecto } of estado.proyectos.values()) {
    if (usuarioId !== estado.sesionUsuarioId) continue;
    for (const bloque of proyecto.bloques) {
      const campo = bloque.campos.find((item) => item.id === campoId);
      if (campo) return { bloque, campo };
    }
  }
  return undefined;
}

const decimalesDe = (cantidad: number) => (String(cantidad).split('.')[1] ?? '').length;

function errorDeCantidad(cantidad: unknown) {
  if (typeof cantidad !== 'number' || !Number.isFinite(cantidad)) return 'Escribí la cantidad';
  if (cantidad <= 0) return 'La cantidad tiene que ser mayor a cero';
  if (decimalesDe(cantidad) > MAX_DECIMALES)
    return `La cantidad admite hasta ${MAX_DECIMALES} decimales`;
  if (cantidad > CANTIDAD_MAXIMA) return 'La cantidad es demasiado grande';
  return undefined;
}

const redondear = (numero: number) => Number(numero.toFixed(6));

/** La receta con el costo calculado con el precio actual de cada insumo. */
export function costear(receta: GuardarReceta | undefined): Receta {
  const ubicado = receta?.campoPrecioId ? campoDeLaSesion(receta.campoPrecioId) : undefined;
  if (!receta || !ubicado || receta.lineas.length === 0) {
    return { bloqueInsumosId: null, campoPrecioId: null, lineas: [], costo: 0, incompleta: false };
  }
  const lineas = receta.lineas.map((linea) => {
    const insumo = estado.registros.get(linea.insumoId);
    const existe = insumo?.bloqueId === ubicado.bloque.id;
    const precio = existe ? insumo?.valores[ubicado.campo.id] : undefined;
    const precioUnitario = typeof precio === 'number' ? precio : null;
    return {
      insumoId: linea.insumoId,
      insumo: existe && insumo ? etiquetaDe(ubicado.bloque, insumo) : INSUMO_ELIMINADO,
      cantidad: linea.cantidad,
      unidad: linea.unidad,
      precioUnitario,
      subtotal: precioUnitario === null ? null : redondear(linea.cantidad * precioUnitario),
    };
  });
  return {
    bloqueInsumosId: ubicado.bloque.id,
    campoPrecioId: ubicado.campo.id,
    lineas,
    costo: redondear(lineas.reduce((total, linea) => total + (linea.subtotal ?? 0), 0)),
    incompleta: lineas.some((linea) => linea.subtotal === null),
  };
}

export const handlersReceta = [
  http.get(api('/registros/:id/receta'), async ({ request, params }) => {
    const error = privado(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const producto = estado.registros.get(String(params.id));
    if (!producto || !bloqueDeLaSesion(producto.bloqueId)) {
      return errorResponse(404, 'El registro no existe', pathDe(request));
    }
    return HttpResponse.json(costear(estado.recetas.get(producto.id)));
  }),

  http.put(api('/registros/:id/receta'), async ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const path = pathDe(request);
    const producto = estado.registros.get(String(params.id));
    if (!producto || !bloqueDeLaSesion(producto.bloqueId)) {
      return errorResponse(404, 'El registro no existe', path);
    }
    const cuerpo: unknown = await request.json().catch(() => null);
    if (!cuerpo || typeof cuerpo !== 'object') {
      return errorResponse(400, 'El cuerpo JSON no es válido', path);
    }
    const { campoPrecioId, lineas } = cuerpo as { campoPrecioId?: unknown; lineas?: unknown };
    const recibidas = Array.isArray(lineas) ? (lineas as Record<string, unknown>[]) : [];
    const errores: Record<string, string> = {};

    if (recibidas.length === 0) {
      estado.recetas.delete(producto.id);
      return HttpResponse.json(costear(undefined));
    }
    if (recibidas.length > MAX_LINEAS_RECETA)
      errores.lineas = `Una receta puede tener hasta ${MAX_LINEAS_RECETA} insumos`;
    if (typeof campoPrecioId !== 'string' || !campoPrecioId)
      errores.campoPrecioId = 'Elegí la columna con el precio de los insumos';

    const vistos = new Set<string>();
    recibidas.forEach((linea, indice) => {
      const { insumoId, cantidad, unidad } = linea;
      const problema =
        typeof insumoId !== 'string' || !insumoId
          ? 'Elegí el insumo'
          : insumoId === producto.id
            ? 'Un producto no puede ser insumo de sí mismo'
            : vistos.has(insumoId)
              ? 'Ese insumo está repetido'
              : typeof unidad === 'string' && unidad.trim().length > MAX_UNIDAD_RECETA
                ? `La unidad puede tener hasta ${MAX_UNIDAD_RECETA} caracteres`
                : errorDeCantidad(cantidad);
      if (problema) errores[`lineas[${indice}]`] = problema;
      if (typeof insumoId === 'string') vistos.add(insumoId);
    });
    if (Object.keys(errores).length > 0) {
      return errorResponse(400, 'Revisá los datos marcados', path, errores);
    }

    const ubicado = campoDeLaSesion(String(campoPrecioId));
    if (!ubicado || ubicado.campo.tipo !== 'NUMERO') {
      return errorResponse(400, 'Revisá los datos marcados', path, {
        campoPrecioId: 'Elegí una columna numérica con el precio de los insumos',
      });
    }
    recibidas.forEach((linea, indice) => {
      const insumo = estado.registros.get(String(linea.insumoId));
      if (insumo?.bloqueId !== ubicado.bloque.id)
        errores[`lineas[${indice}]`] = 'Ese insumo ya no existe';
    });
    if (Object.keys(errores).length > 0) {
      return errorResponse(400, 'Revisá los datos marcados', path, errores);
    }

    estado.recetas.set(producto.id, {
      campoPrecioId: ubicado.campo.id,
      lineas: recibidas.map((linea) => ({
        insumoId: String(linea.insumoId),
        cantidad: linea.cantidad as number,
        unidad:
          typeof linea.unidad === 'string' && linea.unidad.trim() ? linea.unidad.trim() : null,
      })),
    });
    return HttpResponse.json(costear(estado.recetas.get(producto.id)));
  }),
];
