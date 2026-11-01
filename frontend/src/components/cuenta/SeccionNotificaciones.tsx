import { useAjustesDeAvisos } from '@/hooks/avisos/useAjustesDeAvisos';
import { GRUPOS, type GrupoDeAviso } from '@/lib/sistema/alertas';
import { MINIMO_DE_COMERCIOS } from '@/lib/sistema/zona';
import { Icon } from '../sistema/Icon';

const GRUPOS_EN_ORDEN = Object.entries(GRUPOS) as [GrupoDeAviso, string][];

// Qué le avisa Sami a la persona: los temas se prenden y se apagan acá, y se pueden volver a mostrar los avisos ocultos.
export function SeccionNotificaciones() {
  const ajustes = useAjustesDeAvisos();

  return (
    <section className="cuenta-tarjeta" aria-labelledby="cuenta-avisos-titulo">
      <h2 id="cuenta-avisos-titulo">Qué me avisa Sami</h2>
      <p className="cuenta-ayuda">Elegí de qué temas querés recibir avisos.</p>
      <div className="cuenta-temas">
        {GRUPOS_EN_ORDEN.map(([clave, nombre]) => (
          <label className="check-row ajuste-tema" key={clave}>
            <input
              type="checkbox"
              checked={ajustes.grupos[clave] !== false}
              onChange={(evento) => ajustes.alternarGrupo(clave, evento.target.checked)}
            />
            {nombre}
          </label>
        ))}
      </div>
      {ajustes.descartadas > 0 && (
        <button type="button" className="text-link" onClick={ajustes.restaurar}>
          Volver a mostrar{' '}
          {ajustes.descartadas === 1 ? '1 aviso oculto' : `${ajustes.descartadas} avisos ocultos`}
        </button>
      )}
      <p className="alertas-nota">
        <Icon name="help" />
        Sami arma los avisos con reglas sobre tus datos y sobre datos agregados de tu zona: nunca se
        muestra un comercio puntual y hace falta un mínimo de {MINIMO_DE_COMERCIOS} para que un dato
        cuente. Cada aviso muestra de dónde sale. No cambia nada sin que vos lo apliques.
      </p>
    </section>
  );
}
