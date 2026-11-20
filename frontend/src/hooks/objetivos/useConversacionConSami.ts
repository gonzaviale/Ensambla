import { useState } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import {
  editablesDe,
  paraGuardar,
  problemaDelManual,
  tipoQueQueda,
  type MiniEditable,
} from '@/lib/objetivosSami';
import type { MensajeDeConversacion, PropuestaObjetivo } from '@/types/objetivos';
import { useGuardarObjetivoConSami, useProponerObjetivo } from './mutations';
import { useCuotaObjetivos } from './queries';

const ERROR_QUE_LA_PERSONA_PUEDE_CORREGIR = 400;

const mensajeDeLaPersona = (texto: string): MensajeDeConversacion => ({ rol: 'PERSONA', texto });

const respuestaDeSami = (texto: string): MensajeDeConversacion => ({ rol: 'SAMI', texto });

const primerMensajeDeLaPersona = (mensajes: MensajeDeConversacion[]) =>
  mensajes.find((mensaje) => mensaje.rol === 'PERSONA')?.texto;

const sePuedeGuardar = (propuesta: PropuestaObjetivo | null, minis: MiniEditable[]) =>
  propuesta?.origen === 'IA' &&
  propuesta.puedeGuardar &&
  minis.length > 0 &&
  !minis.some((mini) => problemaDelManual(mini) !== null);

// La conversación para armar un objetivo con Sami: lo que se dijo, lo que Sami propuso y qué se puede guardar.
// Nada se guarda ni gasta cuota hasta aceptar la propuesta.
export function useConversacionConSami() {
  const proponer = useProponerObjetivo();
  const guardar = useGuardarObjetivoConSami();
  const { cuota: cuotaDelMes } = useCuotaObjetivos();
  const [texto, setTexto] = useState('');
  const [mensajes, setMensajes] = useState<MensajeDeConversacion[]>([]);
  const [propuesta, setPropuesta] = useState<PropuestaObjetivo | null>(null);
  const [minis, setMinis] = useState<MiniEditable[]>([]);
  const [pendiente, setPendiente] = useState<string | null>(null);

  const errorDeSami = leerErrorServidor(proponer.error);
  const cuota = propuesta?.cuota ?? cuotaDelMes;
  const objetivoTexto = primerMensajeDeLaPersona(mensajes) ?? texto.trim();

  const mostrarPropuesta = (historial: MensajeDeConversacion[], nueva: PropuestaObjetivo) => {
    setMensajes([...historial, respuestaDeSami(nueva.respuesta)]);
    setPropuesta(nueva);
    setMinis(editablesDe(nueva));
  };

  const conversar = (historial: MensajeDeConversacion[], escrito: string) => {
    setPendiente(escrito);
    setTexto('');
    proponer.mutate(historial, {
      onSuccess: (nueva) => mostrarPropuesta(historial, nueva),
      onError: () => setTexto(escrito),
      onSettled: () => setPendiente(null),
    });
  };

  const enviar = () => {
    const escrito = texto.trim();
    if (!escrito || proponer.isPending) return;
    conversar([...mensajes, mensajeDeLaPersona(escrito)], escrito);
  };

  const guardarLoPropuesto = (alGuardar: () => void) =>
    guardar.mutate(
      { texto: objetivoTexto, minis: minis.flatMap((mini) => paraGuardar(mini) ?? []) },
      { onSuccess: alGuardar },
    );

  const guardarSeguimientoManual = (alGuardar: () => void) =>
    guardar.mutate(
      { texto: objetivoTexto, seguimientoManual: true, minis: [] },
      { onSuccess: alGuardar },
    );

  const cambiarMini = (
    clave: string,
    cambios: Partial<Pick<MiniEditable, 'titulo' | 'justificacion'>>,
  ) =>
    setMinis((actuales) =>
      actuales.map((mini) => (mini.clave === clave ? { ...mini, ...cambios } : mini)),
    );

  const quitarMini = (clave: string) =>
    setMinis((actuales) => actuales.filter((mini) => mini.clave !== clave));

  return {
    texto,
    setTexto,
    mensajes,
    pendiente,
    propuesta,
    minis,
    cuota,
    objetivoTexto,
    tipo: tipoQueQueda(minis),
    enviando: proponer.isPending,
    guardando: guardar.isPending,
    errorDeSami,
    errorAlGuardar: leerErrorServidor(guardar.error),
    iaNoRespondio:
      errorDeSami !== undefined && errorDeSami.status !== ERROR_QUE_LA_PERSONA_PUEDE_CORREGIR,
    sinCuota: cuota?.restantes === 0,
    esRespaldo: propuesta?.origen === 'RESPALDO',
    puedeSeguirHablando: propuesta === null || propuesta.mensajesRestantes > 0,
    puedeGuardarLoPropuesto: sePuedeGuardar(propuesta, minis) && !guardar.isPending,
    enviar,
    guardarLoPropuesto,
    guardarSeguimientoManual,
    cambiarMini,
    quitarMini,
  };
}
