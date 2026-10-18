import type { CSSProperties, DragEvent } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import { findBlock, totals } from '@/lib/sistema/core';
import type { Ancho, Bloque, Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { varsDeColor } from './colores';
import { Icon } from './Icon';
import { IconBtn } from './IconBtn';
import { ValorDeCampo } from './ValorDeCampo';
import { WidthButtons } from './WidthButtons';

type PreviewBoardProps = {
  sistema: Sistema;
  /** Más chico, para el inicio y la vista previa de plantillas. */
  compacto?: boolean;
  seccionId?: string | null;
  /** La propuesta de Sami: se puede renombrar, quitar, ensanchar y arrastrar cada pieza. */
  editable?: boolean;
  onRenombrar?: (bloqueId: string) => void;
  onQuitar?: (bloqueId: string) => void;
  onAncho?: (bloqueId: string, ancho: Ancho) => void;
  onArrastrar?: (evento: DragEvent<HTMLButtonElement>, bloqueId: string) => void;
};

const CAMPOS_OCULTOS = ['notes', 'detail', 'minimum'];

const VACIO_POR_TIPO: Partial<Record<Bloque['type'], string>> = {
  entries: 'Cada ingreso suma stock.',
  exits: 'Cada retiro descuenta stock.',
  sales: 'Cada venta descuenta stock.',
};

function valorDeVista(sistema: Sistema, campo: Campo, valor: Valor) {
  if (campo.type === 'relation') {
    const objetivo = findBlock(sistema, campo.target);
    const clave = objetivo?.fields[0]?.key;
    const fila = objetivo?.rows.find((item) => item.id === valor);
    return <>{String((clave && fila?.[clave]) || '—')}</>;
  }
  return <ValorDeCampo sistema={sistema} campo={campo} valor={valor} />;
}

function Contenido({
  bloque,
  sistema,
  compacto,
}: {
  bloque: Bloque;
  sistema: Sistema;
  compacto: boolean;
}) {
  if (bloque.type === 'summary') {
    const resumen = totals(sistema);
    return (
      <div className="preview-summary">
        <strong>{resumen.stock}</strong>
        <span>unidades de stock</span>
        <strong>{resumen.clients}</strong>
        <span>clientes</span>
      </div>
    );
  }
  if (bloque.type === 'note') return <p className="preview-note">{bloque.content}</p>;
  const campos = bloque.fields
    .filter((campo) => !CAMPOS_OCULTOS.includes(campo.key))
    .slice(0, compacto ? 3 : 4);
  const filas = bloque.rows.slice(0, compacto ? 2 : 3);
  return (
    <div className="preview-table-scroll">
      <table className="preview-table">
        <thead>
          <tr>
            {campos.map((campo) => (
              <th key={campo.key}>{campo.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.length ? (
            filas.map((fila) => (
              <tr key={fila.id}>
                {campos.map((campo) => (
                  <td key={campo.key}>{valorDeVista(sistema, campo, fila[campo.key])}</td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={campos.length}>
                <span className="empty-preview">
                  {VACIO_POR_TIPO[bloque.type] ?? 'Un lugar listo para tus registros.'}
                </span>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

// La vista previa de un sistema con los bloques en su distribución: el inicio, las plantillas y la propuesta de Sami.
export function PreviewBoard({
  sistema,
  compacto = false,
  seccionId = null,
  editable = false,
  onRenombrar,
  onQuitar,
  onAncho,
  onArrastrar,
}: PreviewBoardProps) {
  const seccion = sistema.sections.find((item) => item.id === seccionId) ?? sistema.sections[0];
  const bloques = seccion?.blocks ?? [];

  return (
    <div
      className={`preview-grid ${compacto ? 'compact' : ''}`.trim()}
      id={editable ? 'draft-canvas' : undefined}
      aria-label={`Propuesta visual de ${sistema.name}`}
      data-tour={editable ? 'draft-canvas' : undefined}
    >
      {bloques.length === 0 && (
        <div className="draft-empty">
          <Icon name="plus" />
          Elegí una pieza de arriba para empezar.
        </div>
      )}
      {bloques.map((bloque) => {
        const estilo: CSSProperties = {
          ...varsDeColor(bloque.color),
          ...(bloque.startRow ? { gridColumnStart: 1 } : {}),
        };
        return (
          <section
            key={bloque.id}
            className="preview-block"
            data-width={bloque.width}
            data-draft-block={editable ? bloque.id : undefined}
            style={estilo}
          >
            <div className="preview-block-head">
              <span className="preview-block-icon">
                <Icon name={CATALOG[bloque.type].icon} />
              </span>
              {editable ? (
                <button
                  type="button"
                  className="preview-name"
                  onClick={() => onRenombrar?.(bloque.id)}
                >
                  {bloque.name} <Icon name="edit" />
                </button>
              ) : (
                <strong>{bloque.name}</strong>
              )}
              {editable && (
                <div className="preview-controls">
                  <button
                    type="button"
                    className="icon-btn"
                    title="Arrastrar esta pieza"
                    aria-label="Arrastrar esta pieza"
                    draggable
                    data-drag-kind="draft"
                    data-value={bloque.id}
                    onDragStart={(evento) => onArrastrar?.(evento, bloque.id)}
                  >
                    <Icon name="grip" />
                  </button>
                  <IconBtn
                    name="close"
                    label="Quitar de la propuesta"
                    onClick={() => onQuitar?.(bloque.id)}
                  />
                </div>
              )}
            </div>
            {editable && (
              <div className="preview-sizing">
                <span>Arrastrá para ordenar</span>
                <WidthButtons
                  ancho={bloque.width}
                  onElegir={(ancho) => onAncho?.(bloque.id, ancho)}
                />
              </div>
            )}
            <Contenido bloque={bloque} sistema={sistema} compacto={compacto} />
            {!compacto && (
              <div className="preview-field-tags">
                {bloque.fields.slice(0, 5).map((campo) => (
                  <span key={campo.key}>{campo.label}</span>
                ))}
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}
