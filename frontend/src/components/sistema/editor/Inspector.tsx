import { COLORS, NOMBRES_DE_COLOR } from '@/lib/sistema/catalogo';
import type { Ancho, Bloque, Color, Sistema } from '@/lib/sistema/tipos';
import {
  cambiarAncho,
  cambiarColor,
  cambiarDatoDelBloque,
  cambiarSeccionDelBloque,
} from '@/stores/accionesDelEditor';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { PanelDelBloque } from './PanelesDelInspector';
import { TextoAlSalir } from './TextoAlSalir';

const ANCHOS: [Ancho, string][] = [
  ['full', 'Toda la fila'],
  ['half', 'Media fila'],
  ['wide', 'Dos tercios de la fila'],
  ['third', 'Un tercio de la fila'],
];

// El panel que se despliega a la derecha en Construir con los ajustes del bloque elegido; la X lo cierra.
export function Inspector({ bloque, sistema }: { bloque: Bloque; sistema: Sistema }) {
  const seleccionar = useSistemaStore((estado) => estado.seleccionar);

  const seccionDelBloque = sistema.sections.find((seccion) =>
    seccion.blocks.some((otro) => otro.id === bloque.id),
  );

  return (
    <aside className="inspector has-selection" aria-label={`Personalizar ${bloque.name}`}>
      <div className="row between inspector-heading">
        <div>
          <h2>Personalizar bloque</h2>
          <p>Cada cambio se guarda solo.</p>
        </div>
        <IconBtn
          name="close"
          label="Cerrar personalización"
          data-mobile-close="true"
          onClick={() => seleccionar(null)}
        />
      </div>
      <div className="field">
        <label htmlFor="block-name">Nombre</label>
        <TextoAlSalir
          id="block-name"
          data-tour="rename"
          maxLength={80}
          valor={bloque.name}
          alGuardar={(valor) => valor && cambiarDatoDelBloque(bloque.id, 'name', valor)}
        />
      </div>
      <div className="field">
        <label htmlFor="block-description">Ayuda breve</label>
        <TextoAlSalir
          area
          id="block-description"
          maxLength={220}
          valor={bloque.description}
          alGuardar={(valor) => cambiarDatoDelBloque(bloque.id, 'description', valor)}
        />
      </div>
      <div className="field">
        <span className="field-label">Color del bloque</span>
        <div className="swatches">
          {(Object.entries(COLORS) as [Color, string][]).map(([color, hex]) => (
            <button
              key={color}
              type="button"
              className={`swatch ${bloque.color === color ? 'selected' : ''}`.trim()}
              style={{ '--swatch': hex } as React.CSSProperties}
              aria-label={NOMBRES_DE_COLOR[color]}
              aria-pressed={bloque.color === color}
              onClick={() => cambiarColor(bloque.id, color)}
            >
              {bloque.color === color && <Icon name="check" />}
            </button>
          ))}
        </div>
      </div>
      <div className="field">
        <label htmlFor="block-width">Ancho en la pantalla</label>
        <select
          id="block-width"
          value={bloque.width}
          onChange={(evento) => cambiarAncho(bloque.id, evento.target.value as Ancho)}
        >
          {ANCHOS.map(([valor, texto]) => (
            <option key={valor} value={valor}>
              {texto}
            </option>
          ))}
        </select>
        <span className="input-hint">En celulares, cada bloque ocupa una fila.</span>
      </div>
      <div className="field">
        <label htmlFor="block-section">Pestaña</label>
        <select
          id="block-section"
          value={seccionDelBloque?.id}
          onChange={(evento) => cambiarSeccionDelBloque(bloque.id, evento.target.value)}
        >
          {sistema.sections.map((seccion) => (
            <option key={seccion.id} value={seccion.id}>
              {seccion.name}
            </option>
          ))}
        </select>
      </div>
      <PanelDelBloque bloque={bloque} />
    </aside>
  );
}
