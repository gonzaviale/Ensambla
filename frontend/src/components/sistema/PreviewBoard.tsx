import { useState, type CSSProperties, type DragEvent } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import { findBlock, totals } from '@/lib/sistema/core';
import type { Ancho, Bloque, Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { LIENZO } from './arrastre';
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
  /** Editar un registro de ejemplo (o agregar uno, con `filaId` en `null`) antes de crear el sistema. */
  onRegistro?: (bloqueId: string, filaId: string | null) => void;
  onQuitar?: (bloqueId: string) => void;
  onAncho?: (bloqueId: string, ancho: Ancho) => void;
  /** Arrastrar y soltar las piezas de la propuesta. */
  arrastrando?: string | null;
  /** La pieza (o `#canvas`, el lienzo vacío) sobre la que está pasando lo que se arrastra. */
  sobre?: string | null;
  onArrastrar?: (evento: DragEvent<HTMLButtonElement>, bloqueId: string) => void;
  onSobre?: (destino: string) => void;
  onSoltar?: (antesDe: string | null) => void;
  onFinArrastre?: () => void;
};

const bloqueDeEvento = (evento: DragEvent<HTMLElement>) =>
  (evento.target as HTMLElement).closest<HTMLElement>('[data-draft-block]')?.dataset.draftBlock ??
  null;

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
  onRegistro,
}: {
  bloque: Bloque;
  sistema: Sistema;
  compacto: boolean;
  /** Con esto la tabla deja editar y agregar registros. */
  onRegistro?: (bloqueId: string, filaId: string | null) => void;
}) {
  const [verTodas, setVerTodas] = useState(false);
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
  const limite = compacto ? 2 : 3;
  const filas = verTodas ? bloque.rows : bloque.rows.slice(0, limite);
  return (
    <div className={`preview-table-scroll${verTodas ? 'preview-table-expandida' : ''}`}>
      <table className="preview-table">
        <thead>
          <tr>
            {campos.map((campo) => (
              <th key={campo.key}>{campo.label}</th>
            ))}
            {onRegistro && (
              <th className="preview-row-actions">
                <span className="sr-only">Acciones</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {filas.length ? (
            filas.map((fila) => (
              <tr key={fila.id} className={fila.__example ? 'example-row' : undefined}>
                {campos.map((campo, indice) => (
                  <td key={campo.key}>
                    {indice === 0 && fila.__example && <em className="example-badge">Ejemplo</em>}
                    {valorDeVista(sistema, campo, fila[campo.key])}
                  </td>
                ))}
                {onRegistro && (
                  <td className="preview-row-actions">
                    <IconBtn
                      name="edit"
                      label="Editar registro"
                      onClick={() => onRegistro(bloque.id, fila.id)}
                    />
                  </td>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={campos.length + (onRegistro ? 1 : 0)}>
                <span className="empty-preview">
                  {VACIO_POR_TIPO[bloque.type] ?? 'Un lugar listo para tus registros.'}
                </span>
              </td>
            </tr>
          )}
        </tbody>
      </table>
      {bloque.rows.length > limite && (
        <button
          type="button"
          className="preview-more"
          onClick={() => setVerTodas((valor) => !valor)}
        >
          {verTodas ? 'Ver menos' : `Ver todas (${bloque.rows.length})`}
        </button>
      )}
      {onRegistro && (
        <button type="button" className="preview-more" onClick={() => onRegistro(bloque.id, null)}>
          + Agregar registro
        </button>
      )}
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
  onRegistro,
  onQuitar,
  onAncho,
  arrastrando = null,
  sobre = null,
  onArrastrar,
  onSobre,
  onSoltar,
  onFinArrastre,
}: PreviewBoardProps) {
  const seccion = sistema.sections.find((item) => item.id === seccionId) ?? sistema.sections[0];
  const bloques = seccion?.blocks ?? [];

  return (
    <div
      className={`preview-grid ${compacto ? 'compact' : ''} ${sobre === LIENZO ? 'drop-active' : ''}`.trim()}
      id={editable ? 'draft-canvas' : undefined}
      onDragOver={
        editable && arrastrando
          ? (evento) => {
              evento.preventDefault();
              const bloque = bloqueDeEvento(evento);
              onSobre?.(bloque && bloque !== arrastrando ? bloque : LIENZO);
            }
          : undefined
      }
      onDrop={
        editable && arrastrando
          ? (evento) => {
              evento.preventDefault();
              onSoltar?.(bloqueDeEvento(evento));
            }
          : undefined
      }
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
            className={`preview-block ${arrastrando === bloque.id ? 'dragging' : ''} ${sobre === bloque.id ? 'drag-over' : ''}`.trim()}
            data-width={bloque.width}
            data-color={bloque.color}
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
                    onDragEnd={onFinArrastre}
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
              <div className="preview-sizing" data-tour="review">
                <span>Arrastrá para ordenar</span>
                <WidthButtons
                  ancho={bloque.width}
                  accion="draft-width"
                  onElegir={(ancho) => onAncho?.(bloque.id, ancho)}
                />
              </div>
            )}
            <Contenido
              bloque={bloque}
              sistema={sistema}
              compacto={compacto}
              onRegistro={editable ? onRegistro : undefined}
            />
          </section>
        );
      })}
    </div>
  );
}
