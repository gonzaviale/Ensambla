import {
  destinoDe,
  itemDeAviso,
  numero,
  plural,
  productosDe,
  type ContextoDeChat,
  type ItemDeChat,
  type Producto,
  type Respuesta,
} from './datos';

const AVISOS_A_MOSTRAR = 4;
const PRODUCTOS_A_MOSTRAR = 6;

export function avisosDelSistema(contexto: ContextoDeChat): Respuesta {
  const avisos = [...contexto.avisos].sort((a, b) => b.prioridad - a.prioridad);
  if (!avisos.length) {
    return {
      texto:
        'Por ahora no encontré nada para mirar. Sigo atenta al stock, las ventas y los precios de tu zona.',
      sugerencias: ['¿Cuánto vendí?', '¿Qué me falta?'],
    };
  }
  return {
    texto: `Encontré ${plural(avisos.length, 'cosa', 'cosas')} para mirar${cuantosUrgentes(avisos.filter((aviso) => aviso.prioridad === 3).length)}. Estas son las más importantes:`,
    items: avisos.slice(0, AVISOS_A_MOSTRAR).map(itemDeAviso),
    verTodos: masDeLosQueSeMuestran(avisos.length),
    sugerencias: ['¿Qué me falta?', '¿Cuánto vendí?'],
  };
}

const cuantosUrgentes = (urgentes: number) => {
  if (!urgentes) return '';
  return urgentes === 1 ? ', 1 es urgente' : `, ${urgentes} son urgentes`;
};

const masDeLosQueSeMuestran = (cantidad: number) =>
  cantidad > AVISOS_A_MOSTRAR ? cantidad : undefined;

export function deZona(contexto: ContextoDeChat): Respuesta {
  const avisos = contexto.avisos.filter(
    (aviso) => aviso.grupo === 'zona' || aviso.tipo === 'precio_zona',
  );
  if (!avisos.length) {
    return {
      texto: 'No veo diferencias importantes con los precios de tu zona por ahora.',
      sugerencias: ['¿Qué avisos tengo?'],
    };
  }
  return {
    texto: `Comparando con tu zona encontré ${plural(avisos.length, 'cosa', 'cosas')}:`,
    items: avisos.slice(0, AVISOS_A_MOSTRAR).map(itemDeAviso),
    verTodos: masDeLosQueSeMuestran(avisos.length),
  };
}

const cantidadDe = (producto: Producto) => numero(producto.fila.quantity);

const estaAgotado = (producto: Producto) => cantidadDe(producto) <= 0;

const tienePocoStock = (producto: Producto) =>
  estaAgotado(producto) || cantidadDe(producto) <= numero(producto.fila.minimum);

export function stockBajo(contexto: ContextoDeChat): Respuesta {
  const productos = productosDe(contexto.sistema);
  if (!productos.length) {
    return {
      texto: 'Todavía no hay productos cargados en la sección, así que no puedo mirar el stock.',
    };
  }
  const bajos = productos.filter(tienePocoStock).sort((a, b) => cantidadDe(a) - cantidadDe(b));
  if (!bajos.length) {
    return {
      texto: `Todo el stock está por encima del mínimo (${plural(productos.length, 'producto', 'productos')} revisados).`,
      sugerencias: ['¿Cuánto vendí?', '¿Qué avisos tengo?'],
    };
  }
  return {
    texto: `Tenés ${plural(bajos.length, 'producto', 'productos')} con poco stock${cuantosAgotados(bajos.filter(estaAgotado).length)}:`,
    items: bajos.slice(0, PRODUCTOS_A_MOSTRAR).map((producto) => itemDeStock(producto, contexto)),
    sugerencias: ['¿Qué avisos tengo?', '¿Cuánto vendí?'],
  };
}

const cuantosAgotados = (agotados: number) => {
  if (!agotados) return '';
  return agotados === 1 ? ', 1 agotado' : `, ${agotados} agotados`;
};

function itemDeStock(producto: Producto, contexto: ContextoDeChat): ItemDeChat {
  const minimo = numero(producto.fila.minimum);
  return {
    id: producto.fila.id,
    titulo: producto.nombre,
    detalle: estaAgotado(producto)
      ? `Agotado · mínimo ${minimo}`
      : `Quedan ${cantidadDe(producto)} · mínimo ${minimo}`,
    nivel: estaAgotado(producto) ? 3 : 2,
    destino: destinoDe(producto, contexto.sistema),
  };
}
