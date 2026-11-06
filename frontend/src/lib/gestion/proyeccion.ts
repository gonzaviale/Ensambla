import { normalize } from '@/lib/sistema/core';
import { pesos, porcentaje } from './formato';
import {
  cuentaDelMes,
  GASTOS_FIJOS_MENSUALES,
  ventasDelPeriodo,
  type Cuenta,
  type DatosDelNegocio,
  type Producto,
} from './resultado';

// «¿Me conviene?»: una decisión cualquiera se traduce en supuestos sobre la cuenta del último mes (un precio, ventas
// de más, un producto nuevo, un gasto fijo, una compra grande) y se vuelve a hacer la misma cuenta de la ganancia
// real, así antes y después se comparan con la misma regla. Cada supuesto dice de dónde sale su número. Lo que la
// persona no puede saber (cuánto más vendería) no se le pregunta: se calcula desde cuánto le conviene.

/** De dónde sale el número de un supuesto. */
export type Origen = 'cuaderno' | 'zona' | 'supuesto' | 'vos';

type Comun = {
  id: string;
  origen: Origen;
  /** Por qué Sami eligió ese número. */
  nota?: string;
};

export type Supuesto = Comun &
  (
    | { tipo: 'precio'; productoId: string; cambio: number; sensibilidad: number }
    | {
        tipo: 'producto';
        nombre: string;
        costo: number;
        precio: number;
        unidadesPorDia: number;
        /** No se sabe cuánto se va a vender: se calcula desde cuántas unidades conviene. */
        incognita?: boolean;
      }
    | {
        tipo: 'ventas';
        etiqueta: string;
        /** Lo que se vende de más cada vez. */
        monto: number;
        vecesPorMes: number;
        /** «por día», «cada domingo»: cómo se dice el umbral. */
        cuando: string;
        incognita?: boolean;
      }
    | { tipo: 'gastoFijo'; concepto: string; mensual: number }
    | { tipo: 'inversion'; concepto: string; monto: number }
    /** Lo que pesa en la decisión pero no es plata: el tiempo, el cansancio, el espacio. */
    | { tipo: 'aparte'; texto: string }
  );

export type TipoDeSupuesto = Supuesto['tipo'];

export type Decision = {
  supuestos: Supuesto[];
  /** Sami reconoció el tipo de decisión; si no, armó la cuenta con lo básico para completar. */
  entendida: boolean;
};

export type Veredicto = 'conviene' | 'no-conviene' | 'parecido';

export type Umbral = {
  supuestoId: string;
  valor: number;
  /** «Te conviene si vendés más de $ 2.300 por día: un 6 % más que hoy». */
  frase: string;
};

export type Proyeccion = {
  antes: Cuenta;
  despues: Cuenta;
  /** Cuánto cambia la ganancia de verdad por mes. */
  diferencia: number;
  /** Lo que se pone una sola vez (compras grandes). */
  inversion: number;
  /** En cuántos días se recupera la inversión; `null` si no hay o no se recupera. */
  recuperoEnDias: number | null;
  veredicto: Veredicto;
  /** Lo que Sami explica, en frases cortas. */
  porQue: string[];
  /** El número que define la decisión, cuando hay algo que no se puede saber. */
  umbral: Umbral | null;
  /** Ganancia acumulada en los próximos 90 días, de a una semana. */
  serie: { dia: number; antes: number; despues: number }[];
};

/** Cuánto cae la venta por cada 1 % que sube el precio, según el tipo de producto (comercios parecidos de la zona). */
export function sensibilidadDe(nombre: string) {
  const texto = normalize(nombre);
  const basicos = [
    'yerba',
    'aceite',
    'harina',
    'azucar',
    'fideos',
    'arroz',
    'huevos',
    'pan',
    'leche',
    'sal',
  ];
  const bebidas = ['coca', 'sprite', 'cerveza', 'vino', 'fernet'];
  if (basicos.some((palabra) => texto.includes(palabra))) return 0.6;
  if (bebidas.some((palabra) => texto.includes(palabra))) return 1.3;
  return 0.9;
}

type Totales = { ventas: number; costo: number; unidadesPorProducto: Map<string, number> };

function totalesDelMes(datos: DatosDelNegocio, hoy: string): Totales {
  const porId = new Map(datos.productos.map((producto) => [producto.id, producto]));
  const unidadesPorProducto = new Map<string, number>();
  let ventas = 0;
  let costo = 0;
  for (const venta of ventasDelPeriodo(datos.ventas, hoy, 30)) {
    ventas += venta.precio * venta.cantidad;
    costo += (porId.get(venta.productoId)?.costo ?? 0) * venta.cantidad;
    unidadesPorProducto.set(
      venta.productoId,
      (unidadesPorProducto.get(venta.productoId) ?? 0) + venta.cantidad,
    );
  }
  return { ventas, costo, unidadesPorProducto };
}

type Aplicado = { ventas: number; costo: number; fijos: number; inversion: number };

/** La cuenta del mes con todos los supuestos puestos. */
function aplicar(datos: DatosDelNegocio, base: Totales, supuestos: Supuesto[]): Aplicado {
  const proporcionDeCosto = base.ventas > 0 ? base.costo / base.ventas : 0;
  const total: Aplicado = {
    ventas: base.ventas,
    costo: base.costo,
    fijos: GASTOS_FIJOS_MENSUALES,
    inversion: 0,
  };
  for (const supuesto of supuestos) {
    if (supuesto.tipo === 'precio') {
      const producto = datos.productos.find((item) => item.id === supuesto.productoId);
      const unidades = producto ? (base.unidadesPorProducto.get(producto.id) ?? 0) : 0;
      if (!producto || unidades === 0) continue;
      const unidadesNuevas = Math.max(0, unidades * (1 - supuesto.sensibilidad * supuesto.cambio));
      total.ventas +=
        unidadesNuevas * producto.precio * (1 + supuesto.cambio) - unidades * producto.precio;
      total.costo += (unidadesNuevas - unidades) * producto.costo;
    }
    if (supuesto.tipo === 'producto') {
      const mensuales = supuesto.unidadesPorDia * 30;
      total.ventas += mensuales * supuesto.precio;
      total.costo += mensuales * supuesto.costo;
    }
    if (supuesto.tipo === 'ventas') {
      const extra = supuesto.monto * supuesto.vecesPorMes;
      // Lo que se vende de más tiene la mezcla de siempre: el mismo costo sobre lo vendido.
      total.ventas += extra;
      total.costo += extra * proporcionDeCosto;
    }
    if (supuesto.tipo === 'gastoFijo') total.fijos += supuesto.mensual;
    if (supuesto.tipo === 'inversion') total.inversion += supuesto.monto;
  }
  return total;
}

/** Con una compra grande, la decisión se mira a un año: lo que deja por mes contra lo que se pone de una vez. */
const MESES_PARA_RECUPERAR = 12;

const conValor = (supuesto: Supuesto, valor: number): Supuesto =>
  supuesto.tipo === 'ventas'
    ? { ...supuesto, monto: valor }
    : supuesto.tipo === 'producto'
      ? { ...supuesto, unidadesPorDia: valor }
      : supuesto;

/**
 * El valor del supuesto desconocido desde el que la decisión conviene. La ganancia es lineal en ese valor, así que
 * alcanza con dos cuentas para despejarlo.
 */
function umbralDe(
  datos: DatosDelNegocio,
  base: Totales,
  supuestos: Supuesto[],
  antes: Cuenta,
): Umbral | null {
  const incognita = supuestos.find(
    (supuesto) =>
      (supuesto.tipo === 'ventas' || supuesto.tipo === 'producto') && supuesto.incognita,
  );
  if (!incognita) return null;
  const ganancia = (valor: number) => {
    const lista = supuestos.map((supuesto) =>
      supuesto.id === incognita.id ? conValor(supuesto, valor) : supuesto,
    );
    const total = aplicar(datos, base, lista);
    return {
      mensual: cuentaDelMes(total.ventas, total.costo, total.fijos).ganancia - antes.ganancia,
      inversion: total.inversion,
    };
  };
  const cero = ganancia(0);
  const paso = incognita.tipo === 'ventas' ? 1000 : 1;
  const pendiente = (ganancia(paso).mensual - cero.mensual) / paso;
  const objetivo = cero.inversion / MESES_PARA_RECUPERAR;
  if (pendiente <= 0) return null;
  const valor = Math.max(0, (objetivo - cero.mensual) / pendiente);

  if (incognita.tipo === 'producto') {
    const frase =
      valor === 0
        ? `Cada ${incognita.nombre || 'unidad'} que vendas te deja ganancia: conviene desde la primera.`
        : `Te conviene si vendés más de ${Math.ceil(valor * 10) / 10} por día.`;
    return { supuestoId: incognita.id, valor, frase };
  }
  if (incognita.tipo !== 'ventas') return null;
  const porDia = antes.ventas / 30;
  const comparado =
    incognita.vecesPorMes >= 30 && porDia > 0
      ? `: un ${porcentaje(valor / porDia)} más que hoy`
      : '';
  const frase =
    valor === 0
      ? 'Te conviene aunque no vendas nada de más.'
      : `Te conviene si vendés más de ${pesos(Math.ceil(valor / 100) * 100)} ${incognita.cuando}${comparado}.`;
  return { supuestoId: incognita.id, valor, frase };
}

function explicar(datos: DatosDelNegocio, base: Totales, supuesto: Supuesto, antes: Cuenta) {
  const semana = (valor: number) => Math.round((valor / 30) * 7);
  switch (supuesto.tipo) {
    case 'precio': {
      const producto = datos.productos.find((item) => item.id === supuesto.productoId);
      const unidades = producto ? (base.unidadesPorProducto.get(producto.id) ?? 0) : 0;
      if (!producto || unidades === 0)
        return 'Ese producto no se vendió en el último mes: cambiarle el precio no mueve la cuenta.';
      const nuevas = Math.max(0, unidades * (1 - supuesto.sensibilidad * supuesto.cambio));
      return `${producto.nombre}: hoy vendés unas ${semana(unidades)} por semana a ${pesos(producto.precio)}. A ${pesos(producto.precio * (1 + supuesto.cambio))} calculo unas ${semana(nuevas)}.`;
    }
    case 'producto': {
      const primeraCompra = supuesto.costo * supuesto.unidadesPorDia * 7;
      return `Cada ${supuesto.nombre || 'unidad'} te deja ${pesos(supuesto.precio - supuesto.costo)} antes de comisiones y pérdidas. La primera compra (una semana) es de ${pesos(primeraCompra)}.`;
    }
    case 'ventas':
      return supuesto.monto > 0
        ? `${supuesto.etiqueta}: ${pesos(supuesto.monto * supuesto.vecesPorMes)} más por mes, con el mismo margen que lo que ya vendés.`
        : null;
    case 'gastoFijo':
      return `${supuesto.concepto}: tus gastos fijos suben ${pesos(supuesto.mensual)} por mes. De cada $ 100 que vendés te quedan ${pesos(antes.contribucion * 100)} para pagarlos.`;
    case 'inversion':
      return `${supuesto.concepto}: ponés ${pesos(supuesto.monto)} una sola vez.`;
    case 'aparte':
      return null;
  }
}

export function proyectar(datos: DatosDelNegocio, hoy: string, supuestos: Supuesto[]): Proyeccion {
  const base = totalesDelMes(datos, hoy);
  const antes = cuentaDelMes(base.ventas, base.costo, GASTOS_FIJOS_MENSUALES);
  const total = aplicar(datos, base, supuestos);
  const despues = cuentaDelMes(total.ventas, total.costo, total.fijos);
  const diferencia = despues.ganancia - antes.ganancia;
  const { inversion } = total;
  const recuperoEnDias =
    inversion > 0 && diferencia > 0 ? Math.ceil(inversion / (diferencia / 30)) : null;

  // Con una inversión, se compara lo que deja en un año contra lo que se pone.
  const resultado = inversion > 0 ? diferencia * MESES_PARA_RECUPERAR - inversion : diferencia;
  const umbralDeParecido = Math.max(1000, Math.abs(antes.ganancia) * 0.02);
  const veredicto: Veredicto =
    resultado > umbralDeParecido
      ? 'conviene'
      : resultado < -umbralDeParecido
        ? 'no-conviene'
        : 'parecido';

  const porQue = supuestos
    .map((supuesto) => explicar(datos, base, supuesto, antes))
    .filter((frase): frase is string => Boolean(frase));
  if (inversion > 0)
    porQue.push(
      recuperoEnDias === null
        ? `Con lo que deja por mes, los ${pesos(inversion)} no se recuperan.`
        : `Recuperás los ${pesos(inversion)} en unos ${recuperoEnDias} días.`,
    );
  porQue.push(
    veredicto === 'parecido'
      ? 'La ganancia queda casi igual: decidí por lo que no es plata (tiempo, clientes, espacio).'
      : `Tu ganancia de verdad ${diferencia > 0 ? 'sube' : 'baja'} ${pesos(Math.abs(diferencia))} por mes.`,
  );

  const serie = Array.from({ length: 14 }, (_, semana) => {
    const dia = semana * 7;
    return {
      dia,
      antes: (antes.ganancia / 30) * dia,
      despues: (despues.ganancia / 30) * dia - inversion,
    };
  });

  return {
    antes,
    despues,
    diferencia,
    inversion,
    recuperoEnDias,
    veredicto,
    porQue,
    umbral: umbralDe(datos, base, supuestos, antes),
    serie,
  };
}

/** «350 mil», «350.000», «$60000» o «10%» como número; `null` si no hay. */
function numeroEn(texto: string): number | null {
  const encontrado = texto.match(/(\d+(?:[.,]\d+)*)\s*(mil|k)?/);
  if (!encontrado?.[1]) return null;
  const crudo = encontrado[1];
  // «2,5» es decimal; «350.000» o «1,200» son miles.
  const valor = /^\d+[.,]\d{3}(?:[.,]\d{3})*$/.test(crudo)
    ? Number(crudo.replace(/[.,]/g, ''))
    : Number(crudo.replace(',', '.'));
  return encontrado[2] ? valor * 1000 : valor;
}

/** El producto que nombra el texto, por la primera palabra de su nombre («coca», «yerba»). */
function productoNombrado(texto: string, productos: Producto[]) {
  return productos.find((producto) => {
    const primera = normalize(producto.nombre).split(/[\s-]+/)[0] ?? '';
    return primera.length > 2 && texto.includes(primera);
  });
}

let siguiente = 0;
/** Un id para cada supuesto nuevo: sólo tiene que ser distinto dentro de la decisión. */
export const nuevoId = () => `s${++siguiente}`;

/** Cuánto vende por día un producto típico del cuaderno: la mediana de los que se vendieron en el mes. */
function ritmoTipico(datos: DatosDelNegocio, hoy: string) {
  const { unidadesPorProducto } = totalesDelMes(datos, hoy);
  const ritmos = [...unidadesPorProducto.values()]
    .map((unidades) => unidades / 30)
    .sort((a, b) => a - b);
  const medio = ritmos[Math.floor(ritmos.length / 2)] ?? 1;
  return Math.max(0.5, Math.round(medio * 2) / 2);
}

const ventasPorDia = (datos: DatosDelNegocio, hoy: string) => totalesDelMes(datos, hoy).ventas / 30;

/** El supuesto de cambiar el precio de un producto, con la sensibilidad de comercios parecidos. */
export const supuestoDePrecio = (producto: Producto, cambio: number): Supuesto => ({
  id: nuevoId(),
  tipo: 'precio',
  productoId: producto.id,
  cambio,
  sensibilidad: sensibilidadDe(producto.nombre),
  origen: 'zona',
  nota: 'El precio de hoy sale de tu cuaderno. Cuánto cae la venta, de comercios parecidos de tu zona: todavía no cambiaste este precio.',
});

/** Un supuesto nuevo del tipo pedido, para sumar a mano a cualquier decisión. */
export function supuestoNuevo(tipo: TipoDeSupuesto, datos: DatosDelNegocio, hoy: string): Supuesto {
  const id = nuevoId();
  switch (tipo) {
    case 'precio': {
      const producto = datos.productos[0];
      return producto
        ? supuestoDePrecio(producto, 0.1)
        : { id, tipo: 'aparte', texto: 'No hay productos en tu cuaderno.', origen: 'cuaderno' };
    }
    case 'producto':
      return {
        id,
        tipo,
        nombre: 'Producto nuevo',
        costo: 1000,
        precio: 1500,
        unidadesPorDia: ritmoTipico(datos, hoy),
        incognita: true,
        origen: 'supuesto',
        nota: 'Las unidades son lo que vende un producto típico tuyo. El costo y el precio, cambialos.',
      };
    case 'ventas':
      return {
        id,
        tipo,
        etiqueta: 'Ventas de más por día',
        monto: 0,
        vecesPorMes: 30,
        cuando: 'por día',
        incognita: true,
        origen: 'supuesto',
        nota: 'No lo sabés de antemano: te calculo desde cuánto conviene.',
      };
    case 'gastoFijo':
      return { id, tipo, concepto: 'Un gasto nuevo', mensual: 10000, origen: 'supuesto' };
    case 'inversion':
      return { id, tipo, concepto: 'Una compra grande', monto: 100000, origen: 'supuesto' };
    case 'aparte':
      return { id, tipo, texto: 'Algo que no es plata', origen: 'vos' };
  }
}

const aparteDelTiempo = (texto: string): Supuesto => ({
  id: nuevoId(),
  tipo: 'aparte',
  texto,
  origen: 'supuesto',
});

/**
 * Lo que entiende Sami de una pregunta escrita, como supuestos sobre la cuenta. En el mockup es por palabras; en el
 * producto lo haría la IA, que puede armar cualquier combinación de supuestos. Si no reconoce la decisión, arma la
 * cuenta con lo básico (ventas de más y un gasto) para que la persona la complete.
 */
export function interpretarPregunta(
  pregunta: string,
  datos: DatosDelNegocio,
  hoy: string,
): Decision {
  const texto = normalize(pregunta);
  const producto = productoNombrado(texto, datos.productos);
  const numero = numeroEn(texto);
  const porDia = ventasPorDia(datos, hoy);
  const ventasDeMas = (): Supuesto => supuestoNuevo('ventas', datos, hoy);

  if (/domingo|feriado|horario|abrir mas|abro mas|mas horas|de noche|24 ?h/.test(texto)) {
    const dia = /domingo/.test(texto) ? 'domingo' : 'día extra';
    return {
      entendida: true,
      supuestos: [
        {
          id: nuevoId(),
          tipo: 'ventas',
          etiqueta: `Lo que vendés un ${dia}`,
          monto: Math.round((porDia * 0.7) / 100) * 100,
          vecesPorMes: 4,
          cuando: `cada ${dia}`,
          incognita: true,
          origen: 'supuesto',
          nota: `Un 70 % de un día común (de tu cuaderno: ${pesos(porDia)} por día).`,
        },
        {
          id: nuevoId(),
          tipo: 'gastoFijo',
          concepto: 'Luz y gastos de abrir',
          mensual: numero ?? 4000,
          origen: numero ? 'vos' : 'supuesto',
        },
        aparteDelTiempo(`Tu tiempo: 4 ${dia}s por mes atendiendo.`),
      ],
    };
  }
  if (/emplead|ayudante|sueldo|contrat|tomo a|tomar a|una persona/.test(texto)) {
    return {
      entendida: true,
      supuestos: [
        {
          id: nuevoId(),
          tipo: 'gastoFijo',
          concepto: 'Sueldo del ayudante',
          mensual: numero ?? 60000,
          origen: numero ? 'vos' : 'supuesto',
        },
        {
          ...ventasDeMas(),
          nota: 'Con alguien más quizás atendés más rápido o abrís más horas. No lo sabés de antemano: te calculo desde cuánto conviene.',
        },
        aparteDelTiempo('Tu tiempo libre: el ayudante te lo devuelve.'),
      ],
    };
  }
  if (/alquiler|local|mudar|mudo/.test(texto)) {
    return {
      entendida: true,
      supuestos: [
        {
          id: nuevoId(),
          tipo: 'gastoFijo',
          concepto: 'Más alquiler',
          mensual: numero ?? 50000,
          origen: numero ? 'vos' : 'supuesto',
        },
        {
          ...ventasDeMas(),
          nota: 'Un local mejor ubicado puede vender más. No lo sabés de antemano: te calculo desde cuánto conviene.',
        },
      ],
    };
  }
  if (/delivery|reparto|envio|envios|moto/.test(texto)) {
    return {
      entendida: true,
      supuestos: [
        {
          id: nuevoId(),
          tipo: 'gastoFijo',
          concepto: 'Nafta y repartidor',
          mensual: numero ?? 40000,
          origen: numero ? 'vos' : 'supuesto',
        },
        {
          ...ventasDeMas(),
          nota: 'Los pedidos con envío. No lo sabés de antemano: te calculo desde cuánto conviene.',
        },
      ],
    };
  }
  if (
    /heladera|freezer|maquina|balanza|posnet|comprar un|comprar una|compro un|compro una|invert/.test(
      texto,
    )
  ) {
    const concepto =
      pregunta.match(
        /(?:comprar|compro|invertir en)\s+(?:un|una)?\s*(.+?)(?:\s+(?:de|por|a)\s+\$?\d.*)?[?.!]*$/i,
      )?.[1] ?? 'La compra';
    return {
      entendida: true,
      supuestos: [
        {
          id: nuevoId(),
          tipo: 'inversion',
          concepto: concepto.charAt(0).toUpperCase() + concepto.slice(1),
          monto: numero ?? 300000,
          origen: numero ? 'vos' : 'supuesto',
        },
        {
          ...ventasDeMas(),
          nota: 'Lo que te deja vender que hoy no podés. No lo sabés de antemano: te calculo desde cuánto conviene.',
        },
      ],
    };
  }
  if (producto && /%|precio|subo|subir|bajo|bajar|aument|descuento|cobro/.test(texto)) {
    const cambio = (numero ?? 10) / 100;
    const baja = /bajo|bajar|descuento|rebaj/.test(texto);
    return { entendida: true, supuestos: [supuestoDePrecio(producto, baja ? -cambio : cambio)] };
  }
  if (/vender|sumar|agregar|traer|empiezo a|empezar a/.test(texto)) {
    const nombre =
      pregunta.match(
        /(?:vender|sumar|agregar|traer|empiezo a vender|empezar a vender)\s+(.+?)[?.!]*$/i,
      )?.[1] ?? 'Producto nuevo';
    const nuevo = supuestoNuevo('producto', datos, hoy);
    return {
      entendida: true,
      supuestos: [nuevo.tipo === 'producto' ? { ...nuevo, nombre } : nuevo],
    };
  }
  return {
    entendida: false,
    supuestos: [
      ventasDeMas(),
      {
        id: nuevoId(),
        tipo: 'gastoFijo',
        concepto: 'Lo que te cuesta por mes',
        mensual: 0,
        origen: 'supuesto',
      },
    ],
  };
}
