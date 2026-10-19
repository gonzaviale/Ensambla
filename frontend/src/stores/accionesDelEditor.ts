import { CATALOG, COLORS, PRESETS_DE_SECCION, TEMPLATES } from '@/lib/sistema/catalogo';
import {
  allBlocks,
  applyLayout,
  clearExamples,
  clone,
  findBlock,
  id as nuevoId,
  makeBlock,
  makeSystem,
  makeTemplate,
  moveBlock,
  normalize,
  removeBlock,
  removeRow,
  resolveValues,
  splitBlocks,
  upsertRow,
  validateRow,
  wireSystem,
} from '@/lib/sistema/core';
import type {
  Ancho,
  Campo,
  ClavePlantilla,
  Color,
  Seccion,
  Sistema,
  TipoBloque,
  Valor,
} from '@/lib/sistema/tipos';
import { seleccionarSistemaActivo, useSistemaStore } from './sistemaStore';

// Lo que cada botón del editor hace con el sistema: el cuerpo de los `case` del onClick de app.js.
// Todas pasan por `commit`, así que quedan en el historial de «Deshacer» y se guardan en el servidor.

const store = () => useSistemaStore.getState();
const sistemaActivo = () => seleccionarSistemaActivo(store());
const seccionActivaDe = (sistema: Sistema) =>
  sistema.sections.find((seccion) => seccion.id === store().sectionId) ?? sistema.sections[0];

/** Los vínculos que apuntaban a estos registros quedan vacíos. */
function limpiarVinculos(sistema: Sistema, ids: string[]) {
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields.filter((item) => item.type === 'relation')) {
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
  useSistemaStore.setState({ selectedId: creado.id, sectionId: destinoId, modal: null });
  store().commit((s) => {
    const seccion = s.sections.find((item) => item.id === destinoId);
    if (!seccion) return;
    const lugar = antesDeId ? seccion.blocks.findIndex((bloque) => bloque.id === antesDeId) : -1;
    seccion.blocks.splice(lugar < 0 ? seccion.blocks.length : lugar, 0, creado);
    wireSystem(s);
  });
  store().avisar(`${CATALOG[tipo].name} agregado. Podés personalizarlo a la derecha.`);
}

/** Suelta una pieza de la biblioteca o un bloque junto a otro: comparten la fila. */
export function juntarBloques(
  origen: { tipo: 'catalogo'; valor: TipoBloque } | { tipo: 'bloque'; valor: string },
  destinoId: string,
  lado: 'left' | 'right',
  seccionId: string,
) {
  if (origen.tipo === 'bloque') {
    if (origen.valor === destinoId) return;
    useSistemaStore.setState({ sectionId: seccionId, selectedId: origen.valor });
    store().commit((s) => {
      splitBlocks(s, origen.valor, destinoId, lado);
    });
  } else {
    const creado = makeBlock(origen.valor);
    useSistemaStore.setState({ sectionId: seccionId, selectedId: creado.id });
    store().commit((s) => {
      s.sections.find((seccion) => seccion.id === seccionId)?.blocks.push(creado);
      splitBlocks(s, creado.id, destinoId, lado);
      wireSystem(s);
    });
  }
  store().avisar('Las dos piezas comparten la fila.');
}

export function reubicarBloque(bloqueId: string, seccionId: string, antesDeId: string | null) {
  useSistemaStore.setState({ sectionId: seccionId, selectedId: bloqueId });
  store().commit((s) => {
    moveBlock(s, bloqueId, seccionId, antesDeId);
  });
  store().avisar('Bloque reubicado.');
}

export function moverBloque(bloqueId: string, direccion: 'arriba' | 'abajo') {
  store().commit((s) => {
    const lista = seccionActivaDe(s)?.blocks;
    if (!lista) return;
    const indice = lista.findIndex((bloque) => bloque.id === bloqueId);
    const destino = indice + (direccion === 'arriba' ? -1 : 1);
    const [bloque] = lista.splice(indice, 1);
    if (bloque && destino >= 0 && destino <= lista.length) lista.splice(destino, 0, bloque);
    else if (bloque) lista.splice(indice, 0, bloque);
  });
}

export function duplicarBloque(bloqueId: string) {
  const copiaId = nuevoId();
  store().commit((s) => {
    const original = findBlock(s, bloqueId);
    const seccion = seccionActivaDe(s);
    if (!original || !seccion) return;
    const copia = clone(original);
    copia.id = copiaId;
    copia.name += ' (copia)';
    if (['entries', 'exits', 'sales'].includes(copia.type)) copia.rows = [];
    else for (const fila of copia.rows) fila.id = nuevoId();
    seccion.blocks.splice(
      seccion.blocks.findIndex((bloque) => bloque.id === bloqueId) + 1,
      0,
      copia,
    );
  });
  useSistemaStore.setState({ selectedId: copiaId });
  store().avisar(
    'Bloque duplicado. Los movimientos y las ventas comienzan sin registros para conservar el stock.',
  );
}

export function pedirQuitarBloque(bloqueId: string) {
  const bloque = findBlock(sistemaActivo(), bloqueId);
  if (!bloque) return;
  store().abrirModal({
    tipo: 'confirmar',
    titulo: `¿Quitar ${bloque.name}?`,
    mensaje: `Se quitarán este bloque y sus ${bloque.rows.length} registros. Los vínculos que apuntan a él quedarán vacíos. Podés deshacer este cambio.`,
    etiqueta: 'Quitar bloque',
    onConfirmar: () => quitarBloque(bloqueId),
  });
}

function quitarBloque(bloqueId: string) {
  const sistema = sistemaActivo();
  if (!sistema) return;
  const ensayo = clone(sistema);
  const control = removeBlock(ensayo, bloqueId);
  if (control.errors) {
    store().avisar(Object.values(control.errors)[0] ?? 'No se pudo quitar el bloque.');
    return;
  }
  store().commit((s) => {
    const ids = (findBlock(s, bloqueId)?.rows ?? []).map((fila) => fila.id);
    removeBlock(s, bloqueId);
    limpiarVinculos(s, ids);
  });
  if (store().selectedId === bloqueId) store().seleccionar(null);
  store().avisar('Bloque quitado. Podés deshacer el cambio.');
}

export function cambiarColor(bloqueId: string, color: Color) {
  if (!COLORS[color]) return;
  store().commit((s) => {
    const bloque = findBlock(s, bloqueId);
    if (bloque) bloque.color = color;
  });
}

export function cambiarAncho(bloqueId: string, ancho: Ancho) {
  store().commit((s) => {
    const bloque = findBlock(s, bloqueId);
    if (bloque) bloque.width = ancho;
  });
}

export function cambiarDatoDelBloque(
  bloqueId: string,
  dato: 'name' | 'description' | 'content',
  valor: string,
) {
  store().commit((s) => {
    const bloque = findBlock(s, bloqueId);
    if (bloque) bloque[dato] = valor;
  });
}

export function cambiarSeccionDelBloque(bloqueId: string, seccionId: string) {
  useSistemaStore.setState({ sectionId: seccionId });
  store().commit((s) => {
    moveBlock(s, bloqueId, seccionId);
  });
}

export function aplicarDistribucion(distribucion: NonNullable<Sistema['layout']>) {
  store().commit((s) => {
    const seccion = seccionActivaDe(s);
    if (seccion) applyLayout(s, distribucion, seccion.id);
  });
}

export function agregarSeccion(nombre: string, inicio: string) {
  const preset = PRESETS_DE_SECCION[inicio];
  const nueva: Seccion = {
    id: nuevoId(),
    name: nombre || preset?.name || 'Nueva sección',
    blocks: preset ? (makeSystem('_', preset.types).sections[0]?.blocks ?? []) : [],
  };
  useSistemaStore.setState({ modal: null, selectedId: null, sectionId: nueva.id });
  store().commit((s) => {
    s.sections.push(nueva);
  });
  store().avisar('Sección agregada. Ya podés sumar o mover bloques.');
}

export function agregarConjunto(clave: ClavePlantilla) {
  const compuesta = makeTemplate(clave, {}, false).sections[0];
  if (!compuesta) return;
  useSistemaStore.setState({ sectionId: compuesta.id, selectedId: null });
  store().commit((s) => {
    s.sections.push({ ...compuesta, name: TEMPLATES[clave].name });
  });
  store().avisar('Conjunto agregado en una sección nueva.');
}

export function renombrarSecciones(nombres: [string, string][]) {
  store().commit((s) => {
    for (const [seccionId, nombre] of nombres) {
      const seccion = s.sections.find((item) => item.id === seccionId);
      if (seccion && nombre) seccion.name = nombre;
    }
  });
}

export function moverSeccion(seccionId: string, direccion: 'arriba' | 'abajo') {
  store().commit((s) => {
    const indice = s.sections.findIndex((seccion) => seccion.id === seccionId);
    const destino = indice + (direccion === 'arriba' ? -1 : 1);
    if (indice < 0 || destino < 0 || destino >= s.sections.length) return;
    const [seccion] = s.sections.splice(indice, 1);
    if (seccion) s.sections.splice(destino, 0, seccion);
  });
}

export function quitarSeccion(seccionId: string) {
  const sistema = sistemaActivo();
  if (!sistema || sistema.sections.length < 2) return;
  const destino = sistema.sections.find((seccion) => seccion.id !== seccionId);
  if (!destino) return;
  store().commit((s) => {
    const origen = s.sections.find((seccion) => seccion.id === seccionId);
    const receptora = s.sections.find((seccion) => seccion.id === destino.id);
    if (origen && receptora) receptora.blocks.push(...origen.blocks);
    s.sections = s.sections.filter((seccion) => seccion.id !== seccionId);
  });
  if (store().sectionId === seccionId) useSistemaStore.setState({ sectionId: destino.id });
  store().avisar('Sección quitada. Los bloques y sus datos se conservaron.');
}

export function pedirQuitarCampo(bloqueId: string, clave: string) {
  const bloque = findBlock(sistemaActivo(), bloqueId);
  if (!bloque) return;
  if (bloque.fields.length <= 1) {
    store().avisar('Conservá al menos un campo para identificar los registros.');
    return;
  }
  store().abrirModal({
    tipo: 'confirmar',
    titulo: '¿Quitar este campo?',
    mensaje:
      'También se quitarán sus valores de los registros existentes. Podés deshacer el cambio.',
    etiqueta: 'Quitar campo',
    onConfirmar: () => {
      store().commit((s) => {
        const objetivo = findBlock(s, bloqueId);
        if (!objetivo) return;
        objetivo.fields = objetivo.fields.filter((campo) => campo.key !== clave);
        for (const fila of objetivo.rows) Reflect.deleteProperty(fila, clave);
      });
      store().avisar('Campo quitado.');
    },
  });
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
  const label = datos.label.trim();
  if (!label) return 'Elegí un nombre para el campo.';
  if (
    bloque.fields.some(
      (campo) => campo.key !== clave && normalize(campo.label) === normalize(label),
    )
  ) {
    return 'Ya hay un campo con ese nombre.';
  }
  const campo: Campo = {
    key: clave ?? nuevoId(),
    label,
    type: datos.type,
    required: datos.required,
  };
  if (datos.type === 'select') {
    campo.options = [
      ...new Set(
        datos.options
          .split(/[,\n]/)
          .map((opcion) => opcion.trim())
          .filter(Boolean),
      ),
    ];
    if (!campo.options.length) return 'Agregá al menos una opción.';
  }
  if (datos.type === 'relation') {
    campo.target = datos.target;
    if (!campo.target) return 'Elegí con qué bloque querés vincularlo.';
  }
  if (clave || datos.required) {
    const candidato = clone(bloque);
    if (clave)
      candidato.fields = candidato.fields.map((item) => (item.key === clave ? campo : item));
    else candidato.fields.push(campo);
    if (bloque.rows.some((fila) => validateRow(candidato, fila, sistema)[campo.key])) {
      return 'Hay registros que no cumplen esta configuración. Conservá un tipo compatible o agregá primero un campo opcional.';
    }
  }
  store().commit((s) => {
    const objetivo = findBlock(s, bloqueId);
    if (!objetivo) return;
    if (clave) objetivo.fields = objetivo.fields.map((item) => (item.key === clave ? campo : item));
    else objetivo.fields.push(campo);
  });
  store().cerrarModal();
  store().avisar('Campo guardado.');
  return null;
}

/** Guarda un registro (nuevo o editado). Devuelve los errores por campo, o `null` si quedó guardado. */
export function guardarRegistro(
  bloqueId: string,
  crudos: Record<string, Valor>,
  filaId: string | null,
) {
  const sistema = sistemaActivo();
  const bloque = findBlock(sistema, bloqueId);
  if (!sistema || !bloque) return { _form: 'Este bloque ya no existe.' };
  const valores = resolveValues(bloque, crudos, sistema);
  const errores = validateRow(bloque, valores, sistema, filaId);
  if (Object.keys(errores).length) return errores;
  store().commit((s) => {
    const objetivo = findBlock(s, bloqueId);
    if (objetivo) upsertRow(objetivo, valores, filaId, s);
  });
  store().cerrarModal();
  store().avisar(filaId ? 'Registro actualizado.' : 'Registro guardado.');
  return null;
}

export function pedirQuitarRegistro(bloqueId: string, filaId: string) {
  store().abrirModal({
    tipo: 'confirmar',
    titulo: '¿Eliminar este registro?',
    mensaje: 'Se quitará del bloque y se vaciarán sus vínculos. Podés deshacer este cambio.',
    etiqueta: 'Eliminar',
    onConfirmar: () => {
      const sistema = sistemaActivo();
      if (!sistema) return;
      const copia = clone(sistema);
      const objetivo = findBlock(copia, bloqueId);
      const control = objetivo ? removeRow(objetivo, filaId, copia) : undefined;
      if (control?.errors) {
        store().avisar(Object.values(control.errors)[0] ?? 'No se pudo eliminar el registro.');
        return;
      }
      store().commit((s) => {
        const bloque = findBlock(s, bloqueId);
        if (bloque) removeRow(bloque, filaId, s);
        limpiarVinculos(s, [filaId]);
      });
      store().avisar('Registro eliminado.');
    },
  });
}

export function pedirQuitarEjemplos() {
  store().abrirModal({
    tipo: 'confirmar',
    titulo: '¿Quitar los datos de ejemplo?',
    mensaje:
      'Se conservan tus bloques, campos y los registros que cargaste o editaste. Solo se eliminan los ejemplos que siguen sin modificar.',
    etiqueta: 'Quitar ejemplos',
    onConfirmar: () => {
      let resultado: ReturnType<typeof clearExamples> | undefined;
      store().commit((s) => {
        resultado = clearExamples(s);
        limpiarVinculos(s, resultado.removed);
      });
      store().avisar(
        resultado?.retained.length
          ? 'Se quitaron los ejemplos. Se conservaron los movimientos necesarios para tu stock.'
          : 'Listo. Se conservaron tus datos y los vínculos que utilizaste.',
      );
    },
  });
}

export function deshacerCambio() {
  if (store().deshacer()) {
    store().cerrarModal();
    store().avisar('Cambio deshecho.');
  }
}
