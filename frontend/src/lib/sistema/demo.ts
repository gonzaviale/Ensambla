import { id, makeSystem, wireSystem } from './core';
import type { Bloque, Fila, Sistema, TipoBloque } from './tipos';

const BLOQUES_DEL_ALMACEN: TipoBloque[] = ['inventory', 'sales', 'clients', 'entries'];

type BloquesDelAlmacen = { inventario: Bloque; ventas: Bloque; clientes: Bloque };

type ProductoDeDemo = {
  nombre: string;
  stock: number;
  precio: number;
  costo: number;
  minimo: number;
};

const sumarDias = (iso: string, dias: number) => {
  const fecha = new Date(`${iso}T12:00:00`);
  fecha.setDate(fecha.getDate() + dias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
};

function almacenVacio(nombre: string): Sistema {
  const sistema = makeSystem(nombre, BLOQUES_DEL_ALMACEN, false);
  wireSystem(sistema);
  sistema.demoAlmacen = true;
  return sistema;
}

function bloquesDelAlmacen(sistema: Sistema): BloquesDelAlmacen | undefined {
  const [inventario, ventas, clientes] = sistema.sections[0]?.blocks ?? [];
  return inventario && ventas && clientes ? { inventario, ventas, clientes } : undefined;
}

function cargarProductosYClientes(
  { inventario, clientes }: BloquesDelAlmacen,
  productos: ProductoDeDemo[],
  conocidos: string[][],
) {
  inventario.rows = productos.map((producto) => ({
    id: id(),
    name: producto.nombre,
    quantity: producto.stock,
    price: producto.precio,
    cost: producto.costo,
    minimum: producto.minimo,
  }));
  clientes.rows = conocidos.map(([name, phone]) => ({
    id: id(),
    name: name ?? '',
    phone: phone ?? '',
    email: '',
    notes: '',
  }));
}

/** Anota las ventas de la demo: cada una apunta al producto y, si tiene, al cliente que se la llevó. */
class Mostrador {
  readonly filas: Fila[] = [];

  constructor(
    private readonly bloques: BloquesDelAlmacen,
    private readonly productos: ProductoDeDemo[],
    private readonly hoy: string,
  ) {}

  vender(indice: number, dias: number, cantidad: number, quien = -1, fiado = false) {
    const producto = this.productos[indice];
    if (!producto) return;
    this.filas.push({
      id: id(),
      product: this.bloques.inventario.rows[indice]?.id ?? '',
      quantity: cantidad,
      price: producto.precio,
      date: sumarDias(this.hoy, -dias),
      customer: quien >= 0 ? this.cliente(quien) : '',
      credit: fiado,
    });
  }

  private cliente(indice: number) {
    const { rows } = this.bloques.clientes;
    return rows[indice % rows.length]?.id ?? '';
  }
}

type Almacen = {
  nombre: string;
  productos: ProductoDeDemo[];
  clientes: string[][];
  anotarVentas: (mostrador: Mostrador) => void;
};

function armarAlmacen(
  { nombre, productos, clientes, anotarVentas }: Almacen,
  hoy: string,
): Sistema {
  const sistema = almacenVacio(nombre);
  const bloques = bloquesDelAlmacen(sistema);
  if (!bloques) return sistema;
  cargarProductosYClientes(bloques, productos, clientes);
  const mostrador = new Mostrador(bloques, productos, hoy);
  anotarVentas(mostrador);
  bloques.ventas.rows = mostrador.filas;
  return sistema;
}

const productoDeDemo = (
  nombre: string,
  stock: number,
  precio: number,
  costo: number,
  minimo: number,
): ProductoDeDemo => ({ nombre, stock, precio, costo, minimo });

function anotarVentasDeEjemplo(mostrador: Mostrador) {
  for (let dia = 0; dia < 30; dia++) mostrador.vender(0, dia, 2); // la yerba vuela
  for (let dia = 0; dia < 30; dia += 2) mostrador.vender(2, dia, 1); // la gaseosa, un día por medio
  for (let dia = 1; dia < 30; dia += 3) mostrador.vender(1, dia, 1); // el aceite
  for (const dia of [4, 11, 17, 24]) mostrador.vender(4, dia, 1); // los fideos, poco
  for (const dia of [1, 5, 9, 14, 19, 26]) mostrador.vender(5, dia, 1); // la lavandina
  for (const dia of [45, 52, 58]) mostrador.vender(3, dia, 2); // el azúcar: hace más de un mes que no sale
  // El alfajor y las galletitas salen poco, pero siempre juntos para el mismo cliente.
  [3, 9, 15, 22].forEach((dia, indice) => {
    mostrador.vender(6, dia, 1, indice);
    mostrador.vender(7, dia, 1, indice);
  });
}

// Un almacén chico con 60 días de ventas, pensado para que Sami tenga algo que decir: stock bajo, un
// producto que se vende mucho y barato, otro caro que no se mueve, productos que se llevan juntos y
// cosas que se venden en la zona y no están. Se cruza con los datos de ejemplo de la zona.
const ALMACEN_DE_EJEMPLO: Almacen = {
  nombre: 'Almacén de ejemplo',
  productos: [
    productoDeDemo('Yerba Playadito 1 kg', 4, 3600, 2700, 10),
    productoDeDemo('Aceite Natura 900 ml', 0, 2600, 1900, 6),
    productoDeDemo('Coca-Cola 2,25 L', 18, 2700, 1950, 12),
    productoDeDemo('Azúcar Ledesma 1 kg', 25, 1500, 900, 10),
    productoDeDemo('Fideos Matarazzo 500 g', 30, 1100, 800, 10),
    productoDeDemo('Lavandina Ayudín 1 L', 2, 1350, 950, 6),
    productoDeDemo('Alfajor Jorgito', 40, 800, 520, 12),
    productoDeDemo('Galletitas Criollitas 3 U', 25, 1250, 880, 10),
  ],
  clientes: [
    ['Doña Rosa', '11 5555-0111'],
    ['Bar La Esquina', '11 5555-0122'],
    ['Club Social', '11 5555-0133'],
  ],
  anotarVentas: anotarVentasDeEjemplo,
};

export const crearAlmacenDeEjemplo = (hoy: string): Sistema =>
  armarAlmacen(ALMACEN_DE_EJEMPLO, hoy);

type ProductoCargado = ProductoDeDemo & {
  /** Cada cuántos días hay una venta, cuántas unidades y desde qué día hacia atrás (0 = hoy). */
  cada: number;
  unidades: number;
  desde?: number;
};

// Los nombres con zona se cruzan con los datos de ejemplo de la zona; el resto sólo mira stock y ventas.
const PRODUCTOS_CARGADOS: ProductoCargado[] = [
  {
    nombre: 'Yerba Playadito 1 kg',
    stock: 4,
    precio: 3600,
    costo: 2700,
    minimo: 10,
    cada: 1,
    unidades: 2,
  },
  {
    nombre: 'Aceite Natura 900 ml',
    stock: 0,
    precio: 2600,
    costo: 1900,
    minimo: 6,
    cada: 3,
    unidades: 1,
  },
  {
    nombre: 'Coca-Cola 2,25 L',
    stock: 18,
    precio: 2700,
    costo: 1950,
    minimo: 12,
    cada: 2,
    unidades: 2,
  },
  {
    nombre: 'Azúcar Ledesma 1 kg',
    stock: 25,
    precio: 1500,
    costo: 900,
    minimo: 10,
    cada: 6,
    unidades: 2,
    desde: 40,
  },
  {
    nombre: 'Fideos Matarazzo 500 g',
    stock: 30,
    precio: 1100,
    costo: 800,
    minimo: 10,
    cada: 9,
    unidades: 1,
  },
  {
    nombre: 'Lavandina Ayudín 1 L',
    stock: 2,
    precio: 1350,
    costo: 950,
    minimo: 6,
    cada: 5,
    unidades: 1,
  },
  {
    nombre: 'Alfajor Jorgito',
    stock: 40,
    precio: 800,
    costo: 520,
    minimo: 12,
    cada: 7,
    unidades: 1,
  },
  {
    nombre: 'Galletitas Criollitas 3 U',
    stock: 25,
    precio: 1250,
    costo: 880,
    minimo: 10,
    cada: 7,
    unidades: 1,
  },
  {
    nombre: 'Pan lactal Bimbo 360 g',
    stock: 6,
    precio: 2100,
    costo: 1600,
    minimo: 12,
    cada: 1,
    unidades: 1,
  },
  {
    nombre: 'Cerveza Quilmes 1 L',
    stock: 22,
    precio: 2900,
    costo: 2000,
    minimo: 12,
    cada: 4,
    unidades: 2,
    desde: 35,
  },
  {
    nombre: 'Fernet Branca 750 ml',
    stock: 5,
    precio: 9500,
    costo: 7200,
    minimo: 3,
    cada: 10,
    unidades: 1,
  },
  {
    nombre: 'Harina 000 Pureza 1 kg',
    stock: 3,
    precio: 1200,
    costo: 800,
    minimo: 8,
    cada: 2,
    unidades: 1,
  },
  {
    nombre: 'Arroz Gallo Oro 1 kg',
    stock: 20,
    precio: 1900,
    costo: 1300,
    minimo: 8,
    cada: 3,
    unidades: 1,
  },
  {
    nombre: 'Café La Virginia 250 g',
    stock: 2,
    precio: 3300,
    costo: 2400,
    minimo: 5,
    cada: 4,
    unidades: 1,
  },
  {
    nombre: 'Mate cocido Taragüí x25',
    stock: 14,
    precio: 1400,
    costo: 950,
    minimo: 6,
    cada: 8,
    unidades: 1,
  },
  {
    nombre: 'Detergente Magistral 750 ml',
    stock: 4,
    precio: 2800,
    costo: 2000,
    minimo: 6,
    cada: 4,
    unidades: 1,
  },
  {
    nombre: 'Jabón en polvo Skip 800 g',
    stock: 9,
    precio: 4200,
    costo: 3200,
    minimo: 5,
    cada: 9,
    unidades: 1,
  },
  {
    nombre: 'Papel higiénico Elite x4',
    stock: 0,
    precio: 2500,
    costo: 1800,
    minimo: 6,
    cada: 2,
    unidades: 1,
  },
  {
    nombre: 'Salsa de tomate Arcor 340 g',
    stock: 30,
    precio: 900,
    costo: 600,
    minimo: 10,
    cada: 9,
    unidades: 1,
  },
  {
    nombre: 'Atún La Campagnola 170 g',
    stock: 24,
    precio: 2300,
    costo: 1700,
    minimo: 8,
    cada: 6,
    unidades: 1,
  },
  {
    nombre: 'Dulce de leche La Serenísima 400 g',
    stock: 12,
    precio: 3100,
    costo: 2300,
    minimo: 5,
    cada: 7,
    unidades: 1,
  },
  {
    nombre: 'Huevos maple x30',
    stock: 1,
    precio: 7800,
    costo: 6200,
    minimo: 4,
    cada: 2,
    unidades: 1,
  },
  {
    nombre: 'Manteca La Serenísima 200 g',
    stock: 11,
    precio: 2600,
    costo: 1950,
    minimo: 5,
    cada: 5,
    unidades: 1,
  },
  {
    nombre: 'Sprite 2,25 L',
    stock: 16,
    precio: 2600,
    costo: 1900,
    minimo: 8,
    cada: 3,
    unidades: 2,
  },
  {
    nombre: 'Vino Termidor 1 L',
    stock: 18,
    precio: 2200,
    costo: 1500,
    minimo: 6,
    cada: 6,
    unidades: 1,
  },
  {
    nombre: 'Polenta Presto Pronta 500 g',
    stock: 15,
    precio: 1100,
    costo: 750,
    minimo: 6,
    cada: 12,
    unidades: 1,
  },
  {
    nombre: 'Sal Celusal 500 g',
    stock: 22,
    precio: 800,
    costo: 500,
    minimo: 6,
    cada: 14,
    unidades: 1,
  },
  {
    nombre: 'Lentejas Alcazar 400 g',
    stock: 8,
    precio: 1500,
    costo: 1050,
    minimo: 5,
    cada: 15,
    unidades: 1,
    desde: 45,
  },
  {
    nombre: 'Galletitas Terrabusi Variedad',
    stock: 20,
    precio: 2900,
    costo: 2100,
    minimo: 6,
    cada: 11,
    unidades: 1,
  },
];

const CLIENTES_CARGADOS = [
  ['Doña Rosa', '11 5555-0111'],
  ['Bar La Esquina', '11 5555-0122'],
  ['Club Social', '11 5555-0133'],
  ['Panadería San José', '11 5555-0144'],
  ['Kiosco El Sol', '11 5555-0155'],
  ['Don Alberto', '11 5555-0166'],
  ['Familia Gómez', '11 5555-0177'],
  ['Escuela N° 14', '11 5555-0188'],
  ['Doña Marta', '11 5555-0199'],
  ['Parrilla Los Amigos', '11 5555-0210'],
  ['Carlos del 3° B', '11 5555-0221'],
  ['Sociedad de Fomento', '11 5555-0232'],
];

function anotarVentasCargadas(mostrador: Mostrador) {
  PRODUCTOS_CARGADOS.forEach((producto, indice) => anotarLaRutina(mostrador, producto, indice));
  // Dos pares que se llevan siempre juntos para el mismo cliente: Sami propone armar un combo.
  venderJuntos(mostrador, 'Alfajor Jorgito', 'Galletitas Criollitas 3 U', [3, 10, 17, 25]);
  venderJuntos(mostrador, 'Salsa de tomate Arcor 340 g', 'Fideos Matarazzo 500 g', [2, 8, 15, 22]);
}

const seLlevaFiado = (dia: number, indice: number) => (dia + indice) % 7 === 0;

function anotarLaRutina(mostrador: Mostrador, producto: ProductoCargado, indice: number) {
  for (let dia = producto.desde ?? 0; dia < 90; dia += producto.cada) {
    // Un cliente de cada tres; el resto, venta de mostrador.
    const quien = (dia + indice) % 3 === 0 ? dia + indice : -1;
    mostrador.vender(
      indice,
      dia,
      producto.unidades,
      quien,
      quien >= 0 && seLlevaFiado(dia, indice),
    );
  }
}

const indiceDelProducto = (nombre: string) =>
  PRODUCTOS_CARGADOS.findIndex((producto) => producto.nombre === nombre);

function venderJuntos(mostrador: Mostrador, a: string, b: string, dias: number[]) {
  const indices = [indiceDelProducto(a), indiceDelProducto(b)];
  dias.forEach((dia, quien) => {
    for (const indice of indices)
      mostrador.vender(indice, dia, 1, quien + 2, seLlevaFiado(dia, indice));
  });
}

/** Un almacén de barrio completo para ver cómo se siente con muchos datos: 90 días de ventas y avisos de todos los tipos. */
export const crearAlmacenCargado = (hoy: string): Sistema =>
  armarAlmacen(
    {
      nombre: 'Almacén de barrio',
      productos: PRODUCTOS_CARGADOS,
      clientes: CLIENTES_CARGADOS,
      anotarVentas: anotarVentasCargadas,
    },
    hoy,
  );
