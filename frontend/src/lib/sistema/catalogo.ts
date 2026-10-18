import type {
  Campo,
  ClavePlantilla,
  Color,
  DefinicionDeBloque,
  DefinicionDePlantilla,
  TipoBloque,
  TipoCampo,
} from './tipos';

const campo = (
  key: string,
  label: string,
  type: TipoCampo = 'text',
  extra: Partial<Campo> = {},
): Campo => ({ key, label, type, required: false, ...extra });

// Los bloques que se pueden sumar a un sistema. Las piezas son las del mockup; los ejemplos y los
// textos son de comercio (almacenes, kioscos y minimercados) y se agrega el bloque Ventas.
export const CATALOG: Record<TipoBloque, DefinicionDeBloque> = {
  inventory: {
    name: 'Inventario',
    singular: 'producto',
    icon: 'box',
    color: 'orange',
    description: 'Productos, cantidades, costos y precios.',
    purpose: 'Sabé qué tenés, a cuánto lo vendés y qué necesitás reponer.',
    fields: [
      campo('name', 'Producto', 'text', { required: true }),
      campo('quantity', 'Cantidad', 'number', { required: true, min: 0 }),
      campo('price', 'Precio', 'currency'),
      campo('cost', 'Costo', 'currency'),
      campo('minimum', 'Stock mínimo', 'number'),
    ],
    examples: [
      { name: 'Yerba Playadito 1 kg', quantity: 24, price: 3600, cost: 2700, minimum: 10 },
      { name: 'Coca-Cola 2,25 L', quantity: 18, price: 2700, cost: 1950, minimum: 12 },
      { name: 'Fideos Matarazzo 500 g', quantity: 30, price: 1100, cost: 800, minimum: 10 },
    ],
  },
  orders: {
    name: 'Pedidos',
    singular: 'pedido',
    icon: 'orders',
    color: 'coral',
    description: 'Cada pedido y su estado.',
    purpose: 'Seguí tus pedidos desde que llegan hasta que los entregás.',
    fields: [
      campo('name', 'Pedido', 'text', { required: true }),
      campo('customer', 'Cliente'),
      campo('date', 'Fecha', 'date'),
      campo('total', 'Total', 'currency'),
      campo('status', 'Estado', 'select', {
        options: ['Pendiente', 'En preparación', 'Entregado'],
      }),
    ],
    examples: [
      {
        name: 'Pedido #001',
        customer: 'Lucía Pérez',
        date: '2026-10-15',
        total: 17000,
        status: 'En preparación',
      },
      {
        name: 'Pedido #002',
        customer: 'Martín García',
        date: '2026-10-16',
        total: 25500,
        status: 'Pendiente',
      },
      {
        name: 'Pedido #003',
        customer: 'Ana López',
        date: '2026-10-16',
        total: 6200,
        status: 'Entregado',
      },
    ],
  },
  clients: {
    name: 'Clientes',
    singular: 'cliente',
    icon: 'users',
    color: 'blue',
    description: 'Contactos y fiado, siempre a mano.',
    purpose: 'Reuní los datos de quienes te compran y lo que te deben.',
    fields: [
      campo('name', 'Nombre', 'text', { required: true }),
      campo('phone', 'Teléfono', 'tel'),
      campo('email', 'Correo', 'email'),
      campo('notes', 'Notas', 'textarea'),
    ],
    examples: [
      {
        name: 'Lucía Pérez',
        phone: '11 5555-0101',
        email: 'lucia@example.com',
        notes: 'Compra seguido los sábados.',
      },
      {
        name: 'Martín García',
        phone: '11 5555-0102',
        email: 'martin@example.com',
        notes: 'Lleva fiado hasta fin de mes.',
      },
      {
        name: 'Ana López',
        phone: '11 5555-0103',
        email: 'ana@example.com',
        notes: 'Pide que le guarden el pan.',
      },
    ],
  },
  sales: {
    name: 'Ventas',
    singular: 'venta',
    icon: 'check-circle',
    color: 'yellow',
    description: 'Lo que vendés, producto por producto.',
    purpose: 'Cada venta descuenta stock y muestra qué se mueve y qué no.',
    fields: [
      campo('product', 'Producto', 'relation', { required: true }),
      campo('quantity', 'Cantidad', 'number', { required: true, min: 1 }),
      campo('price', 'Precio', 'currency'),
      campo('date', 'Fecha', 'date'),
      campo('customer', 'Cliente'),
      campo('credit', 'Fiado', 'checkbox'),
    ],
    examples: [],
  },
  expenses: {
    name: 'Gastos',
    singular: 'gasto',
    icon: 'wallet',
    color: 'yellow',
    description: 'Lo que comprás e invertís.',
    purpose: 'Registrá compras y gastos para entender tus costos.',
    fields: [
      campo('name', 'Concepto', 'text', { required: true }),
      campo('date', 'Fecha', 'date'),
      campo('total', 'Importe', 'currency', { required: true }),
      campo('category', 'Categoría', 'select', {
        options: ['Mercadería', 'Servicios', 'Alquiler', 'Otros'],
      }),
    ],
    examples: [
      { name: 'Compra a proveedor', date: '2026-10-15', total: 32000, category: 'Mercadería' },
      { name: 'Bolsas y embalajes', date: '2026-10-16', total: 7800, category: 'Otros' },
    ],
  },
  summary: {
    name: 'Resumen',
    singular: 'indicador',
    icon: 'chart',
    color: 'petrol',
    description: 'Tus números, de un vistazo.',
    purpose: 'Mirá totales que se actualizan con tus registros.',
    fields: [],
    examples: [],
  },
  form: {
    name: 'Formulario',
    singular: 'registro',
    icon: 'form',
    color: 'blue',
    description: 'Una carga simple, a tu medida.',
    purpose: 'Pedí exactamente los datos que necesitás.',
    fields: [
      campo('name', 'Nombre', 'text', { required: true }),
      campo('detail', 'Detalle', 'textarea'),
      campo('date', 'Fecha', 'date'),
    ],
    examples: [],
  },
  note: {
    name: 'Nota',
    singular: 'nota',
    icon: 'note',
    color: 'yellow',
    description: 'Ideas, instrucciones o pendientes.',
    purpose: 'Dejá un mensaje útil junto a tus herramientas.',
    fields: [],
    examples: [],
  },
  entries: {
    name: 'Entradas',
    singular: 'entrada',
    icon: 'download',
    color: 'blue',
    description: 'Compras, ingresos y reposición.',
    purpose: 'Cada entrada suma unidades al stock del producto.',
    fields: [
      campo('product', 'Producto', 'relation', { required: true }),
      campo('quantity', 'Cantidad', 'number', { required: true, min: 1 }),
      campo('date', 'Fecha', 'date'),
      campo('notes', 'Referencia'),
    ],
    examples: [],
  },
  exits: {
    name: 'Salidas',
    singular: 'salida',
    icon: 'upload',
    color: 'coral',
    description: 'Consumos, mermas y retiros.',
    purpose: 'Cada salida descuenta unidades del stock disponible.',
    fields: [
      campo('product', 'Producto', 'relation', { required: true }),
      campo('quantity', 'Cantidad', 'number', { required: true, min: 1 }),
      campo('date', 'Fecha', 'date'),
      campo('notes', 'Referencia'),
    ],
    examples: [],
  },
};

// Una plantilla es una composición de bloques, relaciones y comportamientos.
export const TEMPLATES: Record<ClavePlantilla, DefinicionDePlantilla> = {
  shop: {
    name: 'Almacén o kiosco',
    subtitle: 'Vendé, descontá stock y mirá qué se mueve.',
    description:
      'Para almacenes, kioscos y minimercados: productos con costo y precio, cada venta y tus clientes con su fiado.',
    icon: 'check-circle',
    color: 'yellow',
    types: ['inventory', 'sales', 'clients'],
    tags: ['Productos', 'Ventas', 'Clientes'],
    defaultName: 'Mi almacén',
  },
  warehouse: {
    name: 'Depósito',
    subtitle: 'Sabé qué entra, qué sale y qué queda.',
    description:
      'Productos, cantidades y movimientos en un mismo lugar. Sumá o descontá stock al registrar cada movimiento.',
    icon: 'box',
    color: 'orange',
    types: ['inventory', 'entries', 'exits'],
    tags: ['Productos', 'Entradas', 'Salidas'],
    defaultName: 'Mi depósito',
  },
  business: {
    name: 'Pedidos y clientes',
    subtitle: 'Las piezas de tu comercio, juntas.',
    description:
      'Un punto de partida para vender por encargo: productos, precios, pedidos y clientes.',
    icon: 'grid',
    color: 'coral',
    types: ['orders', 'clients', 'inventory'],
    tags: ['Pedidos', 'Clientes', 'Productos y stock'],
    defaultName: 'Mi negocio',
  },
};

export const COLORS: Record<Color, string> = {
  orange: '#EE8000',
  coral: '#F85F5A',
  yellow: '#EBCE32',
  blue: '#4EB2D4',
  petrol: '#00506A',
};

// Fondo suave de cada color de bloque.
export const TINTS: Record<Color, string> = {
  orange: '#fff0da',
  coral: '#ffe8e4',
  yellow: '#fbf5d5',
  blue: '#e8f5fa',
  petrol: '#e5f1f0',
};

export const NOMBRES_DE_COLOR: Record<Color, string> = {
  orange: 'Naranja',
  coral: 'Coral',
  yellow: 'Amarillo',
  blue: 'Celeste',
  petrol: 'Petróleo',
};

export const FIELD_TYPES: Record<TipoCampo, string> = {
  text: 'Texto',
  textarea: 'Texto largo',
  number: 'Número',
  currency: 'Dinero',
  date: 'Fecha',
  select: 'Opciones',
  email: 'Correo',
  tel: 'Teléfono',
  checkbox: 'Sí / No',
  relation: 'Vínculo con otro bloque',
  time: 'Horario',
};
