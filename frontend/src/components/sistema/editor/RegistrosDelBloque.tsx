import { useEffect, useMemo } from 'react';
import { useTandas } from '@/hooks/sistemas/useTandas';
import { CATALOG } from '@/lib/sistema/catalogo';
import { claseDeLaCelda } from '@/lib/sistema/celdas';
import { comportamientoDe } from '@/lib/sistema/comportamiento';
import { normalize } from '@/lib/sistema/core';
import { textoPlano } from '@/lib/sistema/formato';
import { alPedirFila, TANDA_DE_REGISTROS } from '@/lib/sistema/tandas';
import type { Bloque, Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { ValorDeCampo } from '../ValorDeCampo';

// La tabla del bloque: los registros se dibujan de a 10 (con el scroll del propio bloque) y la búsqueda filtra sobre
// todos. Una sección con cientos de registros no estira la página ni dibuja de más.
export function RegistrosDelBloque({
  bloque,
  sistema,
  consulta,
}: {
  bloque: Bloque;
  sistema: Sistema;
  consulta: string;
}) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const buscado = normalize(consulta);
  const coincidentes = useMemo(
    () =>
      buscado
        ? bloque.rows.filter((fila) =>
            normalize(
              bloque.fields.map((campo) => textoPlano(sistema, campo, fila[campo.key])).join(' '),
            ).includes(buscado),
          )
        : bloque.rows,
    [bloque.rows, bloque.fields, sistema, buscado],
  );
  const { visibles, quedan, sumar, mostrarHasta, alDesplazar } = useTandas(
    coincidentes.length,
    TANDA_DE_REGISTROS,
    buscado,
  );

  // Si piden llevar la vista a un registro que todavía no se dibujó, se dibujan los que hagan falta hasta él.
  useEffect(() =>
    alPedirFila(({ bloqueId, filaId }) => {
      if (bloqueId !== bloque.id) return;
      const indice = coincidentes.findIndex((fila) => fila.id === filaId);
      if (indice >= 0) mostrarHasta(indice);
    }),
  );

  if (!bloque.rows.length) {
    return (
      <div className="data-empty">
        <Icon name={CATALOG[bloque.type].icon} />
        Todavía no hay registros.
        <br />
        <button
          type="button"
          className="text-link"
          data-accion="add-record"
          onClick={() => abrirModal({ tipo: 'registro', bloqueId: bloque.id })}
        >
          Cargá el primero <Icon name="arrow" />
        </button>
      </div>
    );
  }
  const filas = coincidentes.slice(0, visibles);
  return (
    <>
      <div className="table-scroll" onScroll={alDesplazar}>
        <table className="data-table">
          <thead>
            <tr>
              {bloque.fields.map((campo) => (
                <th scope="col" key={campo.key}>
                  {campo.label}
                  {comportamientoDe(campo.type).marcaDeColumna()}
                </th>
              ))}
              <th scope="col">
                <span className="sr-only">Acciones</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {filas.map((fila) => (
              <tr key={fila.id} data-record={fila.id}>
                {bloque.fields.map((campo) => (
                  <td
                    key={campo.key}
                    title={textoPlano(sistema, campo, fila[campo.key])}
                    className={claseDeLaCelda(bloque, campo, fila)}
                  >
                    <ValorDeCampo sistema={sistema} campo={campo} valor={fila[campo.key]} />
                  </td>
                ))}
                <td className="row-actions">
                  <IconBtn
                    name="edit"
                    label="Editar registro"
                    data-accion="edit-record"
                    onClick={() =>
                      abrirModal({ tipo: 'registro', bloqueId: bloque.id, filaId: fila.id })
                    }
                  />
                </td>
              </tr>
            ))}
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
  );
}
