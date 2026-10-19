import { delay, http, HttpResponse } from 'msw';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

// Simulación de POST /api/sami/taller, que en el backend real conversa con un modelo que pide herramientas. Sólo se usa
// en el modo `completo`. Acá no hay IA: unas pocas reglas devuelven el mismo tipo de respuesta (texto, acciones y
// botonera) para poder ver y probar el circuito.

const DEMORA_MS = import.meta.env.MODE === 'test' ? 0 : 650;

type Pieza = { id: string; tipo: string; nombre: string };
type Pedido = {
  mensaje?: string;
  estado?: { nombre?: string; distribucion?: string; piezas?: Pieza[] };
};

const sinAcciones = (respuesta: string) => ({ respuesta, acciones: [], opciones: null });

const respuestaDe = ({ mensaje = '', estado }: Pedido) => {
  const texto = mensaje.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  const piezas = estado?.piezas ?? [];
  const inventario = piezas.find((pieza) => pieza.tipo === 'inventory');

  if (/turno|factura/.test(texto)) {
    return sinAcciones(
      'Eso todavía no lo tengo como pieza. Lo más cercano son los pedidos: ¿querés que los sume?',
    );
  }
  if (/proveedor/.test(texto) && inventario) {
    return {
      respuesta: 'Listo, sumé el proveedor al inventario.',
      acciones: [
        {
          herramienta: 'agregar_campo',
          argumentos: { pieza: inventario.id, etiqueta: 'Proveedor', tipo: 'text' },
        },
      ],
      opciones: null,
    };
  }
  if (/gasto/.test(texto) && !piezas.some((pieza) => pieza.tipo === 'expenses')) {
    return {
      respuesta: 'Listo, sumé los gastos.',
      acciones: [{ herramienta: 'agregar_pieza', argumentos: { tipo: 'expenses' } }],
      opciones: null,
    };
  }
  if (/una columna|todo en una/.test(texto)) {
    return {
      respuesta: 'Listo, una pieza por fila.',
      acciones: [{ herramienta: 'cambiar_distribucion', argumentos: { layout: 'single' } }],
      opciones: null,
    };
  }
  if (/gracias/.test(texto)) return sinAcciones('¡De nada! Cuando quieras seguimos.');
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
