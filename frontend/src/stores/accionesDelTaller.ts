import { CATALOG } from '@/lib/sistema/catalogo';
import {
  armarSeccion,
  moverSeccionEn,
  quitarSeccionDe,
  renombrarSeccionesEn,
  seccionReceptora,
  sumarBloque,
} from '@/lib/sistema/secciones';
import type { TipoBloque } from '@/lib/sistema/tipos';
import { useSistemaStore } from './sistemaStore';

// Lo mismo que hace el editor con las secciones y los bloques, pero sobre la propuesta de Sami (`taller.borrador`):
// no pasa por el historial ni se guarda hasta que la persona confirma.

const store = () => useSistemaStore.getState();

/** La sección del taller que se está viendo (la primera, si la que estaba ya no existe). */
const seccionVigente = () => {
  const taller = store().taller;
  const borrador = taller?.borrador;
  return borrador?.sections.find((seccion) => seccion.id === taller?.seccionId) ?? borrador?.sections[0];
};

export function sumarBloqueAlBorrador(tipo: TipoBloque) {
  const destino = seccionVigente();
  if (!destino) return;
  store().editarBorrador((copia) => {
    sumarBloque(copia, tipo, destino.id, { conEjemplos: true });
  });
  store().cerrarModal();
  store().avisar(`${CATALOG[tipo].name} agregado a la propuesta.`);
}

export function agregarSeccionAlBorrador(nombre: string, inicio: string) {
  const nueva = armarSeccion(nombre, inicio);
  store().editarBorrador((copia) => {
    copia.sections.push(nueva);
  });
  store().actualizarTaller({ seccionId: nueva.id });
  store().cerrarModal();
  store().avisar('Sección agregada. Ya podés sumar o mover bloques.');
}

export function renombrarSeccionesDelBorrador(nombres: [string, string][]) {
  store().editarBorrador((copia) => renombrarSeccionesEn(copia, nombres));
}

export function moverSeccionDelBorrador(seccionId: string, direccion: 'arriba' | 'abajo') {
  store().editarBorrador((copia) => moverSeccionEn(copia, seccionId, direccion));
}

export function quitarSeccionDelBorrador(seccionId: string) {
  const borrador = store().taller?.borrador;
  if (!borrador) return;
  const destino = seccionReceptora(borrador, seccionId);
  if (!destino) return;
  store().editarBorrador((copia) => {
    quitarSeccionDe(copia, seccionId);
  });
  if (store().taller?.seccionId === seccionId) store().actualizarTaller({ seccionId: destino.id });
  store().avisar('Sección quitada. Los bloques y sus datos se conservaron.');
}
