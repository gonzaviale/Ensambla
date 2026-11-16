import type { Color, Seccion } from '@/lib/sistema/tipos';
import { varsDeColor } from '../colores';

const COLORES_DE_SECCION: Color[] = ['orange', 'blue', 'coral', 'petrol', 'yellow'];

type SeccionesTabsProps = {
  secciones: Seccion[];
  activaId: string;
  onElegir: (seccionId: string) => void;
  /** La pestaña sobre la que está pasando lo que se arrastra. */
  sobreId?: string | null;
};

// Las pastillas de pestaña de la sección. Con una sola pestaña no hace falta la barra: quien la usa decide cuándo
// mostrarla (se agrega otra desde el ícono «Agregar y organizar»).
export function SeccionesTabs({
  secciones,
  activaId,
  onElegir,
  sobreId = null,
}: SeccionesTabsProps) {
  return (
    <div className="tabs-y-distribucion">
      <nav className="section-tabs" aria-label="Pestañas de la sección">
        {secciones.map((otra, indice) => {
          const color = COLORES_DE_SECCION[indice % COLORES_DE_SECCION.length] ?? 'orange';
          return (
            <div
              key={otra.id}
              style={varsDeColor(color)}
              data-color={color}
              className={`section-tab ${otra.id === activaId ? 'active' : ''} ${sobreId === otra.id ? 'drop-active' : ''}`
                .replace(/\s+/g, ' ')
                .trim()}
              data-drop-section={otra.id}
            >
              <button
                type="button"
                aria-current={otra.id === activaId ? 'page' : 'false'}
                onClick={() => onElegir(otra.id)}
              >
                {otra.name}
                <span className="section-tab-cuenta" aria-hidden="true">
                  {otra.blocks.length}
                </span>
              </button>
            </div>
          );
        })}
      </nav>
    </div>
  );
}
