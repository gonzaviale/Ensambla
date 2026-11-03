import { useMemo, useState, type CSSProperties, type DragEvent } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import { normalize, totals } from '@/lib/sistema/core';
import { textoPlano } from '@/lib/sistema/formato';
import { useTandas } from '@/hooks/sistemas/useTandas';
import { conNuevasPrimero, TANDA_DE_REGISTROS } from '@/lib/sistema/tandas';
import type { Ancho, Bloque, Sistema } from '@/lib/sistema/tipos';
import { LIENZO } from './arrastre';
import { Btn } from './Btn';
import { varsDeColor } from './colores';
import { Icon } from './Icon';
import { IconBtn } from './IconBtn';
import { IconTile } from './IconTile';
import { ValorDeCampo } from './ValorDeCampo';
import { WidthButtons } from './WidthButtons';

const bloqueDeEvento = (evento: DragEvent<HTMLElement>) =>
  (evento.target as HTMLElement).closest<HTMLElement>('[data-draft-block]')?.dataset.draftBlock ??
  null;

function Resumen({ sistema }: { sistema: Sistema }) {
  const total = totals(sistema);
  const indicadores: [number, string][] = [
    [total.records, 'Registros en tu cuaderno'],
    [total.pending, 'Pedidos pendientes'],
    [total.stock, 'Unidades en inventario'],
    [total.clients, 'Clientes registrados'],
  ];
  return (
    <div className="summary-block">
      {indicadores.map(([valor, etiqueta]) => (
        <div key={etiqueta}>
          <div className="summary-value">{valor}</div>
          <div className="summary-label">{etiqueta}</div>
        </div>
      ))}
    </div>
  );
}

type TablaProps = {
  bloque: Bloque;
  sistema: Sistema;
  nuevas: ReadonlySet<string>;
  /** Editar un registro de la propuesta (o agregar uno, con `filaId` en `null`). */
  onRegistro: (bloqueId: string, filaId: string | null) => void;
};

// La tabla de un bloque: los registros nuevos arriba y resaltados, y el resto por tandas dentro del propio bloque
// (con su scroll), así una pieza con cientos de registros no estira la página.
function Tabla({ bloque, sistema, nuevas, onRegistro }: TablaProps) {
  const [consulta, setConsulta] = useState('');
  const buscado = normalize(consulta);
  const coincidentes = useMemo(() => {
    const ordenadas = conNuevasPrimero(bloque.rows, nuevas);
    if (!buscado) return ordenadas;
    return ordenadas.filter((fila) =>
      normalize(bloque.fields.map((campo) => textoPlano(sistema, campo, fila[campo.key])).join(' ')).includes(
        buscado,
      ),
    );
  }, [bloque.rows, bloque.fields, nuevas, buscado, sistema]);

  const { visibles, quedan, sumar, alDesplazar } = useTandas(
    coincidentes.length,
    TANDA_DE_REGISTROS,
    buscado,
  );
  const filas = coincidentes.slice(0, visibles);

  return (
    <>
      <div className="block-bar">
        <label className="searchbox">
          <Icon name="search" />
          <input
            className="input"
            placeholder={`Buscar en ${bloque.name.toLowerCase()}`}
            aria-label={`Buscar en ${bloque.name}`}
            value={consulta}
            onChange={(evento) => setConsulta(evento.target.value)}
          />
        </label>
        <span className="record-count">{bloque.rows.length} registros</span>
        <Btn cls="soft small" data-accion="add-record" onClick={() => onRegistro(bloque.id, null)}>
          <Icon name="plus" /> Registrar
        </Btn>
      </div>
      {bloque.rows.length === 0 ? (
        <div className="data-empty">
          <Icon name={CATALOG[bloque.type].icon} />
          Todavía no hay registros.
        </div>
      ) : (
        <>
          <div className="table-scroll" onScroll={alDesplazar}>
            <table className="data-table">
              <thead>
                <tr>
                  {bloque.fields.map((campo) => (
                    <th scope="col" key={campo.key}>
                      {campo.label}
                      {campo.type === 'relation' ? ' ↗' : ''}
                    </th>
                  ))}
                  <th scope="col">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {filas.map((fila) => {
                  const esNueva = nuevas.has(fila.id);
                  return (
                    <tr
                      key={fila.id}
                      data-record={fila.id}
                      className={
                        esNueva ? 'fila-nueva' : fila.__example ? 'example-row' : undefined
                      }
                    >
                      {bloque.fields.map((campo, indice) => (
                        <td key={campo.key} title={textoPlano(sistema, campo, fila[campo.key])}>
                          {indice === 0 && esNueva && <em className="nuevo-badge">Nuevo</em>}
                          {indice === 0 && fila.__example && (
                            <em className="example-badge">Ejemplo</em>
                          )}
                          <ValorDeCampo sistema={sistema} campo={campo} valor={fila[campo.key]} />
                        </td>
                      ))}
                      <td className="row-actions">
                        <IconBtn
                          name="edit"
                          label="Editar registro"
                          data-accion="edit-record"
                          onClick={() => onRegistro(bloque.id, fila.id)}
                        />
                      </td>
                    </tr>
                  );
                })}
                {!filas.length && (
                  <tr>
                    <td colSpan={bloque.fields.length + 1}>
                      <span className="empty-preview">No hay registros que coincidan.</span>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          {quedan > 0 && (
            <button type="button" className="tabla-mas" onClick={sumar}>
              Mostrar {Math.min(TANDA_DE_REGISTROS, quedan)} más · quedan {quedan}
            </button>
          )}
        </>
      )}
    </>
  );
}

type TableroDelTallerProps = {
  sistema: Sistema;
  seccionId: string | null;
  /** Los ids de los registros que se cargaron en este armado (no estaban guardados): van primero y resaltados. */
  nuevas: ReadonlySet<string>;
  arrastrando: string | null;
  /** La pieza (o `#canvas`, el lienzo vacío) sobre la que está pasando lo que se arrastra. */
  sobre: string | null;
  onRenombrar: (bloqueId: string) => void;
  onRegistro: (bloqueId: string, filaId: string | null) => void;
  onQuitar: (bloqueId: string) => void;
  onAncho: (bloqueId: string, ancho: Ancho) => void;
  onTablaCompleta: (bloqueId: string) => void;
  onArrastrar: (evento: DragEvent<HTMLButtonElement>, bloqueId: string) => void;
  onSobre: (destino: string) => void;
  onSoltar: (antesDe: string | null) => void;
  onFinArrastre: () => void;
};

// La propuesta de Sami con el aspecto del cuaderno: los mismos bloques, cabeceras y tablas, pero editando el borrador.
export function TableroDelTaller({
  sistema,
  seccionId,
  nuevas,
  arrastrando,
  sobre,
  onRenombrar,
  onRegistro,
  onQuitar,
  onAncho,
  onTablaCompleta,
  onArrastrar,
  onSobre,
  onSoltar,
  onFinArrastre,
}: TableroDelTallerProps) {
  const seccion = sistema.sections.find((item) => item.id === seccionId) ?? sistema.sections[0];
  const bloques = seccion?.blocks ?? [];

  return (
    <div
      className={`canvas-grid ${sobre === LIENZO ? 'drop-active' : ''}`.trim()}
      id="draft-canvas"
      data-tour="draft-canvas"
      aria-label={`Propuesta visual de ${sistema.name}`}
      onDragOver={
        arrastrando
          ? (evento) => {
              evento.preventDefault();
              const bloque = bloqueDeEvento(evento);
              onSobre(bloque && bloque !== arrastrando ? bloque : LIENZO);
            }
          : undefined
      }
      onDrop={
        arrastrando
          ? (evento) => {
              evento.preventDefault();
              onSoltar(bloqueDeEvento(evento));
            }
          : undefined
      }
    >
      {bloques.length === 0 && (
        <div className="canvas-empty">
          <h2>Este espacio lo armás vos.</h2>
          <p>Sumá un bloque desde el ícono «+» o pedíselo a Sami.</p>
        </div>
      )}
      {bloques.map((bloque) => {
        const estilo: CSSProperties = {
          ...varsDeColor(bloque.color),
          ...(bloque.startRow ? { gridColumnStart: 1 } : {}),
        };
        const nuevos = bloque.rows.filter((fila) => nuevas.has(fila.id)).length;
        const conTabla = bloque.type !== 'summary' && bloque.type !== 'note';
        return (
          <section
            key={bloque.id}
            className={`block ${arrastrando === bloque.id ? 'dragging' : ''} ${sobre === bloque.id ? 'drag-over' : ''} ${nuevos ? 'con-nuevos' : ''}`
              .replace(/\s+/g, ' ')
              .trim()}
            style={estilo}
            data-draft-block={bloque.id}
            data-width={bloque.width}
            data-color={bloque.color}
          >
            <header className="block-head">
              <IconTile name={CATALOG[bloque.type].icon} color={bloque.color} />
              <div className="block-title">
                <h2>
                  <button
                    type="button"
                    className="block-nombre"
                    title="Cambiar el nombre"
                    onClick={() => onRenombrar(bloque.id)}
                  >
                    {bloque.name} <Icon name="edit" />
                  </button>
                </h2>
                <p>{bloque.description}</p>
              </div>
              {nuevos > 0 && (
                <span className="block-nuevos">
                  {nuevos === 1 ? '1 nuevo' : `${nuevos} nuevos`}
                </span>
              )}
              <div className="block-tools">
                {conTabla && (
                  <IconBtn
                    name="expand"
                    label={`Ver ${bloque.name} a pantalla completa`}
                    data-accion="table-fullscreen"
                    onClick={() => onTablaCompleta(bloque.id)}
                  />
                )}
                <button
                  type="button"
                  className="icon-btn"
                  title="Arrastrar esta pieza"
                  aria-label="Arrastrar esta pieza"
                  draggable
                  data-drag-kind="draft"
                  data-value={bloque.id}
                  onDragStart={(evento) => onArrastrar(evento, bloque.id)}
                  onDragEnd={onFinArrastre}
                >
                  <Icon name="grip" />
                </button>
                <IconBtn
                  name="trash"
                  label="Quitar de la propuesta"
                  onClick={() => onQuitar(bloque.id)}
                />
              </div>
            </header>
            <div className="block-width-row" data-tour="review">
              <span>
                <Icon name="grid" />
                Ancho de esta pieza
              </span>
              <WidthButtons
                ancho={bloque.width}
                accion="draft-width"
                onElegir={(ancho) => onAncho(bloque.id, ancho)}
              />
            </div>
            {bloque.type === 'summary' ? (
              <Resumen sistema={sistema} />
            ) : bloque.type === 'note' ? (
              <div className="note-content">{bloque.content}</div>
            ) : (
              <Tabla bloque={bloque} sistema={sistema} nuevas={nuevas} onRegistro={onRegistro} />
            )}
          </section>
        );
      })}
    </div>
  );
}
