import { useEffect } from 'react';
import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import {
  cuandoSeDetecto,
  datosDelAviso,
  ICONO_DE_AVISO,
  NIVEL_DE_AVISO,
  NOMBRE_DE_TIPO,
  razonDelAviso,
  suavizarTexto,
  textoDeAccion,
} from '../avisos/presentacion';

// Por qué Sami avisó: los números, la razón y lo que se puede hacer, sin abrir la fila ni salir de la lista.
export function ModalDetalleDeAviso({
  avisoId,
  onAccion,
}: {
  avisoId: string;
  onAccion: (accion: AccionDeAviso) => void;
}) {
  const aviso = useSistemaStore((estado) => estado.avisos.find((item) => item.id === avisoId));
  useCerrarSiYaNoExiste(aviso);

  if (!aviso) return null;
  return (
    <Modal>
      <EncabezadoDelDetalle aviso={aviso} />
      <CuerpoDelDetalle aviso={aviso} />
      <AccionesDelDetalle aviso={aviso} onAccion={onAccion} />
    </Modal>
  );
}

function useCerrarSiYaNoExiste(aviso: Aviso | undefined) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  useEffect(() => {
    if (!aviso) cerrarModal();
  }, [aviso, cerrarModal]);
}

function EncabezadoDelDetalle({ aviso }: { aviso: Aviso }) {
  return (
    <header className="aviso-detalle-cabeza" data-tipo={aviso.tipo}>
      <span className={`aviso-fila-icono p${aviso.prioridad}`}>
        <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
      </span>
      <div>
        <p className="aviso-detalle-meta">
          <span>{NOMBRE_DE_TIPO[aviso.tipo]}</span>
          <span>{NIVEL_DE_AVISO[aviso.prioridad]}</span>
        </p>
        <h2 id="modal-title">{aviso.titulo}</h2>
      </div>
    </header>
  );
}

function CuerpoDelDetalle({ aviso }: { aviso: Aviso }) {
  return (
    <div className="aviso-detalle-cuerpo">
      <TextoDelAviso aviso={aviso} />
      <DatosDelAviso aviso={aviso} />
      <RazonDelAviso aviso={aviso} />
    </div>
  );
}

const TextoDelAviso = ({ aviso }: { aviso: Aviso }) => <p>{suavizarTexto(aviso.detalle)}</p>;

function DatosDelAviso({ aviso }: { aviso: Aviso }) {
  const datos = datosDelAviso(aviso);
  if (!datos.length) return null;
  return (
    <dl className="aviso-datos">
      {datos.map((dato) => (
        <div key={dato.etiqueta} data-tono={dato.tono}>
          <dt>{dato.etiqueta}</dt>
          <dd>{dato.valor}</dd>
        </div>
      ))}
    </dl>
  );
}

function RazonDelAviso({ aviso }: { aviso: Aviso }) {
  const razon = razonDelAviso(aviso);
  if (!razon) return null;
  return (
    <section className="aviso-razon">
      <h3>Por qué de este aviso</h3>
      <p>{razon}</p>
    </section>
  );
}

function FechaDelAviso({ aviso }: { aviso: Aviso }) {
  const detectado = cuandoSeDetecto(aviso.creadoEn);
  if (!detectado) return null;
  return (
    <span className="aviso-detalle-fecha" title={`Aviso detectado: ${detectado.toLowerCase()}`}>
      <Icon name="clock" />
      {detectado}
    </span>
  );
}

function AccionesDelDetalle({
  aviso,
  onAccion,
}: {
  aviso: Aviso;
  onAccion: (accion: AccionDeAviso) => void;
}) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const [principal, ...otras] = aviso.acciones;
  const aplicar = (accion: AccionDeAviso) => {
    cerrarModal();
    onAccion(accion);
  };

  return (
    <div className="modal-footer">
      <FechaDelAviso aviso={aviso} />
      <Btn cls="ghost" onClick={cerrarModal}>
        Cerrar
      </Btn>
      {otras.map((accion, indice) => (
        <Btn key={`${accion.tipo}-${indice}`} cls="secondary" onClick={() => aplicar(accion)}>
          {textoDeAccion(accion)}
        </Btn>
      ))}
      {principal && (
        <Btn cls="primary" onClick={() => aplicar(principal)}>
          {textoDeAccion(principal)}
        </Btn>
      )}
    </div>
  );
}
