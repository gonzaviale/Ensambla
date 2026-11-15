import { useRespuestasPendientes } from '@/hooks/objetivos/queries';
import { MiniManualCard } from './autogestion/MiniManualCard';

// Lo que Sami espera saber, arriba de los objetivos. Es un recordatorio tranquilo: sin contadores rojos ni plazos, y
// se responde ahí mismo, sin entrar al objetivo.
export function PendientesDeRespuesta() {
  const { pendientes } = useRespuestasPendientes();
  if (pendientes.length === 0) return null;
  return (
    <section className="objetivos-pendientes" aria-labelledby="objetivos-pendientes-titulo">
      <h2 id="objetivos-pendientes-titulo">
        Sami quiere saber cómo te fue{' '}
        <span className="objetivos-pendientes-cuenta">({pendientes.length})</span>
      </h2>
      <div className="objetivos-pendientes-lista">
        {pendientes.map(({ objetivoId, objetivoTexto, miniObjetivo }) => (
          <MiniManualCard
            key={miniObjetivo.id}
            mini={miniObjetivo}
            objetivoId={objetivoId}
            objetivoTexto={objetivoTexto}
            pendiente
          />
        ))}
      </div>
    </section>
  );
}
