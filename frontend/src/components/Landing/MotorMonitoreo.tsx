import { Link } from '@tanstack/react-router';
import { BellRingingIcon, CheckCircleIcon, XCircleIcon } from '@phosphor-icons/react';
import { AVISOS, useAvisos } from '@/hooks/landing/useAvisos';
import { cn } from '@/lib/cn';
import { variables } from './estilos';

const CAPACIDADES = [
  { color: 'coral', texto: 'Precios mal ubicados frente a los comercios de tu zona' },
  { color: 'naranja', texto: 'Productos muy vendidos que aguantan un aumento' },
  { color: 'celeste', texto: 'Productos estancados, con sugerencia de rebaja' },
  { color: 'amarillo', texto: 'Combos con lo que sale poco y qué se consume cerca' },
] as const;

export function MotorMonitoreo() {
  const { resueltos, pendientes, resolver, reiniciar } = useAvisos();

  return (
    <section className="seccion" id="monitoreo">
      <div className="contenedor monitoreo">
        <div className="monitoreo__texto revelar">
          <p className="sobretitulo">Motor de monitoreo</p>
          <h2>Tu negocio te avisa lo que importa.</h2>
          <p className="monitoreo__bajada">
            Ensambla mira tus ventas, tu stock y tus precios, los compara con tu zona y te avisa en
            lenguaje simple dónde hay una oportunidad o un riesgo. Cada aviso trae su motivo y una
            acción sugerida: la aplicás o la descartás.
          </p>
          <ul className="monitoreo__lista">
            {CAPACIDADES.map(({ color, texto }) => (
              <li key={texto}>
                <span
                  className={`monitoreo__punto monitoreo__punto--${color}`}
                  aria-hidden="true"
                />
                {texto}
              </li>
            ))}
          </ul>
          <p className="monitoreo__nota">
            Los avisos de zona usan datos agregados y anónimos de varios comercios. Nunca se muestra
            el dato de un negocio en particular.
          </p>
          <Link to="/registro" className="boton boton--primario">
            Quiero mis avisos
          </Link>
        </div>

        <div className="avisos revelar" style={variables({ '--d': '.1s' })}>
          <div className="avisos__barra">
            <span className="avisos__campana" aria-hidden="true">
              <BellRingingIcon size={20} weight="fill" />
            </span>
            <span className="avisos__titulo">Centro de avisos</span>
            <span className="avisos__contador" aria-live="polite">
              {pendientes === 0
                ? 'Todo al día'
                : pendientes === 1
                  ? '1 aviso'
                  : `${pendientes} avisos`}
            </span>
          </div>

          <ul className="avisos__lista">
            {AVISOS.map((aviso) => {
              const resolucion = resueltos[aviso.id];
              return (
                <li key={aviso.id}>
                  <article
                    className={cn(
                      'aviso',
                      `aviso--${aviso.color}`,
                      resolucion && 'aviso--resuelto',
                    )}
                    aria-label={aviso.titulo}
                  >
                    {resolucion ? (
                      <p className="aviso__resultado">
                        {resolucion === 'aplicado' ? (
                          <CheckCircleIcon aria-hidden size={20} weight="fill" />
                        ) : (
                          <XCircleIcon aria-hidden size={20} weight="fill" />
                        )}
                        <span>
                          {resolucion === 'aplicado' ? aviso.resultado : 'Aviso descartado.'}
                        </span>
                      </p>
                    ) : (
                      <>
                        <span className="aviso__tipo">{aviso.tipo}</span>
                        <h3>{aviso.titulo}</h3>
                        <p className="aviso__motivo">{aviso.motivo}</p>
                        <div className="aviso__acciones">
                          <button
                            type="button"
                            className="aviso__boton aviso__boton--aplicar"
                            aria-label={`Aplicar: ${aviso.accion}`}
                            onClick={() => resolver(aviso.id, 'aplicado')}
                          >
                            {aviso.accion}
                          </button>
                          <button
                            type="button"
                            className="aviso__boton"
                            aria-label={`Descartar aviso: ${aviso.titulo}`}
                            onClick={() => resolver(aviso.id, 'descartado')}
                          >
                            Descartar
                          </button>
                        </div>
                      </>
                    )}
                  </article>
                </li>
              );
            })}
          </ul>

          <div className="avisos__pie">
            <span>Ejemplo con datos de muestra.</span>
            {pendientes === 0 && (
              <button type="button" className="avisos__reiniciar" onClick={reiniciar}>
                Ver los avisos de nuevo
              </button>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
