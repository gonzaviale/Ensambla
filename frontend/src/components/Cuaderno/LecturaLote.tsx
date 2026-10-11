import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, SmileySadIcon, WarningIcon } from '@phosphor-icons/react';
import { Button, Panel, Spinner, buttonClasses } from '@/components/ui';
import { useReintentarLectura } from '@/hooks/lotes/mutations';
import { useLectura, useLote } from '@/hooks/lotes/queries';
import { UMBRAL_CONFIANZA, type PropuestaLectura } from '@/types/lectura';

type Props = { loteId: string; onSubirMas: () => void };

const MENSAJE_ERROR_CONSULTA = 'No pudimos consultar la lectura. Probá de nuevo en unos minutos.';

const recibidas = (paginas: number) =>
  paginas === 1 ? 'Recibimos 1 página.' : `Recibimos ${paginas} páginas.`;

// Sigue la lectura de un lote: mientras la IA trabaja avisa, y al terminar muestra lo que leyó (sólo lectura).
// Vive en su propia dirección (/cuaderno/{loteId}) para sobrevivir a una recarga.
export function LecturaLote({ loteId, onSubirMas }: Props) {
  const lectura = useLectura(loteId);
  const lote = useLote(loteId);
  const reintentar = useReintentarLectura(loteId);
  const paginas = lote.data?.fotos.length;

  if (lectura.isError) {
    return (
      <Panel role="alert" className="mx-auto flex max-w-xl flex-col items-start gap-3">
        <SmileySadIcon aria-hidden size={32} className="text-bad" />
        <h1 className="m-0 text-[24px]">No pudimos ver cómo va tu cuaderno</h1>
        <p className="text-soft">{lectura.error.message || MENSAJE_ERROR_CONSULTA}</p>
        <Button variant="primary" onClick={() => void lectura.refetch()}>
          Volver a consultar
        </Button>
      </Panel>
    );
  }

  if (lectura.data?.estado === 'ERROR') {
    return (
      <Panel role="alert" className="mx-auto flex max-w-xl flex-col items-start gap-3">
        <SmileySadIcon aria-hidden size={32} className="text-bad" />
        <h1 className="m-0 text-[24px]">No pudimos leer tu cuaderno</h1>
        <p className="text-soft">
          {lectura.data.motivoError ?? 'La lectura falló.'} Podés intentarlo de nuevo o subir fotos
          más nítidas.
        </p>
        {reintentar.isError && (
          <p role="alert" className="m-0 text-bad">
            {reintentar.error.message}
          </p>
        )}
        <div className="flex flex-wrap gap-2">
          <Button
            variant="primary"
            disabled={reintentar.isPending}
            onClick={() => reintentar.mutate()}
          >
            {reintentar.isPending ? 'Reintentando…' : 'Reintentar lectura'}
          </Button>
          <Button onClick={onSubirMas}>Subir otras fotos</Button>
        </div>
      </Panel>
    );
  }

  if (lectura.data?.estado === 'LISTO' && lectura.data.propuesta) {
    return (
      <div className="flex flex-col gap-4">
        <Panel className="flex flex-col items-start gap-3">
          <CheckCircleIcon aria-hidden size={32} className="text-ok" />
          <h1 className="m-0 text-[24px]">Esto es lo que leímos de tu cuaderno</h1>
          <p className="text-soft">
            Revisá los datos. Los que aparecen marcados son los que no pudimos leer con seguridad.
          </p>
        </Panel>
        <TablaPropuesta propuesta={lectura.data.propuesta} />
        <div className="flex flex-wrap gap-2">
          <Button variant="primary" onClick={onSubirMas}>
            Subir más páginas
          </Button>
          <Link to="/proyectos" className={buttonClasses()}>
            Ir a mis proyectos
          </Link>
        </div>
      </div>
    );
  }

  return (
    <Panel role="status" className="mx-auto flex max-w-xl flex-col items-start gap-3">
      <h1 className="m-0 text-[24px]">Estamos leyendo tu cuaderno</h1>
      <p className="text-soft">
        {paginas !== undefined && `${recibidas(paginas)} `}Puede tardar un minuto. No hace falta
        que te quedes en esta pantalla.
      </p>
      <Spinner label="Leyendo…" />
    </Panel>
  );
}

function TablaPropuesta({ propuesta }: { propuesta: PropuestaLectura }) {
  const titulo = propuesta.nombre ?? 'Datos leídos';
  return (
    <Panel className="flex flex-col gap-3">
      <h2 className="m-0 text-[17px]">{titulo}</h2>
      {propuesta.advertencias?.map((aviso) => (
        <p key={aviso} className="m-0 flex items-start gap-2 text-[13px] text-warn">
          <WarningIcon aria-hidden size={18} className="mt-0.5 shrink-0" />
          {aviso}
        </p>
      ))}
      {propuesta.filas.length === 0 ? (
        <p className="m-0 text-soft">No encontramos filas para leer en estas páginas.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-[14px]">
            <caption className="sr-only">{titulo}</caption>
            <thead>
              <tr>
                <th scope="col" className="px-2 py-1.5 text-soft">
                  Pág.
                </th>
                {propuesta.campos.map((campo) => (
                  <th key={campo.clave} scope="col" className="px-2 py-1.5 text-soft">
                    {campo.nombre}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {propuesta.filas.map((fila) => (
                <tr key={`${fila.pagina}-${fila.renglon}`} className="border-t border-line">
                  <td className="px-2 py-1.5 text-soft">{fila.pagina}</td>
                  {propuesta.campos.map((campo) => {
                    const dato = fila.datos[campo.clave];
                    const dudoso =
                      !dato || dato.valor === null || dato.confianza < UMBRAL_CONFIANZA;
                    return (
                      <td
                        key={campo.clave}
                        className={dudoso ? 'bg-warn/10 px-2 py-1.5 text-warn' : 'px-2 py-1.5'}
                        title={dudoso ? 'No estamos seguros de este dato' : undefined}
                      >
                        {dato?.valor ?? '—'}
                        {dudoso && <span className="sr-only"> (dato dudoso)</span>}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}
