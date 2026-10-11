import { esValorInicialValido, mismoNombre, opcionRepetida } from '@/lib/campos';
import { MAX_NOMBRE_CAMPO, type TipoCampo } from '@/types/campo';
import {
  UMBRAL_CONFIANZA,
  type ConfirmarLectura,
  type PropuestaLectura,
  type ValorConfirmado,
} from '@/types/lectura';

// Lo que la persona va corrigiendo antes de guardar. Todo vive en el navegador: nada llega al
// servidor hasta confirmar (H-30). Los valores se editan como texto y se tipan recién al enviar.

export type ColumnaRevision = {
  clave: string;
  nombre: string;
  tipo: TipoCampo;
  /** Una por renglón tal como se escribe; los vacíos se descartan al validar y al enviar. */
  opciones: string[];
  /** Campo del bloque existente que le corresponde; sólo cuenta al sumar las filas a ese bloque. */
  campoExistenteId: string | null;
};

export type CeldaRevision = {
  texto: string;
  /** null en lo que se agregó a mano: ahí no hay nada que dudar. */
  confianza: number | null;
  /** Una celda corregida o ya mirada deja de figurar como duda. */
  revisada: boolean;
};

export type FilaRevision = {
  id: string;
  /** Página del cuaderno (desde 1); null en las filas agregadas a mano. */
  pagina: number | null;
  celdas: Record<string, CeldaRevision>;
};

export type BorradorRevision = {
  nombreBloque: string;
  columnas: ColumnaRevision[];
  filas: FilaRevision[];
};

export type Destino =
  | { modo: 'nuevo'; nombreProyecto: string }
  | { modo: 'proyecto'; proyectoId: string }
  | { modo: 'bloque'; bloqueId: string };

export type PosicionCelda = { filaId: string; clave: string };

export type AccionRevision =
  | { tipo: 'editarCelda'; filaId: string; clave: string; texto: string }
  | { tipo: 'marcarRevisada'; filaId: string; clave: string }
  | { tipo: 'quitarFila'; filaId: string }
  | { tipo: 'agregarFila'; id: string }
  | { tipo: 'renombrarBloque'; nombre: string }
  | {
      tipo: 'editarColumna';
      clave: string;
      cambios: Partial<Pick<ColumnaRevision, 'nombre' | 'tipo' | 'opciones'>>;
    }
  | { tipo: 'quitarColumna'; clave: string }
  | { tipo: 'agregarColumna' };

const MAX_TEXTO = 1000;

/** Id del control de una celda: sirve para llevarle el foco desde «Siguiente duda». */
export const idCelda = ({ filaId, clave }: PosicionCelda) => `celda-${filaId}-${clave}`;

/** Clave de los errores que devuelve el servidor, ubicados por fila y columna. */
export const claveErrorServidor = (filaId: string, clave: string) => `${filaId}:${clave}`;

const MENSAJE_CELDA: Record<Exclude<TipoCampo, 'TEXTO'>, string> = {
  NUMERO: 'Escribí un número, por ejemplo 12 o 12,5.',
  FECHA: 'Elegí una fecha válida.',
  OPCION: 'Elegí una de las opciones.',
  SI_NO: 'Elegí Sí o No.',
};

const celdaVacia = (): CeldaRevision => ({ texto: '', confianza: null, revisada: true });

export function iniciarBorrador(
  propuesta: PropuestaLectura,
  nuevoId: () => string = () => crypto.randomUUID(),
): BorradorRevision {
  const columnas: ColumnaRevision[] = propuesta.campos.map((campo) => ({
    clave: campo.clave,
    nombre: campo.nombre,
    tipo: campo.tipo,
    opciones: campo.opciones ?? [],
    campoExistenteId: propuesta.mapeoCampos?.[campo.clave] ?? null,
  }));
  const filas = propuesta.filas.map((fila) => ({
    id: nuevoId(),
    pagina: fila.pagina,
    celdas: Object.fromEntries(
      columnas.map((columna) => {
        const dato = fila.datos[columna.clave];
        const celda: CeldaRevision = {
          texto: dato?.valor ?? '',
          confianza: dato ? dato.confianza : 0,
          revisada: false,
        };
        return [columna.clave, celda];
      }),
    ),
  }));
  return { nombreBloque: propuesta.nombre?.trim() || 'Datos del cuaderno', columnas, filas };
}

function conCelda(
  borrador: BorradorRevision,
  filaId: string,
  clave: string,
  cambiar: (celda: CeldaRevision) => CeldaRevision,
): BorradorRevision {
  return {
    ...borrador,
    filas: borrador.filas.map((fila) => {
      const celda = fila.celdas[clave];
      if (fila.id !== filaId || !celda) return fila;
      return { ...fila, celdas: { ...fila.celdas, [clave]: cambiar(celda) } };
    }),
  };
}

function siguienteNombreLibre(columnas: ColumnaRevision[]) {
  let numero = columnas.length + 1;
  while (columnas.some((columna) => mismoNombre(columna.nombre, `Columna ${numero}`))) numero += 1;
  return numero;
}

export function reducirBorrador(
  borrador: BorradorRevision,
  accion: AccionRevision,
): BorradorRevision {
  switch (accion.tipo) {
    case 'editarCelda':
      return conCelda(borrador, accion.filaId, accion.clave, (celda) => ({
        ...celda,
        texto: accion.texto,
        revisada: true,
      }));
    case 'marcarRevisada':
      return conCelda(borrador, accion.filaId, accion.clave, (celda) => ({
        ...celda,
        revisada: true,
      }));
    case 'quitarFila':
      return { ...borrador, filas: borrador.filas.filter((fila) => fila.id !== accion.filaId) };
    case 'agregarFila':
      return {
        ...borrador,
        filas: [
          ...borrador.filas,
          {
            id: accion.id,
            pagina: null,
            celdas: Object.fromEntries(borrador.columnas.map((c) => [c.clave, celdaVacia()])),
          },
        ],
      };
    case 'renombrarBloque':
      return { ...borrador, nombreBloque: accion.nombre };
    case 'editarColumna':
      return {
        ...borrador,
        columnas: borrador.columnas.map((columna) =>
          columna.clave === accion.clave ? { ...columna, ...accion.cambios } : columna,
        ),
      };
    case 'quitarColumna':
      return {
        ...borrador,
        columnas: borrador.columnas.filter((columna) => columna.clave !== accion.clave),
        filas: borrador.filas.map((fila) => ({
          ...fila,
          celdas: Object.fromEntries(
            Object.entries(fila.celdas).filter(([clave]) => clave !== accion.clave),
          ),
        })),
      };
    case 'agregarColumna': {
      const numero = siguienteNombreLibre(borrador.columnas);
      let clave = `columna_${numero}`;
      while (borrador.columnas.some((columna) => columna.clave === clave)) clave += '_';
      const columna: ColumnaRevision = {
        clave,
        nombre: `Columna ${numero}`,
        tipo: 'TEXTO',
        opciones: [],
        campoExistenteId: null,
      };
      return {
        ...borrador,
        columnas: [...borrador.columnas, columna],
        filas: borrador.filas.map((fila) => ({
          ...fila,
          celdas: { ...fila.celdas, [clave]: celdaVacia() },
        })),
      };
    }
  }
}

export const opcionesLimpias = (columna: ColumnaRevision) =>
  columna.opciones.map((opcion) => opcion.trim()).filter(Boolean);

export function errorDeColumna(columna: ColumnaRevision, columnas: ColumnaRevision[]) {
  const nombre = columna.nombre.trim();
  if (!nombre) return 'Escribí un nombre para la columna.';
  if (nombre.length > MAX_NOMBRE_CAMPO) return `Usá hasta ${MAX_NOMBRE_CAMPO} caracteres.`;
  if (columnas.some((otra) => otra.clave !== columna.clave && mismoNombre(otra.nombre, nombre))) {
    return 'Ya hay otra columna con este nombre.';
  }
  if (columna.tipo === 'OPCION') {
    const opciones = opcionesLimpias(columna);
    if (opciones.length === 0) return 'Agregá al menos una opción.';
    const repetida = opcionRepetida(opciones);
    if (repetida) return `La opción «${repetida}» está repetida.`;
  }
  return undefined;
}

export function errorDeCelda(columna: ColumnaRevision, celda: CeldaRevision) {
  const texto = celda.texto.trim();
  if (texto === '') return undefined;
  if (columna.tipo === 'TEXTO') {
    return texto.length > MAX_TEXTO ? `Usá hasta ${MAX_TEXTO} caracteres.` : undefined;
  }
  const normalizado = columna.tipo === 'NUMERO' ? texto.replace(',', '.') : texto;
  const valido = esValorInicialValido(columna.tipo, normalizado, opcionesLimpias(columna));
  return valido ? undefined : MENSAJE_CELDA[columna.tipo];
}

/** Dato que la IA no leyó con seguridad y todavía nadie miró. */
export function esDudosa(celda: CeldaRevision) {
  if (celda.revisada || celda.confianza === null) return false;
  return celda.texto.trim() === '' || celda.confianza < UMBRAL_CONFIANZA;
}

const pendiente = (columna: ColumnaRevision, celda: CeldaRevision) =>
  esDudosa(celda) || errorDeCelda(columna, celda) !== undefined;

function celdasEnOrden(borrador: BorradorRevision) {
  return borrador.filas.flatMap((fila) =>
    borrador.columnas.flatMap((columna) => {
      const celda = fila.celdas[columna.clave];
      return celda ? [{ filaId: fila.id, clave: columna.clave, columna, celda }] : [];
    }),
  );
}

export function cantidadPorRevisar(borrador: BorradorRevision) {
  return celdasEnOrden(borrador).filter(({ columna, celda }) => pendiente(columna, celda)).length;
}

/** La próxima celda dudosa o inválida después de `desde`; al llegar al final vuelve a empezar. */
export function siguienteDuda(
  borrador: BorradorRevision,
  desde?: PosicionCelda,
): PosicionCelda | undefined {
  const celdas = celdasEnOrden(borrador);
  const indice = desde
    ? celdas.findIndex((c) => c.filaId === desde.filaId && c.clave === desde.clave)
    : -1;
  const ordenadas = [...celdas.slice(indice + 1), ...celdas.slice(0, indice + 1)];
  const siguiente = ordenadas.find(({ columna, celda }) => pendiente(columna, celda));
  return siguiente && { filaId: siguiente.filaId, clave: siguiente.clave };
}

export function erroresDeEstructura(borrador: BorradorRevision) {
  const errores: string[] = [];
  if (!borrador.nombreBloque.trim()) errores.push('Escribí un nombre para la tabla.');
  if (borrador.columnas.length === 0) errores.push('Dejá al menos una columna.');
  if (borrador.columnas.some((columna) => errorDeColumna(columna, borrador.columnas))) {
    errores.push('Hay columnas para corregir.');
  }
  return errores;
}

export function cantidadInvalidas(borrador: BorradorRevision) {
  return celdasEnOrden(borrador).filter(({ columna, celda }) => errorDeCelda(columna, celda))
    .length;
}

export function puedeGuardar(borrador: BorradorRevision) {
  return erroresDeEstructura(borrador).length === 0 && cantidadInvalidas(borrador) === 0;
}

function valorTipado(columna: ColumnaRevision, celda: CeldaRevision): ValorConfirmado | undefined {
  const texto = celda.texto.trim();
  if (texto === '') return undefined;
  if (columna.tipo === 'NUMERO') return Number(texto.replace(',', '.'));
  if (columna.tipo === 'SI_NO') return texto === 'true';
  return texto;
}

export function aConfirmacion(borrador: BorradorRevision, destino: Destino): ConfirmarLectura {
  const sumaAlBloque = destino.modo === 'bloque';
  return {
    proyectoId: destino.modo === 'proyecto' ? destino.proyectoId : null,
    nombreProyecto: destino.modo === 'nuevo' ? destino.nombreProyecto.trim() : null,
    bloqueId: sumaAlBloque ? destino.bloqueId : null,
    nombreBloque: sumaAlBloque ? null : borrador.nombreBloque.trim(),
    campos: borrador.columnas.map((columna) => ({
      clave: columna.clave,
      campoId: sumaAlBloque ? columna.campoExistenteId : null,
      nombre: columna.nombre.trim(),
      tipo: columna.tipo,
      opciones: columna.tipo === 'OPCION' ? opcionesLimpias(columna) : [],
    })),
    filas: borrador.filas.map((fila) => {
      const valores: Record<string, ValorConfirmado> = {};
      for (const columna of borrador.columnas) {
        const celda = fila.celdas[columna.clave];
        const valor = celda && valorTipado(columna, celda);
        if (valor !== undefined) valores[columna.clave] = valor;
      }
      return { pagina: fila.pagina, valores };
    }),
  };
}

/** Con destino en un bloque existente, sus columnas ya mapeadas no se renombran ni cambian de tipo. */
export const estaBloqueada = (columna: ColumnaRevision, destino: Destino) =>
  destino.modo === 'bloque' && columna.campoExistenteId !== null;

// --- Persistencia en el navegador: recargar la página no hace perder lo corregido ---

const PREFIJO_BORRADOR = 'revision-lectura:';

export type BorradorGuardado = { borrador: BorradorRevision; destino: Destino };

export function leerBorradorGuardado(loteId: string): BorradorGuardado | undefined {
  try {
    const crudo = sessionStorage.getItem(PREFIJO_BORRADOR + loteId);
    if (!crudo) return undefined;
    const datos = JSON.parse(crudo) as Partial<BorradorGuardado>;
    const { borrador, destino } = datos;
    if (!borrador || !destino || !Array.isArray(borrador.columnas) || !Array.isArray(borrador.filas)) {
      return undefined;
    }
    return { borrador, destino };
  } catch {
    return undefined;
  }
}

export function guardarBorrador(loteId: string, guardado: BorradorGuardado) {
  try {
    sessionStorage.setItem(PREFIJO_BORRADOR + loteId, JSON.stringify(guardado));
  } catch {
    // Sin almacenamiento la pantalla funciona igual; sólo no sobrevive a una recarga.
  }
}

export function borrarBorrador(loteId: string) {
  try {
    sessionStorage.removeItem(PREFIJO_BORRADOR + loteId);
  } catch {
    // Nada que limpiar.
  }
}
