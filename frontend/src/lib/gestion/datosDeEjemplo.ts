// Datos del mockup de «Mi negocio» que el backend todavía no guarda: gastos fijos, cómo cobra el comercio,
// proveedores, la hoja del día que «lee» Sami y el remito que llega. Todo inventado para el almacén de la demo;
// las ventas, el stock y los costos salen del cuaderno de verdad.

export type GastoFijo = { concepto: string; mensual: number };

/** Lo que el comercio paga todos los meses venda o no venda. */
export const GASTOS_FIJOS: GastoFijo[] = [
  { concepto: 'Alquiler del local', mensual: 80000 },
  { concepto: 'Luz', mensual: 26000 },
  { concepto: 'Monotributo', mensual: 22000 },
  { concepto: 'Internet y celular', mensual: 12000 },
];

export type MedioDeCobro = { medio: string; parte: number; comision: number };

/** Cómo le pagan los clientes: qué parte de las ventas entra por cada medio y cuánto se queda el que cobra. */
export const MEDIOS_DE_COBRO: MedioDeCobro[] = [
  { medio: 'Efectivo', parte: 0.5, comision: 0 },
  { medio: 'QR y transferencia', parte: 0.3, comision: 0.008 },
  { medio: 'Tarjeta (posnet)', parte: 0.2, comision: 0.039 },
];

/** Lo que se pierde de lo que entra: vencido, roto o consumo propio, como parte del costo de lo vendido. */
export const MERMA_SOBRE_COSTO = 0.015;

export type Proveedor = {
  id: string;
  nombre: string;
  telefono: string;
  /** Cada cuántos días pasa o entrega. */
  entregaCadaDias: number;
  /** Palabras del nombre del producto que le corresponden. */
  palabras: string[];
};

export const PROVEEDORES: Proveedor[] = [
  {
    id: 'bebidas',
    nombre: 'Bebidas del Sur',
    telefono: '11 4444-1010',
    entregaCadaDias: 7,
    palabras: ['coca', 'sprite', 'cerveza', 'vino', 'fernet'],
  },
  {
    id: 'lacteos',
    nombre: 'Lácteos y Panificados Don Pepe',
    telefono: '11 4444-2020',
    entregaCadaDias: 3,
    palabras: ['pan', 'manteca', 'dulce de leche', 'huevos'],
  },
  {
    id: 'limpieza',
    nombre: 'Limpieza Brillo',
    telefono: '11 4444-3030',
    entregaCadaDias: 14,
    palabras: ['lavandina', 'detergente', 'jabon', 'papel'],
  },
  {
    id: 'almacen',
    nombre: 'Distribuidora Norte',
    telefono: '11 4444-4040',
    entregaCadaDias: 7,
    palabras: [],
  },
];

/** Plata que el comercio tiene para comprar esta semana sin quedarse sin cambio. */
export const CAJA_PARA_COMPRAS = 150000;

export type TipoDeRenglon = 'venta' | 'fiado' | 'cobro' | 'pago' | 'gasto' | 'merma';

export type Renglon = {
  id: string;
  /** Lo que dice la hoja, tal cual. */
  escrito: string;
  tipo: TipoDeRenglon;
  importe: number;
  detalle: string;
  /** Para ventas y cobros: si entró al cajón o a la cuenta. */
  medio?: 'efectivo' | 'cuenta';
  cliente?: string;
  /** Sami no está segura y lo marca para revisar. */
  dudoso?: boolean;
};

/** La hoja de hoy del cuaderno, como la lee Sami. */
export const HOJA_DEL_DIA: Renglon[] = [
  {
    id: 'r1',
    escrito: 'Mostrador efvo 24.600',
    tipo: 'venta',
    importe: 24600,
    detalle: 'Ventas de mostrador',
    medio: 'efectivo',
  },
  {
    id: 'r2',
    escrito: 'QR / transf 8.500',
    tipo: 'venta',
    importe: 8500,
    detalle: 'Ventas cobradas por QR o transferencia',
    medio: 'cuenta',
  },
  {
    id: 'r3',
    escrito: 'Marta pan + huevos 4300 (debe)',
    tipo: 'fiado',
    importe: 4300,
    detalle: 'Fiado a Doña Marta',
    cliente: 'Doña Marta',
  },
  {
    id: 'r4',
    escrito: 'Bar Esquina pagó 6000',
    tipo: 'cobro',
    importe: 6000,
    detalle: 'Cobro de fiado de Bar La Esquina',
    medio: 'efectivo',
    cliente: 'Bar La Esquina',
  },
  {
    id: 'r5',
    escrito: 'Dist. Norte 15.000',
    tipo: 'pago',
    importe: 15000,
    detalle: 'Pago a Distribuidora Norte (mercadería)',
    medio: 'efectivo',
  },
  {
    id: 'r6',
    escrito: 'bolsas 1200',
    tipo: 'gasto',
    importe: 1200,
    detalle: 'Bolsas',
    medio: 'efectivo',
  },
  {
    id: 'r7',
    escrito: 'se rompió 1 aceite',
    tipo: 'merma',
    importe: 1900,
    detalle: 'Aceite Natura roto (a costo)',
    dudoso: true,
  },
];

/** El cambio con que se abrió la caja. */
export const CAMBIO_INICIAL = 10000;

/** Cobros de fiado ya registrados antes de hoy, por cliente. */
export const COBROS_DE_FIADO: Record<string, number> = {
  'Bar La Esquina': 9000,
  'Club Social': 4000,
  'Familia Gómez': 2500,
};

/** Lo que trae el remito de Distribuidora Norte: el costo nuevo de cada producto, por nombre. */
export const COSTOS_DEL_REMITO: Record<string, number> = {
  'Yerba Playadito 1 kg': 3150,
  'Harina 000 Pureza 1 kg': 920,
  'Aceite Natura 900 ml': 2050,
};
