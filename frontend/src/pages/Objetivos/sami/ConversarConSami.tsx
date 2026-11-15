import { useId, useState, type FormEvent } from 'react';
import { PaperPlaneRightIcon } from '@phosphor-icons/react';
import { leerErrorServidor } from '@/hooks/errores';
import { useGuardarObjetivoConSami, useProponerObjetivo } from '@/hooks/objetivos/mutations';
import { useCuotaObjetivos } from '@/hooks/objetivos/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import {
  COMO_SE_MIDE,
  MAXIMO_DEL_MENSAJE,
  datosUsados,
  editablesDe,
  paraGuardar,
  problemaDelManual,
  tipoQueQueda,
  type MiniEditable,
} from '@/lib/objetivosSami';
import type { MensajeDeConversacion, PropuestaObjetivo } from '@/types/objetivos';
import { CuotaDeObjetivos } from './CuotaDeObjetivos';
import { MiniPropuestoCard } from './MiniPropuestoCard';

type Props = {
  /** Se guardó el objetivo. */
  onGuardado: () => void;
  /** Era una meta en pesos: se sigue con el formulario de siempre, con lo que la persona escribió. */
  onMetaEnPesos: (texto: string) => void;
  onCancelar?: () => void;
};

const EJEMPLOS = [
  'Quiero vender más yerba',
  'Quiero que más gente conozca mi local',
  'Quiero gastar menos en luz',
] as const;

// «Contale tu objetivo a Sami»: una conversación corta. Sami dice cómo lo entendió (con datos, sin datos o mezclado) y
// propone hasta 3 mini-objetivos que la persona puede quitar o retocar antes de guardar. Nada se guarda ni gasta
// cuota hasta tocar «Guardar objetivo».
export function ConversarConSami({ onGuardado, onMetaEnPesos, onCancelar }: Props) {
  const idCampo = useId();
  const proponer = useProponerObjetivo();
  const guardar = useGuardarObjetivoConSami();
  const { cuota: cuotaDelMes } = useCuotaObjetivos();
  const navegacion = useNavegacion();
  const [texto, setTexto] = useState('');
  const [mensajes, setMensajes] = useState<MensajeDeConversacion[]>([]);
  const [propuesta, setPropuesta] = useState<PropuestaObjetivo | null>(null);
  const [minis, setMinis] = useState<MiniEditable[]>([]);

  const objetivoTexto =
    mensajes.find((mensaje) => mensaje.rol === 'PERSONA')?.texto ?? texto.trim();
  const errorDeSami = leerErrorServidor(proponer.error);
  // Un 400 es algo que la persona puede corregir (mensaje vacío, tope de mensajes); cualquier otro error es que la
  // IA no respondió, y entonces se puede guardar como seguimiento manual.
  const iaNoRespondio = errorDeSami !== undefined && errorDeSami.status !== 400;
  const cuota = propuesta?.cuota ?? cuotaDelMes;
  const sinCuota = cuota?.restantes === 0;
  const esRespaldo = propuesta?.origen === 'RESPALDO';
  const errorAlGuardar = leerErrorServidor(guardar.error);
  const hayProblemas = minis.some((mini) => problemaDelManual(mini) !== null);
  const puedeGuardarLoPropuesto =
    propuesta !== null &&
    propuesta.origen === 'IA' &&
    propuesta.puedeGuardar &&
    minis.length > 0 &&
    !hayProblemas &&
    !guardar.isPending;
  const puedeSeguirHablando = propuesta === null || propuesta.mensajesRestantes > 0;
  const tipo = tipoQueQueda(minis);

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    const escrito = texto.trim();
    if (!escrito || proponer.isPending) return;
    const historial: MensajeDeConversacion[] = [...mensajes, { rol: 'PERSONA', texto: escrito }];
    proponer.mutate(historial, {
      onSuccess: (nueva) => {
        setMensajes([...historial, { rol: 'SAMI', texto: nueva.respuesta }]);
        setPropuesta(nueva);
        setMinis(editablesDe(nueva));
        setTexto('');
      },
    });
  };

  const guardarLoPropuesto = () => {
    const aceptados = minis.flatMap((mini) => paraGuardar(mini) ?? []);
    guardar.mutate({ texto: objetivoTexto, minis: aceptados }, { onSuccess: onGuardado });
  };

  const guardarSeguimientoManual = () =>
    guardar.mutate(
      { texto: objetivoTexto, seguimientoManual: true, minis: [] },
      { onSuccess: onGuardado },
    );

  const cambiar = (
    clave: string,
    cambios: Partial<Pick<MiniEditable, 'titulo' | 'justificacion'>>,
  ) =>
    setMinis((actuales) =>
      actuales.map((mini) => (mini.clave === clave ? { ...mini, ...cambios } : mini)),
    );

  const ofrecerSeguimiento = iaNoRespondio || esRespaldo;

  return (
    <section className="objetivos-definir sami-conversar" aria-labelledby={`${idCampo}-titulo`}>
      <div className="objetivos-formulario">
        <h2 id={`${idCampo}-titulo`}>¿Qué querés lograr?</h2>
        <CuotaDeObjetivos cuota={cuota} />

        {mensajes.length > 0 && (
          <ol className="sami-hilo" aria-label="Conversación con Sami">
            {mensajes.map((mensaje, indice) => (
              <li key={`${indice}-${mensaje.rol}`} data-rol={mensaje.rol}>
                <span className="sami-hilo-quien">{mensaje.rol === 'SAMI' ? 'Sami' : 'Vos'}</span>
                <p>{mensaje.texto}</p>
              </li>
            ))}
          </ol>
        )}

        {propuesta && !esRespaldo && !propuesta.esMonetario && propuesta.entendido && (
          <section
            className="objetivos-lectura"
            aria-live="polite"
            aria-label="Lo que entendió Sami"
          >
            <p className="objetivos-etiqueta">Lo que entendió Sami</p>
            <p>{propuesta.entendido}</p>
            {tipo && <p className="sami-tipo">{COMO_SE_MIDE[tipo]}</p>}
            {propuesta.datos.length > 0 && (
              <p className="sami-datos">{datosUsados(propuesta.datos)}</p>
            )}
          </section>
        )}

        {propuesta?.esMonetario && (
          <div className="sami-aparte" role="note">
            <p>Una meta en pesos se define con el formulario de siempre.</p>
            <button
              type="button"
              className="btn secondary"
              onClick={() => onMetaEnPesos(objetivoTexto)}
            >
              Poner mi meta en pesos
            </button>
          </div>
        )}

        {propuesta && !esRespaldo && minis.length > 0 && (
          <div>
            <h3 className="sami-subtitulo">Tus mini-objetivos de la semana</h3>
            <ol className="sami-minis">
              {minis.map((mini, indice) => (
                <MiniPropuestoCard
                  key={mini.clave}
                  mini={mini}
                  posicion={indice + 1}
                  onCambiar={(cambios) => cambiar(mini.clave, cambios)}
                  onQuitar={() =>
                    setMinis((actuales) => actuales.filter((item) => item.clave !== mini.clave))
                  }
                />
              ))}
            </ol>
          </div>
        )}

        {ofrecerSeguimiento && (
          <div className="sami-aparte" role="note">
            <p>
              {iaNoRespondio
                ? 'Sami no pudo responder ahora. Podés guardar tu objetivo igual: cada semana me contás cómo te fue.'
                : sinCuota
                  ? 'Usaste todos los objetivos con IA de este mes. Podés guardarlo igual: cada semana me contás cómo te fue.'
                  : 'Sami no pudo armarlo ahora. Podés guardarlo igual: cada semana me contás cómo te fue.'}
            </p>
            <div className="sami-aparte-acciones">
              <button
                type="button"
                className="btn primary"
                disabled={!objetivoTexto || guardar.isPending}
                onClick={guardarSeguimientoManual}
              >
                Guardar como seguimiento manual
              </button>
              {sinCuota && (
                <button
                  type="button"
                  className="btn secondary"
                  onClick={() => void navegacion.irAPlanes()}
                >
                  Ver planes
                </button>
              )}
            </div>
          </div>
        )}

        {puedeSeguirHablando ? (
          <form onSubmit={enviar} className="sami-escribir">
            <div className="field">
              <label htmlFor={idCampo}>
                {propuesta ? '¿Querés cambiar algo?' : 'Contale tu objetivo a Sami'}
              </label>
              <textarea
                id={idCampo}
                rows={propuesta ? 2 : 4}
                value={texto}
                maxLength={MAXIMO_DEL_MENSAJE}
                placeholder={
                  propuesta
                    ? 'Ej.: Prefiero algo para el local'
                    : 'Ej.: Quiero que más gente conozca mi local'
                }
                onChange={(evento) => setTexto(evento.target.value)}
              />
            </div>
            {!propuesta && (
              <div>
                <p className="objetivos-ejemplos-titulo">O empezá con uno de estos:</p>
                <div className="example-chips">
                  {EJEMPLOS.map((ejemplo) => (
                    <button type="button" key={ejemplo} onClick={() => setTexto(ejemplo)}>
                      {ejemplo}
                    </button>
                  ))}
                </div>
              </div>
            )}
            <button
              type="submit"
              className={propuesta ? 'btn secondary' : 'btn primary'}
              disabled={!texto.trim() || proponer.isPending}
            >
              <PaperPlaneRightIcon aria-hidden size={18} />{' '}
              {proponer.isPending
                ? 'Sami está pensando…'
                : propuesta
                  ? 'Enviar'
                  : 'Hablar con Sami'}
            </button>
          </form>
        ) : (
          <p className="input-hint">
            Llegaste al máximo de mensajes de esta conversación. Guardá lo que armaste.
          </p>
        )}

        {errorDeSami && !iaNoRespondio && (
          <p className="input-hint" role="alert">
            {errorDeSami.mensaje}
          </p>
        )}
        {errorAlGuardar && (
          <p className="input-hint" role="alert">
            {errorAlGuardar.mensaje}
          </p>
        )}

        <div className="objetivos-formulario-acciones">
          {onCancelar && (
            <button type="button" className="btn secondary" onClick={onCancelar}>
              Cancelar
            </button>
          )}
          {propuesta && !esRespaldo && (
            <button
              type="button"
              className="btn primary"
              disabled={!puedeGuardarLoPropuesto}
              onClick={guardarLoPropuesto}
            >
              {guardar.isPending ? 'Guardando…' : 'Guardar objetivo'}
            </button>
          )}
        </div>
      </div>
    </section>
  );
}
