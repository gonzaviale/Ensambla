import { clone, id, normalize, validateRow } from './core';
import type { Bloque, Campo, ErroresFila, Sistema, TipoCampo, Valor } from './tipos';

// La revisión de lo que leyó una foto: los registros como tarjetas, con sus datos dudosos, y las
// columnas que el bloque no tiene (campos nuevos) para aceptar, renombrar, cambiar de tipo o descartar.
// Es lógica pura, sin React: el reductor y los cálculos se prueban solos.

/** Lo que necesita la revisión de una lectura (`Lectura` de `types/importacion` lo cumple). */
export type LecturaParaRevisar = {
  filas: Record<string, Valor>[];
  camposNuevos: Campo[];
  dudas: { fila: number; campo: string; motivo: string }[];
};

export type EstadoDeCampoNuevo = 'pendiente' | 'aceptado' | 'descartado';

export type CampoNuevo = {
  /** Clave con la que vienen sus valores en cada registro leído. */
  clave: string;
  label: string;
  tipo: TipoCampo;
  estado: EstadoDeCampoNuevo;
};

export type FilaRevisada = {
  id: string;
  valores: Record<string, Valor>;
  /** Datos dudosos que todavía nadie resolvió: clave del campo → por qué se duda. */
  dudas: Record<string, string>;
  /** La persona cambió algo: deja de ser un dato de ejemplo. */
  editada: boolean;
};

export type Revision = { filas: FilaRevisada[]; camposNuevos: CampoNuevo[] };

/** Un campo nuevo puede ser de cualquier tipo menos un vínculo con otro bloque: eso se arma en el editor. */
export const TIPOS_DE_CAMPO_NUEVO: TipoCampo[] = [
  'text',
  'textarea',
  'number',
  'currency',
  'date',
  'select',
  'email',
  'tel',
  'checkbox',
  'time',
];

export type AccionDeRevision =
  | { tipo: 'editar'; filaId: string; clave: string; valor: Valor }
  | { tipo: 'estaBien'; filaId: string; clave: string }
  | { tipo: 'quitarFila'; filaId: string }
  | { tipo: 'restaurarFila'; fila: FilaRevisada; indice: number }
  | { tipo: 'aceptarCampo'; clave: string }
  | { tipo: 'descartarCampo'; clave: string }
  | { tipo: 'reabrirCampo'; clave: string }
  | { tipo: 'renombrarCampo'; clave: string; label: string }
  | { tipo: 'cambiarTipoDeCampo'; clave: string; tipoDeCampo: TipoCampo };

export function iniciarRevision(lectura: LecturaParaRevisar): Revision {
  return {
    filas: lectura.filas.map((valores, indice) => ({
      id: `registro-${indice}`,
      valores: { ...valores },
      dudas: Object.fromEntries(
        lectura.dudas
          .filter((duda) => duda.fila === indice)
          .map((duda) => [duda.campo, duda.motivo]),
      ),
      editada: false,
    })),
    camposNuevos: lectura.camposNuevos.map((campo) => ({
      clave: campo.key,
      label: campo.label,
      tipo: campo.type,
      estado: 'pendiente',
    })),
  };
}

const sinClave = (dudas: Record<string, string>, clave: string) =>
  Object.fromEntries(Object.entries(dudas).filter(([otra]) => otra !== clave));

const cambiarFila = (
  revision: Revision,
  filaId: string,
  cambio: (fila: FilaRevisada) => FilaRevisada,
): Revision => ({
  ...revision,
  filas: revision.filas.map((fila) => (fila.id === filaId ? cambio(fila) : fila)),
});

const cambiarCampo = (
  revision: Revision,
  clave: string,
  cambio: (campo: CampoNuevo) => CampoNuevo,
): Revision => ({
  ...revision,
  camposNuevos: revision.camposNuevos.map((campo) =>
    campo.clave === clave ? cambio(campo) : campo,
  ),
});

export function reducirRevision(revision: Revision, accion: AccionDeRevision): Revision {
  switch (accion.tipo) {
    case 'editar':
      // Corregir un dato dudoso lo resuelve.
      return cambiarFila(revision, accion.filaId, (fila) => ({
        ...fila,
        valores: { ...fila.valores, [accion.clave]: accion.valor },
        dudas: sinClave(fila.dudas, accion.clave),
        editada: true,
      }));
    case 'estaBien':
      return cambiarFila(revision, accion.filaId, (fila) => ({
        ...fila,
        dudas: sinClave(fila.dudas, accion.clave),
      }));
    case 'quitarFila':
      return { ...revision, filas: revision.filas.filter((fila) => fila.id !== accion.filaId) };
    case 'restaurarFila': {
      const filas = [...revision.filas];
      filas.splice(accion.indice, 0, accion.fila);
      return { ...revision, filas };
    }
    case 'aceptarCampo':
      return cambiarCampo(revision, accion.clave, (campo) => ({ ...campo, estado: 'aceptado' }));
    case 'descartarCampo':
      return cambiarCampo(revision, accion.clave, (campo) => ({ ...campo, estado: 'descartado' }));
    case 'reabrirCampo':
      return cambiarCampo(revision, accion.clave, (campo) => ({ ...campo, estado: 'pendiente' }));
    case 'renombrarCampo':
      return cambiarCampo(revision, accion.clave, (campo) => ({ ...campo, label: accion.label }));
    case 'cambiarTipoDeCampo':
      return cambiarCampo(revision, accion.clave, (campo) => ({
        ...campo,
        tipo: accion.tipoDeCampo,
      }));
  }
}

/** Las opciones de una columna de tipo «Opciones»: los valores distintos que se leyeron. */
export function opcionesDeLaColumna(revision: Revision, clave: string): string[] {
  const valores = revision.filas
    .map((fila) => fila.valores[clave])
    .filter((valor): valor is string | number => valor != null && String(valor).trim() !== '')
    .map((valor) => String(valor).trim());
  return [...new Set(valores)];
}

/** El campo que se crearía en el bloque para una columna nueva. */
export function campoDeColumnaNueva(campo: CampoNuevo, revision: Revision): Campo {
  return {
    key: campo.clave,
    label: campo.label.trim(),
    type: campo.tipo,
    required: false,
    ...(campo.tipo === 'select' ? { options: opcionesDeLaColumna(revision, campo.clave) } : {}),
  };
}

/** Los campos con los que se muestra cada tarjeta: los del bloque y las columnas nuevas no descartadas. */
export function camposDeLasTarjetas(existentes: Campo[], revision: Revision): Campo[] {
  return [
    ...existentes,
    ...revision.camposNuevos
      .filter((campo) => campo.estado !== 'descartado')
      .map((campo) => campoDeColumnaNueva(campo, revision)),
  ];
}

/** Qué hay que corregir en el nombre de una columna nueva, o `undefined` si está bien. */
export function errorDeNombre(
  campo: CampoNuevo,
  revision: Revision,
  existentes: Campo[],
): string | undefined {
  const nombre = normalize(campo.label);
  if (!nombre) return 'Elegí un nombre para el campo.';
  const repetido =
    existentes.some((otro) => normalize(otro.label) === nombre) ||
    revision.camposNuevos.some(
      (otro) =>
        otro.clave !== campo.clave &&
        otro.estado !== 'descartado' &&
        normalize(otro.label) === nombre,
    );
  return repetido ? 'Ya hay un campo con ese nombre.' : undefined;
}

export const cantidadDeDudas = (revision: Revision) =>
  revision.filas.reduce((total, fila) => total + Object.keys(fila.dudas).length, 0);

export const camposSinDecidir = (revision: Revision) =>
  revision.camposNuevos.filter((campo) => campo.estado === 'pendiente');

/** Todo lo que impide incorporar los registros todavía (vacío si se puede). */
export function motivosPendientes(revision: Revision, existentes: Campo[]): string[] {
  const motivos: string[] = [];
  if (!revision.filas.length) motivos.push('Dejá al menos un registro.');
  if (camposSinDecidir(revision).length) {
    motivos.push('Aceptá o descartá los campos nuevos.');
  }
  const conNombreInvalido = revision.camposNuevos.some(
    (campo) => campo.estado === 'aceptado' && errorDeNombre(campo, revision, existentes),
  );
  if (conNombreInvalido) motivos.push('Corregí el nombre de los campos nuevos.');
  return motivos;
}

export type Incorporacion = {
  /** El bloque con los campos nuevos aceptados al final (con claves propias). */
  bloque: Bloque;
  /** Los registros por la clave de cada campo del bloque; las columnas descartadas no están. */
  filas: Record<string, Valor>[];
};

/**
 * Lo que se incorpora al confirmar: el bloque con las columnas aceptadas como campos nuevos y los registros
 * revisados. Los registros que el bloque ya tenía no se tocan: los campos nuevos quedan vacíos en ellos.
 */
export function aIncorporacion(
  destino: Bloque,
  revision: Revision,
  nuevaClave: () => string = id,
): Incorporacion {
  const aceptadas = revision.camposNuevos.filter((campo) => campo.estado === 'aceptado');
  const claves = new Map(aceptadas.map((campo) => [campo.clave, nuevaClave()]));
  const bloque = clone(destino);
  for (const campo of aceptadas) {
    bloque.fields.push({
      ...campoDeColumnaNueva(campo, revision),
      key: claves.get(campo.clave) ?? campo.clave,
    });
  }
  const filas = revision.filas.map((fila) => {
    const valores: Record<string, Valor> = {};
    for (const campo of destino.fields) valores[campo.key] = fila.valores[campo.key];
    for (const campo of aceptadas) {
      valores[claves.get(campo.clave) ?? campo.clave] = fila.valores[campo.clave];
    }
    if (fila.editada) valores.__example = false;
    return valores;
  });
  return { bloque, filas };
}

/**
 * Lo que hay que corregir en cada registro (por id de registro y clave de campo), mirando los campos con los
 * que se muestran las tarjetas. Un registro sin errores no aparece.
 */
export function erroresDeLosRegistros(
  destino: Bloque,
  revision: Revision,
  sistema: Sistema,
): Record<string, ErroresFila> {
  const bloque: Bloque = { ...destino, fields: camposDeLasTarjetas(destino.fields, revision) };
  const errores: Record<string, ErroresFila> = {};
  for (const fila of revision.filas) {
    const deLaFila = validateRow(bloque, fila.valores, sistema);
    if (Object.keys(deLaFila).length) errores[fila.id] = deLaFila;
  }
  return errores;
}
