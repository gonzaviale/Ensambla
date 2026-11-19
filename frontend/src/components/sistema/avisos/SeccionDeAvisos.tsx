import { useId } from 'react';
import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { Icon } from '../Icon';
import { TarjetaDeAviso } from './TarjetaDeAviso';
import { ICONO_DE_AVISO, NIVEL_DE_AVISO, NOMBRE_DE_TIPO, tituloDeGrupo } from './presentacion';

type Props = {
  /** Los avisos de un mismo tipo y nivel. */
  avisos: Aviso[];
  foco?: string | null;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

// Los avisos de un mismo tipo y nivel, siempre a la vista: un encabezado que dice cuántos son y una fila por aviso.
export function SeccionDeAvisos({ avisos, foco, onAccion, onDescartar }: Props) {
  const idTitulo = useId();
  const [primero] = avisos;
  if (!primero) return null;

  return (
    <section
      className={`aviso-seccion p${primero.prioridad}`}
      data-tipo={primero.tipo}
      data-grupo={primero.tipo}
      aria-labelledby={idTitulo}
    >
      <EncabezadoDeSeccion idTitulo={idTitulo} primero={primero} cantidad={avisos.length} />
      <ul className="aviso-seccion-filas">
        {avisos.map((aviso) => (
          <li key={aviso.id}>
            <TarjetaDeAviso
              aviso={aviso}
              enfocado={foco === aviso.id}
              conNegocio={hayVariosNegocios(avisos)}
              onAccion={onAccion}
              onDescartar={onDescartar}
            />
          </li>
        ))}
      </ul>
    </section>
  );
}

const hayVariosNegocios = (avisos: Aviso[]) =>
  new Set(avisos.map((aviso) => aviso.sistemaId)).size > 1;

function EncabezadoDeSeccion({
  idTitulo,
  primero,
  cantidad,
}: {
  idTitulo: string;
  primero: Aviso;
  cantidad: number;
}) {
  return (
    <header className="aviso-seccion-cabeza">
      <span className="aviso-seccion-icono">
        <Icon name={ICONO_DE_AVISO[primero.tipo]} />
      </span>
      <div className="aviso-seccion-textos">
        <span className="aviso-seccion-meta">
          <span className="aviso-seccion-tipo">{NOMBRE_DE_TIPO[primero.tipo]}</span>
          <span className="aviso-seccion-nivel">{NIVEL_DE_AVISO[primero.prioridad]}</span>
        </span>
        <h2 id={idTitulo}>{tituloDeGrupo(primero.tipo, primero.prioridad, cantidad)}</h2>
      </div>
    </header>
  );
}
