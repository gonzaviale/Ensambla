import { id, makeSystem, wireSystem } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';

// Los otros dos negocios de la cuenta de demostración (T-208), para que elegir negocio y compararlos muestre casos
// distintos al almacén: un kiosco que vende todos los días pero este mes gastó más de lo que entró, y un puesto de
// feria que no vende desde hace más de un mes. Las fechas se cuentan desde hoy, así la demo siempre se ve igual.

const sumarDias = (iso: string, dias: number) => {
  const fecha = new Date(`${iso}T12:00:00`);
  fecha.setDate(fecha.getDate() + dias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
};

type Producto = readonly [
  nombre: string,
  stock: number,
  precio: number,
  costo: number,
  minimo: number,
];

function conProductos(sistema: Sistema, productos: readonly Producto[]) {
  const inventario = sistema.sections[0]?.blocks.find((bloque) => bloque.type === 'inventory');
  if (!inventario) return [];
  inventario.rows = productos.map(([name, quantity, price, cost, minimum]) => ({
    id: id(),
    name,
    quantity,
    price,
    cost,
    minimum,
  }));
  return inventario.rows;
}

const bloque = (sistema: Sistema, tipo: string) =>
  sistema.sections[0]?.blocks.find((otro) => otro.type === tipo);

const PRODUCTOS_DEL_KIOSCO: readonly Producto[] = [
  ['Coca-Cola 500 ml', 24, 1400, 950, 12],
  ['Agua Villavicencio 500 ml', 30, 900, 520, 12],
  ['Alfajor Guaymallén', 60, 450, 260, 24],
  ['Chicles Beldent', 40, 700, 420, 20],
  ['Galletitas Oreo 118 g', 8, 1600, 1150, 10],
  ['Tarjeta SUBE (carga)', 0, 1000, 1000, 0],
];

/** Vende todos los días, pero el alquiler, la luz y una compra grande dejan el mes en rojo. */
export function crearKioscoCargado(hoy: string): Sistema {
  const sistema = makeSystem('Kiosco', ['inventory', 'sales', 'expenses'], false);
  wireSystem(sistema);
  sistema.demoAlmacen = true;
  const productos = conProductos(sistema, PRODUCTOS_DEL_KIOSCO);
  const ventas = bloque(sistema, 'sales');
  const gastos = bloque(sistema, 'expenses');
  if (!ventas || !gastos) return sistema;

  const filas: Fila[] = [];
  const vender = (indice: number, dias: number, cantidad: number) => {
    const origen = PRODUCTOS_DEL_KIOSCO[indice];
    if (!origen) return;
    filas.push({
      id: id(),
      product: productos[indice]?.id ?? '',
      quantity: cantidad,
      price: origen[2],
      date: sumarDias(hoy, -dias),
      customer: '',
      credit: false,
    });
  };
  for (let dia = 0; dia < 60; dia++) {
    vender(0, dia, 3);
    vender(1, dia, 2);
    vender(2, dia, 4);
    if (dia % 2 === 0) vender(3, dia, 2);
    if (dia % 4 === 0) vender(4, dia, 1);
  }
  ventas.rows = filas;

  gastos.rows = [
    ['Alquiler del local', 3, 220000, 'Alquiler'],
    ['Luz', 9, 48000, 'Servicios'],
    ['Compra al distribuidor de golosinas', 12, 95000, 'Mercadería'],
    ['Compra de bebidas', 20, 70000, 'Mercadería'],
    ['Alquiler del local', 33, 220000, 'Alquiler'],
  ].map(([name, dias, total, category]) => ({
    id: id(),
    name,
    date: sumarDias(hoy, -Number(dias)),
    total,
    category,
  }));
  return sistema;
}

const PRODUCTOS_DE_LA_FERIA: readonly Producto[] = [
  ['Frasco de miel 500 g', 14, 6500, 4200, 5],
  ['Dulce de leche casero', 9, 5200, 3100, 5],
  ['Mermelada de frutilla', 11, 4800, 2900, 5],
];

/** Vendió en las ferias de hace dos meses y desde entonces no volvió a salir: no tiene ventas en el período. */
export function crearPuestoDeFeria(hoy: string): Sistema {
  const sistema = makeSystem('Puesto de la feria', ['inventory', 'sales'], false);
  wireSystem(sistema);
  sistema.demoAlmacen = true;
  const productos = conProductos(sistema, PRODUCTOS_DE_LA_FERIA);
  const ventas = bloque(sistema, 'sales');
  if (!ventas) return sistema;

  ventas.rows = [38, 45, 52, 59, 66].flatMap((dias, feria) =>
    PRODUCTOS_DE_LA_FERIA.map(([, , precio], indice) => ({
      id: id(),
      product: productos[indice]?.id ?? '',
      quantity: 2 + ((feria + indice) % 3),
      price: precio,
      date: sumarDias(hoy, -dias),
      customer: '',
      credit: false,
    })),
  );
  return sistema;
}
