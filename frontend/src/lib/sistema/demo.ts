import { id, makeSystem, wireSystem } from './core';
import type { Fila, Sistema } from './tipos';

const sumarDias = (iso: string, dias: number) => {
  const fecha = new Date(`${iso}T12:00:00`);
  fecha.setDate(fecha.getDate() + dias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
};

// Un almacén chico con 60 días de ventas, pensado para que Sami tenga algo que decir: stock bajo, un
// producto que se vende mucho y barato, otro caro que no se mueve, productos que se llevan juntos y
// cosas que se venden en la zona y no están. Se cruza con los datos de ejemplo de la zona.
export function crearAlmacenDeEjemplo(hoy: string): Sistema {
  const sistema = makeSystem(
    'Almacén de ejemplo',
    ['inventory', 'sales', 'clients', 'entries'],
    false,
  );
  wireSystem(sistema);
  sistema.demoAlmacen = true;
  const [inventario, ventas, clientes] = sistema.sections[0]?.blocks ?? [];
  if (!inventario || !ventas || !clientes) return sistema;

  const productos = [
    ['Yerba Playadito 1 kg', 4, 3600, 2700, 10],
    ['Aceite Natura 900 ml', 0, 2600, 1900, 6],
    ['Coca-Cola 2,25 L', 18, 2700, 1950, 12],
    ['Azúcar Ledesma 1 kg', 25, 1500, 900, 10],
    ['Fideos Matarazzo 500 g', 30, 1100, 800, 10],
    ['Lavandina Ayudín 1 L', 2, 1350, 950, 6],
    ['Alfajor Jorgito', 40, 800, 520, 12],
    ['Galletitas Criollitas 3 U', 25, 1250, 880, 10],
  ] as const;
  inventario.rows = productos.map(([name, quantity, price, cost, minimum]) => ({
    id: id(),
    name,
    quantity,
    price,
    cost,
    minimum,
  }));
  const producto = (indice: number) => inventario.rows[indice]?.id ?? '';

  clientes.rows = [
    ['Doña Rosa', '11 5555-0111'],
    ['Bar La Esquina', '11 5555-0122'],
    ['Club Social', '11 5555-0133'],
  ].map(([name, phone]) => ({
    id: id(),
    name: name ?? '',
    phone: phone ?? '',
    email: '',
    notes: '',
  }));
  const cliente = (indice: number) => clientes.rows[indice % clientes.rows.length]?.id ?? '';

  const filas: Fila[] = [];
  const vender = (indiceDeProducto: number, dias: number, cantidad: number, quien = -1) => {
    const origen = productos[indiceDeProducto];
    if (!origen) return;
    filas.push({
      id: id(),
      product: producto(indiceDeProducto),
      quantity: cantidad,
      price: origen[2],
      date: sumarDias(hoy, -dias),
      customer: quien >= 0 ? cliente(quien) : '',
      credit: false,
    });
  };

  for (let dia = 0; dia < 30; dia++) vender(0, dia, 2); // la yerba vuela
  for (let dia = 0; dia < 30; dia += 2) vender(2, dia, 1); // la gaseosa, un día por medio
  for (let dia = 1; dia < 30; dia += 3) vender(1, dia, 1); // el aceite
  for (const dia of [4, 11, 17, 24]) vender(4, dia, 1); // los fideos, poco
  for (const dia of [1, 5, 9, 14, 19, 26]) vender(5, dia, 1); // la lavandina
  for (const dia of [45, 52, 58]) vender(3, dia, 2); // el azúcar: hace más de un mes que no sale
  // El alfajor y las galletitas salen poco, pero siempre juntos para el mismo cliente.
  [3, 9, 15, 22].forEach((dia, indice) => {
    vender(6, dia, 1, indice);
    vender(7, dia, 1, indice);
  });
  ventas.rows = filas;
  return sistema;
}
