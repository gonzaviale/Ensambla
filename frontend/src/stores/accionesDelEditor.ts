import { CATALOG, COLORS, TEMPLATES } from '@/lib/sistema/catalogo';
import { comportamientoDe } from '@/lib/sistema/comportamiento';
import {
  allBlocks,
  applyLayout,
  camposDeVinculo,
  clearExamples,
  clone,
  findBlock,
  id as nuevoId,
  makeBlock,
  makeTemplate,
  moveBlock,
  normalize,
  removeBlock,
  removeRow,
  resolveValues,
  upsertRow,
  validateRow,
  wireSystem,
} from '@/lib/sistema/core';
import {
  esColumnaFija,
  motivoParaNoQuitarLaColumna,
  motivoParaNoQuitarLaSeccion,
  problemaAlCambiarLaColumna,
  sePuedeQuitarLaSeccion,
} from '@/lib/sistema/estructuraFija';
import {
  armarSeccion,
  moverSeccionEn,
  quitarSeccionDe,
  renombrarSeccionesEn,
  seccionReceptora,
} from '@/lib/sistema/secciones';
import type {
  Ancho,
  Bloque,
  Campo,
  ClavePlantilla,
  Color,
  ErroresFila,
  Sistema,
  TipoBloque,
  Valor,
} from '@/lib/sistema/tipos';
import { seleccionarSistemaActivo, useSistemaStore, type SistemaStore } from './sistemaStore';

// Lo que cada botón del editor hace con el sistema: el cuerpo de los `case` del onClick de app.js.
// Todas pasan por `guardarCambio`, así que quedan en el historial de «Deshacer» y se guardan en el servidor.

type Confirmacion = { titulo: string; mensaje: string; etiqueta: string; onConfirmar: () => void };

const MOVIMIENTOS: TipoBloque[] = ['entries', 'exits', 'sales'];

const store = () => useSistemaStore.getState();
const sistemaActivo = () => seleccionarSistemaActivo(store());
const borradorDelTaller = () => store().taller?.borrador ?? undefined;
const guardarCambio = <T>(cambio: (sistema: Sistema) => T) => store().commit(cambio);
const cambiarElBorrador = (cambio: (borrador: Sistema) => void) => store().editarBorrador(cambio);
const mostrarEnElEditor = (cambio: Partial<SistemaStore>) => useSistemaStore.setState(cambio);
const avisar = (texto: string) => store().avisar(texto);
const cerrarElModal = () => store().cerrarModal();
const pedirConfirmacion = (confirmacion: Confirmacion) =>
  store().abrirModal({ tipo: 'confirmar', ...confirmacion });

const seccionActivaDe = (sistema: Sistema) =>
  sistema.sections.find((seccion) => seccion.id === store().sectionId) ?? sistema.sections[0];

const primerError = (errores: ErroresFila, otro: string) => Object.values(errores)[0] ?? otro;

/** Los vínculos que apuntaban a estos registros quedan vacíos. */
function limpiarVinculos(sistema: Sistema, ids: string[]) {
  for (const bloque of allBlocks(sistema)) {
    for (const campo of camposDeVinculo(bloque)) {
      for (const fila of bloque.rows)
        if (ids.includes(String(fila[campo.key]))) fila[campo.key] = '';
    }
  }
}

export function agregarBloque(
  tipo: TipoBloque,
  antesDeId: string | null = null,
  seccionId: string | undefined = undefined,
) {
  const sistema = sistemaActivo();
  const destinoId = seccionId ?? (sistema ? seccionActivaDe(sistema)?.id : undefined);
  if (!sistema || !destinoId) return;
  const creado = makeBlock(tipo);
  mostrarEnElEditor({ selectedId: creado.id, sectionId: destinoId, modal: null });
  guardarCambio((s) => ponerElBloque(s, creado, destinoId, antesDeId));
  avisar(`${CATALOG[tipo].name} agregado. Podés personalizarlo a la derecha.`);
}

function ponerElBloque(
  sistema: Sistema,
  bloque: Bloque,
  seccionId: string,
  antesDeId: string | null,
) {
  const seccion = sistema.sections.find((item) => item.id === seccionId);
  if (!seccion) return;
  const lugar = antesDeId ? seccion.blocks.findIndex((otro) => otro.id === antesDeId) : -1;
  seccion.blocks.splice(lugar < 0 ? seccion.blocks.length : lugar, 0, bloque);
  wireSystem(sistema);
}

/** Soltar un bloque sobre otro de la misma pestaña los cambia de lugar; cada uno conserva su ancho. */
export function intercambiarBloques(bloqueId: string, otroId: string) {
  if (bloqueId === otroId) return;
  guardarCambio((s) => cambiarDeLugar(s, bloqueId, otroId));
  avisar('Las tablas cambiaron de lugar.');
}

function cambiarDeLugar(sistema: Sistema, bloqueId: string, otroId: string) {
  const lista = sistema.sections.find((seccion) =>
    seccion.blocks.some((bloque) => bloque.id === bloqueId),
  )?.blocks;
  if (!lista) return;
  const uno = lista.findIndex((bloque) => bloque.id === bloqueId);
  const otro = lista.findIndex((bloque) => bloque.id === otroId);
  const [a, b] = [lista[uno], lista[otro]];
  if (!a || !b) return;
  lista[uno] = b;
  lista[otro] = a;
}

export function reubicarBloque(bloqueId: string, seccionId: string, antesDeId: string | null) {
  mostrarEnElEditor({ sectionId: seccionId, selectedId: bloqueId });
  guardarCambio((s) => {
    moveBlock(s, bloqueId, seccionId, antesDeId);
  });
  avisar('Bloque reubicado.');
}

export function moverBloque(bloqueId: string, direccion: 'arriba' | 'abajo') {
  guardarCambio((s) => correrUnLugar(seccionActivaDe(s)?.blocks, bloqueId, direccion));
}

function correrUnLugar(
  lista: Bloque[] | undefined,
  bloqueId: string,
  direccion: 'arriba' | 'abajo',
) {
  if (!lista) return;
  const indice = lista.findIndex((bloque) => bloque.id === bloqueId);
  const destino = indice + (direccion === 'arriba' ? -1 : 1);
  const [bloque] = lista.splice(indice, 1);
  if (!bloque) return;
  const entra = destino >= 0 && destino <= lista.length;
  lista.splice(entra ? destino : indice, 0, bloque);
}

export function duplicarBloque(bloqueId: string) {
  const copiaId = nuevoId();
  guardarCambio((s) => ponerUnaCopia(s, bloqueId, copiaId));
  mostrarEnElEditor({ selectedId: copiaId });
  avisar(
    'Bloque duplicado. Los movimientos y las ventas comienzan sin registros para conservar el stock.',
  );
}

function ponerUnaCopia(sistema: Sistema, bloqueId: string, copiaId: string) {
  const original = findBlock(sistema, bloqueId);
  const seccion = seccionActivaDe(sistema);
  if (!original || !seccion) return;
  const lugar = seccion.blocks.findIndex((bloque) => bloque.id === bloqueId) + 1;
  seccion.blocks.splice(lugar, 0, copiaDe(original, copiaId));
}

// Las copias de movimientos y ventas arrancan vacías: si copiaran las filas, moverían el stock dos veces.
function copiaDe(original: Bloque, copiaId: string): Bloque {
  const copia = { ...clone(original), id: copiaId, name: `${original.name} (copia)` };
  if (MOVIMIENTOS.includes(copia.type)) copia.rows = [];
  else for (const fila of copia.rows) fila.id = nuevoId();
  return copia;
}

export function pedirQuitarBloque(bloqueId: string) {
  const sistema = sistemaActivo();
  const bloque = findBlock(sistema, bloqueId);
  if (!sistema || !bloque) return;
  if (!sePuedeQuitarLaSeccion(sistema, bloque)) {
    avisar(motivoParaNoQuitarLaSeccion(bloque));
    return;
  }
  pedirConfirmacion({
    titulo: `¿Quitar ${bloque.name}?`,
    mensaje: `Se quitarán este bloque y sus ${bloque.rows.length} registros. Los vínculos que apuntan a él quedarán vacíos. Podés deshacer este cambio.`,
    etiqueta: 'Quitar bloque',
    onConfirmar: () => quitarBloque(bloqueId),
  });
}

function quitarBloque(bloqueId: string) {
  const sistema = sistemaActivo();
  if (!sistema) return;
  const control = removeBlock(clone(sistema), bloqueId);
  if (control.errors) {
    avisar(primerError(control.errors, 'No se pudo quitar el bloque.'));
    return;
  }
  guardarCambio((s) => quitarConSusVinculos(s, bloqueId));
  if (store().selectedId === bloqueId) store().seleccionar(null);
  avisar('Bloque quitado. Podés deshacer el cambio.');
}

function quitarConSusVinculos(sistema: Sistema, bloqueId: string) {
  const ids = (findBlock(sistema, bloqueId)?.rows ?? []).map((fila) => fila.id);
  removeBlock(sistema, bloqueId);
  limpiarVinculos(sistema, ids);
}

const cambiarElBloque = (bloqueId: string, cambio: (bloque: Bloque) => void) =>
  guardarCambio((s) => {
    const bloque = findBlock(s, bloqueId);
    if (bloque) cambio(bloque);
  });

export function cambiarColor(bloqueId: string, color: Color) {
  if (!COLORS[color]) return;
  cambiarElBloque(bloqueId, (bloque) => {
    bloque.color = color;
  });
}

export function cambiarAncho(bloqueId: string, ancho: Ancho) {
  cambiarElBloque(bloqueId, (bloque) => {
    bloque.width = ancho;
  });
}

export function cambiarDatoDelBloque(
  bloqueId: string,
  dato: 'name' | 'description' | 'content',
  valor: string,
) {
  cambiarElBloque(bloqueId, (bloque) => {
    bloque[dato] = valor;
  });
}

export function cambiarSeccionDelBloque(bloqueId: string, seccionId: string) {
  mostrarEnElEditor({ sectionId: seccionId });
  guardarCambio((s) => {
    moveBlock(s, bloqueId, seccionId);
  });
}

export function aplicarDistribucion(distribucion: NonNullable<Sistema['layout']>) {
  guardarCambio((s) => {
    const seccion = seccionActivaDe(s);
    if (seccion) applyLayout(s, distribucion, seccion.id);
  });
}

export function agregarSeccion(nombre: string, inicio: string) {
  const nueva = armarSeccion(nombre, inicio);
  mostrarEnElEditor({ modal: null, selectedId: null, sectionId: nueva.id });
  guardarCambio((s) => {
    s.sections.push(nueva);
  });
  avisar('Pestaña agregada. Ya podés sumar o mover bloques.');
}

export function agregarConjunto(clave: ClavePlantilla) {
  const compuesta = makeTemplate(clave, {}, false).sections[0];
  if (!compuesta) return;
  mostrarEnElEditor({ sectionId: compuesta.id, selectedId: null });
  guardarCambio((s) => {
    s.sections.push({ ...compuesta, name: TEMPLATES[clave].name });
  });
  avisar('Conjunto agregado en una pestaña nueva.');
}

export function renombrarSecciones(nombres: [string, string][]) {
  guardarCambio((s) => renombrarSeccionesEn(s, nombres));
}

export function moverSeccion(seccionId: string, direccion: 'arriba' | 'abajo') {
  guardarCambio((s) => moverSeccionEn(s, seccionId, direccion));
}

export function quitarSeccion(seccionId: string) {
  const sistema = sistemaActivo();
  if (!sistema || sistema.sections.length < 2) return;
  const destino = seccionReceptora(sistema, seccionId);
  if (!destino) return;
  guardarCambio((s) => {
    quitarSeccionDe(s, seccionId);
  });
  if (store().sectionId === seccionId) mostrarEnElEditor({ sectionId: destino.id });
  avisar('Pestaña quitada. Los bloques y sus datos se conservaron.');
}

export function pedirQuitarCampo(bloqueId: string, clave: string) {
  const bloque = findBlock(sistemaActivo(), bloqueId);
  if (!bloque) return;
  const campo = bloque.fields.find((candidato) => candidato.key === clave);
  if (campo && esColumnaFija(campo)) {
    avisar(motivoParaNoQuitarLaColumna(campo));
    return;
  }
  if (bloque.fields.length <= 1) {
    avisar('Conservá al menos un campo para identificar los registros.');
    return;
  }
  pedirConfirmacion({
    titulo: '¿Quitar este campo?',
    mensaje:
      'También se quitarán sus valores de los registros existentes. Podés deshacer el cambio.',
    etiqueta: 'Quitar campo',
    onConfirmar: () => quitarCampo(bloqueId, clave),
  });
}

function quitarCampo(bloqueId: string, clave: string) {
  cambiarElBloque(bloqueId, (bloque) => {
    bloque.fields = bloque.fields.filter((campo) => campo.key !== clave);
    for (const fila of bloque.rows) Reflect.deleteProperty(fila, clave);
  });
  avisar('Campo quitado.');
}

export type DatosDeCampo = {
  label: string;
  type: Campo['type'];
  required: boolean;
  options: string;
  target: string;
};

/** Guarda un campo nuevo o editado. Devuelve lo que hay que corregir, o `null` si quedó guardado. */
export function guardarCampo(bloqueId: string, clave: string | null, datos: DatosDeCampo) {
  const sistema = sistemaActivo();
  const bloque = findBlock(sistema, bloqueId);
  if (!sistema || !bloque) return 'Este bloque ya no existe.';
  const campo = campoPedido(clave, datos);
  const problema =
    problemaAlCambiarLaColumna(bloque.fields.find((item) => item.key === clave), campo) ??
    problemaDelCampo(bloque, clave, campo) ??
    problemaConLosRegistros(sistema, bloque, clave, campo, datos);
  if (problema) return problema;
  cambiarElBloque(bloqueId, (objetivo) => {
    objetivo.fields = conElCampo(objetivo.fields, clave, campo);
  });
  cerrarElModal();
  avisar('Campo guardado.');
  return null;
}

function campoPedido(clave: string | null, datos: DatosDeCampo): Campo {
  const campo: Campo = {
    key: clave ?? nuevoId(),
    label: datos.label.trim(),
    type: datos.type,
    required: datos.required,
  };
  if (comportamientoDe(datos.type).llevaOpciones()) campo.options = opcionesEscritas(datos.options);
  if (comportamientoDe(datos.type).esVinculo()) campo.target = datos.target;
  return campo;
}

const opcionesEscritas = (texto: string) => [
  ...new Set(
    texto
      .split(/[,\n]/)
      .map((opcion) => opcion.trim())
      .filter(Boolean),
  ),
];

function problemaDelCampo(bloque: Bloque, clave: string | null, campo: Campo) {
  if (!campo.label) return 'Elegí un nombre para el campo.';
  const repetido = bloque.fields.some(
    (otro) => otro.key !== clave && normalize(otro.label) === normalize(campo.label),
  );
  if (repetido) return 'Ya hay un campo con ese nombre.';
  if (campo.options && !campo.options.length) return 'Agregá al menos una opción.';
  if (comportamientoDe(campo.type).esVinculo() && !campo.target)
    return 'Elegí con qué bloque querés vincularlo.';
  return undefined;
}

const conElCampo = (campos: Campo[], clave: string | null, campo: Campo) =>
  clave ? campos.map((item) => (item.key === clave ? conservandoLoFijo(item, campo) : item)) : [...campos, campo];

const conservandoLoFijo = (anterior: Campo, campo: Campo): Campo =>
  anterior.system ? { ...campo, system: true } : campo;

function problemaConLosRegistros(
  sistema: Sistema,
  bloque: Bloque,
  clave: string | null,
  campo: Campo,
  datos: DatosDeCampo,
) {
  if (!clave && !datos.required) return undefined;
  const candidato = { ...clone(bloque), fields: conElCampo(clone(bloque).fields, clave, campo) };
  const algunoNoCumple = bloque.rows.some(
    (fila) => validateRow(candidato, fila, sistema)[campo.key],
  );
  if (!algunoNoCumple) return undefined;
  return 'Hay registros que no cumplen esta configuración. Conservá un tipo compatible o agregá primero un campo opcional.';
}

/** Los valores listos para guardar, o los errores de cada campo si el registro no es válido. */
function revisarRegistro(
  sistema: Sistema | undefined,
  bloqueId: string,
  crudos: Record<string, Valor>,
  filaId: string | null,
) {
  const bloque = findBlock(sistema, bloqueId);
  if (!sistema || !bloque) return { errores: { _form: 'Este bloque ya no existe.' } };
  const valores = resolveValues(bloque, crudos, sistema);
  const errores = validateRow(bloque, valores, sistema, filaId);
  return Object.keys(errores).length ? { errores } : { valores };
}

const guardarLaFila = (
  sistema: Sistema,
  bloqueId: string,
  valores: Record<string, Valor>,
  filaId: string | null,
) => {
  const objetivo = findBlock(sistema, bloqueId);
  if (objetivo) upsertRow(objetivo, valores, filaId, sistema);
};

function cerrarYAvisarElRegistro(filaId: string | null) {
  cerrarElModal();
  avisar(filaId ? 'Registro actualizado.' : 'Registro guardado.');
  return null;
}

/** Guarda un registro (nuevo o editado). Devuelve los errores por campo, o `null` si quedó guardado. */
export function guardarRegistro(
  bloqueId: string,
  crudos: Record<string, Valor>,
  filaId: string | null,
) {
  const revision = revisarRegistro(sistemaActivo(), bloqueId, crudos, filaId);
  if ('errores' in revision) return revision.errores;
  guardarCambio((s) => guardarLaFila(s, bloqueId, revision.valores, filaId));
  return cerrarYAvisarElRegistro(filaId);
}

/**
 * Lo mismo sobre la propuesta de Sami, antes de crear el sistema. Un ejemplo que se edita deja de serlo, así que
 * pasa al sistema al crearlo.
 */
export function guardarRegistroDelBorrador(
  bloqueId: string,
  crudos: Record<string, Valor>,
  filaId: string | null,
) {
  const revision = revisarRegistro(borradorDelTaller(), bloqueId, crudos, filaId);
  if ('errores' in revision) return revision.errores;
  cambiarElBorrador((copia) => guardarLaFila(copia, bloqueId, revision.valores, filaId));
  return cerrarYAvisarElRegistro(filaId);
}

export function pedirQuitarRegistro(bloqueId: string, filaId: string) {
  pedirConfirmacion({
    titulo: '¿Eliminar este registro?',
    mensaje: 'Se quitará del bloque y se vaciarán sus vínculos. Podés deshacer este cambio.',
    etiqueta: 'Eliminar',
    onConfirmar: () => quitarRegistro(bloqueId, filaId),
  });
}

function quitarRegistro(bloqueId: string, filaId: string) {
  const sistema = sistemaActivo();
  if (!sistema) return;
  const errores = erroresAlQuitarEnUnaCopia(sistema, bloqueId, filaId);
  if (errores) {
    avisar(primerError(errores, 'No se pudo eliminar el registro.'));
    return;
  }
  guardarCambio((s) => quitarLaFilaConSusVinculos(s, bloqueId, filaId));
  avisar('Registro eliminado.');
}

function erroresAlQuitarEnUnaCopia(sistema: Sistema, bloqueId: string, filaId: string) {
  const copia = clone(sistema);
  const objetivo = findBlock(copia, bloqueId);
  return objetivo ? removeRow(objetivo, filaId, copia).errors : undefined;
}

function quitarLaFilaConSusVinculos(sistema: Sistema, bloqueId: string, filaId: string) {
  const bloque = findBlock(sistema, bloqueId);
  if (bloque) removeRow(bloque, filaId, sistema);
  limpiarVinculos(sistema, [filaId]);
}

/** Quita los ejemplos y vacía los vínculos que apuntaban a ellos. Devuelve si hubo que conservar alguno. */
function quitarLosEjemplos(sistema: Sistema) {
  const resultado = clearExamples(sistema);
  limpiarVinculos(sistema, resultado.removed);
  return resultado.retained.length > 0;
}

export function pedirQuitarEjemplos() {
  pedirConfirmacion({
    titulo: '¿Quitar los datos de ejemplo?',
    mensaje:
      'Se conservan tus bloques, campos y los registros que cargaste o editaste. Solo se eliminan los ejemplos que siguen sin modificar.',
    etiqueta: 'Quitar ejemplos',
    onConfirmar: () => {
      const conservoMovimientos = guardarCambio(quitarLosEjemplos);
      avisar(
        conservoMovimientos
          ? 'Se quitaron los ejemplos. Se conservaron los movimientos necesarios para tu stock.'
          : 'Listo. Se conservaron tus datos y los vínculos que utilizaste.',
      );
    },
  });
}

/** Lo mismo que `pedirQuitarEjemplos`, pero sobre la propuesta de Sami que todavía no se guardó. */
export function pedirQuitarEjemplosDelBorrador() {
  pedirConfirmacion({
    titulo: '¿Borrar los datos de ejemplo?',
    mensaje:
      'Se quitan las filas de ejemplo de esta propuesta. Se conservan las piezas, los campos y los registros reales.',
    etiqueta: 'Borrar ejemplos',
    onConfirmar: () => {
      let conservoMovimientos = false;
      cambiarElBorrador((copia) => {
        conservoMovimientos = quitarLosEjemplos(copia);
      });
      avisar(
        conservoMovimientos
          ? 'Borré los ejemplos. Quedaron los movimientos necesarios para el stock.'
          : 'Listo, borré los ejemplos.',
      );
    },
  });
}

export function deshacerCambio() {
  if (!store().deshacer()) return;
  cerrarElModal();
  avisar('Cambio deshecho.');
}
