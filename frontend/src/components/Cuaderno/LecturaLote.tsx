import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, SmileySadIcon } from '@phosphor-icons/react';
import { Button, Panel, Spinner, Stepper, buttonClasses } from '@/components/ui';
import { useReintentarLectura } from '@/hooks/lotes/mutations';
import { useLectura, useLote } from '@/hooks/lotes/queries';
import { ESTADO_CONFIRMADO } from '@/types/lectura';
import { RevisionLectura } from './Revision/RevisionLectura';

type Props = { loteId: string; onSubirMas: () => void };

const MENSAJE_ERROR_CONSULTA = 'No pudimos consultar la lectura. Probá de nuevo en unos minutos.';
const PASOS_LECTURA = ['Subida', 'Leyendo', 'Para revisar'];

const recibidas = (hojas: number) =>
  hojas === 1 ? 'Recibimos 1 hoja.' : `Recibimos ${hojas} hojas.`;

// Sigue la lectura de un lote: mientras la IA trabaja avisa, al terminar deja revisar y corregir lo que
// leyó (H-30), y cuando se guarda lo dice. Vive en su propia dirección para sobrevivir a una recarga.
export function LecturaLote({ loteId, onSubirMas }: Props) {
  const lectura = useLectura(loteId);
  const lote = useLote(loteId);
  const reintentar = useReintentarLectura(loteId);
  const hojas = lote.data?.fotos.length;

  if (lectura.isError) {
    return (
      <Panel
        role="alert"
        className="mx-auto flex w-full max-w-xl flex-col items-start gap-4 border-bad/20 p-6 sm:p-8"
      >
        <SmileySadIcon aria-hidden size={32} className="text-bad" />
        <h1 className="m-0 text-h3">No pudimos ver cómo va tu cuaderno</h1>
        <p className="m-0 text-body text-soft">{lectura.error.message || MENSAJE_ERROR_CONSULTA}</p>
        <Button variant="solid" onClick={() => void lectura.refetch()}>
          Volver a consultar
        </Button>
      </Panel>
    );
  }

  if (lectura.data?.estado === 'ERROR') {
    return (
      <Panel
        role="alert"
        className="mx-auto flex w-full max-w-xl flex-col items-start gap-5 border-bad/20 p-6 sm:p-8"
      >
        <Stepper pasos={PASOS_LECTURA} actual={1} label="Avance de la lectura" />
        <SmileySadIcon aria-hidden size={32} className="text-bad" />
        <h1 className="m-0 text-h3">No pudimos leer tu cuaderno</h1>
        <p className="m-0 text-body text-soft">
          Hubo un problema al leer las fotos. Probá de nuevo, o subí fotos más nítidas con buena
          luz.
        </p>
        {reintentar.isError && (
          <p role="alert" className="m-0 font-semibold text-bad">
            {reintentar.error.message}
          </p>
        )}
        <div className="flex flex-wrap gap-3">
          <Button
            variant="solid"
            loading={reintentar.isPending}
            onClick={() => reintentar.mutate()}
          >
            {reintentar.isPending ? 'Reintentando…' : 'Volver a intentar'}
          </Button>
          <Button onClick={onSubirMas}>Subir otras fotos</Button>
          <Link to="/proyectos" className={buttonClasses()}>
            Mis sistemas
          </Link>
        </div>
      </Panel>
    );
  }

  if (lectura.data?.estado === ESTADO_CONFIRMADO) {
    return (
      <Panel className="mx-auto flex w-full max-w-xl flex-col items-start gap-4 border-petrol/15 bg-sage p-6 sm:p-8">
        <span className="flex size-14 items-center justify-center rounded-2xl bg-surface">
          <CheckCircleIcon aria-hidden size={32} className="text-ok" weight="duotone" />
        </span>
        <h1 className="m-0 text-h3 text-petrol">Este cuaderno ya se guardó</h1>
        <p className="m-0 text-body text-soft">
          Sus datos están en tus sistemas. Si tenés más hojas, subilas.
        </p>
        <div className="flex flex-wrap gap-3">
          <Link to="/proyectos" className={buttonClasses({ variant: 'solid' })}>
            Ir a mis sistemas
          </Link>
          <Button onClick={onSubirMas}>Subir más hojas</Button>
        </div>
      </Panel>
    );
  }

  if (lectura.data?.estado === 'LISTO' && lectura.data.propuesta) {
    return <RevisionLectura loteId={loteId} propuesta={lectura.data.propuesta} />;
  }

  return (
    <Panel
      role="status"
      className="mx-auto flex w-full max-w-xl flex-col items-start gap-5 p-6 sm:p-8"
    >
      <Stepper pasos={PASOS_LECTURA} actual={1} label="Avance de la lectura" />
      <div className="flex w-full justify-center rounded-2xl bg-blue-100 py-4">
        <img
          src="/mockup/sami-armando-sin-fondo.png"
          alt=""
          className="h-36 w-auto object-contain sm:h-44"
        />
      </div>
      <h1 className="m-0 text-h3 text-petrol">Estamos leyendo tu cuaderno</h1>
      <p className="m-0 text-body text-soft">
        {hojas !== undefined && `${recibidas(hojas)} `}Puede tardar un minuto. No hace falta que te
        quedes en esta pantalla: podés salir y volver, tu cuaderno se sigue leyendo.
      </p>
      <Spinner label="Leyendo…" />
      <Link to="/proyectos" className={buttonClasses()}>
        Mis sistemas
      </Link>
    </Panel>
  );
}
