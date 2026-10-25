import { PASOS } from '@/lib/sistema/tutorial';
import { salirDelTutorial } from '@/hooks/tutorial/useAccionesDelTutorial';
import { useTutorialEnPantalla } from '@/hooks/tutorial/useTutorialEnPantalla';
import { Btn } from '../Btn';
import { Icon } from '../Icon';

// La guía interactiva: un reflector que deja iluminado sólo lo que toca y un cartel de Sami que explica el
// paso. Se avanza haciendo: cada paso comprueba una acción real (ver lib/sistema/tutorial).
export function Tutorial() {
  const { enPausa, paso, cajas, cartel, refCartel, pensando } = useTutorialEnPantalla();
  if (!paso) return <div id="ensambla-tutorial" hidden />;
  const detalle = PASOS[paso];
  const pensandoAhora = paso === 'describe' && pensando;
  const terminado = paso === 'done';

  return (
    <div id="ensambla-tutorial" hidden={enPausa}>
      <svg className="tour-spotlight" aria-hidden="true">
        <defs>
          <mask id="tour-mask">
            <rect width="100%" height="100%" fill="white" />
            <g className="tour-holes">
              {cajas.map((caja, indice) => (
                <rect
                  key={indice}
                  x={caja.x}
                  y={caja.y}
                  width={caja.w}
                  height={caja.h}
                  rx={12}
                  fill="black"
                />
              ))}
            </g>
          </mask>
        </defs>
        <rect width="100%" height="100%" fill="#102f42" fillOpacity=".62" mask="url(#tour-mask)" />
        <g className="tour-rings">
          {cajas.map((caja, indice) => (
            <rect
              key={indice}
              x={caja.x}
              y={caja.y}
              width={caja.w}
              height={caja.h}
              rx={12}
              fill="none"
              stroke="#EBCE32"
              strokeWidth={3}
            />
          ))}
        </g>
      </svg>
      <section
        ref={refCartel}
        className="tour-coach"
        aria-label="Guía interactiva de Sami"
        aria-describedby="tour-title"
        data-step={`${paso}:${pensandoAhora}`}
        style={{ width: cartel.width, left: cartel.left, top: cartel.top }}
      >
        <div className="tour-coach-top">
          <span>GUÍA DE TU PRIMER SISTEMA</span>
          <button type="button" aria-label="Salir del tutorial" onClick={salirDelTutorial}>
            <Icon name="close" />
          </button>
        </div>
        <div className="tour-progress" aria-label={`Etapa ${detalle.grupo + 1} de 5`}>
          {Array.from({ length: 5 }, (_, indice) => (
            <i key={indice} className={indice <= detalle.grupo ? 'active' : ''} />
          ))}
        </div>
        <div className="tour-message">
          <img
            className="tour-sami"
            src={pensandoAhora ? '/mockup/sami-pensando.webp' : '/mockup/sami-saludando.webp'}
            alt=""
          />
          <div>
            <h2 id="tour-title">
              {pensandoAhora ? 'Estoy pensando tu propuesta…' : detalle.title}
            </h2>
            <p>
              {pensandoAhora
                ? 'Enseguida la vemos armada. Después la acomodamos a tu manera.'
                : detalle.text}
            </p>
          </div>
        </div>
        <div className="tour-hint" role="status">
          <Icon name={terminado ? 'check-circle' : 'arrow'} />
          <span>{pensandoAhora ? 'Preparando las piezas…' : detalle.hint}</span>
        </div>
      </section>
    </div>
  );
}
