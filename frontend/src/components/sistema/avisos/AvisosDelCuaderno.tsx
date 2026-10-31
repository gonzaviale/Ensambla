import { useAbrirAvisos } from '@/hooks/avisos/useAbrirAvisos';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';

const NIVEL = { 3: 'Urgente', 2: 'Para hoy', 1: 'Para mirar' } as const;
const VISIBLES = 3;

// Los avisos de este cuaderno, arriba de todo y sin adornos: qué pasa, una línea de contexto y «Ver».
export function AvisosDelCuaderno({ sistemaId }: { sistemaId: string }) {
  const avisos = useSistemaStore((estado) => estado.avisos)
    .filter((aviso) => aviso.sistemaId === sistemaId)
    .sort((a, b) => b.prioridad - a.prioridad);
  const acciones = useAccionesDeAvisos();
  const abrirAvisos = useAbrirAvisos();

  if (!avisos.length) {
    return (
      <section className="sami-franja ok" aria-label="Avisos de Sami">
        <img className="sami-franja-img" src="/mockup/sami-guino.webp" alt="" />
        <p>
          <strong>Todo en orden.</strong> Sigo mirando tu stock, tus ventas y los precios de tu
          zona.
        </p>
      </section>
    );
  }

  return (
    <section className="avisos-cuaderno" aria-labelledby="avisos-cuaderno-titulo">
      <header className="avisos-cuaderno-cabeza">
        <h2 id="avisos-cuaderno-titulo">
          {avisos.length === 1
            ? 'Encontré algo para mirar'
            : `Encontré ${avisos.length} cosas para mirar`}
        </h2>
        {avisos.length > VISIBLES && (
          <button type="button" className="text-link" onClick={() => void abrirAvisos()}>
            Ver los {avisos.length} <Icon name="arrow" />
          </button>
        )}
      </header>
      <ul className="avisos-cuaderno-lista">
        {avisos.slice(0, VISIBLES).map((aviso) => (
          <li key={aviso.id} className={`aviso-cuaderno p${aviso.prioridad}`}>
            <span className="aviso-cuaderno-nivel">{NIVEL[aviso.prioridad]}</span>
            <div className="aviso-cuaderno-texto">
              <strong>{aviso.titulo}</strong>
              <p>{aviso.detalle}</p>
            </div>
            <button
              type="button"
              className="aviso-cuaderno-ver"
              aria-label={`Ver: ${aviso.titulo}`}
              onClick={() => acciones.ver(aviso)}
            >
              Ver <Icon name="arrow" />
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
