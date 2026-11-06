import type {
  CompraProveedor,
  DemoProveedores,
  LineaPedido,
  ProductoProveedor,
  Proveedor,
  RevisionCompra,
  ComparacionCosto,
} from '@/types/proveedores';
import { demoProveedoresSchema } from '@/types/proveedores';

export const moneda = (valor: number) =>
  new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    maximumFractionDigits: 2,
  }).format(valor);
export const fechaCorta = (fecha: string) =>
  new Intl.DateTimeFormat('es-AR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(`${fecha}T12:00:00`));
export const centavos = (valor: number) => Math.round((valor + Number.EPSILON) * 100);
export const redondear = (valor: number) => centavos(valor) / 100;
export const totalCompra = (compra: Pick<RevisionCompra, 'lineas' | 'extras' | 'descuento'>) =>
  redondear(
    compra.lineas.reduce((total, linea) => total + centavos(linea.cantidad * linea.costo), 0) /
      100 +
      compra.extras -
      compra.descuento,
  );

export function sugerirPedido(productos: ProductoProveedor[]): LineaPedido[] {
  return productos
    .filter((p) => p.stock === 0 || p.stock < p.minimo)
    .map((p) => ({
      productoId: p.id,
      cantidad: Math.max(1, p.minimo * 2 - p.stock),
    }));
}

export function lineasDelPedido(datos: DemoProveedores, proveedorId: string) {
  return datos.pedido.flatMap((linea) => {
    const producto = datos.productos.find(
      (p) => p.id === linea.productoId && p.proveedorId === proveedorId,
    );
    return producto ? [{ ...linea, producto }] : [];
  });
}

export function mensajeDelPedido(datos: DemoProveedores, proveedor: Proveedor) {
  const lineas = lineasDelPedido(datos, proveedor.id).filter((l) => l.cantidad > 0);
  if (!lineas.length) return '';
  return [
    `Hola, ${proveedor.nombre}. Te paso mi pedido:`,
    '',
    ...lineas.map((l) => `• ${l.cantidad} × ${l.producto.nombre} (${l.producto.presentacion})`),
    '',
    '¿Me confirmás disponibilidad, precios y fecha de entrega? Gracias.',
  ].join('\n');
}

const numeroNormalizado = (valor: string) => valor.trim().replace(/\s+/g, '').toUpperCase();
export function validarRevision(datos: DemoProveedores, revision: RevisionCompra): string | null {
  if (!datos.proveedores.some((p) => p.id === revision.proveedorId)) return 'Elegí un proveedor.';
  if (!revision.numero.trim()) return 'Ingresá el número del comprobante.';
  const instante = new Date(`${revision.fecha}T12:00:00`);
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(revision.fecha) ||
    !Number.isFinite(instante.getTime()) ||
    instante.toISOString().slice(0, 10) !== revision.fecha
  )
    return 'Revisá la fecha.';
  if (
    datos.compras.some(
      (c) =>
        c.id === revision.id ||
        (c.proveedorId === revision.proveedorId &&
          numeroNormalizado(c.numero) === numeroNormalizado(revision.numero)),
    )
  )
    return 'Este comprobante ya está registrado para el proveedor.';
  if (!revision.lineas.length) return 'Agregá al menos un producto.';
  if (new Set(revision.lineas.map((l) => l.productoId)).size !== revision.lineas.length)
    return 'Hay productos repetidos. Unificá sus cantidades.';
  for (const linea of revision.lineas) {
    const producto = datos.productos.find((p) => p.id === linea.productoId);
    if (!producto) return 'Vinculá todos los renglones a un producto del stock.';
    if (producto.stock + linea.cantidad > 1_000_000)
      return 'La recepción supera el límite de stock de la demo.';
    if (
      !Number.isInteger(linea.cantidad) ||
      linea.cantidad <= 0 ||
      linea.cantidad > 1_000_000 ||
      !Number.isFinite(linea.costo) ||
      linea.costo <= 0 ||
      linea.costo > 1_000_000_000
    )
      return 'Revisá cantidades y costos: deben ser positivos y válidos.';
  }
  if (
    ![revision.extras, revision.descuento, revision.totalDeclarado].every(
      (n) => Number.isFinite(n) && n >= 0 && n <= 1_000_000_000,
    )
  )
    return 'Revisá los importes del comprobante.';
  const total = totalCompra(revision);
  if (total <= 0 || total > 1_000_000_000 || centavos(total) !== centavos(revision.totalDeclarado))
    return 'El total no coincide. Revisá renglones, otros cargos y descuento.';
  return null;
}

// Una sola transición: compra, stock y gasto quedan vinculados. Repetirla nunca vuelve a sumar unidades.
export function confirmarCompra(datos: DemoProveedores, revision: RevisionCompra): DemoProveedores {
  if (datos.compras.some((c) => c.id === revision.id)) return datos;
  const error = validarRevision(datos, revision);
  if (error) throw new Error(error);
  const compra: CompraProveedor = {
    id: revision.id,
    proveedorId: revision.proveedorId,
    fecha: revision.fecha,
    archivo: revision.archivo,
    lineas: revision.lineas,
    extras: revision.extras,
    descuento: revision.descuento,
    numero: revision.numero.trim(),
    total: totalCompra(revision),
  };
  const productos = datos.productos.map((p) => {
    const linea = compra.lineas.find((l) => l.productoId === p.id);
    // Un comprobante antiguo suma la recepción, pero no reemplaza un costo más reciente.
    const hayPosterior = datos.compras.some(
      (c) => c.fecha > compra.fecha && c.lineas.some((l) => l.productoId === p.id),
    );
    return linea
      ? { ...p, stock: p.stock + linea.cantidad, costo: hayPosterior ? p.costo : linea.costo }
      : p;
  });
  return demoProveedoresSchema.parse({
    ...datos,
    productos,
    compras: [...datos.compras, compra],
    gastos: [
      ...datos.gastos,
      {
        id: `gasto-${compra.id}`,
        name: `Compra ${compra.numero}`,
        date: compra.fecha,
        total: compra.total,
        category: 'Mercadería',
        compraId: compra.id,
      },
    ],
  });
}

export function compararCostos(datos: DemoProveedores, proveedorId: string): ComparacionCosto[] {
  const compras = datos.compras
    .filter((c) => c.proveedorId === proveedorId)
    .sort((a, b) => a.fecha.localeCompare(b.fecha));
  return datos.productos.flatMap((producto) => {
    const coincidencias = compras.flatMap((compra) => {
      const linea = compra.lineas.find((l) => l.productoId === producto.id);
      return linea ? [{ compra, linea }] : [];
    });
    const ultima = coincidencias.at(-1);
    if (!ultima) return [];
    const anterior = coincidencias.at(-2)?.linea.costo ?? null;
    const variacion = anterior && anterior > 0 ? (ultima.linea.costo / anterior - 1) * 100 : null;
    return [
      {
        ...ultima,
        producto,
        anterior,
        variacion,
        margenActual: producto.precio > 0 ? (1 - ultima.linea.costo / producto.precio) * 100 : null,
        precioSugerido:
          anterior && producto.precio > anterior
            ? redondear((producto.precio * ultima.linea.costo) / anterior)
            : null,
      },
    ];
  });
}

export function resumenDelMes(datos: DemoProveedores, mes: string) {
  const ventas = datos.ventas.filter((v) => v.fecha.slice(0, 7) === mes);
  const ingresos = redondear(ventas.reduce((s, v) => s + v.precio * v.cantidad, 0));
  const costoVendido = redondear(ventas.reduce((s, v) => s + v.costo * v.cantidad, 0));
  const gastos = datos.gastos.filter((g) => g.date.slice(0, 7) === mes);
  const operativos = redondear(
    gastos.filter((g) => g.category !== 'Mercadería').reduce((s, g) => s + g.total, 0),
  );
  const compras = redondear(
    gastos.filter((g) => g.category === 'Mercadería').reduce((s, g) => s + g.total, 0),
  );
  return {
    ingresos,
    costoVendido,
    margen: redondear(ingresos - costoVendido),
    operativos,
    resultado: redondear(ingresos - costoVendido - operativos),
    compras,
  };
}
