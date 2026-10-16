import { TableIcon } from '@phosphor-icons/react';
import { EmptyState, Panel } from '@/components/ui';
import { useEtiquetas, useRegistros } from '@/hooks/registros/queries';
import { formatearMomento, formatearValor, SIN_DATO } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';

const MAX_FILAS = 10;
const MAX_DATOS = 3;

/** Las 10 filas más nuevas del bloque, como tarjetas con hasta 3 datos cada una. */
export function UltimasFilas({ bloque }: { bloque: Bloque }) {
  const filas = useRegistros(bloque.id, {
    orden: 'creadoEn',
    direccion: 'desc',
    tamanio: MAX_FILAS,
  });
  const registros = filas.data?.content ?? [];
  // Sólo datos que se cargan: las columnas calculadas no se muestran.
  const campos = [...bloque.campos]
    .filter((campo) => campo.tipo !== 'FORMULA')
    .sort((a, b) => a.posicion - b.posicion);
  const etiquetaDe = useEtiquetas(campos, registros);

  if (filas.isPending) return <p className="m-0 text-body text-soft">Cargando…</p>;
  if (filas.isError) {
    return (
      <p role="alert" className="m-0 text-body text-bad">
        No pudimos cargar las últimas filas. Probá de nuevo en un momento.
      </p>
    );
  }
  if (registros.length === 0) {
    return (
      <EmptyState
        icon={<TableIcon aria-hidden size={32} />}
        title="Todavía no cargaste nada en este bloque"
      >
        <p className="m-0">Lo que guardes con el formulario aparece acá.</p>
      </EmptyState>
    );
  }

  return (
    <ul className="m-0 grid list-none grid-cols-1 gap-3 p-0 md:grid-cols-2">
      {registros.map((registro) => {
        const datos = campos
          .map((campo) => ({
            campo,
            texto: formatearValor(
              campo.tipo,
              registro.valores[campo.id],
              etiquetaDe(campo, registro.valores[campo.id]),
            ),
          }))
          .filter((dato) => dato.texto !== SIN_DATO)
          .slice(0, MAX_DATOS);
        return (
          <li key={registro.id} className="min-w-0">
            <Panel tight className="flex h-full flex-col gap-1">
              {datos.length === 0 ? (
                <p className="m-0 text-body text-soft">Sin datos cargados</p>
              ) : (
                datos.map(({ campo, texto }) => (
                  <p key={campo.id} className="m-0 text-body break-words">
                    <span className="text-soft">{campo.nombre}:</span> <strong>{texto}</strong>
                  </p>
                ))
              )}
              <p className="m-0 mt-1 text-hint text-muted">
                Cargado el {formatearMomento(registro.creadoEn)}
              </p>
            </Panel>
          </li>
        );
      })}
    </ul>
  );
}
