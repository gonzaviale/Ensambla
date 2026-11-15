import { useId } from 'react';
import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { Icon } from '../Icon';
import { TarjetaDeAviso } from './TarjetaDeAviso';
import { ICONO_DE_AVISO, NIVEL_DE_AVISO, NOMBRE_DE_TIPO, tituloDeGrupo } from './presentacion';

type Props = {
  avisos: Aviso[];
  foco?: string | null;
  abierto: boolean;
  onAlternar: () => void;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

// Varios avisos del mismo tipo juntos: una sola tarjeta con el número a la vista y la lista adentro.
export function TarjetaDeGrupo({
  avisos,
  foco,
  abierto,
  onAlternar,
  onAccion,
  onDescartar,
}: Props) {
  const idLista = useId();
  const [primero] = avisos;
  const variosNegocios = new Set(avisos.map((aviso) => aviso.sistemaId)).size > 1;

  if (!primero) return null;

  return (
    <div
      className={`alerta-grupo p${primero.prioridad}`}
      data-tipo={primero.tipo}
      data-grupo={primero.tipo}
    >
      <button
        type="button"
        className="alerta-grupo-cabeza"
        aria-expanded={abierto}
        aria-controls={idLista}
        onClick={onAlternar}
      >
        <span className="alerta-icono">
          <Icon name={ICONO_DE_AVISO[primero.tipo]} />
          <b className="alerta-contador">{avisos.length}</b>
        </span>
        <span className="alerta-cuerpo">
          <span className="alerta-meta">
            <span className="alerta-tipo">{NOMBRE_DE_TIPO[primero.tipo]}</span>
            <span className="alerta-nivel">{NIVEL_DE_AVISO[primero.prioridad]}</span>
          </span>
          <strong className="alerta-grupo-titulo">
            {tituloDeGrupo(primero.tipo, primero.prioridad, avisos.length)}
          </strong>
        </span>
        <span className="alerta-grupo-ver">
          {abierto ? 'Ocultar la lista' : 'Ver la lista'}
          <Icon name="arrow" />
        </span>
      </button>
      <div id={idLista} className={abierto ? 'alerta-grupo-lista' : 'alerta-grupo-lista cerrado'}>
        {avisos.map((aviso) => (
          <TarjetaDeAviso
            key={aviso.id}
            aviso={aviso}
            enfocado={foco === aviso.id}
            enGrupo
            conNegocio={variosNegocios}
            onAccion={onAccion}
            onDescartar={onDescartar}
          />
        ))}
      </div>
    </div>
  );
}
