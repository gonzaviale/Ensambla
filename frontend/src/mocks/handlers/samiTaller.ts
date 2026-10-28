import { delay, http, HttpResponse } from 'msw';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

// Simulación de POST /api/sami/taller, que en el backend real conversa con un modelo que ensambla el sistema con
// herramientas. Sólo se usa en el modo `completo`. Acá no hay IA: tres mini-kits (velas, almacén y peluquería) se
// detectan por palabras y responden lo mismo que el backend: `agregar_pieza` con nombre, datos y ejemplos, `conectar`,
// `cargar_ejemplos` y una pregunta con respuestas rápidas. Nunca devuelve `usar_kit`: eso lo expande el backend.
// Sobre un sistema armado entiende algunos ajustes, entre ellos renombrar un dato o ponerlo primero (`editar_campo`).
// Con una foto no la mira: responde siempre lo mismo, unas ventas «leídas» de un cuaderno (`cargar_registros`); «el
// precio era 9000» corrige la primera de esas ventas (`editar_registros`).

const DEMORA_MS = import.meta.env.MODE === 'test' ? 0 : 650;

type Pieza = {
  id: string;
  tipo: string;
  nombre: string;
  campos?: { etiqueta: string; tipo: string }[];
  registros?: { id: string; datos: Record<string, unknown> }[];
};
type Pedido = {
  mensaje?: string;
  estado?: { nombre?: string; distribucion?: string; piezas?: Pieza[] };
  imagen?: { tipo?: string; datos?: string };
};
type Accion = { herramienta: string; argumentos: Record<string, unknown> };
type Opciones = { pregunta: string; opciones: string[] };
type Kit = {
  palabras: RegExp;
  nombre: string;
  respuesta: string;
  acciones: Accion[];
  opciones: Opciones;
};

const sinAcciones = (respuesta: string) => ({ respuesta, acciones: [], preguntas: [] });
const accion = (herramienta: string, argumentos: Record<string, unknown>): Accion => ({
  herramienta,
  argumentos,
});
const preguntarPorGastos = (pregunta: string): Opciones => ({
  pregunta,
  opciones: ['Sí, sumá los gastos', 'No, por ahora así'],
});

const KITS: Kit[] = [
  {
    palabras: /vela/,
    nombre: 'Mis velas',
    respuesta:
      'Armé tus velas con su aroma, las clientas y los pedidos, y conecté cada pedido con la vela que lleva. Ya lo podés ver a la derecha.',
    acciones: [
      accion('agregar_pieza', {
        tipo: 'inventory',
        nombre: 'Velas',
        campos: [{ etiqueta: 'Aroma', tipo: 'text' }],
        ejemplos: [
          {
            Producto: 'Vela de soja lavanda',
            Cantidad: 12,
            Precio: 6500,
            Costo: 2800,
            Aroma: 'Lavanda',
          },
          {
            Producto: 'Vela en frasco vainilla',
            Cantidad: 8,
            Precio: 8200,
            Costo: 3500,
            Aroma: 'Vainilla',
          },
          { Producto: 'Vela flotante', Cantidad: 30, Precio: 1500, Costo: 600, Aroma: 'Sin aroma' },
        ],
      }),
      accion('agregar_pieza', {
        tipo: 'clients',
        nombre: 'Clientas',
        ejemplos: [
          { Nombre: 'Carla Gómez', Teléfono: '11 5555-0201', Notas: 'Compra por Instagram.' },
          { Nombre: 'Juli Sosa', Teléfono: '11 5555-0202', Notas: 'La conocí en la feria.' },
        ],
      }),
      accion('agregar_pieza', {
        tipo: 'orders',
        nombre: 'Pedidos',
        campos: [
          { etiqueta: 'Canal', tipo: 'select', opciones: ['Instagram', 'Feria', 'WhatsApp'] },
        ],
      }),
      accion('conectar', { desde: 'Pedidos', hacia: 'Velas', etiqueta: 'Vela' }),
      accion('cargar_ejemplos', {
        pieza: 'Pedidos',
        filas: [
          {
            Pedido: 'Pedido #101',
            Cliente: 'Carla Gómez',
            Vela: 'Vela de soja lavanda',
            Fecha: '2026-10-15',
            Total: 13000,
            Estado: 'Pendiente',
            Canal: 'Instagram',
          },
          {
            Pedido: 'Pedido #102',
            Cliente: 'Juli Sosa',
            Vela: 'Vela en frasco vainilla',
            Fecha: '2026-10-16',
            Total: 8200,
            Estado: 'Entregado',
            Canal: 'Feria',
          },
        ],
      }),
    ],
    opciones: preguntarPorGastos(
      '¿Querés llevar también lo que gastás en cera, mechas y esencias?',
    ),
  },
  {
    palabras: /almacen|kiosco|despensa|minimercado/,
    nombre: 'Mi almacén',
    respuesta:
      'Te armé los productos con su stock, las ventas y los clientes con lo que te deben. Ya lo podés ver a la derecha.',
    acciones: [
      accion('agregar_pieza', {
        tipo: 'inventory',
        nombre: 'Productos',
        ejemplos: [
          {
            Producto: 'Yerba Playadito 1 kg',
            Cantidad: 24,
            Precio: 3600,
            Costo: 2700,
            'Stock mínimo': 10,
          },
          {
            Producto: 'Galletitas Express',
            Cantidad: 15,
            Precio: 1300,
            Costo: 900,
            'Stock mínimo': 6,
          },
        ],
      }),
      accion('agregar_pieza', {
        tipo: 'sales',
        nombre: 'Ventas',
        ejemplos: [
          {
            Producto: 'Yerba Playadito 1 kg',
            Cantidad: 2,
            Precio: 3600,
            Fecha: '2026-10-16',
            Cliente: 'Martín García',
            Fiado: true,
          },
        ],
      }),
      accion('agregar_pieza', {
        tipo: 'clients',
        nombre: 'Clientes',
        campos: [{ etiqueta: 'Debe', tipo: 'currency' }],
        ejemplos: [
          { Nombre: 'Martín García', Teléfono: '11 5555-0102', Debe: 7200 },
          { Nombre: 'Lucía Pérez', Teléfono: '11 5555-0101', Debe: 0 },
        ],
      }),
    ],
    opciones: preguntarPorGastos('¿Querés anotar también los gastos del local?'),
  },
  {
    palabras: /peluquer|barberia|turno/,
    nombre: 'Mi peluquería',
    respuesta:
      'Te armé una tabla de turnos conectada con tus clientes, y los productos que usás. Ya lo podés ver a la derecha.',
    acciones: [
      accion('agregar_pieza', {
        tipo: 'clients',
        nombre: 'Clientes',
        ejemplos: [
          { Nombre: 'Sofía Ruiz', Teléfono: '11 5555-0301' },
          { Nombre: 'Diego Paz', Teléfono: '11 5555-0302' },
        ],
      }),
      accion('agregar_pieza', {
        tipo: 'form',
        nombre: 'Turnos',
        campos: [
          { etiqueta: 'Cliente', tipo: 'text' },
          { etiqueta: 'Servicio', tipo: 'select', opciones: ['Corte', 'Color', 'Peinado'] },
          { etiqueta: 'Hora', tipo: 'time' },
        ],
        ejemplos: [
          {
            Nombre: 'Corte de Sofía',
            Cliente: 'Sofía Ruiz',
            Servicio: 'Corte',
            Fecha: '2026-10-20',
            Hora: '10:30',
          },
          {
            Nombre: 'Color de Diego',
            Cliente: 'Diego Paz',
            Servicio: 'Color',
            Fecha: '2026-10-20',
            Hora: '15:00',
          },
        ],
      }),
      accion('conectar', { desde: 'Turnos', hacia: 'Clientes', etiqueta: 'Cliente' }),
      accion('agregar_pieza', {
        tipo: 'inventory',
        nombre: 'Productos',
        ejemplos: [
          {
            Producto: 'Tintura rubio ceniza',
            Cantidad: 6,
            Precio: 9000,
            Costo: 5200,
            'Stock mínimo': 3,
          },
          {
            Producto: 'Shampoo profesional 1 L',
            Cantidad: 4,
            Precio: 12000,
            Costo: 7000,
            'Stock mínimo': 2,
          },
        ],
      }),
    ],
    opciones: preguntarPorGastos('¿Querés sumar también los gastos del local?'),
  },
];

// Sin un rubro reconocible y sin nada armado, Sami pregunta (en el backend, con `ofrecer_opciones`).
const PREGUNTA_POR_EL_RUBRO = {
  respuesta: 'Contame un poco más así lo armo a tu medida.',
  acciones: [],
  preguntas: [
    {
      pregunta: '¿De qué es tu negocio?',
      opciones: ['Tengo un almacén', 'Hago velas artesanales', 'Tengo una peluquería'],
    },
  ],
};

const sinTildes = (texto: string) =>
  texto
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();
const sinComillas = (texto: string) => texto.replace(/^[«"'“\s]+|[»"'”.!?\s]+$/g, '').trim();

/** La pieza que tiene un dato con esa etiqueta; si se nombró la pieza («… en ventas»), se busca ahí. */
const buscarDato = (piezas: Pieza[], etiqueta: string, enPieza?: string) => {
  const candidatas = enPieza
    ? piezas.filter((pieza) => sinTildes(pieza.nombre) === sinTildes(enPieza))
    : piezas;
  for (const pieza of candidatas) {
    const campo = pieza.campos?.find((item) => sinTildes(item.etiqueta) === sinTildes(etiqueta));
    if (campo) return { pieza, campo };
  }
  return undefined;
};

const RENOMBRAR_DATO = [
  /cambi\S*\s+el\s+nombre\s+(?:a|al|de|del)\s+(.+?)\s+por\s+(.+)/i,
  /renombr\S*\s+(.+?)\s+(?:a|como|por)\s+(.+)/i,
];
const PONER_PRIMERO =
  /pon\S*\s+(?:el\s+|la\s+)?(.+?)\s+(?:primero|primera|al\s+principio|adelante)\b(.*)/i;

/** «Cambiale el nombre a Cliente por Clienta», «poné Cliente primero» (con «en ventas» opcional). */
const ajusteDeDato = (mensaje: string, piezas: Pieza[]) => {
  // «… en ventas» al final nombra la pieza, si hay una que se llame así.
  const separarPieza = (texto: string) => {
    const [, resto, pieza = ''] = /^(.*)\s+en\s+(?:la\s+pieza\s+)?(.+)$/i.exec(texto) ?? [];
    const nombrada = piezas.find(
      (item) => sinTildes(item.nombre) === sinTildes(sinComillas(pieza)),
    );
    return nombrada && resto !== undefined
      ? { texto: sinComillas(resto), pieza: nombrada.nombre }
      : { texto: sinComillas(texto), pieza: undefined };
  };
  for (const patron of RENOMBRAR_DATO) {
    const [, crudo, nuevoCrudo] = patron.exec(mensaje) ?? [];
    if (!crudo || !nuevoCrudo) continue;
    const nuevo = separarPieza(nuevoCrudo);
    const actual = sinComillas(crudo);
    const encontrado = buscarDato(piezas, actual, nuevo.pieza);
    // «Renombrá …» también puede ser otra cosa (el sistema, una pieza): si no es un dato, sigue de largo.
    if ((!encontrado || !nuevo.texto) && patron !== RENOMBRAR_DATO[0]) continue;
    if (!encontrado || !nuevo.texto) {
      return sinAcciones(`No encontré el dato «${actual}». ¿Cómo se llama en tu cuaderno?`);
    }
    return {
      respuesta: `Listo, «${encontrado.campo.etiqueta}» ahora se llama «${nuevo.texto}» en ${encontrado.pieza.nombre.toLowerCase()}.`,
      acciones: [
        accion('editar_campo', {
          pieza: encontrado.pieza.id,
          campo: encontrado.campo.etiqueta,
          etiqueta: nuevo.texto,
        }),
      ],
      preguntas: [],
    };
  }
  const [, crudo, cola = ''] = PONER_PRIMERO.exec(mensaje) ?? [];
  if (crudo) {
    const actual = sinComillas(crudo);
    const enPieza = separarPieza(`x${cola}`).pieza;
    const encontrado = buscarDato(piezas, actual, enPieza);
    if (!encontrado) {
      return sinAcciones(`No encontré el dato «${actual}». ¿Cómo se llama en tu cuaderno?`);
    }
    return {
      respuesta: `Listo, puse «${encontrado.campo.etiqueta}» primero en ${encontrado.pieza.nombre.toLowerCase()}.`,
      acciones: [
        accion('editar_campo', {
          pieza: encontrado.pieza.id,
          campo: encontrado.campo.etiqueta,
          posicion: 1,
        }),
      ],
      preguntas: [],
    };
  }
  return undefined;
};

// Lo que Sami «leyó» de la foto: tres ventas del cuaderno, que se cargan como registros reales. Si no hay dónde
// anotarlas, suma las ventas (y los productos, para que cada venta diga qué se vendió).
const VENTAS_DE_LA_FOTO = [
  { Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2, Precio: 13000, Fecha: '2026-10-18' },
  { Producto: 'Vela vainilla', Cliente: 'Marta', Cantidad: 1, Precio: 8200, Fecha: '2026-10-18' },
  { Producto: 'Vela flotante', Cliente: 'Lucía', Cantidad: 4, Precio: 6000, Fecha: '2026-10-19' },
];

const respuestaALaFoto = (piezas: Pieza[]) => {
  const ventas = piezas.find((pieza) => pieza.tipo === 'sales');
  const productos = piezas.some((pieza) => pieza.tipo === 'inventory');
  return {
    respuesta:
      'Miré tu foto: anotaste tres ventas. Las cargué en tus ventas, con el producto y la clienta de cada una. Fijate si están bien.',
    acciones: [
      ...(productos ? [] : [accion('agregar_pieza', { tipo: 'inventory', nombre: 'Productos' })]),
      ...(ventas ? [] : [accion('agregar_pieza', { tipo: 'sales', nombre: 'Ventas' })]),
      accion('cargar_registros', { pieza: ventas?.id ?? 'Ventas', filas: VENTAS_DE_LA_FOTO }),
    ],
    preguntas: [
      {
        tema: 'Tildes',
        pregunta: '¿Qué significan los tildes?',
        opciones: ['Ya entregado y cobrado', 'Entregado pero sin cobrar'],
      },
      {
        tema: 'Número de la derecha',
        pregunta: '¿El número de la derecha es costo o lo que deben?',
        opciones: ['El costo', 'Lo que me deben'],
      },
    ],
  };
};

// «El precio era 9000»: corrige la primera venta cargada (`editar_registros`), sin cargarla de nuevo.
const correccionDePrecio = (texto: string, piezas: Pieza[]) => {
  const precio = /precio\D*(\d+)/.exec(texto)?.[1];
  const pieza = piezas.find((item) => item.tipo === 'sales' && item.registros?.length);
  const registro = pieza?.registros?.[0];
  if (!precio || !pieza || !registro) return undefined;
  return {
    respuesta: `Listo, corregí el precio a ${precio}.`,
    acciones: [
      accion('editar_registros', {
        pieza: pieza.id,
        registros: [{ id: registro.id, datos: { Precio: Number(precio) } }],
      }),
    ],
    preguntas: [],
  };
};

const respuestaDe = ({ mensaje = '', estado, imagen }: Pedido) => {
  if (imagen?.datos) return respuestaALaFoto(estado?.piezas ?? []);

  const texto = mensaje.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  const piezas = estado?.piezas ?? [];
  const deTipo = (tipo: string) => piezas.find((pieza) => pieza.tipo === tipo);

  const kit = piezas.length ? undefined : KITS.find((candidato) => candidato.palabras.test(texto));
  if (kit) {
    const renombrar =
      !estado?.nombre || estado.nombre === 'Mi cuaderno' || estado.nombre === 'Mi sistema'
        ? [accion('renombrar_sistema', { nombre: kit.nombre })]
        : [];
    return {
      respuesta: kit.respuesta,
      acciones: [...renombrar, ...kit.acciones],
      preguntas: [kit.opciones],
    };
  }

  const correccion = correccionDePrecio(texto, piezas);
  if (correccion) return correccion;

  const ajuste = piezas.length ? ajusteDeDato(mensaje, piezas) : undefined;
  if (ajuste) return ajuste;

  const nombre = /llamal[oa]\s+(.+)/i
    .exec(mensaje)?.[1]
    ?.replace(/[.!¡¿?]+$/g, '')
    .trim();
  if (nombre) {
    return {
      respuesta: `Listo, ahora se llama ${nombre}.`,
      acciones: [accion('renombrar_sistema', { nombre })],
      preguntas: [],
    };
  }
  const clientes = deTipo('clients');
  if (/\b(saca|quita)\w*\b.*cliente/.test(texto) && clientes) {
    return {
      respuesta: `Listo, saqué ${clientes.nombre.toLowerCase()}.`,
      acciones: [accion('quitar_pieza', { pieza: clientes.id })],
      preguntas: [],
    };
  }
  if (/gasto/.test(texto) && !deTipo('expenses')) {
    return {
      respuesta: 'Listo, sumé los gastos.',
      acciones: [accion('agregar_pieza', { tipo: 'expenses' })],
      preguntas: [],
    };
  }
  const inventario = deTipo('inventory');
  if (/proveedor/.test(texto) && inventario) {
    return {
      respuesta: 'Listo, sumé el proveedor al inventario.',
      acciones: [
        accion('agregar_campo', { pieza: inventario.id, etiqueta: 'Proveedor', tipo: 'text' }),
      ],
      preguntas: [],
    };
  }
  if (/una columna|todo en una/.test(texto)) {
    return {
      respuesta: 'Listo, una pieza por fila.',
      acciones: [accion('cambiar_distribucion', { layout: 'single' })],
      preguntas: [],
    };
  }
  if (/gracias|^no,? por ahora/.test(texto)) return sinAcciones('¡Dale! Cuando quieras seguimos.');
  if (!piezas.length) return PREGUNTA_POR_EL_RUBRO;
  return {
    respuesta: 'Te cuento qué puedo hacer. ¿Por dónde seguimos?',
    acciones: [],
    preguntas: [
      {
        pregunta: '¿Qué querés hacer con tu cuaderno?',
        opciones: [
          'Sumar gastos',
          'Agregar un proveedor al inventario',
          'Poner todo en una columna',
        ],
      },
    ],
  };
};

export const handlersSamiTaller = [
  http.post(api('/sami/taller'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_MS);
    const pedido = (await request.json()) as Pedido;
    if (!pedido.mensaje?.trim()) {
      return errorResponse(400, 'Los datos enviados no son válidos', pathDe(request), {
        mensaje: 'must not be blank',
      });
    }
    return HttpResponse.json(respuestaDe(pedido));
  }),
];
