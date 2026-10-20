import { useMutation } from '@tanstack/react-query';
import { conversarEnTaller } from '@/api/samiTaller';
import { aplicarAcciones, mismoSistema, resumirSistema } from '@/lib/sistema/acciones';
import { applyLayout, clone, makeSystem } from '@/lib/sistema/core';
import { aBase64, reducirImagen } from '@/lib/sistema/imagen';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore, type Taller } from '@/stores/sistemaStore';
import {
  TIPOS_DE_IMAGEN_PARA_SAMI,
  type ImagenParaSami,
  type OpcionesDeSami,
} from '@/types/samiTaller';

const MENSAJE_CORTO = 'Contame un poquito más para encontrar un punto de partida.';
const MENSAJE_FALLO = 'Sami no pudo pensar la propuesta. Probá de nuevo en un momento.';
const MENSAJE_DESHECHO = 'Listo, volví atrás. ¿Probamos otra cosa?';
const MENSAJE_SIN_CAMBIOS = 'No pude hacer ese cambio. ¿Me lo contás de otra manera?';
const MENSAJES_DE_HISTORIAL = 6;
const LARGO_MAXIMO_DE_MENSAJE = 1000;
const MENSAJE_SOLO_FOTO = 'Te mando una foto.';
const MENSAJE_FOTO_PESADA = 'La foto pesa más de 5 MB aun achicada. Probá con otra más liviana.';
const MENSAJE_FOTO_TIPO = 'Esa foto no la puedo mirar. Elegí una JPG, PNG o WEBP.';
const MENSAJE_FOTO_FALLO = 'No pude abrir esa foto. Probá con otra.';
/** Para que Sami lea una anotación alcanza con este lado mayor; la foto viaja más liviana. */
const LADO_DE_LA_FOTO = 1600;
const BYTES_DE_LA_FOTO = 5 * 1024 * 1024;

const esTipoParaSami = (tipo: string): tipo is ImagenParaSami['tipo'] =>
  (TIPOS_DE_IMAGEN_PARA_SAMI as readonly string[]).includes(tipo);

/** La foto achicada y en base64, lista para viajar a Sami; si no se puede mandar, el motivo. */
async function prepararFoto(archivo: File): Promise<ImagenParaSami | string> {
  try {
    const reducida = await reducirImagen(archivo, LADO_DE_LA_FOTO);
    const tipo = reducida.type || archivo.type;
    if (!esTipoParaSami(tipo)) return MENSAJE_FOTO_TIPO;
    if (reducida.size > BYTES_DE_LA_FOTO) return MENSAJE_FOTO_PESADA;
    return { tipo, datos: await aBase64(reducida) };
  } catch {
    return MENSAJE_FOTO_FALLO;
  }
}

/** La foto como dirección `data:`, para mostrarla en una miniatura. */
export const vistaDeFoto = (foto: ImagenParaSami) => `data:${foto.tipo};base64,${foto.datos}`;

type Turno = {
  borrador: Sistema | null;
  respuesta: string;
  opciones: OpcionesDeSami | null;
  /** La propuesta antes de este cambio, si cambió algo. */
  anterior: Sistema | null;
};

/** Lo que Sami dice en la charla. Si no dijo nada, se cuenta lo que armó (o se repite su pregunta). */
function textoDeSami(
  respuesta: string,
  aplicadas: string[],
  opciones: OpcionesDeSami | null,
  primeraVez: boolean,
) {
  if (respuesta.trim()) return respuesta.trim();
  if (!aplicadas.length) return opciones?.pregunta.trim() || MENSAJE_SIN_CAMBIOS;
  const cierre = primeraVez ? 'Ya podés verlo a la derecha.' : 'Mirá cómo queda.';
  return `${aplicadas.join(' ')} ${cierre} ¿Qué cambiarías?`;
}

// La charla con Sami en el taller. Cada mensaje (también el primero) va a POST /api/sami/taller: el modelo conversa
// y pide herramientas para ensamblar componentes del catálogo, el backend las valida y acá se aplican al borrador.
// Sin borrador, Sami arma sobre un sistema vacío; si no sumó nada, sigue sin borrador. Cada cambio se puede deshacer.
export function useTaller() {
  const consulta = useMutation({ mutationFn: conversarEnTaller });

  const resolverTurno = async (
    texto: string,
    taller: Taller,
    imagen: ImagenParaSami | null,
  ): Promise<Turno> => {
    const { borrador } = taller;
    const base = borrador ?? makeSystem(taller.nombre || 'Mi sistema', [], false);
    const charla = taller.mensajes.map((mensaje) => ({
      quien: mensaje.quien,
      texto: mensaje.texto.slice(0, LARGO_MAXIMO_DE_MENSAJE),
    }));
    // La pregunta de la botonera no siempre está en el texto de Sami; sin ella, «Sólo servicios» se entiende como
    // «dejá sólo la pieza Servicios». Va como lo último que dijo Sami.
    const pregunta = taller.opciones?.pregunta;
    if (pregunta && !charla.at(-1)?.texto.includes(pregunta)) {
      charla.push({ quien: 'sami', texto: pregunta });
    }
    const historial = charla.slice(-MENSAJES_DE_HISTORIAL);
    const respuesta = await consulta.mutateAsync({
      mensaje: texto,
      estado: resumirSistema(base),
      historial,
      ...(imagen && { imagen }),
    });
    const copia = clone(base);
    const { aplicadas } = aplicarAcciones(copia, respuesta.acciones);
    const cambio = !mismoSistema(copia, base);
    // Lo primero que arma Sami se acomoda como el resto de las propuestas: el stock a lo ancho, lo demás de a dos.
    if (cambio && !borrador && !copia.layout) applyLayout(copia, 'mosaic');
    const opciones = respuesta.opciones ?? null;
    return {
      borrador: borrador || cambio ? copia : null,
      respuesta: textoDeSami(respuesta.respuesta, cambio ? aplicadas : [], opciones, !borrador),
      opciones,
      anterior: borrador && cambio ? borrador : null,
    };
  };

  /** `esOpcion`: lo que se manda es una respuesta rápida de la botonera, que puede ser corta («Sí»). */
  const enviar = async (mensaje?: string, { esOpcion = false } = {}) => {
    const store = useSistemaStore.getState();
    const taller = store.taller;
    if (!taller || taller.pensando) return;
    // La foto adjunta va con este mensaje (sea cual sea) y se puede mandar sola.
    const { foto } = taller;
    const escrito = (mensaje ?? taller.entrada).trim();
    const texto = foto && !escrito ? MENSAJE_SOLO_FOTO : escrito;
    // Un saludo corto («hola») ya es una charla con Sami: sólo se frena lo que no llega a ser una palabra.
    if (texto.length < (esOpcion ? 1 : 2)) {
      store.avisar(MENSAJE_CORTO);
      return;
    }
    store.actualizarTaller((actual) => ({
      entrada: '',
      foto: null,
      pensando: true,
      opciones: null,
      mensajes: [
        ...actual.mensajes,
        { quien: 'you', texto, ...(foto && { foto: vistaDeFoto(foto) }) },
      ],
    }));

    // Si mientras tanto empezó otra conversación, la respuesta de esta ya no importa.
    const vigente = () => useSistemaStore.getState().taller?.id === taller.id;
    const nombreAlEnviar = taller.borrador?.name;
    try {
      const turno = await resolverTurno(texto, taller, foto);
      if (!vigente()) return;
      useSistemaStore.getState().actualizarTaller((actual) => {
        const sistema = turno.borrador;
        // El nombre que la persona fue escribiendo mientras Sami pensaba no se pisa.
        if (sistema && actual.borrador && actual.borrador.name !== nombreAlEnviar) {
          sistema.name = actual.borrador.name;
        }
        const seccionVigente = sistema?.sections.some((seccion) => seccion.id === actual.seccionId);
        return {
          borrador: sistema,
          seccionId: seccionVigente ? actual.seccionId : (sistema?.sections[0]?.id ?? null),
          pensando: false,
          opciones: turno.opciones,
          anterior: turno.anterior,
          mensajes: [...actual.mensajes, { quien: 'sami', texto: turno.respuesta }],
        };
      });
    } catch (error) {
      if (!vigente()) return;
      useSistemaStore.getState().actualizarTaller({ pensando: false });
      useSistemaStore.getState().avisar(error instanceof Error ? error.message : MENSAJE_FALLO);
    }
  };

  /** Vuelve la propuesta a como estaba antes del último cambio de Sami. */
  const deshacer = () => {
    const store = useSistemaStore.getState();
    if (!store.taller?.anterior || store.taller.pensando) return;
    store.actualizarTaller((actual) => ({
      borrador: actual.anterior,
      seccionId: actual.anterior?.sections.some((seccion) => seccion.id === actual.seccionId)
        ? actual.seccionId
        : (actual.anterior?.sections[0]?.id ?? null),
      anterior: null,
      opciones: null,
      mensajes: [...actual.mensajes, { quien: 'sami', texto: MENSAJE_DESHECHO }],
    }));
  };

  /** Achica la foto elegida y la deja lista para el próximo mensaje; si no sirve, avisa y no la adjunta. */
  const adjuntarFoto = async (archivo: File) => {
    const taller = useSistemaStore.getState().taller;
    if (!taller) return;
    const foto = await prepararFoto(archivo);
    const store = useSistemaStore.getState();
    // Si mientras tanto empezó otra conversación, la foto ya no va.
    if (store.taller?.id !== taller.id) return;
    if (typeof foto === 'string') store.avisar(foto);
    else store.actualizarTaller({ foto });
  };

  const quitarFoto = () => useSistemaStore.getState().actualizarTaller({ foto: null });

  return { enviar, deshacer, adjuntarFoto, quitarFoto };
}
