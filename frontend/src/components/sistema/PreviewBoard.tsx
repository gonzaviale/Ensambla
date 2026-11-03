import { useState, type CSSProperties } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import { findBlock, totals } from '@/lib/sistema/core';
import type { Bloque, Campo, Sistema, Valor } from '@/lib/sistema/tipos';
import { varsDeColor } from './colores';
import { Icon } from './Icon';
import { ValorDeCampo } from './ValorDeCampo';

type PreviewBoardProps = {
  sistema: Sistema;
  /** Más chico, para el inicio y la vista previa de plantillas. */
  compacto?: boolean;
  seccionId?: string | null;
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
    <div className={`preview-table-scroll ${verTodas ? 'preview-table-expandida' : ''}`.trim()}>
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
              <tr key={fila.id} className={fila.__example ? 'example-row' : undefined}>
                {campos.map((campo, indice) => (
                  <td key={campo.key}>
                    {indice === 0 && fila.__example && <em className="example-badge">Ejemplo</em>}
                    {valorDeVista(sistema, campo, fila[campo.key])}
                  </td>
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
      {bloque.rows.length > limite && (
        <button
          type="button"
          className="preview-more"
          onClick={() => setVerTodas((valor) => !valor)}
        >
          {verTodas ? 'Ver menos' : `Ver todas (${bloque.rows.length})`}
        </button>
      )}
    </div>
  );
}

// La vista previa de un sistema con los bloques en su distribución: el inicio y las plantillas.
export function PreviewBoard({ sistema, compacto = false, seccionId = null }: PreviewBoardProps) {
  const seccion = sistema.sections.find((item) => item.id === seccionId) ?? sistema.sections[0];
  const bloques = seccion?.blocks ?? [];

  return (
    <div
      className={`preview-grid ${compacto ? 'compact' : ''}`.trim()}
      aria-label={`Propuesta visual de ${sistema.name}`}
    >
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
            data-color={bloque.color}
            style={estilo}
          >
            <div className="preview-block-head">
              <span className="preview-block-icon">
                <Icon name={CATALOG[bloque.type].icon} />
              </span>
              <strong>{bloque.name}</strong>
            </div>
            <Contenido bloque={bloque} sistema={sistema} compacto={compacto} />
          </section>
        );
      })}
    </div>
  );
}
