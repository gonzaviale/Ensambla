import { MAX_NOMBRE_CAMPO, type GuardarCampo, type TipoCampo } from '@/types/campo';

// Reglas del valor inicial que comparten el formulario del constructor y la API simulada (T-018).

const NUMERO = /^-?\d+(\.\d+)?$/;
const FECHA = /^(\d{4})-(\d{2})-(\d{2})$/;

function esFechaReal(texto: string) {
  const partes = FECHA.exec(texto);
  if (!partes) return false;
  const [anio, mes, dia] = partes.slice(1).map(Number) as [number, number, number];
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  return (
    fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia
  );
}

/** `valor` ya normalizado (sin espacios, punto decimal). */
export function esValorInicialValido(tipo: TipoCampo, valor: string, opciones: readonly string[]) {
  switch (tipo) {
    case 'TEXTO':
      return true;
    case 'NUMERO':
      return NUMERO.test(valor);
    case 'FECHA':
      return esFechaReal(valor);
    case 'SI_NO':
      return valor === 'true' || valor === 'false';
    case 'OPCION':
      return opciones.includes(valor);
    // Una conexión o un cálculo no tienen valor inicial: cada fila elige o calcula el suyo.
    case 'RELACION':
    case 'FORMULA':
      return false;
  }
}

const clave = (texto: string) => texto.trim().toLocaleLowerCase('es');

/** Primera opción repetida (sin distinguir mayúsculas), si hay. */
export function opcionRepetida(opciones: readonly string[]) {
  const vistas = new Set<string>();
  for (const opcion of opciones) {
    if (vistas.has(clave(opcion))) return opcion;
    vistas.add(clave(opcion));
  }
  return undefined;
}

export const mismoNombre = (a: string, b: string) => clave(a) === clave(b);

/** Lo que la persona escribe en el formulario: todo como texto, el número admite coma decimal. */
export type BorradorCampo = {
  nombre: string;
  tipo: TipoCampo;
  obligatorio: boolean;
  valorInicial: string;
  opciones: string[];
  /** Tabla con la que se conecta (sólo RELACION); vacío si todavía no se eligió. */
  bloqueRelacionadoId: string;
  /** Expresión del cálculo (sólo FORMULA), con las columnas citadas como {id}. */
  formula: string;
};

export type ErroresCampo = Partial<
  Record<'nombre' | 'tipo' | 'opciones' | 'valorInicial' | 'bloqueRelacionadoId' | 'formula', string>
>;

const MENSAJE_VALOR: Record<TipoCampo, string> = {
  TEXTO: 'Revisá el valor inicial.',
  NUMERO: 'Escribí un número, por ejemplo 12 o 12,5.',
  FECHA: 'Elegí una fecha válida.',
  OPCION: 'Elegí una de las opciones de la lista.',
  SI_NO: 'Elegí Sí o No.',
  RELACION: 'Una conexión no tiene valor inicial.',
  FORMULA: 'Un cálculo no tiene valor inicial.',
};

function normalizarValor(tipo: TipoCampo, texto: string) {
  const valor = tipo === 'TEXTO' ? texto : texto.trim();
  return tipo === 'NUMERO' ? valor.replace(',', '.') : valor;
}

/** Valida el borrador y lo convierte al cuerpo que espera el backend. */
export function prepararCampo(
  borrador: BorradorCampo,
): { ok: true; datos: GuardarCampo } | { ok: false; errores: ErroresCampo } {
  const errores: ErroresCampo = {};
  const nombre = borrador.nombre.trim();
  if (!nombre) errores.nombre = 'Escribí un nombre para la columna.';
  else if (nombre.length > MAX_NOMBRE_CAMPO)
    errores.nombre = `Usá hasta ${MAX_NOMBRE_CAMPO} caracteres.`;

  // Los renglones vacíos se descartan: no son opciones.
  const opciones =
    borrador.tipo === 'OPCION'
      ? borrador.opciones.map((opcion) => opcion.trim()).filter(Boolean)
      : [];
  if (borrador.tipo === 'OPCION') {
    const repetida = opcionRepetida(opciones);
    if (opciones.length === 0) errores.opciones = 'Agregá al menos una opción.';
    else if (repetida) errores.opciones = `La opción «${repetida}» está repetida.`;
  }

  const esRelacion = borrador.tipo === 'RELACION';
  const bloqueRelacionadoId = esRelacion ? borrador.bloqueRelacionadoId : '';
  if (esRelacion && !bloqueRelacionadoId)
    errores.bloqueRelacionadoId = 'Elegí la tabla con la que se conecta.';

  const esFormula = borrador.tipo === 'FORMULA';
  const formula = esFormula ? borrador.formula.trim() : '';
  if (esFormula && !formula) errores.formula = 'Armá el cálculo con al menos una columna.';

  const valor = esRelacion || esFormula ? '' : normalizarValor(borrador.tipo, borrador.valorInicial);
  const valorInicial = valor.trim() === '' ? null : valor;
  if (valorInicial !== null && !esValorInicialValido(borrador.tipo, valorInicial, opciones)) {
    errores.valorInicial = MENSAJE_VALOR[borrador.tipo];
  }

  if (Object.keys(errores).length > 0) return { ok: false, errores };
  return {
    ok: true,
    datos: {
      nombre,
      tipo: borrador.tipo,
      obligatorio: borrador.obligatorio,
      valorInicial,
      opciones,
      bloqueRelacionadoId: bloqueRelacionadoId || null,
      formula: formula || null,
    },
  };
}

/** Valor inicial guardado → texto para mostrar o editar (coma decimal, fecha dd/mm/aaaa, Sí/No). */
export function describirValorInicial(tipo: TipoCampo, valor: string | null) {
  if (valor === null || tipo === 'RELACION' || tipo === 'FORMULA') return null;
  if (tipo === 'SI_NO') return valor === 'true' ? 'Sí' : 'No';
  if (tipo === 'NUMERO') return valor.replace('.', ',');
  if (tipo === 'FECHA') {
    const partes = FECHA.exec(valor);
    return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : valor;
  }
  return valor;
}

const PIEZA_FORMULA = /\{[0-9a-f-]{36}\}|\d+(?:\.\d+)?|[-+*/()]/gi;

/** Las piezas de una fórmula (columnas, números, operadores y paréntesis), en orden. */
export function piezasDeFormula(formula: string) {
  return formula.match(PIEZA_FORMULA) ?? [];
}

const SIMBOLO_FORMULA: Record<string, string> = { '*': '×', '/': '÷', '-': '−' };

/** La fórmula como la lee una persona: «Cantidad» × «Precio». */
export function describirFormula(
  formula: string,
  campos: readonly { id: string; nombre: string }[],
) {
  return piezasDeFormula(formula)
    .map((pieza) => {
      if (pieza.startsWith('{')) {
        const nombre = campos.find((campo) => campo.id === pieza.slice(1, -1))?.nombre;
        return nombre ? `«${nombre}»` : '«columna eliminada»';
      }
      return (SIMBOLO_FORMULA[pieza] ?? pieza).replace('.', ',');
    })
    .join(' ');
}
