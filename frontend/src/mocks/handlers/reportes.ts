import { delay, http, HttpResponse } from 'msw';
import { planesDe } from '../db/planes';
import { estado, sistemasDe } from '../db/estado';
import type { Sistema } from '@/lib/sistema/tipos';
import type { AvisoDelServidor } from '@/types/avisos';
import type { Metricas } from '@/types/metricas';
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
      fiado: fila.credit === true,
    }))
    .filter((venta) => venta.cantidad > 0 && venta.fecha && venta.fecha <= hoy);
  const enElPeriodo = (desde: string, hasta: string) =>
    ventas.filter((venta) => venta.fecha >= desde && venta.fecha <= hasta);
  const ingresosDe = (desde: string, hasta: string) =>
    enElPeriodo(desde, hasta).reduce(
      (total, venta) => total + (venta.precio ?? 0) * venta.cantidad,
      0,
    );
  const ventasDelPeriodo = (
    desde: string,
    hasta: string,
    anteriorDesde: string,
    anteriorHasta: string,
  ) => {
    const filasDelPeriodo = enElPeriodo(desde, hasta);
    const aFiado = filasDelPeriodo.filter((venta) => venta.fiado);
    return {
      ingresos: ingresosDe(desde, hasta),
      unidades: filasDelPeriodo.reduce((total, venta) => total + venta.cantidad, 0),
      cantidad: filasDelPeriodo.length,
      sinPrecio: filasDelPeriodo.filter((venta) => venta.precio === null).length,
      ingresosAnteriores: ingresosDe(anteriorDesde, anteriorHasta),
      fiado: aFiado.reduce((total, venta) => total + (venta.precio ?? 0) * venta.cantidad, 0),
      ventasFiadas: aFiado.length,
    };
  };

  const ultimaFecha = ventas
    .map((venta) => venta.fecha)
    .sort()
    .at(-1);
  const primeraFecha = ventas
    .map((venta) => venta.fecha)
    .sort()
    .at(0);

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
    ventas
      .filter((venta) => venta.fecha >= haceDias(hoy, QUINCENA_EN_DIAS - 1))
      .map((v) => v.producto),
  );
  const hayQuincena =
    primeraFecha !== undefined && primeraFecha <= haceDias(hoy, QUINCENA_EN_DIAS - 1);

  return {
    sistema: sistema.name,
    hoy,
    hayVentas: ventas.length > 0,
    hayProductos: productos.size > 0,
    ventas: {
      hoy: ventasDelPeriodo(hoy, hoy, haceDias(hoy, SEMANA_EN_DIAS), haceDias(hoy, SEMANA_EN_DIAS)),
      semana: ventasDelPeriodo(
        haceDias(hoy, SEMANA_EN_DIAS - 1),
        hoy,
        haceDias(hoy, 2 * SEMANA_EN_DIAS - 1),
        haceDias(hoy, SEMANA_EN_DIAS),
      ),
      mes: ventasDelPeriodo(
        haceDias(hoy, MES_EN_DIAS - 1),
        hoy,
        haceDias(hoy, 2 * MES_EN_DIAS - 1),
        haceDias(hoy, MES_EN_DIAS),
      ),
    },
    ventasPorDia: serieDiaria(
      ventas.map((venta) => ({
        fecha: venta.fecha,
        ingreso: (venta.precio ?? 0) * venta.cantidad,
      })),
      haceDias(hoy, MES_EN_DIAS - 1),
      hoy,
    ),
    ultimoDiaConVentas: ultimaFecha
      ? { fecha: ultimaFecha, ingresos: ingresosDe(ultimaFecha, ultimaFecha) }
      : null,
    masVendido:
      masVendido && totales
        ? {
            nombre: String(masVendido.name ?? 'Producto sin nombre'),
            unidades: totales.unidades,
            ingresos: totales.ingresos,
            ganancia: costo === null ? null : totales.ingresos - costo * totales.unidades,
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

const DIAS_DEL_PERIODO = { HOY: 1, SEMANA: SEMANA_EN_DIAS, MES: MES_EN_DIAS } as const;
type PeriodoDelServidor = keyof typeof DIAS_DEL_PERIODO;

const esPeriodo = (valor: string | null): valor is PeriodoDelServidor =>
  valor !== null && Object.hasOwn(DIAS_DEL_PERIODO, valor);

// Un punto por día entre las dos fechas, con cero en los días sin ventas: lo mismo que arma el backend.
function serieDiaria(ventas: { fecha: string; ingreso: number }[], desde: string, hasta: string) {
  const serie: { fecha: string; ingresos: number }[] = [];
  for (let dia = desde; dia <= hasta; dia = haceDias(dia, -1)) {
    serie.push({
      fecha: dia,
      ingresos: ventas
        .filter((venta) => venta.fecha === dia)
        .reduce((total, venta) => total + venta.ingreso, 0),
    });
  }
  return serie;
}

// Lo que Sami avisó sobre este cuaderno: lo pendiente ahora y lo que se resolvió dentro del período.
function avisosDe(
  sistemaId: string,
  todos: AvisoDelServidor[],
  desde: string,
  hasta: string,
): Metricas['avisos'] {
  const delCuaderno = todos.filter((aviso) => aviso.proyectoId === sistemaId);
  const pendientes = new Map<string, number>();
  delCuaderno
    .filter((aviso) => aviso.estado === 'PENDIENTE')
    .forEach((aviso) => pendientes.set(aviso.tipo, (pendientes.get(aviso.tipo) ?? 0) + 1));
  const resueltos = (resultado: 'APLICADO' | 'DESCARTADO') =>
    delCuaderno.filter((aviso) => {
      // El día se cuenta en hora local, como el resto de las fechas del mock (no el día en UTC).
      const dia = aviso.resueltoEn ? aIso(new Date(aviso.resueltoEn)) : undefined;
      return aviso.estado === resultado && dia !== undefined && dia >= desde && dia <= hasta;
    }).length;
  return {
    totalPendientes: [...pendientes.values()].reduce((total, cantidad) => total + cantidad, 0),
    pendientes: [...pendientes.entries()].map(([tipo, cantidad]) => ({
      tipo: tipo as AvisoDelServidor['tipo'],
      cantidad,
    })),
    aplicados: resueltos('APLICADO'),
    descartados: resueltos('DESCARTADO'),
    medidos: 0,
    ingresoSemanalDelta: 0,
  };
}

// Las métricas detalladas de «Mis métricas», con las mismas reglas que el servidor real.
export function metricasDe(
  sistema: Sistema,
  periodo: PeriodoDelServidor,
  hoy: string,
  avisosDelServidor: AvisoDelServidor[] = [],
): Metricas {
  const bloques = sistema.sections.flatMap((seccion) => seccion.blocks);
  const filas = (tipo: 'sales' | 'inventory' | 'expenses' | 'clients') =>
    bloques
      .filter((bloque) => bloque.type === tipo)
      .flatMap((bloque) => bloque.rows)
      .filter((fila) => !fila.__example);
  const productos = new Map(filas('inventory').map((fila) => [fila.id, fila]));
  const clientes = new Map(filas('clients').map((fila) => [fila.id, String(fila.name ?? '')]));
  const ventas = filas('sales')
    .map((fila) => {
      const cantidad = Number(fila.quantity) || 0;
      const precio = Number(fila.price) > 0 ? Number(fila.price) : null;
      return {
        producto: String(fila.product ?? ''),
        cliente: String(fila.customer ?? ''),
        cantidad,
        precio,
        ingreso: (precio ?? 0) * cantidad,
        fecha: String(fila.date ?? ''),
        fiado: fila.credit === true,
      };
    })
    .filter((venta) => venta.cantidad > 0 && venta.fecha && venta.fecha <= hoy);

  const dias = DIAS_DEL_PERIODO[periodo];
  const desde = haceDias(hoy, dias - 1);
  const entre = (inicio: string, fin: string) =>
    ventas.filter((venta) => venta.fecha >= inicio && venta.fecha <= fin);
  const sumar = (lista: { ingreso: number }[]) =>
    lista.reduce((total, venta) => total + venta.ingreso, 0);
  const delPeriodo = entre(desde, hoy);
  const ingresos = sumar(delPeriodo);
  // Hoy se compara con el mismo día de la semana pasada; semana y mes, con el período anterior del mismo largo.
  const ingresosAnteriores =
    periodo === 'HOY'
      ? sumar(entre(haceDias(hoy, SEMANA_EN_DIAS), haceDias(hoy, SEMANA_EN_DIAS)))
      : sumar(entre(haceDias(hoy, 2 * dias - 1), haceDias(hoy, dias)));

  const costoDe = (id: string) => {
    const costo = Number(productos.get(id)?.cost);
    return costo > 0 ? costo : null;
  };
  const conCosto = delPeriodo.filter(
    (venta) => venta.precio !== null && costoDe(venta.producto) !== null,
  );
  const gananciaEstimada =
    conCosto.length === 0
      ? null
      : conCosto.reduce(
          (total, venta) => total + venta.ingreso - (costoDe(venta.producto) ?? 0) * venta.cantidad,
          0,
        );

  const porProducto = new Map<string, { unidades: number; ingresos: number }>();
  delPeriodo.forEach((venta) => {
    const actual = porProducto.get(venta.producto) ?? { unidades: 0, ingresos: 0 };
    porProducto.set(venta.producto, {
      unidades: actual.unidades + venta.cantidad,
      ingresos: actual.ingresos + venta.ingreso,
    });
  });
  const masVendidos = [...porProducto.entries()]
    .sort((a, b) => b[1].unidades - a[1].unidades || b[1].ingresos - a[1].ingresos)
    .slice(0, 10)
    .map(([id, totales]) => {
      const costo = costoDe(id);
      return {
        nombre: String(productos.get(id)?.name ?? 'Producto sin nombre'),
        unidades: totales.unidades,
        ingresos: totales.ingresos,
        ganancia: costo === null ? null : totales.ingresos - costo * totales.unidades,
      };
    });

  const primeraFecha = ventas
    .map((venta) => venta.fecha)
    .sort()
    .at(0);
  const inicioQuincena = haceDias(hoy, QUINCENA_EN_DIAS - 1);
  const vendidosEnLaQuincena = new Set(entre(inicioQuincena, hoy).map((venta) => venta.producto));
  const parados =
    primeraFecha !== undefined && primeraFecha <= inicioQuincena
      ? [...productos.entries()]
          .filter(([id, fila]) => Number(fila.quantity) > 0 && !vendidosEnLaQuincena.has(id))
          .map(([id, fila]) => {
            const costo = costoDe(id);
            return {
              nombre: String(fila.name ?? 'Producto sin nombre'),
              cantidad: Number(fila.quantity),
              capital: costo === null ? null : costo * Number(fila.quantity),
            };
          })
          .sort((a, b) => (b.capital ?? -1) - (a.capital ?? -1))
          .slice(0, 10)
      : [];

  const faltantes = [...productos.values()]
    .filter((fila) => {
      const cantidad = Number(fila.quantity);
      const minimo = Number(fila.minimum);
      return cantidad <= 0 || (minimo > 0 && cantidad <= minimo);
    })
    .sort((a, b) => Number(a.quantity) - Number(b.quantity));
  const inventario = [...productos.values()];
  const valorDe = (campo: 'cost' | 'price') =>
    inventario.reduce(
      (total, fila) => total + Math.max(Number(fila.quantity) || 0, 0) * (Number(fila[campo]) || 0),
      0,
    );

  const porCliente = new Map<string, { compras: number; ingresos: number }>();
  delPeriodo
    .filter((venta) => venta.cliente)
    .forEach((venta) => {
      const actual = porCliente.get(venta.cliente) ?? { compras: 0, ingresos: 0 };
      porCliente.set(venta.cliente, {
        compras: actual.compras + 1,
        ingresos: actual.ingresos + venta.ingreso,
      });
    });
  const aFiado = delPeriodo.filter((venta) => venta.fiado);

  const bloqueDeGastos = bloques.some((bloque) => bloque.type === 'expenses');
  const gastosDelPeriodo = filas('expenses').filter((fila) => {
    const fecha = String(fila.date ?? '');
    return fecha >= desde && fecha <= hoy;
  });
  const porCategoria = new Map<string, number>();
  gastosDelPeriodo.forEach((fila) => {
    const categoria = String(fila.category ?? '') || 'Sin categoría';
    porCategoria.set(categoria, (porCategoria.get(categoria) ?? 0) + (Number(fila.total) || 0));
  });
  const totalGastos = [...porCategoria.values()].reduce((total, valor) => total + valor, 0);

  return {
    sistema: sistema.name,
    periodo,
    desde,
    hasta: hoy,
    hayVentas: ventas.length > 0,
    ventas: {
      ingresos,
      unidades: delPeriodo.reduce((total, venta) => total + venta.cantidad, 0),
      cantidad: delPeriodo.length,
      sinPrecio: delPeriodo.filter((venta) => venta.precio === null).length,
      ingresoPorDia: Math.round((ingresos / dias) * 100) / 100,
      ingresosAnteriores,
      gananciaEstimada,
    },
    porDia: serieDiaria(ventas, desde, hoy),
    masVendidos,
    parados,
    stock: {
      productos: productos.size,
      totalFaltantes: faltantes.length,
      faltantes: faltantes.slice(0, 10).map((fila) => ({
        nombre: String(fila.name ?? 'Producto sin nombre'),
        cantidad: Number(fila.quantity),
        minimo: Number(fila.minimum) > 0 ? Number(fila.minimum) : null,
        pedir: null,
      })),
      valorACosto: valorDe('cost'),
      valorAPrecio: valorDe('price'),
    },
    clientes: {
      top: [...porCliente.entries()]
        .sort((a, b) => b[1].ingresos - a[1].ingresos)
        .slice(0, 5)
        .map(([id, totales]) => ({ nombre: clientes.get(id) || id, ...totales })),
      fiado: sumar(aFiado),
      ventasFiadas: aFiado.length,
    },
    // La demostración no tiene agregados de la zona por producto: sin zona, como una cuenta sin rangos comparables.
    zona: null,
    avisos: avisosDe(sistema.id, avisosDelServidor, desde, hoy),
    gastos: bloqueDeGastos
      ? {
          total: totalGastos,
          cantidad: gastosDelPeriodo.length,
          resultado: ingresos - totalGastos,
          porCategoria: [...porCategoria.entries()]
            .sort((a, b) => b[1] - a[1])
            .map(([categoria, total]) => ({ categoria, total })),
        }
      : null,
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
  http.get(api('/proyectos/:sistemaId/resumen/metricas'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    const periodo = new URL(request.url).searchParams.get('periodo') ?? 'SEMANA';
    if (!esPeriodo(periodo)) return errorResponse(400, 'Período inválido', pathDe(request));
    return HttpResponse.json(
      metricasDe(
        sistema,
        periodo,
        aIso(new Date()),
        estado.avisosDelServidor.get(sesion.usuario.email) ?? [],
      ),
    );
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
