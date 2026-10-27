import { useEffect, useState } from 'react';
import { COLORS, FIELD_TYPES, NOMBRES_DE_COLOR } from '@/lib/sistema/catalogo';
import type { Ancho, Bloque, Color, Sistema, TipoCampo } from '@/lib/sistema/tipos';
import {
  cambiarAncho,
  cambiarColor,
  cambiarDatoDelBloque,
  cambiarSeccionDelBloque,
} from '@/stores/accionesDelEditor';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';

const ANCHOS: [Ancho, string][] = [
  ['full', 'Toda la fila'],
  ['half', 'Media fila'],
  ['wide', 'Dos tercios de la fila'],
  ['third', 'Un tercio de la fila'],
];

const INSIGNIAS: Record<TipoCampo, string> = {
  text: 'Aa',
  textarea: '¶',
  number: '#',
  currency: '$',
  date: '◷',
  time: '◷',
  select: '≡',
  email: '@',
  tel: '☎',
  checkbox: '✓',
  relation: '↗',
};

// Un texto que se guarda al salir del campo («change» en el mockup), no con cada tecla.
function TextoAlSalir({
  valor,
  alGuardar,
  area = false,
  ...resto
}: {
  valor: string;
  alGuardar: (valor: string) => void;
  area?: boolean;
  id: string;
  maxLength: number;
  'data-tour'?: string;
}) {
  const [escrito, setEscrito] = useState(valor);
  useEffect(() => setEscrito(valor), [valor]);
  const salir = () => {
    const limpio = escrito.trim();
    if (limpio === valor) return;
    alGuardar(limpio);
  };
  return area ? (
    <textarea
      {...resto}
      value={escrito}
      onChange={(e) => setEscrito(e.target.value)}
      onBlur={salir}
    />
  ) : (
    <input
      {...resto}
      className="input"
      value={escrito}
      onChange={(e) => setEscrito(e.target.value)}
      onBlur={salir}
    />
  );
}

// El panel que se despliega a la derecha en Construir con los ajustes del bloque elegido; la X lo cierra.
export function Inspector({ bloque, sistema }: { bloque: Bloque; sistema: Sistema }) {
  const seleccionar = useSistemaStore((estado) => estado.seleccionar);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);

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
        <label htmlFor="block-section">Sección</label>
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
      {bloque.type === 'note' ? (
        <div className="field">
          <label htmlFor="note-content">Tu nota</label>
          <TextoAlSalir
            area
            id="note-content"
            maxLength={3000}
            valor={bloque.content}
            alGuardar={(valor) => cambiarDatoDelBloque(bloque.id, 'content', valor)}
          />
        </div>
      ) : bloque.type === 'summary' ? (
        <p className="inspector-hint">
          Estos indicadores toman los registros de todos los bloques del cuaderno y se actualizan
          cuando cargás datos.
        </p>
      ) : (
        <>
          <div className="inspector-section-label">
            Campos de tus registros<span className="tag outline">{bloque.fields.length}</span>
          </div>
          {bloque.fields.map((campo) => (
            <button
              key={campo.key}
              type="button"
              className="field-item"
              onClick={() => abrirModal({ tipo: 'campo', bloqueId: bloque.id, clave: campo.key })}
            >
              <span className="field-type-badge" data-tipo={campo.type}>
                {INSIGNIAS[campo.type]}
              </span>
              <span>
                <strong>
                  {campo.label}
                  {campo.required ? ' *' : ''}
                </strong>
                <small>{FIELD_TYPES[campo.type]}</small>
              </span>
              <Icon name="edit" />
            </button>
          ))}
          <Btn
            cls="secondary small full"
            onClick={() => abrirModal({ tipo: 'campo', bloqueId: bloque.id })}
          >
            <Icon name="plus" /> Agregar campo
          </Btn>
          <p className="inspector-hint">
            Cambiá los nombres sin perder datos. Los vínculos conectan registros de distintos
            bloques.
          </p>
        </>
      )}
    </aside>
  );
}
