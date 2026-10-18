import { CATALOG, TEMPLATES } from '@/lib/sistema/catalogo';
import type { ClavePlantilla } from '@/lib/sistema/tipos';
import { Btn } from './Btn';
import { Icon } from './Icon';
import { varsDeColor } from './colores';

// Una plantilla como conjunto de piezas, igual a templateCard() del mockup.
export function TemplateCard({
  clave,
  oculta = false,
  onVer,
}: {
  clave: ClavePlantilla;
  oculta?: boolean;
  onVer: (clave: ClavePlantilla) => void;
}) {
  const plantilla = TEMPLATES[clave];
  return (
    <article
      className="template-card template-set"
      data-template={clave}
      style={varsDeColor(plantilla.color)}
      hidden={oculta}
    >
      <div className="set-visual" aria-hidden="true">
        <div className="set-top">
          <span />
          <span />
          <span />
        </div>
        <div className={`set-modules ${clave}`}>
          {plantilla.types.map((tipo) => (
            <div key={tipo} className="set-piece" style={varsDeColor(CATALOG[tipo].color)}>
              <Icon name={CATALOG[tipo].icon} />
              <strong>{CATALOG[tipo].name}</strong>
              <span />
              <span />
            </div>
          ))}
        </div>
        <span className="set-count">{plantilla.types.length} bloques conectados</span>
      </div>
      <div className="template-body">
        <div className="row between">
          <h3>{plantilla.name}</h3>
          <span className="icon-tile" style={varsDeColor(plantilla.color)}>
            <Icon name={plantilla.icon} />
          </span>
        </div>
        <strong className="template-subtitle">{plantilla.subtitle}</strong>
        <p>{plantilla.description}</p>
        <div className="field-tags">
          {plantilla.tags.map((etiqueta) => (
            <span key={etiqueta}>{etiqueta}</span>
          ))}
        </div>
        <div className="template-actions">
          <Btn cls="primary small" onClick={() => onVer(clave)}>
            Ver el sistema <Icon name="arrow" />
          </Btn>
        </div>
      </div>
    </article>
  );
}
