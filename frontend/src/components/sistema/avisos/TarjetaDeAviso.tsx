import { useEffect, useId, useState } from 'react';
import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { Icon } from '../Icon';
import {
  cuandoSeDetecto,
  datosDelAviso,
  ICONO_DE_AVISO,
  NIVEL_DE_AVISO,
  NOMBRE_DE_TIPO,
  propuestaDelAviso,
  resumenDelAviso,
  suavizarTexto,
  textoDeAccion,
} from './presentacion';

/** Lo que dura el desvanecido de una tarjeta descartada, en milisegundos (el mismo número va en el CSS). */
const DURACION_DEL_DESVANECIDO = 280;

type Props = {
  aviso: Aviso;
  enfocado?: boolean;
  /** Dentro de un grupo el tipo y el nivel ya están en el título del grupo: no se repiten. */
  enGrupo?: boolean;
  /** Dentro de un grupo con varios negocios, para saber de cuál es cada aviso. */
  conNegocio?: boolean;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

// Un aviso: ícono y color del tipo, cuándo se detectó, qué pasa en una línea, el botón para resolverlo y, si se abre, por qué.
export function TarjetaDeAviso({
  aviso,
  enfocado = false,
  enGrupo = false,
  conNegocio = false,
  onAccion,
  onDescartar,
}: Props) {
  const [abierto, setAbierto] = useState(enfocado);
  const [saliendo, setSaliendo] = useState(false);
  const idDetalle = useId();
  const [principal, ...otras] = aviso.acciones;
  const propuesta = propuestaDelAviso(aviso);
  const datos = datosDelAviso(aviso);
  const detectado = cuandoSeDetecto(aviso.creadoEn);
  const completo = suavizarTexto(aviso.detalle);
  const resumen = resumenDelAviso(aviso.detalle);

  // Si se llega desde la campana o el toast, el aviso se abre solo para dar contexto.
  useEffect(() => {
    if (enfocado) setAbierto(true);
  }, [enfocado]);

  // Al descartar, la tarjeta se desvanece despacio y recién después se quita. Sin animación si la persona la desactivó.
  const descartar = () => {
    const sinAnimacion =
      typeof window.matchMedia !== 'function' ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (sinAnimacion) return onDescartar(aviso);
    setSaliendo(true);
    setTimeout(() => onDescartar(aviso), DURACION_DEL_DESVANECIDO);
  };

  return (
    <article
      className={`alerta p${aviso.prioridad} ${enfocado ? 'foco' : ''} ${saliendo ? 'saliendo' : ''}`.trim()}
      data-alerta={aviso.id}
      data-tipo={aviso.tipo}
    >
      <div className="alerta-cabeza">
        <span className="alerta-icono">
          <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
        </span>
        <div className="alerta-cuerpo">
          <div className="alerta-meta">
            {detectado && (
              <span className="alerta-reloj">
                <Icon name="clock" />
                {detectado}
              </span>
            )}
            {!enGrupo && (
              <>
                <span className="alerta-tipo">{NOMBRE_DE_TIPO[aviso.tipo]}</span>
                <span className="alerta-nivel">{NIVEL_DE_AVISO[aviso.prioridad]}</span>
              </>
            )}
            <span className="alerta-sistema">{aviso.sistema}</span>
            {conNegocio && <span className="alerta-negocio">{aviso.sistema}</span>}
          </div>
          <h3>{aviso.titulo}</h3>
          <p className="alerta-resumen">{resumen}</p>
          {propuesta && principal && propuesta !== principal.texto && (
            <p className="alerta-propuesta-linea">
              <Icon name="arrow" />
              {propuesta}
            </p>
          )}
        </div>
        <button
          type="button"
          className="alerta-descartar"
          aria-label="Descartar"
          title="Descartar"
          disabled={saliendo}
          onClick={descartar}
        >
          <Icon name="trash" />
        </button>
      </div>
      <div className="alerta-pie">
        {principal && (
          <button type="button" className="btn petrol small" onClick={() => onAccion(principal)}>
            {textoDeAccion(principal)}
          </button>
        )}
        <button
          type="button"
          className="alerta-ver"
          aria-expanded={abierto}
          aria-controls={idDetalle}
          onClick={() => setAbierto((valor) => !valor)}
        >
          {abierto ? 'Ocultar el detalle' : 'Ver por qué'}
          <Icon name="arrow" />
        </button>
      </div>
      {abierto && (
        <div className="alerta-detalle" id={idDetalle}>
          {completo !== resumen && <p className="alerta-completo">{completo}</p>}
          {datos.length > 0 && (
            <dl className="alerta-datos">
              {datos.map((dato) => (
                <div key={dato.etiqueta} data-tono={dato.tono}>
                  <dt>{dato.etiqueta}</dt>
                  <dd>{dato.valor}</dd>
                </div>
              ))}
            </dl>
          )}
          {aviso.porque.length > 0 && (
            <>
              <h4>Por qué de este aviso</h4>
              <ul className="alerta-porque">
                {aviso.porque.map((motivo) => (
                  <li key={motivo}>{suavizarTexto(motivo)}</li>
                ))}
              </ul>
            </>
          )}
          {otras.length > 0 && (
            <div className="alerta-acciones">
              {otras.map((accion, indice) => (
                <button
                  key={`${accion.tipo}-${indice}`}
                  type="button"
                  className="btn secondary small"
                  onClick={() => onAccion(accion)}
                >
                  {textoDeAccion(accion)}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </article>
  );
}
