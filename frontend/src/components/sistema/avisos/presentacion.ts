import { pesos, type AccionDeAviso, type Aviso, type TipoDeAviso } from '@/lib/sistema/alertas';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

export const ICONO_DE_AVISO: Record<TipoDeAviso, NombreDeIcono> = {
  stock: 'box',
  precio_zona: 'ubicacion',
  estancado: 'clock',
  muy_vendido: 'spark',
  combo: 'orders',
  consumo_zona: 'ubicacion',
  revisar_precio: 'search',
};

/** El nombre corto de cada tipo: lo primero que se lee para saber de qué se trata. */
export const NOMBRE_DE_TIPO: Record<TipoDeAviso, string> = {
  stock: 'Stock',
  precio_zona: 'Precio vs. zona',
  estancado: 'Sin movimiento',
  muy_vendido: 'Se vende mucho',
  combo: 'Combo',
  consumo_zona: 'Lo que pide la zona',
  revisar_precio: 'Revisar el precio',
};

export const NIVEL_DE_AVISO = { 3: 'Urgente', 2: 'Para hoy', 1: 'Para mirar' } as const;

/** Lo que dice el botón: la acción, tal cual («Subir a $ 3.820», «Registrar entrada»). */
export function textoDeAccion(accion: AccionDeAviso) {
  return accion.texto;
}

const numero = (valor: unknown) => {
  const convertido = typeof valor === 'string' ? Number(valor) : valor;
  return typeof convertido === 'number' && Number.isFinite(convertido) ? convertido : undefined;
};

export type DatoDelAviso = { etiqueta: string; valor: string; tono?: 'bueno' | 'malo' };

/** Los números del aviso listos para mostrar. Vacío cuando el aviso no los trae (los calculados en el navegador). */
export function datosDelAviso(aviso: Aviso): DatoDelAviso[] {
  const datos = aviso.datos ?? {};
  if (aviso.tipo === 'stock') {
    const cantidad = numero(datos.cantidad);
    const minimo = numero(datos.minimo);
    if (cantidad === undefined) return [];
    return [
      { etiqueta: 'Te quedan', valor: `${cantidad} u.`, tono: 'malo' },
      ...(minimo ? [{ etiqueta: 'Tu mínimo', valor: `${minimo} u.` }] : []),
    ];
  }
  if (aviso.tipo === 'estancado') {
    const precio = numero(datos.precio);
    const sugerido = numero(datos.sugerido);
    if (precio === undefined || sugerido === undefined) return [];
    const baja = precio > 0 ? Math.round(((precio - sugerido) / precio) * 100) : 0;
    return [
      { etiqueta: 'Lo tenés a', valor: pesos(precio), tono: 'malo' },
      { etiqueta: 'Precio sugerido', valor: pesos(sugerido), tono: 'bueno' },
      ...(baja > 0 ? [{ etiqueta: 'Rebaja', valor: `${baja} %` }] : []),
    ];
  }
  if (aviso.tipo === 'revisar_precio') {
    const anterior = numero(datos.precioAnterior);
    const nuevo = numero(datos.precioNuevo);
    if (anterior === undefined || nuevo === undefined) return [];
    return [
      { etiqueta: 'Precio anterior', valor: pesos(anterior), tono: 'bueno' },
      { etiqueta: 'Precio actual', valor: pesos(nuevo), tono: 'malo' },
    ];
  }
  return [];
}

/** Qué se propone hacer, en una línea y con números cuando se conocen. */
export function propuestaDelAviso(aviso: Aviso): string | undefined {
  const accion = aviso.acciones[0];
  if (!accion) return undefined;
  const datos = aviso.datos ?? {};
  if (aviso.tipo === 'estancado') {
    const precio = numero(datos.precio);
    const sugerido = numero(datos.sugerido);
    if (precio !== undefined && sugerido !== undefined) {
      return `Bajar el precio de ${pesos(precio)} a ${pesos(sugerido)}`;
    }
  }
  if (aviso.tipo === 'stock') {
    const pedir = /pedí ([^.]+)\./.exec(aviso.detalle)?.[1];
    if (pedir) return `Te conviene pedir ${pedir}`;
  }
  return accion.texto;
}

/** «hoy», «ayer» o «hace N días», para saber cuánto hace que el motor lo detectó. */
export function haceCuanto(creadoEn: string | undefined, ahora = new Date()): string | undefined {
  if (!creadoEn) return undefined;
  const fecha = new Date(creadoEn);
  if (Number.isNaN(fecha.getTime())) return undefined;
  const dias = Math.floor((ahora.getTime() - fecha.getTime()) / 86_400_000);
  if (dias <= 0) return 'hoy';
  if (dias === 1) return 'ayer';
  return `hace ${dias} días`;
}

/** «Hoy», «Ayer» o «Hace N días»: lo que lleva el reloj arriba de cada aviso. */
export function cuandoSeDetecto(creadoEn: string | undefined, ahora = new Date()) {
  const texto = haceCuanto(creadoEn, ahora);
  return texto ? texto.charAt(0).toUpperCase() + texto.slice(1) : undefined;
}

/** Lleva a un tono más serio los textos que llegan del motor de avisos («Para no quedarte corto, pedí 5.»). */
export function suavizarTexto(texto: string) {
  return texto.replace(
    /Para no quedarte corto, pedí ([^.]+)\./g,
    'Te conviene pedir $1 para no quedarte sin stock.',
  );
}

/**
 * Lo que sigue a la primera idea del aviso, para «Ver por qué»: no repite el resumen de la tarjeta ni lo que ya dice
 * la línea de propuesta («Te conviene pedir 5»). Vacío si no queda nada más para decir.
 */
export function restoDelAviso(detalle: string) {
  const [primera = '', ...resto] = suavizarTexto(detalle).split(/(?<=\.)\s+/);
  const sinPropuesta = resto.filter((frase) => !frase.startsWith('Te conviene pedir'));
  return resumenDelAviso(detalle) === primera
    ? sinPropuesta.join(' ')
    : [primera, ...sinPropuesta].join(' ');
}

const RAZON_POR_TIPO: Partial<Record<TipoDeAviso, RegExp>> = {
  muy_vendido: /^En tu zona,/,
  estancado: /^En tu zona,/,
  precio_zona: /^En tu zona,/,
  combo: /^Por separado/,
};

/**
 * La razón principal del aviso, en una línea, para «Por qué de este aviso». El resto de los motivos no se muestran, y
 * la aclaración de que los datos de la zona son de varios comercios va una sola vez al pie de la lista.
 */
export function razonDelAviso(aviso: Aviso): string | undefined {
  const patron = RAZON_POR_TIPO[aviso.tipo];
  const motivo = (patron && aviso.porque.find((texto) => patron.test(texto))) ?? aviso.porque[0];
  return motivo && suavizarTexto(motivo).replace(/\s*Es un dato agregado[^.]*\./, '');
}

export type GrupoDeAvisos = { clave: string; avisos: Aviso[] };

/** Junta los avisos del mismo tipo y nivel, de cualquier negocio, para que no se amontonen hacia abajo. */
export function agruparAvisos(avisos: Aviso[]): GrupoDeAvisos[] {
  const grupos = new Map<string, Aviso[]>();
  for (const aviso of avisos) {
    const clave = `${aviso.tipo}:${aviso.prioridad}`;
    grupos.set(clave, [...(grupos.get(clave) ?? []), aviso]);
  }
  return [...grupos].map(([clave, juntos]) => ({ clave, avisos: juntos }));
}

/** El título de varios avisos juntos: el número va adelante («5 productos bajo el mínimo»). */
export function tituloDeGrupo(tipo: TipoDeAviso, prioridad: number, cantidad: number) {
  const cuantos = (uno: string, varios: string) => `${cantidad} ${cantidad === 1 ? uno : varios}`;
  switch (tipo) {
    case 'stock':
      return prioridad === 3
        ? cuantos('producto sin stock', 'productos sin stock')
        : cuantos('producto bajo el mínimo', 'productos bajo el mínimo');
    case 'precio_zona':
      return prioridad === 2
        ? cuantos('producto casi al costo', 'productos casi al costo')
        : cuantos(
            'producto con precio distinto al de la zona',
            'productos con precio distinto al de la zona',
          );
    case 'estancado':
      return cuantos('producto caro que no se mueve', 'productos caros que no se mueven');
    case 'muy_vendido':
      return cuantos(
        'producto muy vendido con precio bajo',
        'productos muy vendidos con precio bajo',
      );
    case 'combo':
      return cuantos('combo para armar', 'combos para armar');
    case 'consumo_zona':
      return cuantos('producto de la zona que no tenés', 'productos de la zona que no tenés');
    case 'revisar_precio':
      return cuantos('precio para revisar', 'precios para revisar');
  }
}

/** La primera idea del aviso, en una línea o línea y media: el resto queda en «Ver por qué». */
export function resumenDelAviso(detalle: string) {
  const [primera = ''] = suavizarTexto(detalle).split(/(?<=\.)\s+/);
  if (primera.length <= 60) return primera;
  const corte = primera.search(/[;,]/);
  return corte >= 15 ? `${primera.slice(0, corte)}.` : primera;
}
