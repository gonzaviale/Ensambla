import { useEffect, useId, useState } from 'react';
import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { Icon } from '../Icon';
import {
  datosDelAviso,
  haceCuanto,
  ICONO_DE_AVISO,
  NIVEL_DE_AVISO,
  NOMBRE_DE_TIPO,
  propuestaDelAviso,
  textoDeAccion,
} from './presentacion';

type Props = {
  aviso: Aviso;
  enfocado?: boolean;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

// Un aviso: de qué tipo es (por color e ícono), qué pasa, qué propone Sami hacer y, si se abre, por qué lo dice.
export function TarjetaDeAviso({ aviso, enfocado = false, onAccion, onDescartar }: Props) {
  const [abierto, setAbierto] = useState(enfocado);
  const idDetalle = useId();
  const [principal, ...otras] = aviso.acciones;
  const propuesta = propuestaDelAviso(aviso);
  const datos = datosDelAviso(aviso);
  const detectado = haceCuanto(aviso.creadoEn);

  // Si se llega desde la campana o el toast, el aviso se abre solo para dar contexto.
  useEffect(() => {
    if (enfocado) setAbierto(true);
  }, [enfocado]);

  return (
    <article
      className={`alerta p${aviso.prioridad} ${enfocado ? 'foco' : ''}`.trim()}
      data-alerta={aviso.id}
      data-tipo={aviso.tipo}
    >
      <div className="alerta-cabeza">
        <span className="alerta-icono">
          <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
        </span>
        <div className="alerta-cuerpo">
          <div className="alerta-meta">
            <span className="alerta-tipo">{NOMBRE_DE_TIPO[aviso.tipo]}</span>
            <span className="alerta-nivel">{NIVEL_DE_AVISO[aviso.prioridad]}</span>
            <span className="alerta-sistema">{aviso.sistema}</span>
          </div>
          <h3>{aviso.titulo}</h3>
          <p>{aviso.detalle}</p>
        </div>
        <button
          type="button"
          className="alerta-descartar"
          aria-label="Descartar"
          title="Descartar"
          onClick={() => onDescartar(aviso)}
        >
          <Icon name="trash" />
        </button>
      </div>
      {principal && (
        <div className="alerta-propuesta">
          <div>
            <small>Qué te propongo</small>
            <strong>{propuesta}</strong>
          </div>
          <button type="button" className="btn petrol small" onClick={() => onAccion(principal)}>
            {textoDeAccion(principal)}
          </button>
        </div>
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
      {abierto && (
        <div className="alerta-detalle" id={idDetalle}>
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
              <h4>Por qué te lo aviso</h4>
              <ul className="alerta-porque">
                {aviso.porque.map((motivo) => (
                  <li key={motivo}>{motivo}</li>
                ))}
              </ul>
            </>
          )}
          <p className="alerta-origen">
            Sección: <strong>{aviso.sistema}</strong>
            {detectado ? ` · detectado ${detectado}` : ''}
          </p>
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
