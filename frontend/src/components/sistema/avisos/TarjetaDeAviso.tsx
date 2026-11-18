import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { useDescarteSuave } from '@/hooks/avisos/useDescarteSuave';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import {
  cuandoSeDetecto,
  ICONO_DE_AVISO,
  propuestaDelAviso,
  resumenDelAviso,
  textoDeAccion,
  tieneDetalle,
} from './presentacion';

type Props = {
  aviso: Aviso;
  enfocado?: boolean;
  /** Con avisos de varios negocios juntos, cada fila dice de cuál es. */
  conNegocio?: boolean;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

// Un aviso en una fila: qué pasa, el botón para resolverlo y, si hay más para contar, un ojo que abre el porqué.
export function TarjetaDeAviso({
  aviso,
  enfocado = false,
  conNegocio = false,
  ...acciones
}: Props) {
  const descarte = useDescarteSuave(() => acciones.onDescartar(aviso));
  const verDetalle = useVerDetalleDeAviso(aviso, acciones.onAccion);

  return (
    <article
      className={claseDeFila(aviso, enfocado, descarte.saliendo)}
      data-alerta={aviso.id}
      data-tipo={aviso.tipo}
    >
      <span className="aviso-fila-icono">
        <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
      </span>
      <CuerpoDeAviso aviso={aviso} conNegocio={conNegocio} />
      <div className="aviso-fila-acciones">
        <AccionPrincipal aviso={aviso} onAccion={acciones.onAccion} />
        <BotonVerPorQue aviso={aviso} onVer={verDetalle} />
        <BotonDescartar deshabilitado={descarte.saliendo} onDescartar={descarte.descartar} />
      </div>
    </article>
  );
}

const claseDeFila = (aviso: Aviso, enfocado: boolean, saliendo: boolean) =>
  ['aviso-fila', `p${aviso.prioridad}`, enfocado && 'foco', saliendo && 'saliendo']
    .filter(Boolean)
    .join(' ');

function useVerDetalleDeAviso(aviso: Aviso, onAccion: (accion: AccionDeAviso) => void) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  return () => abrirModal({ tipo: 'detalleDeAviso', avisoId: aviso.id, onAccion });
}

function CuerpoDeAviso({ aviso, conNegocio }: { aviso: Aviso; conNegocio: boolean }) {
  return (
    <div className="aviso-fila-cuerpo">
      <h3>{aviso.titulo}</h3>
      <LineaDeApoyo aviso={aviso} conNegocio={conNegocio} />
    </div>
  );
}

function LineaDeApoyo({ aviso, conNegocio }: { aviso: Aviso; conNegocio: boolean }) {
  const detectado = cuandoSeDetecto(aviso.creadoEn);
  return (
    <p className="aviso-fila-linea">
      <span className="aviso-fila-resumen">{resumenDelAviso(aviso.detalle)}</span>
      <Propuesta aviso={aviso} />
      {detectado && <Detectado texto={detectado} />}
      {conNegocio && <span className="aviso-fila-negocio">{aviso.sistema}</span>}
    </p>
  );
}

const Detectado = ({ texto }: { texto: string }) => (
  <span className="aviso-fila-reloj">
    <Icon name="clock" />
    {texto}
  </span>
);

function Propuesta({ aviso }: { aviso: Aviso }) {
  const [principal] = aviso.acciones;
  const propuesta = propuestaDelAviso(aviso);
  if (!propuesta || propuesta === principal?.texto) return null;
  return (
    <span className="aviso-fila-propuesta">
      <Icon name="arrow" />
      {propuesta}
    </span>
  );
}

function AccionPrincipal({
  aviso,
  onAccion,
}: {
  aviso: Aviso;
  onAccion: (accion: AccionDeAviso) => void;
}) {
  const [principal] = aviso.acciones;
  if (!principal) return null;
  return (
    <button type="button" className="btn petrol small" onClick={() => onAccion(principal)}>
      {textoDeAccion(principal)}
    </button>
  );
}

function BotonVerPorQue({ aviso, onVer }: { aviso: Aviso; onVer: () => void }) {
  if (!tieneDetalle(aviso)) return null;
  return (
    <button
      type="button"
      className="aviso-fila-ver"
      aria-label="Ver por qué"
      title="Ver por qué"
      onClick={onVer}
    >
      <Icon name="eye" />
    </button>
  );
}

function BotonDescartar({
  deshabilitado,
  onDescartar,
}: {
  deshabilitado: boolean;
  onDescartar: () => void;
}) {
  return (
    <button
      type="button"
      className="aviso-fila-descartar"
      aria-label="Descartar"
      title="Descartar"
      disabled={deshabilitado}
      onClick={onDescartar}
    >
      <Icon name="trash" />
    </button>
  );
}
