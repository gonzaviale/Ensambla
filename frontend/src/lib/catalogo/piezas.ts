import type { Pieza, PiezaId, RubroId } from '@/types/negocio';

// Catálogo de piezas: lo que Ensambla puede ensamblar. Un rubro nuevo es sumar piezas o cambiar
// cuáles son su base; las pantallas no cambian. Las respuestas buscan columnas por `rol`.

export const PIEZAS: Pieza[] = [
  {
    id: 'productos',
    nombre: 'Productos',
    alias: ['Platos', 'Artículos', 'Mercadería', 'Mis productos'],
    nombrePorRubro: { gastronomia: 'Platos' },
    descripcion: 'Lo que vendés, con lo que te cuesta y a cuánto lo vendés.',
    campos: [
      { nombre: 'Nombre', tipo: 'TEXTO', rol: 'nombre', obligatorio: true },
      { nombre: 'Costo', tipo: 'NUMERO', rol: 'costo' },
      { nombre: 'Precio', tipo: 'NUMERO', rol: 'precio' },
      { nombre: 'Stock', tipo: 'NUMERO', rol: 'stock', valorInicial: '0' },
      { nombre: 'Ganancia', tipo: 'FORMULA', rol: 'monto', formula: '{precio} - {costo}' },
    ],
    senales: ['producto', 'precio', 'stock', 'mercaderia', 'plato', 'menu', 'catalogo'],
    base: ['comercio', 'gastronomia'],
  },
  {
    id: 'ventas',
    nombre: 'Ventas',
    alias: ['Venta', 'Cobros'],
    nombrePorRubro: {},
    descripcion: 'Cada venta que hacés: qué, cuántas y por cuánto.',
    campos: [
      { nombre: 'Fecha', tipo: 'FECHA', rol: 'fecha' },
      { nombre: 'Producto', tipo: 'TEXTO', rol: 'producto' },
      { nombre: 'Cantidad', tipo: 'NUMERO', rol: 'cantidad', valorInicial: '1' },
      { nombre: 'Monto', tipo: 'NUMERO', rol: 'monto', obligatorio: true },
    ],
    senales: ['vendo', 'vendi', 'venta', 'facturo'],
    base: ['comercio', 'otro'],
  },
  {
    id: 'gastos',
    nombre: 'Gastos',
    alias: ['Compras', 'Egresos', 'Gasto'],
    nombrePorRubro: {},
    descripcion: 'Lo que gastás en tu negocio: mercadería, alquiler, servicios.',
    campos: [
      { nombre: 'Fecha', tipo: 'FECHA', rol: 'fecha' },
      { nombre: 'Concepto', tipo: 'TEXTO', rol: 'concepto', obligatorio: true },
      { nombre: 'Monto', tipo: 'NUMERO', rol: 'monto', obligatorio: true },
    ],
    senales: ['gasto', 'compro', 'compras', 'pago', 'proveedor', 'alquiler', 'sueldo'],
    base: ['comercio', 'gastronomia', 'otro'],
  },
  {
    id: 'fiados',
    nombre: 'Fiados',
    alias: ['Cuentas corrientes', 'Deudas', 'Fiado'],
    nombrePorRubro: {},
    descripcion: 'Lo que te deben tus clientes y quién ya pagó.',
    campos: [
      { nombre: 'Fecha', tipo: 'FECHA', rol: 'fecha' },
      { nombre: 'Cliente', tipo: 'TEXTO', rol: 'cliente', obligatorio: true },
      { nombre: 'Monto', tipo: 'NUMERO', rol: 'monto', obligatorio: true },
      { nombre: 'Pagado', tipo: 'SI_NO', rol: 'pagado', valorInicial: 'false' },
    ],
    senales: ['fiado', 'fio', 'fiar', 'debe', 'me deben', 'cuenta corriente', 'anoto lo que'],
    base: [],
  },
  {
    id: 'pedidos',
    nombre: 'Pedidos',
    alias: ['Encargos', 'Pedido'],
    nombrePorRubro: {},
    descripcion: 'Pedidos por encargo, con seña y fecha de entrega.',
    campos: [
      { nombre: 'Entrega', tipo: 'FECHA', rol: 'fecha' },
      { nombre: 'Cliente', tipo: 'TEXTO', rol: 'cliente', obligatorio: true },
      { nombre: 'Producto', tipo: 'TEXTO', rol: 'producto' },
      { nombre: 'Cantidad', tipo: 'NUMERO', rol: 'cantidad', valorInicial: '1' },
      { nombre: 'Monto', tipo: 'NUMERO', rol: 'monto' },
      { nombre: 'Seña', tipo: 'NUMERO', rol: 'sena', valorInicial: '0' },
      { nombre: 'Entregado', tipo: 'SI_NO', rol: 'entregado', valorInicial: 'false' },
    ],
    senales: ['encargo', 'encargue', 'pedido', 'sena', 'senia', 'a pedido', 'reserva', 'delivery'],
    base: ['gastronomia'],
  },
  {
    id: 'insumos',
    nombre: 'Insumos',
    alias: ['Materias primas', 'Ingredientes', 'Insumo'],
    nombrePorRubro: {},
    descripcion:
      'Lo que usás para producir, con su precio, para calcular cuánto te cuesta cada plato.',
    campos: [
      { nombre: 'Nombre', tipo: 'TEXTO', rol: 'nombre', obligatorio: true },
      { nombre: 'Precio', tipo: 'NUMERO', rol: 'precio' },
      { nombre: 'Unidad', tipo: 'OPCION', rol: 'unidad', opciones: ['kg', 'litro', 'unidad'] },
    ],
    senales: ['receta', 'ingrediente', 'insumo', 'materia prima', 'cocino', 'elaboro', 'preparo'],
    base: [],
  },
];

const PORID = new Map<PiezaId, Pieza>(PIEZAS.map((pieza) => [pieza.id, pieza]));

export function pieza(id: PiezaId): Pieza {
  const encontrada = PORID.get(id);
  if (!encontrada) throw new Error(`Pieza desconocida: ${id}`);
  return encontrada;
}

/** Nombre del bloque de la pieza según el rubro («Platos» en gastronomía). */
export function nombreDePieza(id: PiezaId, rubro: RubroId = 'comercio'): string {
  const base = pieza(id);
  return base.nombrePorRubro[rubro] ?? base.nombre;
}

export const NOMBRE_RUBRO: Record<RubroId, string> = {
  comercio: 'Comercio de barrio',
  gastronomia: 'Gastronomía y viandas',
  otro: 'Otro negocio',
};

export const RUBROS: RubroId[] = ['comercio', 'gastronomia', 'otro'];

/** Pieza a la que corresponde una tabla ya creada, por nombre (sin tildes ni mayúsculas). */
export function piezaDeNombre(nombre: string): Pieza | undefined {
  const buscado = sinTildes(nombre);
  return PIEZAS.find((candidata) =>
    [candidata.nombre, ...candidata.alias, ...Object.values(candidata.nombrePorRubro)].some(
      (posible) => sinTildes(posible) === buscado,
    ),
  );
}

export function sinTildes(texto: string): string {
  return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
}
