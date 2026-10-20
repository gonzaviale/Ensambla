import { delay, http, HttpResponse } from 'msw';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

// Simulación de POST /api/sami/taller, que en el backend real conversa con un modelo que ensambla el sistema con
// herramientas. Sólo se usa en el modo `completo`. Acá no hay IA: tres mini-kits (velas, almacén y peluquería) se
// detectan por palabras y responden lo mismo que el backend: `agregar_pieza` con nombre, datos y ejemplos, `conectar`,
// `cargar_ejemplos` y una pregunta con respuestas rápidas. Nunca devuelve `usar_kit`: eso lo expande el backend.

const DEMORA_MS = import.meta.env.MODE === 'test' ? 0 : 650;

type Pieza = { id: string; tipo: string; nombre: string };
type Pedido = {
  mensaje?: string;
  estado?: { nombre?: string; distribucion?: string; piezas?: Pieza[] };
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

const sinAcciones = (respuesta: string) => ({ respuesta, acciones: [], opciones: null });
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
  opciones: {
    pregunta: '¿De qué es tu negocio?',
    opciones: ['Tengo un almacén', 'Hago velas artesanales', 'Tengo una peluquería'],
  },
};

const respuestaDe = ({ mensaje = '', estado }: Pedido) => {
  const texto = mensaje.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  const piezas = estado?.piezas ?? [];
  const deTipo = (tipo: string) => piezas.find((pieza) => pieza.tipo === tipo);

  const kit = piezas.length ? undefined : KITS.find((candidato) => candidato.palabras.test(texto));
  if (kit) {
    const renombrar =
      !estado?.nombre || estado.nombre === 'Mi sistema'
        ? [accion('renombrar_sistema', { nombre: kit.nombre })]
        : [];
    return {
      respuesta: kit.respuesta,
      acciones: [...renombrar, ...kit.acciones],
      opciones: kit.opciones,
    };
  }

  const nombre = /llamal[oa]\s+(.+)/i
    .exec(mensaje)?.[1]
    ?.replace(/[.!¡¿?]+$/g, '')
    .trim();
  if (nombre) {
    return {
      respuesta: `Listo, ahora se llama ${nombre}.`,
      acciones: [accion('renombrar_sistema', { nombre })],
      opciones: null,
    };
  }
  const clientes = deTipo('clients');
  if (/\b(saca|quita)\w*\b.*cliente/.test(texto) && clientes) {
    return {
      respuesta: `Listo, saqué ${clientes.nombre.toLowerCase()}.`,
      acciones: [accion('quitar_pieza', { pieza: clientes.id })],
      opciones: null,
    };
  }
  if (/gasto/.test(texto) && !deTipo('expenses')) {
    return {
      respuesta: 'Listo, sumé los gastos.',
      acciones: [accion('agregar_pieza', { tipo: 'expenses' })],
      opciones: null,
    };
  }
  const inventario = deTipo('inventory');
  if (/proveedor/.test(texto) && inventario) {
    return {
      respuesta: 'Listo, sumé el proveedor al inventario.',
      acciones: [
        accion('agregar_campo', { pieza: inventario.id, etiqueta: 'Proveedor', tipo: 'text' }),
      ],
      opciones: null,
    };
  }
  if (/una columna|todo en una/.test(texto)) {
    return {
      respuesta: 'Listo, una pieza por fila.',
      acciones: [accion('cambiar_distribucion', { layout: 'single' })],
      opciones: null,
    };
  }
  if (/gracias|^no,? por ahora/.test(texto)) return sinAcciones('¡Dale! Cuando quieras seguimos.');
  if (!piezas.length) return PREGUNTA_POR_EL_RUBRO;
  return {
    respuesta: 'Te cuento qué puedo hacer. ¿Por dónde seguimos?',
    acciones: [],
    opciones: {
      pregunta: '¿Qué querés hacer con tu sistema?',
      opciones: ['Sumar gastos', 'Agregar un proveedor al inventario', 'Poner todo en una columna'],
    },
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
