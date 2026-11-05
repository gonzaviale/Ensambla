import { delay, http, HttpResponse } from 'msw';
import { planesDe } from '../db/planes';
import { sistemasDe } from '../db/estado';
import type { Sistema } from '@/lib/sistema/tipos';
import type { ResumenInicio } from '@/types/resumenInicio';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

// Un PDF mínimo: el modo de demostración sólo necesita algo que descargar con el nombre y el tipo correctos.
const PDF_DE_EJEMPLO = new TextEncoder().encode('%PDF-1.4\n%%EOF\n');

const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

const SEMANA_EN_DIAS = 7;
const QUINCENA_EN_DIAS = 14;
const MES_EN_DIAS = 30;

const aFecha = (iso: string) => {
  const [anio = 0, mes = 1, dia = 1] = iso.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
};
const aIso = (fecha: Date) =>
  `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
const haceDias = (iso: string, dias: number) => {
  const fecha = aFecha(iso);
  fecha.setDate(fecha.getDate() - dias);
  return aIso(fecha);
};

// Las mismas cuentas que hace el backend con las ventas y el inventario del cuaderno, para que la demostración
// muestre lo que mostraría el servidor real.
export function resumenInicioDe(sistema: Sistema, hoy: string): ResumenInicio {
  const filas = (tipo: 'sales' | 'inventory') =>
    sistema.sections
      .flatMap((seccion) => seccion.blocks)
      .filter((bloque) => bloque.type === tipo)
      .flatMap((bloque) => bloque.rows)
      .filter((fila) => !fila.__example);
  const productos = new Map(filas('inventory').map((fila) => [fila.id, fila]));
  const ventas = filas('sales')
    .map((fila) => ({
      producto: String(fila.product ?? ''),
      cantidad: Number(fila.quantity) || 0,
      precio: Number(fila.price) > 0 ? Number(fila.price) : null,
      fecha: String(fila.date ?? ''),
    }))
    .filter((venta) => venta.cantidad > 0 && venta.fecha && venta.fecha <= hoy);
  const ingresosDe = (desde: string, hasta: string) =>
    ventas
      .filter((venta) => venta.fecha >= desde && venta.fecha <= hasta)
      .reduce((total, venta) => total + (venta.precio ?? 0) * venta.cantidad, 0);

  const deHoy = ventas.filter((venta) => venta.fecha === hoy);
  const ultimaFecha = ventas.map((venta) => venta.fecha).sort().at(-1);
  const primeraFecha = ventas.map((venta) => venta.fecha).sort().at(0);

  const delMes = ventas.filter((venta) => venta.fecha >= haceDias(hoy, MES_EN_DIAS - 1));
  const porProducto = new Map<string, { unidades: number; ingresos: number }>();
  delMes.forEach((venta) => {
    const actual = porProducto.get(venta.producto) ?? { unidades: 0, ingresos: 0 };
    porProducto.set(venta.producto, {
      unidades: actual.unidades + venta.cantidad,
      ingresos: actual.ingresos + (venta.precio ?? 0) * venta.cantidad,
    });
  });
  const [idMasVendido, totales] =
    [...porProducto.entries()].sort((a, b) => b[1].unidades - a[1].unidades)[0] ?? [];
  const masVendido = idMasVendido ? productos.get(idMasVendido) : undefined;
  const costo = Number(masVendido?.cost) > 0 ? Number(masVendido?.cost) : null;

  const faltantes = [...productos.values()]
    .filter((fila) => {
      const cantidad = Number(fila.quantity);
      const minimo = Number(fila.minimum);
      return cantidad <= 0 || (minimo > 0 && cantidad <= minimo);
    })
    .sort((a, b) => Number(a.quantity) - Number(b.quantity));

  const vendidosEnLaQuincena = new Set(
    ventas.filter((venta) => venta.fecha >= haceDias(hoy, QUINCENA_EN_DIAS - 1)).map((v) => v.producto),
  );
  const hayQuincena = primeraFecha !== undefined && primeraFecha <= haceDias(hoy, QUINCENA_EN_DIAS - 1);

  return {
    sistema: sistema.name,
    hoy,
    hayVentas: ventas.length > 0,
    hayProductos: productos.size > 0,
    ventasDeHoy: {
      ingresos: ingresosDe(hoy, hoy),
      unidades: deHoy.reduce((total, venta) => total + venta.cantidad, 0),
      cantidad: deHoy.length,
      sinPrecio: deHoy.filter((venta) => venta.precio === null).length,
      ingresosSemanaPasada: ingresosDe(haceDias(hoy, SEMANA_EN_DIAS), haceDias(hoy, SEMANA_EN_DIAS)),
    },
    ultimoDiaConVentas: ultimaFecha
      ? { fecha: ultimaFecha, ingresos: ingresosDe(ultimaFecha, ultimaFecha) }
      : null,
    masVendido:
      masVendido && totales
        ? {
            nombre: String(masVendido.name ?? 'Producto sin nombre'),
            unidades: totales.unidades,
            ingresos: totales.ingresos,
            ganancia:
              costo === null ? null : totales.ingresos - costo * totales.unidades,
          }
        : null,
    faltantes: {
      total: faltantes.length,
      primeros: faltantes.slice(0, 3).map((fila) => ({
        nombre: String(fila.name ?? 'Producto sin nombre'),
        cantidad: Number(fila.quantity),
        minimo: Number(fila.minimum) > 0 ? Number(fila.minimum) : null,
        pedir: null,
      })),
    },
    parados: hayQuincena
      ? [...productos.entries()]
          .filter(([id, fila]) => Number(fila.quantity) > 0 && !vendidosEnLaQuincena.has(id))
          .map(([, fila]) => ({
            nombre: String(fila.name ?? 'Producto sin nombre'),
            cantidad: Number(fila.quantity),
            capital: Number(fila.cost) > 0 ? Number(fila.cost) * Number(fila.quantity) : null,
          }))
          .sort((a, b) => (b.capital ?? -1) - (a.capital ?? -1))
          .slice(0, 3)
      : [],
  };
}

// Las métricas del inicio y el resumen del negocio en PDF: una función Premium del sistema de la persona.
export const handlersReportes = [
  http.get(api('/proyectos/:sistemaId/resumen/inicio'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    return HttpResponse.json(resumenInicioDe(sistema, aIso(new Date())));
  }),
  http.get(api('/proyectos/:sistemaId/resumen/pdf'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    if (planesDe(sesion.usuario.email).suscripcion.codigoPlanActual !== 'PREMIUM') {
      return errorResponse(403, 'Esta función es parte del plan Premium', pathDe(request));
    }
    const fecha = new Date().toISOString().slice(0, 10).replaceAll('-', '');
    return new HttpResponse(PDF_DE_EJEMPLO, {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="resumen-${sinTildes(sistema.name) || 'sistema'}-${fecha}.pdf"`,
      },
    });
  }),
];
