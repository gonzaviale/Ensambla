import { useId } from 'react';
import { useConversacionConSami } from '@/hooks/objetivos/useConversacionConSami';
import { useEsPlanGratuito } from '@/hooks/planes/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { COMO_SE_MIDE, datosUsados } from '@/lib/objetivosSami';
import type { PropuestaObjetivo, TipoObjetivo } from '@/types/objetivos';
import { ChatDeObjetivos } from './ChatDeObjetivos';
import { CuotaDeObjetivos } from './CuotaDeObjetivos';
import { MiniPropuestoCard } from './MiniPropuestoCard';

type Props = {
  /** Se guardó el objetivo. */
  onGuardado: () => void;
  /** Era una meta en pesos: se sigue con el formulario de siempre, con lo que la persona escribió. */
  onMetaEnPesos: (texto: string) => void;
  onCancelar?: () => void;
};

type Conversacion = ReturnType<typeof useConversacionConSami>;

const textoDelAviso = ({
  iaNoRespondio,
  sinCuota,
}: Pick<Conversacion, 'iaNoRespondio' | 'sinCuota'>) => {
  const seguimiento = 'Podés guardarlo igual: cada semana me contás cómo te fue.';
  if (iaNoRespondio) return `Sami no pudo responder ahora. ${seguimiento}`;
  if (sinCuota) return `Usaste todos los objetivos con IA de este mes. ${seguimiento}`;
  return `Sami no pudo armarlo ahora. ${seguimiento}`;
};

function LoQueEntendioSami({
  propuesta,
  tipo,
}: {
  propuesta: PropuestaObjetivo;
  tipo: TipoObjetivo | null;
}) {
  return (
    <section className="objetivos-lectura" aria-live="polite" aria-label="Lo que entendió Sami">
      <p className="objetivos-etiqueta">Lo que entendió Sami</p>
      <p>{propuesta.entendido}</p>
      {tipo && <p className="sami-tipo">{COMO_SE_MIDE[tipo]}</p>}
      {propuesta.datos.length > 0 && <p className="sami-datos">{datosUsados(propuesta.datos)}</p>}
    </section>
  );
}

function MetaEnPesos({ onPoner }: { onPoner: () => void }) {
  return (
    <div className="sami-aparte" role="note">
      <p>Una meta en pesos se define con el formulario de siempre.</p>
      <button type="button" className="btn secondary" onClick={onPoner}>
        Poner mi meta en pesos
      </button>
    </div>
  );
}

function MinisPropuestos({ conversacion }: { conversacion: Conversacion }) {
  return (
    <div>
      <h3 className="sami-subtitulo">Tus mini-objetivos de la semana</h3>
      <ol className="sami-minis">
        {conversacion.minis.map((mini, orden) => (
          <MiniPropuestoCard
            key={mini.clave}
            mini={mini}
            posicion={orden + 1}
            onCambiar={(cambios) => conversacion.cambiarMini(mini.clave, cambios)}
            onQuitar={() => conversacion.quitarMini(mini.clave)}
          />
        ))}
      </ol>
    </div>
  );
}

function OfertaDeSeguimientoManual({
  conversacion,
  onGuardado,
  puedeVerPlanes,
}: {
  conversacion: Conversacion;
  onGuardado: () => void;
  puedeVerPlanes: boolean;
}) {
  const navegacion = useNavegacion();
  return (
    <div className="sami-aparte" role="note">
      <p>{textoDelAviso(conversacion)}</p>
      <div className="sami-aparte-acciones">
        <button
          type="button"
          className="btn primary"
          disabled={!conversacion.objetivoTexto || conversacion.guardando}
          onClick={() => conversacion.guardarSeguimientoManual(onGuardado)}
        >
          Guardar como seguimiento manual
        </button>
        {puedeVerPlanes && (
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
  );
}

function ErroresDeLaConversacion({ conversacion }: { conversacion: Conversacion }) {
  const { errorDeSami, iaNoRespondio, errorAlGuardar } = conversacion;
  const errorParaCorregir = errorDeSami && !iaNoRespondio ? errorDeSami : undefined;
  return (
    <>
      {errorParaCorregir && (
        <p className="input-hint" role="alert">
          {errorParaCorregir.mensaje}
        </p>
      )}
      {errorAlGuardar && (
        <p className="input-hint" role="alert">
          {errorAlGuardar.mensaje}
        </p>
      )}
    </>
  );
}

function AccionesDeLaConversacion({
  conversacion,
  onGuardado,
  onCancelar,
}: Props & { conversacion: Conversacion }) {
  const hayAlgoParaGuardar = conversacion.propuesta !== null && !conversacion.esRespaldo;
  return (
    <div className="objetivos-formulario-acciones">
      {onCancelar && (
        <button type="button" className="btn secondary" onClick={onCancelar}>
          Cancelar
        </button>
      )}
      {hayAlgoParaGuardar && (
        <button
          type="button"
          className="btn primary"
          disabled={!conversacion.puedeGuardarLoPropuesto}
          onClick={() => conversacion.guardarLoPropuesto(onGuardado)}
        >
          {conversacion.guardando ? 'Guardando…' : 'Guardar objetivo'}
        </button>
      )}
    </div>
  );
}

function LoQueSamiPropone({
  conversacion,
  onMetaEnPesos,
}: Pick<Props, 'onMetaEnPesos'> & { conversacion: Conversacion }) {
  const { propuesta, esRespaldo, minis, tipo, objetivoTexto } = conversacion;
  if (!propuesta || esRespaldo) return null;
  const mostrarLoEntendido = !propuesta.esMonetario && Boolean(propuesta.entendido);
  return (
    <>
      {mostrarLoEntendido && <LoQueEntendioSami propuesta={propuesta} tipo={tipo} />}
      {propuesta.esMonetario && <MetaEnPesos onPoner={() => onMetaEnPesos(objetivoTexto)} />}
      {minis.length > 0 && <MinisPropuestos conversacion={conversacion} />}
    </>
  );
}

// «Contale tu objetivo a Sami»: una conversación corta. Sami dice cómo lo entendió (con datos, sin datos o mezclado) y
// propone hasta 3 mini-objetivos que la persona puede quitar o retocar antes de guardar.
export function ConversarConSami({ onGuardado, onMetaEnPesos, onCancelar }: Props) {
  const idTitulo = `${useId()}-titulo`;
  const conversacion = useConversacionConSami();
  // Se consulta al abrir la pantalla para que, cuando haga falta ofrecer los planes, ya se sepa cuál tiene.
  const esGratuito = useEsPlanGratuito();
  const ofrecerSeguimiento = conversacion.iaNoRespondio || conversacion.esRespaldo;
  return (
    <section className="objetivos-definir sami-conversar" aria-labelledby={idTitulo}>
      <div className="objetivos-formulario">
        <h2 id={idTitulo}>¿Qué querés lograr?</h2>
        <CuotaDeObjetivos cuota={conversacion.cuota} />
        <ChatDeObjetivos
          mensajes={conversacion.mensajes}
          pendiente={conversacion.pendiente}
          texto={conversacion.texto}
          onTexto={conversacion.setTexto}
          onEnviar={conversacion.enviar}
          enviando={conversacion.enviando}
          puedeSeguirHablando={conversacion.puedeSeguirHablando}
          yaHayPropuesta={conversacion.propuesta !== null}
        />
        <LoQueSamiPropone conversacion={conversacion} onMetaEnPesos={onMetaEnPesos} />
        {ofrecerSeguimiento && (
          <OfertaDeSeguimientoManual
            conversacion={conversacion}
            onGuardado={onGuardado}
            puedeVerPlanes={conversacion.sinCuota && esGratuito}
          />
        )}
        <ErroresDeLaConversacion conversacion={conversacion} />
        <AccionesDeLaConversacion
          conversacion={conversacion}
          onGuardado={onGuardado}
          onMetaEnPesos={onMetaEnPesos}
          onCancelar={onCancelar}
        />
      </div>
    </section>
  );
}
