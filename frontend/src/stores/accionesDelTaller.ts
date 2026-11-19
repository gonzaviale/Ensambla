import { CATALOG } from '@/lib/sistema/catalogo';
import {
  armarSeccion,
  moverSeccionEn,
  quitarSeccionDe,
  renombrarSeccionesEn,
  seccionReceptora,
  sumarBloque,
} from '@/lib/sistema/secciones';
import type { Sistema, TipoBloque } from '@/lib/sistema/tipos';
import { useSistemaStore } from './sistemaStore';

// Lo mismo que hace el editor con las pestañas y los bloques, pero sobre la propuesta de Sami (`taller.borrador`):
// no pasa por el historial ni se guarda hasta que la persona confirma.

const store = () => useSistemaStore.getState();
const cambiarElBorrador = (cambio: (borrador: Sistema) => void) => store().editarBorrador(cambio);
const mirarLaPestana = (seccionId: string) => store().actualizarTaller({ seccionId });
const avisar = (texto: string) => store().avisar(texto);
const cerrarElModal = () => store().cerrarModal();
const borradorDelTaller = () => store().taller?.borrador;
const pestanaQueSeMira = () => store().taller?.seccionId;

/** La pestaña del taller que se está viendo (la primera, si la que estaba ya no existe). */
const seccionVigente = () => {
  const taller = store().taller;
  const borrador = taller?.borrador;
  return (
    borrador?.sections.find((seccion) => seccion.id === taller?.seccionId) ?? borrador?.sections[0]
  );
};

export function sumarBloqueAlBorrador(tipo: TipoBloque) {
  const destino = seccionVigente();
  if (!destino) return;
  cambiarElBorrador((copia) => {
    sumarBloque(copia, tipo, destino.id, { conEjemplos: true });
  });
  cerrarElModal();
  avisar(`${CATALOG[tipo].name} agregado a la propuesta.`);
}

export function agregarSeccionAlBorrador(nombre: string, inicio: string) {
  const nueva = armarSeccion(nombre, inicio);
  cambiarElBorrador((copia) => {
    copia.sections.push(nueva);
  });
  mirarLaPestana(nueva.id);
  cerrarElModal();
  avisar('Pestaña agregada. Ya podés sumar o mover bloques.');
}

export function renombrarSeccionesDelBorrador(nombres: [string, string][]) {
  cambiarElBorrador((copia) => renombrarSeccionesEn(copia, nombres));
}

export function moverSeccionDelBorrador(seccionId: string, direccion: 'arriba' | 'abajo') {
  cambiarElBorrador((copia) => moverSeccionEn(copia, seccionId, direccion));
}

export function quitarSeccionDelBorrador(seccionId: string) {
  const borrador = borradorDelTaller();
  if (!borrador) return;
  const destino = seccionReceptora(borrador, seccionId);
  if (!destino) return;
  cambiarElBorrador((copia) => {
    quitarSeccionDe(copia, seccionId);
  });
  if (pestanaQueSeMira() === seccionId) mirarLaPestana(destino.id);
  avisar('Pestaña quitada. Los bloques y sus datos se conservaron.');
}
