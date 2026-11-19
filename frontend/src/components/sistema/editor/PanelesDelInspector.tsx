import type { ComponentType } from 'react';
import { FIELD_TYPES } from '@/lib/sistema/catalogo';
import type { Bloque, TipoBloque, TipoCampo } from '@/lib/sistema/tipos';
import { cambiarDatoDelBloque } from '@/stores/accionesDelEditor';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { TextoAlSalir } from './TextoAlSalir';

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

type PanelProps = { bloque: Bloque };

function PanelDeNota({ bloque }: PanelProps) {
  return (
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
  );
}

function PanelDeResumen() {
  return (
    <p className="inspector-hint">
      Estos indicadores toman los registros de todos los bloques de la sección y se actualizan
      cuando cargás datos.
    </p>
  );
}

function PanelDeCampos({ bloque }: PanelProps) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  return (
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
  );
}

// Los ajustes propios de cada tipo de bloque; los que no tienen paneles propios muestran sus campos.
const PANELES_POR_TIPO: Partial<Record<TipoBloque, ComponentType<PanelProps>>> = {
  note: PanelDeNota,
  summary: PanelDeResumen,
};

export function PanelDelBloque({ bloque }: PanelProps) {
  const Panel = PANELES_POR_TIPO[bloque.type] ?? PanelDeCampos;
  return <Panel bloque={bloque} />;
}
