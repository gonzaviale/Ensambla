import { delay, http, HttpResponse } from 'msw';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

// Simulación de lo que el backend real ya hace (POST /api/sami/conversacion, POST /api/sami/proyectos y
// GET /api/plantillas). Sólo se usa en el modo `completo`; con el backend real (`parcial` o `ninguno`) responde él.
// La lógica de verdad, con el diccionario completo, está en backend/…/service/sami: acá hay una versión chica
// que respeta la misma idea (puntos por plantilla, confianza, una pregunta si no alcanza).

const DEMORA_MS = import.meta.env.MODE === 'test' ? 0 : 650;
const UMBRAL = 0.7;
const MAX_PREGUNTAS = 3;
const PUNTAJE_SUFICIENTE = 5;

type Campo = { nombre: string; tipo: string; opciones?: string[] };
type Plantilla = {
  id: string;
  nombre: string;
  descripcion: string;
  estructura: { bloques: { nombre: string; campos: Campo[] }[] };
};

const plantilla = (
  n: number,
  nombre: string,
  descripcion: string,
  bloque: string,
  campos: Campo[],
) => ({
  id: `00000000-0000-4000-8000-0000000000a${n}`,
  nombre,
  descripcion,
  estructura: { bloques: [{ nombre: bloque, campos }] },
});

const PLANTILLAS: Plantilla[] = [
  plantilla(4, 'Ventas', 'Anotá lo que vendés, a quién y cuánto cobraste.', 'Ventas', [
    { nombre: 'Fecha', tipo: 'FECHA' },
    { nombre: 'Producto', tipo: 'TEXTO' },
    { nombre: 'Cantidad', tipo: 'NUMERO' },
    { nombre: 'Monto', tipo: 'NUMERO' },
    { nombre: 'Cliente', tipo: 'TEXTO' },
  ]),
  plantilla(
    1,
    'Inventario',
    'Controlá tus productos, su stock y el costo de cada uno.',
    'Productos',
    [
      { nombre: 'Nombre', tipo: 'TEXTO' },
      { nombre: 'Stock', tipo: 'NUMERO' },
      { nombre: 'Rubro', tipo: 'OPCION', opciones: ['Insumos', 'Terminados'] },
    ],
  ),
  plantilla(2, 'Pedidos', 'Anotá los pedidos de tus clientes y seguí su estado.', 'Pedidos', [
    { nombre: 'Cliente', tipo: 'TEXTO' },
    { nombre: 'Entrega', tipo: 'FECHA' },
    { nombre: 'Entregado', tipo: 'SI_NO' },
  ]),
  plantilla(3, 'Clientes', 'Tu agenda de clientes con sus datos de contacto.', 'Clientes', [
    { nombre: 'Nombre', tipo: 'TEXTO' },
    { nombre: 'Teléfono', tipo: 'TEXTO' },
  ]),
  plantilla(5, 'Gastos', 'Registrá lo que gastás en el negocio, en qué y cuánto.', 'Gastos', [
    { nombre: 'Fecha', tipo: 'FECHA' },
    { nombre: 'Detalle', tipo: 'TEXTO' },
    { nombre: 'Monto', tipo: 'NUMERO' },
  ]),
];

const TERMINOS: Record<string, [RegExp, number][]> = {
  Ventas: [
    [/\bvend\w*|\bventa/, 3],
    [/\bcobr\w*|\bkiosco|\balmacen|\btienda/, 2],
    [/\bfiad\w*|\bme deb\w*/, 2],
  ],
  Inventario: [
    [/\bstock|\binventario|\bmercaderia/, 3],
    [/\bproductos|\binsumos|\bunidades/, 2],
  ],
  Pedidos: [
    [/\bpedido|\bencargo|\bme piden/, 3],
    [/\bentrega|\breserva|\bdelivery/, 2],
  ],
  Clientes: [
    [/\bagenda|\bcontactos|\bclientela/, 3],
    [/\bclientes/, 1],
  ],
  Gastos: [
    [/\bgast\w*|\begresos/, 3],
    [/\balquiler|\bsueldos|\bimpuestos/, 2],
  ],
};

const COLUMNAS: { columna: Campo; plantillas: string[]; disparadores: RegExp }[] = [
  {
    columna: { nombre: 'Saldo pendiente', tipo: 'NUMERO' },
    plantillas: ['Ventas', 'Pedidos'],
    disparadores: /\bme deb\w*|\bdeben|\bfiad\w*|\bdeuda|\bsaldo/,
  },
  {
    columna: {
      nombre: 'Forma de pago',
      tipo: 'OPCION',
      opciones: ['Efectivo', 'Transferencia', 'Tarjeta'],
    },
    plantillas: ['Ventas', 'Pedidos', 'Gastos'],
    disparadores: /\befectivo|\btransferencia|\btarjeta|\bmercado pago/,
  },
  {
    columna: { nombre: 'Proveedor', tipo: 'TEXTO' },
    plantillas: ['Inventario', 'Gastos'],
    disparadores: /\bproveedor\w*|\bdistribuidor\w*|\bmayorista/,
  },
  {
    columna: { nombre: 'Vencimiento', tipo: 'FECHA' },
    plantillas: ['Inventario'],
    disparadores: /\bvence|\bvencimiento|\bperecedero/,
  },
];

const OPCIONES_PRINCIPAL = [
  { id: 'ventas', texto: 'Lo que vendo', plantilla: 'Ventas' },
  { id: 'stock', texto: 'Lo que tengo en stock', plantilla: 'Inventario' },
  { id: 'pedidos', texto: 'Los pedidos de mis clientes', plantilla: 'Pedidos' },
  { id: 'gastos', texto: 'Mis gastos', plantilla: 'Gastos' },
  { id: 'clientes', texto: 'Los datos de mis clientes', plantilla: 'Clientes' },
  { id: 'otra', texto: 'Otra cosa', plantilla: null },
];

const normalizar = (texto: string) => texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

type Pedido = { texto: string; respuestas: { preguntaId: string; opcionId: string }[] };

const puntuar = (texto: string) =>
  PLANTILLAS.map((candidata) => ({
    nombre: candidata.nombre,
    puntos: (TERMINOS[candidata.nombre] ?? [])
      .filter(([patron]) => patron.test(texto))
      .reduce((suma, [, peso]) => suma + peso, 0),
  }))
    .filter((puntaje) => puntaje.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos);

const propuestaDe = (nombre: string, texto: string, confianza: number) => {
  const elegida = PLANTILLAS.find((candidata) => candidata.nombre === nombre) as Plantilla;
  const existentes = elegida.estructura.bloques.flatMap((bloque) =>
    bloque.campos.map((c) => c.nombre),
  );
  return {
    plantillaId: elegida.id,
    plantilla: elegida.nombre,
    columnas: COLUMNAS.filter(
      (item) =>
        item.plantillas.includes(nombre) &&
        !existentes.includes(item.columna.nombre) &&
        item.disparadores.test(texto),
    ).map((item) => ({ opciones: [], ...item.columna })),
    confianza,
    origen: 'ALGORITMO',
  };
};

function conversar({ texto, respuestas }: Pedido) {
  const normalizado = normalizar(texto);
  const principal = respuestas.find((respuesta) => respuesta.preguntaId === 'principal');
  const elegida = OPCIONES_PRINCIPAL.find((opcion) => opcion.id === principal?.opcionId);
  if (principal) {
    if (!elegida?.plantilla) {
      return {
        tipo: 'RESPALDO_IA',
        pregunta: null,
        propuesta: null,
        preguntasHechas: respuestas.length,
      };
    }
    return {
      tipo: 'PROPUESTA',
      pregunta: null,
      propuesta: propuestaDe(elegida.plantilla, normalizado, 1),
      preguntasHechas: respuestas.length,
    };
  }
  const [mejor, segunda] = puntuar(normalizado);
  const confianza = mejor
    ? Math.round(
        Math.min(1, mejor.puntos / PUNTAJE_SUFICIENTE) *
          ((mejor.puntos - (segunda?.puntos ?? 0)) / mejor.puntos) *
          100,
      ) / 100
    : 0;
  if (mejor && confianza >= UMBRAL) {
    return {
      tipo: 'PROPUESTA',
      pregunta: null,
      propuesta: propuestaDe(mejor.nombre, normalizado, confianza),
      preguntasHechas: respuestas.length,
    };
  }
  if (respuestas.length < MAX_PREGUNTAS) {
    return {
      tipo: 'PREGUNTA',
      pregunta: {
        id: 'principal',
        texto: '¿Qué es lo principal que querés anotar?',
        opciones: OPCIONES_PRINCIPAL.map(({ id, texto: etiqueta }) => ({ id, texto: etiqueta })),
      },
      propuesta: null,
      preguntasHechas: respuestas.length,
    };
  }
  return {
    tipo: 'RESPALDO_IA',
    pregunta: null,
    propuesta: null,
    preguntasHechas: respuestas.length,
  };
}

type PedidoDeProyecto = {
  plantillaId?: string;
  nombre?: string;
  columnas?: { nombre?: string; tipo?: string; opciones?: string[] }[];
};

export const handlersSamiProyecto = [
  http.get(api('/plantillas'), () => HttpResponse.json(PLANTILLAS)),

  http.post(api('/sami/conversacion'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_MS);
    const pedido = (await request.json()) as Partial<Pedido>;
    if (!pedido.texto?.trim()) {
      return errorResponse(400, 'Los datos enviados no son válidos', pathDe(request), {
        texto: 'must not be blank',
      });
    }
    return HttpResponse.json(
      conversar({ texto: pedido.texto, respuestas: pedido.respuestas ?? [] }),
    );
  }),

  http.post(api('/sami/proyectos'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    const pedido = (await request.json()) as PedidoDeProyecto;
    const elegida = PLANTILLAS.find((candidata) => candidata.id === pedido.plantillaId);
    if (!elegida) return errorResponse(404, 'Plantilla no encontrada', pathDe(request));
    const columnas = pedido.columnas ?? [];
    const vacia = columnas.findIndex((columna) => !columna.nombre?.trim());
    if (vacia >= 0) {
      return errorResponse(400, 'Revisá la configuración del campo', pathDe(request), {
        [`columnas[${vacia}].nombre`]: 'Escribí un nombre',
      });
    }
    const bloques = elegida.estructura.bloques.map((bloque, indice) => ({
      id: crypto.randomUUID(),
      nombre: bloque.nombre,
      campos: [
        ...bloque.campos,
        ...(indice === 0
          ? columnas.map((c) => ({ nombre: c.nombre?.trim() ?? '', tipo: c.tipo ?? 'TEXTO' }))
          : []),
      ].map((campo) => ({ id: crypto.randomUUID(), nombre: campo.nombre, tipo: campo.tipo })),
    }));
    return HttpResponse.json(
      { id: crypto.randomUUID(), nombre: pedido.nombre?.trim() || elegida.nombre, bloques },
      { status: 201 },
    );
  }),
];
