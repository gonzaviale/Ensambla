import { delay, http, HttpResponse } from 'msw';
import { sistemasDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';
import type { GrupoDeReposicion, ProductoFaltante } from '@/types/reposicion';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';

// Un PDF mínimo: el modo de demostración sólo necesita algo que descargar con el nombre y el tipo correctos.
const PDF_DE_EJEMPLO = new TextEncoder().encode('%PDF-1.4\n%%EOF\n');
const PEDIDO_SIN_MINIMO = 10;

const numeroDe = (valor: unknown) => (typeof valor === 'number' ? valor : null);

const textoDe = (valor: unknown) => (typeof valor === 'string' && valor.trim() ? valor.trim() : null);

const cantidadAPedir = (cantidad: number, minimo: number | null) =>
  Math.max((minimo ? minimo * 2 : PEDIDO_SIN_MINIMO) - cantidad, 1);

// La misma regla que el servidor: agotado, o en su mínimo o por debajo. Las filas de ejemplo no cuentan.
function faltanteDe(fila: Fila): (ProductoFaltante & { proveedor: string | null }) | null {
  const cantidad = numeroDe(fila.quantity);
  const minimo = numeroDe(fila.minimum);
  const hayMinimo = minimo !== null && minimo > 0;
  if (fila.__example || cantidad === null || (cantidad > 0 && !(hayMinimo && cantidad <= minimo))) return null;
  return {
    id: fila.id,
    nombre: textoDe(fila.name) ?? 'Producto sin nombre',
    cantidadActual: cantidad,
    cantidadAPedir: cantidadAPedir(cantidad, hayMinimo ? minimo : null),
    situacion: cantidad <= 0 ? 'Agotado' : `Quedan ${cantidad} (mínimo ${minimo})`,
    proveedor: textoDe(fila.supplier),
  };
}

const faltantesDe = (sistema: Sistema) =>
  sistema.sections
    .flatMap((seccion) => seccion.blocks)
    .filter((bloque: Bloque) => bloque.type === 'inventory')
    .flatMap((bloque) => bloque.rows.map(faltanteDe))
    .filter((faltante) => faltante !== null);

function agruparPorProveedor(sistema: Sistema): GrupoDeReposicion[] {
  const porProveedor = new Map<string | null, ProductoFaltante[]>();
  faltantesDe(sistema).forEach(({ proveedor, ...producto }) =>
    porProveedor.set(proveedor, [...(porProveedor.get(proveedor) ?? []), producto]),
  );
  return [...porProveedor.entries()]
    .map(([proveedor, productos]) => ({ proveedor, productos }))
    .sort((a, b) => Number(a.proveedor === null) - Number(b.proveedor === null));
}

const nombreDelArchivo = (proveedor: string | null) => {
  const limpio = (proveedor ?? 'pedido')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return `pedido-${limpio}-${new Date().toISOString().slice(0, 10).replaceAll('-', '')}.pdf`;
};

// Lo que hay que reponer de un sistema y el pedido a un proveedor en PDF.
export const handlersReposicion = [
  http.get(api('/proyectos/:sistemaId/reposicion'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    return HttpResponse.json({ grupos: agruparPorProveedor(sistema) });
  }),
  http.post(api('/proyectos/:sistemaId/reposicion/pdf'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    const pedido = (await request.json()) as { proveedor: string | null; productos: unknown[] };
    if (pedido.productos.length === 0) {
      return errorResponse(400, 'Elegí al menos un producto para el pedido', pathDe(request));
    }
    return new HttpResponse(PDF_DE_EJEMPLO, {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="${nombreDelArchivo(pedido.proveedor)}"`,
      },
    });
  }),
];
