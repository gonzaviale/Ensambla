import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, SmileySadIcon } from '@phosphor-icons/react';
import { Button, Panel, Spinner, buttonClasses } from '@/components/ui';
import { useReintentarLectura } from '@/hooks/lotes/mutations';
import { useLectura, useLote } from '@/hooks/lotes/queries';
import { ESTADO_CONFIRMADO } from '@/types/lectura';
import { RevisionLectura } from './Revision/RevisionLectura';

type Props = { loteId: string; onSubirMas: () => void };

const MENSAJE_ERROR_CONSULTA = 'No pudimos consultar la lectura. Probá de nuevo en unos minutos.';

const recibidas = (paginas: number) =>
  paginas === 1 ? 'Recibimos 1 página.' : `Recibimos ${paginas} páginas.`;

// Sigue la lectura de un lote: mientras la IA trabaja avisa, al terminar deja revisar y corregir lo que
// leyó (H-30), y cuando se guarda lo dice. Vive en su propia dirección para sobrevivir a una recarga.
export function LecturaLote({ loteId, onSubirMas }: Props) {
  const lectura = useLectura(loteId);
  const lote = useLote(loteId);
  const reintentar = useReintentarLectura(loteId);
  const paginas = lote.data?.fotos.length;

  if (lectura.isError) {
    return (
      <Panel role="alert" className="mx-auto flex max-w-xl flex-col items-start gap-3">
        <SmileySadIcon aria-hidden size={32} className="text-bad" />
        <h1 className="m-0 text-h3">No pudimos ver cómo va tu cuaderno</h1>
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
        <h1 className="m-0 text-h3">No pudimos leer tu cuaderno</h1>
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

  if (lectura.data?.estado === ESTADO_CONFIRMADO) {
    return (
      <Panel className="mx-auto flex max-w-xl flex-col items-start gap-3">
        <CheckCircleIcon aria-hidden size={32} className="text-ok" />
        <h1 className="m-0 text-h3">Este cuaderno ya se guardó</h1>
        <p className="text-soft">
          Sus datos están en tus proyectos. Si tenés más páginas, subilas.
        </p>
        <div className="flex flex-wrap gap-2">
          <Link to="/proyectos" className={buttonClasses({ variant: 'primary' })}>
            Ir a mis proyectos
          </Link>
          <Button onClick={onSubirMas}>Subir más páginas</Button>
        </div>
      </Panel>
    );
  }

  if (lectura.data?.estado === 'LISTO' && lectura.data.propuesta) {
    return <RevisionLectura loteId={loteId} propuesta={lectura.data.propuesta} />;
  }

  return (
    <Panel role="status" className="mx-auto flex max-w-xl flex-col items-start gap-3">
      <h1 className="m-0 text-h3">Estamos leyendo tu cuaderno</h1>
      <p className="text-soft">
        {paginas !== undefined && `${recibidas(paginas)} `}Puede tardar un minuto. No hace falta que
        te quedes en esta pantalla.
      </p>
      <Spinner label="Leyendo…" />
    </Panel>
  );
}
