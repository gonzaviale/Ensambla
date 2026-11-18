import { pesos, type Aviso } from './alertas';
import { allBlocks, normalize } from './core';
import type { DestinoDeAviso } from './destinoDeAviso';
import { destinoDeAviso } from './destinoDeAviso';
import type { Bloque, Fila, Sistema } from './tipos';

// El chat de Sami: contesta preguntas de stock, ventas y avisos con los datos de la sección abierta. No usa un
// modelo de lenguaje: reconoce de qué se habla por palabras clave y arma la respuesta con cuentas simples, así
// anda sin conexión y siempre dice de dónde sale cada número.

/** Una línea de una respuesta: un producto o un aviso, con a dónde llevar si se toca «Ver». */
export type ItemDeChat = {
  id: string;
  titulo: string;
  detalle?: string;
  /** 3 urgente, 2 para hoy, 1 para mirar: sólo marca el punto de color. */
  nivel?: 1 | 2 | 3;
  aviso?: Aviso;
  destino?: DestinoDeAviso;
};

export type Respuesta = {
  texto: string;
  items?: ItemDeChat[];
  /** Ofrece abrir la pantalla con todos los avisos. */
  verTodos?: number;
  sugerencias?: string[];
};

export type ContextoDeChat = { sistema: Sistema; avisos: Aviso[]; hoy: string };

export const SUGERENCIAS_INICIALES = ['¿Qué avisos tengo?', '¿Qué me falta?', '¿Cuánto vendí?'];

const DIA_MS = 86_400_000;
const numero = (valor: unknown) => Number(valor ?? 0) || 0;
const diasEntre = (hoy: string, fecha: string) =>
  Math.floor((Date.parse(`${hoy}T12:00:00`) - Date.parse(`${fecha}T12:00:00`)) / DIA_MS);
const plural = (cantidad: number, uno: string, varios: string) =>
  `${cantidad} ${cantidad === 1 ? uno : varios}`;

const tiene = (texto: string, palabras: string[]) =>
  palabras.some((palabra) => texto.includes(palabra));

function bloques(sistema: Sistema) {
  const todos = allBlocks(sistema);
  return {
    inventarios: todos.filter((bloque) => bloque.type === 'inventory'),
    ventas: todos.filter((bloque) => bloque.type === 'sales'),
  };
}

type Producto = { bloque: Bloque; fila: Fila; nombre: string };

const productosDe = (inventarios: Bloque[]): Producto[] =>
  inventarios.flatMap((bloque) =>
    bloque.rows.flatMap((fila) => {
      const nombre = String(fila.name ?? '').trim();
      return nombre ? [{ bloque, fila, nombre }] : [];
    }),
  );

type Venta = { productoId: string; unidades: number; total: number; dias: number; fiado: boolean };

function ventasDe(sistema: Sistema, hoy: string): Venta[] {
  return bloques(sistema).ventas.flatMap((bloque) =>
    bloque.rows.flatMap((fila) => {
      if (!fila.date || !fila.product) return [];
      const unidades = numero(fila.quantity);
      return [
        {
          productoId: String(fila.product),
          unidades,
          total: unidades * numero(fila.price),
          dias: diasEntre(hoy, String(fila.date)),
          fiado: fila.credit === true,
        },
      ];
    }),
  );
}

const destinoDe = (producto: Producto, sistema: Sistema): DestinoDeAviso => ({
  sistemaId: sistema.id,
  bloqueId: producto.bloque.id,
  filaId: producto.fila.id,
});

// Las palabras de la pregunta que no nombran un producto.
const RELLENO = new Set([
  'de',
  'del',
  'la',
  'el',
  'los',
  'las',
  'un',
  'una',
  'que',
  'cuanto',
  'cuantos',
  'cuanta',
  'quedan',
  'queda',
  'tengo',
  'hay',
  'cual',
  'es',
  'mi',
  'mis',
  'para',
  'por',
  'con',
  'sin',
  'en',
  'y',
  'o',
  'me',
  'te',
  'se',
  'lo',
  'al',
  'a',
  'stock',
  'precio',
  'vendi',
  'vendo',
  'vendio',
  'ventas',
  'venta',
  'vender',
  'cuesta',
  'sale',
  'costo',
  'margen',
  'hoy',
  'ayer',
  'semana',
  'mes',
  'dias',
  'ultimos',
  'esta',
  'este',
  'rotacion',
  'salida',
  'unidades',
  'dime',
  'decime',
  'mostrame',
  'muestrame',
  'quiero',
  'saber',
  'ver',
  'como',
  'va',
  'estan',
  'esta',
]);

/** Los productos de la sección que la pregunta nombra (por ejemplo «yerba» o «aceite natura»). */
function productosNombrados(pregunta: string, productos: Producto[]): Producto[] {
  const palabras = normalize(pregunta)
    .replace(/[^a-z0-9 ]+/g, ' ')
    .split(/\s+/)
    .filter((palabra) => palabra.length >= 3 && !RELLENO.has(palabra));
  if (!palabras.length) return [];
  const puntuados = productos
    .map((producto) => {
      const nombre = normalize(producto.nombre);
      return { producto, puntos: palabras.filter((palabra) => nombre.includes(palabra)).length };
    })
    .filter((item) => item.puntos > 0);
  const mejor = Math.max(0, ...puntuados.map((item) => item.puntos));
  return puntuados.filter((item) => item.puntos === mejor).map((item) => item.producto);
}

function periodoDe(texto: string): { dias: number; nombre: string } | null {
  if (texto.includes('ayer')) return { dias: 1, nombre: 'ayer' };
  if (tiene(texto, ['hoy', 'del dia'])) return { dias: 0, nombre: 'hoy' };
  if (tiene(texto, ['semana', '7 dias'])) return { dias: 7, nombre: 'en los últimos 7 días' };
  if (tiene(texto, ['mes', '30 dias'])) return { dias: 30, nombre: 'en los últimos 30 días' };
  return null;
}

function suma(ventas: Venta[], desde: number, hasta: number) {
  const dentro = ventas.filter((venta) => venta.dias >= desde && venta.dias <= hasta);
  return {
    total: dentro.reduce((acumulado, venta) => acumulado + venta.total, 0),
    unidades: dentro.reduce((acumulado, venta) => acumulado + venta.unidades, 0),
    cantidad: dentro.length,
  };
}

// ---- Respuestas ----

function avisosDelSistema(contexto: ContextoDeChat): Respuesta {
  const avisos = [...contexto.avisos].sort((a, b) => b.prioridad - a.prioridad);
  if (!avisos.length) {
    return {
      texto:
        'Por ahora no encontré nada para mirar. Sigo atenta al stock, las ventas y los precios de tu zona.',
      sugerencias: ['¿Cuánto vendí?', '¿Qué me falta?'],
    };
  }
  const urgentes = avisos.filter((aviso) => aviso.prioridad === 3).length;
  return {
    texto: `Encontré ${plural(avisos.length, 'cosa', 'cosas')} para mirar${urgentes ? `, ${urgentes === 1 ? '1 es urgente' : `${urgentes} son urgentes`}` : ''}. Estas son las más importantes:`,
    items: avisos.slice(0, 4).map((aviso) => ({
      id: aviso.id,
      titulo: aviso.titulo,
      detalle: aviso.detalle,
      nivel: aviso.prioridad,
      aviso,
      destino: destinoDeAviso(aviso) ?? undefined,
    })),
    verTodos: avisos.length > 4 ? avisos.length : undefined,
    sugerencias: ['¿Qué me falta?', '¿Cuánto vendí?'],
  };
}

function stockBajo(contexto: ContextoDeChat): Respuesta {
  const productos = productosDe(bloques(contexto.sistema).inventarios);
  const bajos = productos
    .filter(
      ({ fila }) => numero(fila.quantity) <= 0 || numero(fila.quantity) <= numero(fila.minimum),
    )
    .sort((a, b) => numero(a.fila.quantity) - numero(b.fila.quantity));
  if (!productos.length) {
    return {
      texto: 'Todavía no hay productos cargados en la sección, así que no puedo mirar el stock.',
    };
  }
  if (!bajos.length) {
    return {
      texto: `Todo el stock está por encima del mínimo (${plural(productos.length, 'producto', 'productos')} revisados).`,
      sugerencias: ['¿Cuánto vendí?', '¿Qué avisos tengo?'],
    };
  }
  const agotados = bajos.filter(({ fila }) => numero(fila.quantity) <= 0).length;
  return {
    texto: `Tenés ${plural(bajos.length, 'producto', 'productos')} con poco stock${agotados ? `, ${agotados === 1 ? '1 agotado' : `${agotados} agotados`}` : ''}:`,
    items: bajos.slice(0, 6).map((producto) => {
      const cantidad = numero(producto.fila.quantity);
      return {
        id: producto.fila.id,
        titulo: producto.nombre,
        detalle:
          cantidad <= 0
            ? `Agotado · mínimo ${numero(producto.fila.minimum)}`
            : `Quedan ${cantidad} · mínimo ${numero(producto.fila.minimum)}`,
        nivel: cantidad <= 0 ? 3 : 2,
        destino: destinoDe(producto, contexto.sistema),
      };
    }),
    sugerencias: ['¿Qué avisos tengo?', '¿Cuánto vendí?'],
  };
}

function ventasGenerales(pregunta: string, contexto: ContextoDeChat): Respuesta {
  const ventas = ventasDe(contexto.sistema, contexto.hoy);
  if (!ventas.length) {
    return {
      texto: 'Todavía no hay ventas anotadas en esta sección.',
      sugerencias: ['¿Qué me falta?'],
    };
  }
  const periodo = periodoDe(normalize(pregunta));
  const linea = (nombre: string, desde: number, hasta: number) => {
    const { total, unidades, cantidad } = suma(ventas, desde, hasta);
    return `${nombre}: ${pesos(total)} (${plural(unidades, 'unidad', 'unidades')} en ${plural(cantidad, 'venta', 'ventas')})`;
  };
  const lineas = periodo
    ? [
        periodo.dias === 7 || periodo.dias === 30
          ? linea(`Vendiste ${periodo.nombre}`, 0, periodo.dias - 1)
          : linea(`Vendiste ${periodo.nombre}`, periodo.dias, periodo.dias),
      ]
    : [linea('Hoy', 0, 0), linea('Últimos 7 días', 0, 6), linea('Últimos 30 días', 0, 29)];

  const productos = productosDe(bloques(contexto.sistema).inventarios);
  const porProducto = new Map<string, number>();
  for (const venta of ventas.filter((item) => item.dias >= 0 && item.dias < 30)) {
    porProducto.set(venta.productoId, (porProducto.get(venta.productoId) ?? 0) + venta.unidades);
  }
  const mas = [...porProducto.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .flatMap(([id, unidades]) => {
      const producto = productos.find((item) => item.fila.id === id);
      return producto
        ? [
            {
              id,
              titulo: producto.nombre,
              detalle: `${plural(unidades, 'unidad', 'unidades')} en 30 días`,
              destino: destinoDe(producto, contexto.sistema),
            } satisfies ItemDeChat,
          ]
        : [];
    });
  return {
    texto: `${lineas.join('\n')}${mas.length ? '\n\nLo que más salió:' : ''}`,
    items: mas,
    sugerencias: ['¿Qué avisos tengo?', '¿Qué me falta?'],
  };
}

function masVendidos(contexto: ContextoDeChat): Respuesta {
  const respuesta = ventasGenerales('mes', contexto);
  return {
    ...respuesta,
    texto: respuesta.items?.length ? 'Lo que más salió en los últimos 30 días:' : respuesta.texto,
  };
}

function deUnProducto(pregunta: string, producto: Producto, contexto: ContextoDeChat): Respuesta {
  const texto = normalize(pregunta);
  const cantidad = numero(producto.fila.quantity);
  const minimo = numero(producto.fila.minimum);
  const precio = numero(producto.fila.price);
  const costo = numero(producto.fila.cost);
  const vendidas = ventasDe(contexto.sistema, contexto.hoy)
    .filter((venta) => venta.productoId === producto.fila.id && venta.dias >= 0 && venta.dias < 30)
    .reduce((acumulado, venta) => acumulado + venta.unidades, 0);
  const item: ItemDeChat = {
    id: producto.fila.id,
    titulo: producto.nombre,
    nivel: cantidad <= 0 ? 3 : cantidad <= minimo ? 2 : undefined,
    destino: destinoDe(producto, contexto.sistema),
  };
  const delAviso = contexto.avisos.filter((aviso) => aviso.objeto?.filaId === producto.fila.id);
  const sugerencias = ['¿Qué me falta?', '¿Qué avisos tengo?'];

  if (tiene(texto, ['precio', 'cuesta', 'sale', 'costo', 'margen', 'ganancia'])) {
    const margen =
      costo > 0 && precio > 0
        ? ` Te deja ${pesos(precio - costo)} por unidad (${Math.round(((precio - costo) / precio) * 100)} %).`
        : '';
    return {
      texto: `${producto.nombre} está a ${pesos(precio)}${costo ? ` y te cuesta ${pesos(costo)}` : ''}.${margen}`,
      items: [item],
      sugerencias,
    };
  }
  if (tiene(texto, ['vend', 'venta', 'rotacion', 'salida'])) {
    return {
      texto: vendidas
        ? `De ${producto.nombre} vendiste ${plural(vendidas, 'unidad', 'unidades')} en los últimos 30 días (${(vendidas / 30).toFixed(1).replace('.', ',')} por día).`
        : `De ${producto.nombre} no anotaste ventas en los últimos 30 días.`,
      items: [item],
      sugerencias,
    };
  }
  const diario = vendidas / 30;
  const alcanza =
    diario > 0 && cantidad > 0
      ? ` Con lo que vendés, te alcanza para ${cantidad / diario < 1 ? 'menos de 1 día' : `unos ${Math.floor(cantidad / diario)} días`}.`
      : '';
  const estado = cantidad <= 0 ? 'No te queda nada' : `Te quedan ${cantidad}`;
  return {
    texto: `${producto.nombre}: ${estado}${minimo ? ` (tu mínimo es ${minimo})` : ''}.${alcanza}${delAviso.length ? ' Tiene un aviso para mirar.' : ''}`,
    items: [item],
    sugerencias,
  };
}

function deZona(contexto: ContextoDeChat): Respuesta {
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
    items: avisos.slice(0, 4).map((aviso) => ({
      id: aviso.id,
      titulo: aviso.titulo,
      detalle: aviso.detalle,
      nivel: aviso.prioridad,
      aviso,
      destino: destinoDeAviso(aviso) ?? undefined,
    })),
    verTodos: avisos.length > 4 ? avisos.length : undefined,
  };
}

export function ayuda(): Respuesta {
  return {
    texto:
      'Puedo contarte cómo viene tu sección. Probá preguntarme por el stock, las ventas, los avisos o un producto, por ejemplo «¿cuánta yerba me queda?».',
    sugerencias: SUGERENCIAS_INICIALES,
  };
}

export function saludo(contexto: ContextoDeChat): Respuesta {
  const cantidad = contexto.avisos.length;
  return {
    texto: cantidad
      ? `¡Hola! Estuve mirando «${contexto.sistema.name}» y encontré ${plural(cantidad, 'cosa', 'cosas')} para mirar. ¿Qué querés ver?`
      : `¡Hola! Estuve mirando «${contexto.sistema.name}» y por ahora está todo en orden. ¿Qué querés ver?`,
    sugerencias: SUGERENCIAS_INICIALES,
  };
}

/** Lo primero que dice Sami al abrir el chat: saluda y, si hay avisos, muestra los más importantes. */
export function inicio(contexto: ContextoDeChat): Respuesta {
  const bienvenida = saludo(contexto);
  if (!contexto.avisos.length) return bienvenida;
  const avisos = avisosDelSistema(contexto);
  return {
    ...avisos,
    texto: bienvenida.texto,
    items: avisos.items?.slice(0, 3),
    verTodos: contexto.avisos.length > 3 ? contexto.avisos.length : undefined,
  };
}

export function responder(pregunta: string, contexto: ContextoDeChat): Respuesta {
  const texto = normalize(pregunta).replace(/[^a-z0-9 ]+/g, ' ');
  const productos = productosDe(bloques(contexto.sistema).inventarios);
  const nombrados = productosNombrados(pregunta, productos);

  if (nombrados.length === 1 && nombrados[0]) return deUnProducto(pregunta, nombrados[0], contexto);
  if (nombrados.length > 1 && nombrados.length <= 4) {
    return {
      texto: `Encontré ${nombrados.length} productos que se parecen. ¿De cuál hablás?`,
      items: nombrados.map((producto) => ({
        id: producto.fila.id,
        titulo: producto.nombre,
        detalle: `Quedan ${numero(producto.fila.quantity)}`,
        destino: destinoDe(producto, contexto.sistema),
      })),
    };
  }
  if (tiene(texto, ['mas vendido', 'mas vende', 'mejor', 'estrella', 'top ', 'lo que mas']))
    return masVendidos(contexto);
  if (
    tiene(texto, [
      'aviso',
      'alerta',
      'notific',
      'novedad',
      'pendiente',
      'urgente',
      'importante',
      'resumen',
      'que pasa',
      'que hay',
      'revisar',
    ])
  ) {
    return avisosDelSistema(contexto);
  }
  if (tiene(texto, ['stock', 'falta', 'reponer', 'pedir', 'agot', 'quedan', 'queda']))
    return stockBajo(contexto);
  if (
    tiene(texto, ['vend', 'venta', 'factur', 'ingres', 'caja', 'recaud', 'cuanto gane', 'ganancia'])
  )
    return ventasGenerales(pregunta, contexto);
  if (tiene(texto, ['zona', 'competencia', 'caro', 'barato', 'precios'])) return deZona(contexto);
  if (tiene(texto, ['hola', 'buenas', 'ayuda', 'que podes', 'que sabes', 'como funciona']))
    return ayuda();
  return {
    texto:
      'No estoy segura de haber entendido. Puedo contarte del stock, las ventas, los avisos o de un producto en particular.',
    sugerencias: SUGERENCIAS_INICIALES,
  };
}
