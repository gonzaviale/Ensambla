import type { MensajeDePedido } from './pedidos';

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
  /** Por qué duda Sami. */
  motivo?: string;
  /** Entró por un pedido de WhatsApp confirmado: ya está en el cuaderno y no sale de la foto. */
  pedidoId?: string;
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
    motivo:
      'La hoja no dice el importe. Lo puse a costo ($ 1.900); a precio de venta serían $ 2.600.',
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

// --- Pedidos, ofertas y monotributo ---

/** Mensajes de clientes que llegaron hoy por WhatsApp, con lo que entendió Sami de cada uno. */
export const PEDIDOS_DE_HOY: MensajeDePedido[] = [
  {
    id: 'p1',
    cliente: 'Doña Rosa',
    hora: '10:12',
    texto: 'Hola! me separás 2 leches, un pan lactal y la yerba de siempre? paso a las 18',
    lineas: [
      { texto: '2 leches', cantidad: 2, busca: 'leche entera' },
      { texto: 'un pan lactal', cantidad: 1, busca: 'pan lactal' },
      { texto: 'la yerba de siempre', cantidad: 1, busca: 'yerba', deSiempre: true },
    ],
    entrega: 'retira',
    pago: 'cobrado',
  },
  {
    id: 'p2',
    cliente: 'Bar La Esquina',
    hora: '11:40',
    texto: 'Buenas! Necesito 6 coca de 2 y cuarto y 8 fernet para hoy, me lo traen?',
    lineas: [
      { texto: '6 coca de 2 y cuarto', cantidad: 6, busca: 'coca' },
      { texto: '8 fernet', cantidad: 8, busca: 'fernet' },
    ],
    entrega: 'envio',
    pago: 'cobrado',
  },
  {
    id: 'p3',
    cliente: 'Familia Gómez',
    hora: '12:05',
    texto: 'hola, 1 aceite natura, 2 fideos y una salsa de tomate. lo anotás en la cuenta?',
    lineas: [
      { texto: '1 aceite natura', cantidad: 1, busca: 'aceite' },
      { texto: '2 fideos', cantidad: 2, busca: 'fideos' },
      { texto: 'una salsa de tomate', cantidad: 1, busca: 'salsa de tomate' },
    ],
    entrega: 'retira',
    pago: 'fiado',
  },
  {
    id: 'p4',
    cliente: 'Carlos del 3° B',
    hora: '12:31',
    texto: 'tenés huevos? un maple. y un dulce de leche. me lo acercás?',
    lineas: [
      { texto: 'un maple de huevos', cantidad: 1, busca: 'huevos' },
      { texto: 'un dulce de leche', cantidad: 1, busca: 'dulce de leche' },
    ],
    entrega: 'envio',
    pago: 'cobrado',
  },
];

/** Clientes que pidieron no recibir ofertas. El resto aceptó cuando Sami les preguntó la primera vez. */
export const SIN_PERMISO_PARA_OFERTAS = ['Escuela N° 14', 'Sociedad de Fomento', 'Club Social'];

/** Ofertas por cliente y por semana, como máximo. */
export const TOPE_DE_OFERTAS_POR_SEMANA = 1;

export type CategoriaMonotributo = { letra: string; tope: number; cuota: number };

/**
 * Categorías de ejemplo: NO son las vigentes. Las reales las publica ARCA y se actualizan dos veces por año; en el
 * producto se leerían de una tabla que se mantiene aparte.
 */
export const CATEGORIAS_DE_EJEMPLO: CategoriaMonotributo[] = [
  { letra: 'A', tope: 6500000, cuota: 18000 },
  { letra: 'B', tope: 9800000, cuota: 22000 },
  { letra: 'C', tope: 13500000, cuota: 27000 },
  { letra: 'D', tope: 17000000, cuota: 35000 },
  { letra: 'E', tope: 20000000, cuota: 43000 },
];

export const CATEGORIA_ACTUAL = 'B';

/** Lo facturado en los 9 meses anteriores al cuaderno (del más viejo al más nuevo), cargado de las facturas viejas. */
export const FACTURADO_ANTES_DEL_CUADERNO = [
  640000, 655000, 670000, 690000, 700000, 715000, 730000, 750000, 770000,
];

/** Día del mes en que vence la cuota. */
export const VENCIMIENTO_CUOTA = 20;
