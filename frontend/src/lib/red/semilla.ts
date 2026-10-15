import type {
  AlternativaProveedor,
  CambioAplicado,
  CostoHistorial,
  ItemLista,
  ListaProveedor,
  OfertaProveedor,
  ProductoRed,
  ProveedorRed,
  RedState,
  ReferenciaZona,
  Umbrales,
} from '@/types/red';
import { sumarDias } from './calculos';

// Datos de demostración: un almacén de barrio, tres proveedores y un barrio con otros 30 comercios.
// Los productos, nombres y números son ilustrativos (docs/diferencial-red-proactiva.md).

const CATEGORIAS = {
  almacen: 'Almacén',
  bebidas: 'Bebidas',
  lacteos: 'Lácteos',
  limpieza: 'Limpieza',
  kiosco: 'Kiosco',
} as const;

export const PROVEEDORES_SEMILLA: ProveedorRed[] = [
  {
    id: 'prov-sur',
    nombre: 'Distribuidora Sur',
    rubro: 'Almacén, bebidas y kiosco',
    zona: 'San Justo',
    telefono: '11 4651-2200',
    origenHabitual: 'whatsapp',
    frecuenciaDias: 15,
  },
  {
    id: 'prov-norte',
    nombre: 'Mayorista Norte',
    rubro: 'Almacén y limpieza',
    zona: 'Ramos Mejía',
    telefono: '11 4658-7311',
    origenHabitual: 'excel',
    frecuenciaDias: 15,
  },
  {
    id: 'prov-valle',
    nombre: 'Lácteos del Valle',
    rubro: 'Lácteos y fiambres',
    zona: 'La Matanza',
    telefono: '11 4480-9054',
    origenHabitual: 'pdf',
    frecuenciaDias: 10,
  },
];

type ProductoSemilla = {
  id: string;
  nombre: string;
  categoria: string;
  proveedorId: string;
  costo: number;
  precio: number;
  stock: number;
  ventasSemana: number;
  unidadesPorBulto: number | null;
  /** Hace cuántos días rige el costo actual y de qué lista salió. */
  costoHaceDias: number;
  costoListaId: string | null;
  alternativas?: { proveedorId: string; costo: number }[];
};

const PRODUCTOS: ProductoSemilla[] = [
  {
    id: 'p-aceite',
    nombre: 'Aceite Natura 900 ml',
    categoria: CATEGORIAS.almacen,
    proveedorId: 'prov-sur',
    costo: 2150,
    precio: 2350,
    stock: 14,
    ventasSemana: 20,
    unidadesPorBulto: 12,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
    alternativas: [{ proveedorId: 'prov-norte', costo: 2090 }],
  },
  {
    id: 'p-azucar',
    nombre: 'Azúcar Ledesma 1 kg',
    categoria: CATEGORIAS.almacen,
    proveedorId: 'prov-sur',
    costo: 1020,
    precio: 1380,
    stock: 6,
    ventasSemana: 28,
    unidadesPorBulto: 10,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
    alternativas: [{ proveedorId: 'prov-norte', costo: 900 }],
  },
  {
    id: 'p-yerba',
    nombre: 'Yerba Playadito 1 kg',
    categoria: CATEGORIAS.almacen,
    proveedorId: 'prov-sur',
    costo: 3600,
    precio: 4850,
    stock: 7,
    ventasSemana: 18,
    unidadesPorBulto: 8,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
    alternativas: [{ proveedorId: 'prov-norte', costo: 3520 }],
  },
  {
    id: 'p-harina',
    nombre: 'Harina 000 Pureza 1 kg',
    categoria: CATEGORIAS.almacen,
    proveedorId: 'prov-norte',
    costo: 780,
    precio: 1050,
    stock: 30,
    ventasSemana: 34,
    unidadesPorBulto: 10,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
    alternativas: [{ proveedorId: 'prov-sur', costo: 760 }],
  },
  {
    id: 'p-fideos',
    nombre: 'Fideos Matarazzo 500 g',
    categoria: CATEGORIAS.almacen,
    proveedorId: 'prov-norte',
    costo: 940,
    precio: 1250,
    stock: 40,
    ventasSemana: 25,
    unidadesPorBulto: 12,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
  },
  {
    id: 'p-arroz',
    nombre: 'Arroz Gallo Oro 1 kg',
    categoria: CATEGORIAS.almacen,
    proveedorId: 'prov-norte',
    costo: 1300,
    precio: 1740,
    stock: 22,
    ventasSemana: 15,
    unidadesPorBulto: 10,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
  },
  {
    id: 'p-coca',
    nombre: 'Coca-Cola 2,25 L',
    categoria: CATEGORIAS.bebidas,
    proveedorId: 'prov-sur',
    costo: 2650,
    precio: 3400,
    stock: 36,
    ventasSemana: 42,
    unidadesPorBulto: 8,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
  },
  {
    id: 'p-agua',
    nombre: 'Agua Villa del Sur 2 L',
    categoria: CATEGORIAS.bebidas,
    proveedorId: 'prov-sur',
    costo: 780,
    precio: 1000,
    stock: 48,
    ventasSemana: 30,
    unidadesPorBulto: 6,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
  },
  {
    id: 'p-sprite',
    nombre: 'Sprite 1,5 L',
    categoria: CATEGORIAS.bebidas,
    proveedorId: 'prov-sur',
    costo: 1500,
    precio: 1930,
    stock: 24,
    ventasSemana: 16,
    unidadesPorBulto: 6,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
  },
  {
    id: 'p-cerveza',
    nombre: 'Cerveza Quilmes 1 L',
    categoria: CATEGORIAS.bebidas,
    proveedorId: 'prov-norte',
    costo: 1500,
    precio: 1950,
    stock: 30,
    ventasSemana: 22,
    unidadesPorBulto: 12,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
  },
  {
    id: 'p-leche',
    nombre: 'Leche La Serenísima 1 L',
    categoria: CATEGORIAS.lacteos,
    proveedorId: 'prov-valle',
    costo: 1180,
    precio: 1450,
    stock: 48,
    ventasSemana: 60,
    unidadesPorBulto: 12,
    costoHaceDias: 9,
    costoListaId: 'lista-valle-oct',
  },
  {
    id: 'p-yogur',
    nombre: 'Yogur Ser 190 g',
    categoria: CATEGORIAS.lacteos,
    proveedorId: 'prov-valle',
    costo: 640,
    precio: 700,
    stock: 26,
    ventasSemana: 24,
    unidadesPorBulto: 12,
    costoHaceDias: 9,
    costoListaId: 'lista-valle-oct',
  },
  {
    id: 'p-lavandina',
    nombre: 'Lavandina Ayudín 1 L',
    categoria: CATEGORIAS.limpieza,
    proveedorId: 'prov-norte',
    costo: 690,
    precio: 970,
    stock: 18,
    ventasSemana: 12,
    unidadesPorBulto: 12,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
  },
  {
    id: 'p-detergente',
    nombre: 'Detergente Magistral 750 ml',
    categoria: CATEGORIAS.limpieza,
    proveedorId: 'prov-norte',
    costo: 1450,
    precio: 2050,
    stock: 20,
    ventasSemana: 10,
    unidadesPorBulto: 12,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
  },
  {
    id: 'p-papel',
    nombre: 'Papel higiénico Elite x4',
    categoria: CATEGORIAS.limpieza,
    proveedorId: 'prov-norte',
    costo: 2600,
    precio: 3650,
    stock: 16,
    ventasSemana: 11,
    unidadesPorBulto: 12,
    costoHaceDias: 17,
    costoListaId: 'lista-norte-sep',
  },
  {
    id: 'p-galletitas',
    nombre: 'Galletitas Oreo 118 g',
    categoria: CATEGORIAS.kiosco,
    proveedorId: 'prov-sur',
    costo: 780,
    precio: 1150,
    stock: 40,
    ventasSemana: 26,
    unidadesPorBulto: 20,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
  },
  {
    id: 'p-alfajor',
    nombre: 'Alfajor Jorgito Negro',
    categoria: CATEGORIAS.kiosco,
    proveedorId: 'prov-sur',
    costo: 520,
    precio: 800,
    stock: 60,
    ventasSemana: 45,
    unidadesPorBulto: 24,
    costoHaceDias: 21,
    costoListaId: 'lista-sur-sep',
  },
];

function construirProductos(ahora: Date): ProductoRed[] {
  return PRODUCTOS.map((semilla) => {
    const desde = sumarDias(ahora, -semilla.costoHaceDias);
    const historial: CostoHistorial[] = [
      {
        fecha: sumarDias(ahora, -(semilla.costoHaceDias + 55)),
        costo: Math.round(semilla.costo * 0.9),
        listaId: null,
        proveedorId: semilla.proveedorId,
      },
      {
        fecha: sumarDias(ahora, -(semilla.costoHaceDias + 28)),
        costo: Math.round(semilla.costo * 0.95),
        listaId: null,
        proveedorId: semilla.proveedorId,
      },
      {
        fecha: desde,
        costo: semilla.costo,
        listaId: semilla.costoListaId,
        proveedorId: semilla.proveedorId,
      },
    ];
    const alternativas: AlternativaProveedor[] = (semilla.alternativas ?? []).map((alt) => ({
      ...alt,
      fecha: sumarDias(ahora, -3),
    }));
    return {
      id: semilla.id,
      nombre: semilla.nombre,
      categoria: semilla.categoria,
      proveedorId: semilla.proveedorId,
      costo: semilla.costo,
      precio: semilla.precio,
      stock: semilla.stock,
      ventasSemana: semilla.ventasSemana,
      unidadesPorBulto: semilla.unidadesPorBulto,
      costoDesde: desde,
      costoListaId: semilla.costoListaId,
      historial,
      alternativas,
    };
  });
}

/* ---------- listas ---------- */

type ItemSemilla = {
  texto: string;
  precioLista: number;
  productoId: string | null;
  porBulto: number | null;
  confianza: number;
  duda?: {
    motivo: ItemLista['motivo'];
    sugeridos?: string[];
    presentacion?: ItemLista['presentacion'];
  };
};

function item(listaId: string, indice: number, semilla: ItemSemilla): ItemLista {
  const dudoso = !!semilla.duda;
  const sinCoincidencia = semilla.productoId === null;
  return {
    id: `${listaId}-${indice + 1}`,
    textoOriginal: semilla.texto,
    precioLista: semilla.precioLista,
    presentacion: semilla.duda?.presentacion ?? (semilla.porBulto ? 'BULTO' : 'UNIDAD'),
    unidadesPorBulto: semilla.porBulto,
    productoId: semilla.productoId,
    confianza: semilla.confianza,
    estado: sinCoincidencia ? 'sin-coincidencia' : dudoso ? 'dudoso' : 'seguro',
    motivo: semilla.duda?.motivo ?? null,
    sugeridos:
      semilla.duda?.sugeridos ?? (semilla.productoId && dudoso ? [semilla.productoId] : []),
    confirmado: false,
    ignorado: false,
  };
}

const ITEMS_SUR: ItemSemilla[] = [
  {
    texto: 'ACEITE NATURA GIRASOL 900 ML X12',
    precioLista: 26520,
    productoId: 'p-aceite',
    porBulto: 12,
    confianza: 0.98,
  },
  {
    texto: 'AZUCAR LEDESMA CLASICA 1 KG X10',
    precioLista: 10700,
    productoId: 'p-azucar',
    porBulto: 10,
    confianza: 0.97,
  },
  {
    texto: 'YERBA PLAYADITO SUAVE 1 KG X8',
    precioLista: 30400,
    productoId: 'p-yerba',
    porBulto: 8,
    confianza: 0.96,
  },
  {
    texto: 'COCA COLA 2,25 L X8',
    precioLista: 21760,
    productoId: 'p-coca',
    porBulto: 8,
    confianza: 0.99,
  },
  {
    texto: 'AGUA MINERAL V. DEL SUR 2 L X6',
    precioLista: 4920,
    productoId: 'p-agua',
    porBulto: 6,
    confianza: 0.72,
    duda: { motivo: 'nombre-parecido', sugeridos: ['p-agua'] },
  },
  {
    texto: 'SPRITE 1,5 L',
    precioLista: 9480,
    productoId: 'p-sprite',
    porBulto: 6,
    confianza: 0.55,
    duda: { motivo: 'unidad-bulto', sugeridos: ['p-sprite'], presentacion: 'UNIDAD' },
  },
  {
    texto: 'GALLETITAS OREO 118 G X20',
    precioLista: 17000,
    productoId: 'p-galletitas',
    porBulto: 20,
    confianza: 0.95,
  },
  {
    texto: 'ALFAJOR JORGITO NEGRO X24',
    precioLista: 13800,
    productoId: 'p-alfajor',
    porBulto: 24,
    confianza: 0.94,
  },
  {
    texto: 'TURRON ARCOR MANI X50',
    precioLista: 14000,
    productoId: null,
    porBulto: 50,
    confianza: 0,
  },
];

const ITEMS_NORTE: ItemSemilla[] = [
  {
    texto: 'HARINA 000 PUREZA X10 1 KG',
    precioLista: 8000,
    productoId: 'p-harina',
    porBulto: 10,
    confianza: 0.98,
  },
  {
    texto: 'FIDEOS MATARAZZO TIRABUZON 500 G X12',
    precioLista: 11760,
    productoId: 'p-fideos',
    porBulto: 12,
    confianza: 0.96,
  },
  {
    texto: 'ARROZ GALLO ORO LARGO FINO 1 KG X10',
    precioLista: 13400,
    productoId: 'p-arroz',
    porBulto: 10,
    confianza: 0.97,
  },
  {
    texto: 'CERVEZA QUILMES CLASICA 1 L X12',
    precioLista: 18600,
    productoId: 'p-cerveza',
    porBulto: 12,
    confianza: 0.95,
  },
  {
    texto: 'LAVANDINA AYUDIN 1 L X12',
    precioLista: 11220,
    productoId: 'p-lavandina',
    porBulto: 12,
    confianza: 0.9,
    duda: { motivo: 'precio-raro', sugeridos: ['p-lavandina'] },
  },
  {
    texto: 'DETERGENTE MAGISTRAL LIMON 750 ML X12',
    precioLista: 17640,
    productoId: 'p-detergente',
    porBulto: 12,
    confianza: 0.96,
  },
  {
    texto: 'PAPEL HIGIENICO ELITE X4 BOLSON 12',
    precioLista: 31800,
    productoId: 'p-papel',
    porBulto: 12,
    confianza: 0.93,
  },
];

function construirListas(ahora: Date): ListaProveedor[] {
  const horas = (h: number) => new Date(ahora.getTime() - h * 60 * 60 * 1000).toISOString();
  return [
    {
      id: 'lista-sur-oct',
      proveedorId: 'prov-sur',
      nombre: 'Lista de precios Distribuidora Sur',
      recibidaEl: horas(3),
      origen: 'whatsapp',
      estado: 'por-revisar',
      aplicadaEl: null,
      items: ITEMS_SUR.map((semilla, i) => item('lista-sur-oct', i, semilla)),
      resumen: null,
    },
    {
      id: 'lista-norte-oct',
      proveedorId: 'prov-norte',
      nombre: 'Lista de precios Mayorista Norte',
      recibidaEl: horas(28),
      origen: 'excel',
      estado: 'por-revisar',
      aplicadaEl: null,
      items: ITEMS_NORTE.map((semilla, i) => item('lista-norte-oct', i, semilla)),
      resumen: null,
    },
    {
      id: 'lista-valle-oct',
      proveedorId: 'prov-valle',
      nombre: 'Lista de precios Lácteos del Valle',
      recibidaEl: sumarDias(ahora, -9),
      origen: 'pdf',
      estado: 'aplicada',
      aplicadaEl: sumarDias(ahora, -9),
      items: [],
      resumen: { productos: 2, aumentoPromedio: 3.1 },
    },
    {
      id: 'lista-norte-sep',
      proveedorId: 'prov-norte',
      nombre: 'Lista de precios Mayorista Norte',
      recibidaEl: sumarDias(ahora, -17),
      origen: 'excel',
      estado: 'aplicada',
      aplicadaEl: sumarDias(ahora, -17),
      items: [],
      resumen: { productos: 7, aumentoPromedio: 2.8 },
    },
    {
      id: 'lista-sur-sep',
      proveedorId: 'prov-sur',
      nombre: 'Lista de precios Distribuidora Sur',
      recibidaEl: sumarDias(ahora, -21),
      origen: 'whatsapp',
      estado: 'aplicada',
      aplicadaEl: sumarDias(ahora, -21),
      items: [],
      resumen: { productos: 8, aumentoPromedio: 4.2 },
    },
  ];
}

/* ---------- cambios ya aplicados (historial de valor) ---------- */

function construirCambios(ahora: Date): CambioAplicado[] {
  const cambio = (
    id: string,
    productoId: string,
    nombre: string,
    haceDias: number,
    listaId: string,
    costoAnterior: number,
    costoNuevo: number,
    precioAnterior: number,
    precioNuevo: number,
    trasladado: boolean,
  ): CambioAplicado => ({
    id,
    productoId,
    nombre,
    precioAnterior,
    precioNuevo,
    costoAnterior,
    costoNuevo,
    fecha: sumarDias(ahora, -haceDias),
    listaId,
    trasladado,
  });
  return [
    cambio('c-1', 'p-coca', 'Coca-Cola 2,25 L', 21, 'lista-sur-sep', 2520, 2650, 3250, 3400, true),
    cambio(
      'c-2',
      'p-yerba',
      'Yerba Playadito 1 kg',
      21,
      'lista-sur-sep',
      3380,
      3600,
      4550,
      4850,
      true,
    ),
    cambio(
      'c-3',
      'p-alfajor',
      'Alfajor Jorgito Negro',
      21,
      'lista-sur-sep',
      450,
      520,
      500,
      800,
      true,
    ),
    cambio(
      'c-4',
      'p-azucar',
      'Azúcar Ledesma 1 kg',
      21,
      'lista-sur-sep',
      960,
      1020,
      1290,
      1380,
      true,
    ),
    cambio(
      'c-5',
      'p-harina',
      'Harina 000 Pureza 1 kg',
      17,
      'lista-norte-sep',
      740,
      780,
      990,
      1050,
      true,
    ),
    cambio(
      'c-6',
      'p-arroz',
      'Arroz Gallo Oro 1 kg',
      17,
      'lista-norte-sep',
      1250,
      1300,
      1660,
      1740,
      true,
    ),
    cambio(
      'c-7',
      'p-lavandina',
      'Lavandina Ayudín 1 L',
      17,
      'lista-norte-sep',
      660,
      690,
      920,
      970,
      true,
    ),
    cambio(
      'c-8',
      'p-leche',
      'Leche La Serenísima 1 L',
      9,
      'lista-valle-oct',
      1120,
      1180,
      1380,
      1450,
      true,
    ),
    cambio('c-9', 'p-yogur', 'Yogur Ser 190 g', 9, 'lista-valle-oct', 600, 640, 660, 700, false),
    cambio(
      'c-10',
      'p-papel',
      'Papel higiénico Elite x4',
      17,
      'lista-norte-sep',
      2480,
      2600,
      3480,
      3650,
      false,
    ),
  ];
}

/* ---------- ofertas y zona ---------- */

function construirOfertas(ahora: Date): OfertaProveedor[] {
  return [
    {
      id: 'oferta-coca',
      proveedorId: 'prov-sur',
      productoId: 'p-coca',
      nombreProducto: 'Coca-Cola 2,25 L',
      costoHabitual: 2650,
      costoOferta: 2440,
      vence: sumarDias(ahora, 3),
      minimoBultos: 2,
    },
    {
      id: 'oferta-fideos',
      proveedorId: 'prov-norte',
      productoId: 'p-fideos',
      nombreProducto: 'Fideos Matarazzo 500 g',
      costoHabitual: 940,
      costoOferta: 860,
      vence: sumarDias(ahora, 6),
      minimoBultos: 2,
    },
    {
      id: 'oferta-dulce',
      proveedorId: 'prov-valle',
      productoId: null,
      nombreProducto: 'Dulce de leche La Serenísima 400 g',
      costoHabitual: 1900,
      costoOferta: 1700,
      vence: sumarDias(ahora, 4),
      minimoBultos: null,
    },
    {
      id: 'oferta-panales',
      proveedorId: 'prov-norte',
      productoId: null,
      nombreProducto: 'Pañales Pampers x30',
      costoHabitual: 9800,
      costoOferta: 8900,
      vence: sumarDias(ahora, 5),
      minimoBultos: 1,
    },
  ];
}

const ZONA: ReferenciaZona[] = [
  { productoId: 'p-harina', costoPromedio: 684, comercios: 9 },
  { productoId: 'p-yerba', costoPromedio: 3350, comercios: 7 },
  { productoId: 'p-aceite', costoPromedio: 2100, comercios: 12 },
  { productoId: 'p-azucar', costoPromedio: 1000, comercios: 11 },
  { productoId: 'p-leche', costoPromedio: 1190, comercios: 14 },
  { productoId: 'p-coca', costoPromedio: 2640, comercios: 10 },
  { productoId: 'p-lavandina', costoPromedio: 700, comercios: 6 },
  { productoId: 'p-arroz', costoPromedio: 1290, comercios: 3 },
];

export const UMBRALES_POR_DEFECTO: Umbrales = {
  diasDeStock: 3,
  aumentoRaro: 25,
  diferenciaCompra: 8,
  minimoComerciosZona: 5,
  activas: {
    'precio-en-riesgo': true,
    'lista-nueva': true,
    'stock-por-agotarse': true,
    'oferta-que-sirve': true,
    'compras-caro': true,
    'mejor-proveedor': true,
    'cambio-raro': true,
  },
};

/** Estado inicial de la demostración. `ahora` permite fijar la fecha en las pruebas. */
export function crearEstadoInicial(ahora: Date = new Date()): RedState {
  return {
    negocio: {
      nombre: 'Almacén Don Carlos',
      rubro: 'Almacén',
      zona: 'San Justo',
      telefono: '11 5555-0142',
      relato:
        'Tengo un almacén de barrio y le compro a tres proveedores. Los precios los cambio a mano mirando el cuaderno.',
      onboardingHecho: true,
    },
    proveedores: PROVEEDORES_SEMILLA.map((proveedor) => ({ ...proveedor })),
    productos: construirProductos(ahora),
    listas: construirListas(ahora),
    cambios: construirCambios(ahora),
    reglas: {
      general: { margen: 33, redondeo: 10 },
      porCategoria: {
        [CATEGORIAS.almacen]: { margen: 33, redondeo: 10 },
        [CATEGORIAS.bebidas]: { margen: 28, redondeo: 10 },
        [CATEGORIAS.lacteos]: { margen: 23, redondeo: 10 },
        [CATEGORIAS.limpieza]: { margen: 40, redondeo: 10 },
        [CATEGORIAS.kiosco]: { margen: 50, redondeo: 50 },
      },
      margenMinimo: 12,
    },
    umbrales: { ...UMBRALES_POR_DEFECTO, activas: { ...UMBRALES_POR_DEFECTO.activas } },
    ofertas: construirOfertas(ahora),
    zona: ZONA.map((referencia) => ({ ...referencia })),
    barrio: {
      zona: 'San Justo',
      comercios: 31,
      proveedores: 6,
      productosEmparejados: 1240,
      listasProcesadas: 87,
    },
    pedido: [],
    alertas: {},
    suscripcion: {
      plan: 'red',
      estado: 'prueba',
      periodo: 'mensual',
      pruebaUsada: true,
      pruebaDesde: sumarDias(ahora, -2),
      pruebaHasta: sumarDias(ahora, 5),
      proximoCobro: null,
      activoHasta: null,
      medioPago: null,
      pagos: [],
    },
  };
}
