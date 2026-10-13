import { delay, http, HttpResponse } from 'msw';
import { TIPOS_MOVIMIENTO, type Movimiento, type TipoMovimiento } from '@/types/stock';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';
import { bloqueDeLaSesion, coincide, etiquetaDe } from './registros';

// Mismo contrato que POST/GET /api/bloques/{id}/movimientos y GET /api/bloques/{id}/stock del backend.
const TAMANIO_POR_DEFECTO = 20;
const TAMANIO_MAXIMO = 100;
const MAXIMO_MOVIMIENTOS = 100;
const MAX_NOTA = 300;
const MAX_DECIMALES = 3;
const CANTIDAD_MAXIMA = 99_999_999_999.999;

const pathDe = (request: Request) => new URL(request.url).pathname;

function privado(request: Request, escritura = false) {
  return (escritura && exigirCsrf(request)) || exigirSesion(request);
}

const hoy = () => new Date().toISOString().slice(0, 10);

/** Entradas menos salidas de una fila (con el redondeo que evita 0.30000000000000004). */
export function stockDe(registroId: string) {
  let total = 0;
  for (const movimiento of estado.movimientos.values()) {
    if (movimiento.registroId !== registroId) continue;
    total += movimiento.tipo === 'ENTRADA' ? movimiento.cantidad : -movimiento.cantidad;
  }
  return Number(total.toFixed(MAX_DECIMALES));
}

const decimalesDe = (cantidad: number) => (String(cantidad).split('.')[1] ?? '').length;

function errorDeCantidad(cantidad: unknown) {
  if (typeof cantidad !== 'number' || !Number.isFinite(cantidad)) return 'Escribí la cantidad';
  if (cantidad <= 0) return 'Tiene que ser mayor a cero';
  if (decimalesDe(cantidad) > MAX_DECIMALES) return `Máximo ${MAX_DECIMALES} decimales`;
  if (cantidad > CANTIDAD_MAXIMA) return 'Es una cantidad demasiado grande';
  return undefined;
}

export const handlersStock = [
  http.post(api('/bloques/:bloqueId/movimientos'), async ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const path = pathDe(request);
    const cuerpo: unknown = await request.json().catch(() => null);
    if (!cuerpo || typeof cuerpo !== 'object') {
      return errorResponse(400, 'El cuerpo JSON no es válido', path);
    }
    const { registroId, tipo, cantidad, fecha, nota } = cuerpo as Record<string, unknown>;
    const errores: Record<string, string> = {};
    if (typeof registroId !== 'string' || !registroId) errores.registroId = 'Elegí el producto';
    const tipoValido = TIPOS_MOVIMIENTO.find((item) => item === tipo);
    if (!tipoValido) errores.tipo = 'Elegí si es una entrada o una salida';
    const errorCantidad = errorDeCantidad(cantidad);
    if (errorCantidad) errores.cantidad = errorCantidad;
    const fechaFinal = typeof fecha === 'string' && fecha ? fecha : hoy();
    if (fechaFinal > hoy()) errores.fecha = 'No puede ser una fecha futura';
    const notaFinal = typeof nota === 'string' && nota.trim() ? nota.trim() : null;
    if (notaFinal && notaFinal.length > MAX_NOTA) errores.nota = `Máximo ${MAX_NOTA} caracteres`;
    if (Object.keys(errores).length > 0) {
      return errorResponse(400, 'Revisá los datos marcados', path, errores);
    }

    const bloque = bloqueDeLaSesion(String(params.bloqueId));
    if (!bloque) return errorResponse(404, 'El bloque no existe', path);
    const producto = estado.registros.get(String(registroId));
    if (!producto || producto.bloqueId !== bloque.id) {
      return errorResponse(404, 'El registro no existe', path);
    }
    const salida = (tipoValido as TipoMovimiento) === 'SALIDA';
    const stock = stockDe(producto.id);
    if (salida && (cantidad as number) > stock) {
      return errorResponse(400, 'Revisá los datos marcados', path, {
        cantidad: `No hay stock suficiente: quedan ${stock}`,
      });
    }

    const movimiento: Movimiento = {
      id: crypto.randomUUID(),
      registroId: producto.id,
      producto: etiquetaDe(bloque, producto),
      tipo: tipoValido as TipoMovimiento,
      cantidad: cantidad as number,
      fecha: fechaFinal,
      nota: notaFinal,
      creadoEn: new Date().toISOString(),
    };
    estado.movimientos.set(movimiento.id, movimiento);
    return HttpResponse.json(
      { movimiento: movimiento, stockActual: stockDe(producto.id) },
      { status: 201 },
    );
  }),

  http.get(api('/bloques/:bloqueId/movimientos'), async ({ request, params }) => {
    const error = privado(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const bloque = bloqueDeLaSesion(String(params.bloqueId));
    if (!bloque) return errorResponse(404, 'El bloque no existe', pathDe(request));
    const registroId = new URL(request.url).searchParams.get('registroId');
    const delBloque = [...estado.movimientos.values()]
      .filter((movimiento) => {
        const producto = estado.registros.get(movimiento.registroId);
        return (
          producto?.bloqueId === bloque.id && (!registroId || movimiento.registroId === registroId)
        );
      })
      .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn) || b.id.localeCompare(a.id))
      .slice(0, MAXIMO_MOVIMIENTOS)
      .map((movimiento) => {
        const producto = estado.registros.get(movimiento.registroId);
        return {
          ...movimiento,
          producto: producto ? etiquetaDe(bloque, producto) : movimiento.producto,
        };
      });
    return HttpResponse.json(delBloque);
  }),

  http.get(api('/bloques/:bloqueId/stock'), async ({ request, params }) => {
    const error = privado(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const bloque = bloqueDeLaSesion(String(params.bloqueId));
    if (!bloque) return errorResponse(404, 'El bloque no existe', pathDe(request));
    const consulta = new URL(request.url).searchParams;
    const texto = consulta.get('q')?.trim().toLowerCase() ?? '';
    const pagina = Math.max(0, Number(consulta.get('pagina')) || 0);
    const tamanio = Math.min(
      TAMANIO_MAXIMO,
      Math.max(1, Number(consulta.get('tamanio')) || TAMANIO_POR_DEFECTO),
    );
    const productos = [...estado.registros.values()]
      .filter(
        (registro) => registro.bloqueId === bloque.id && (!texto || coincide(registro, texto)),
      )
      .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn) || b.id.localeCompare(a.id));
    const totalPages = Math.max(1, Math.ceil(productos.length / tamanio));
    return HttpResponse.json({
      content: productos.slice(pagina * tamanio, (pagina + 1) * tamanio).map((registro) => ({
        registroId: registro.id,
        producto: etiquetaDe(bloque, registro),
        stock: stockDe(registro.id),
      })),
      pageNumber: pagina,
      pageSize: tamanio,
      totalElements: productos.length,
      totalPages,
      first: pagina === 0,
      last: pagina >= totalPages - 1,
    });
  }),
];
