import { CalendarCheckIcon } from '@phosphor-icons/react';
import { semanaDel } from '@/lib/objetivosAutogestion';

// La única señal del cierre del domingo: una banda discreta, sin avisos ni mensajes. Se va al mirarla.
export function ResumenListo({
  ultimoCierre,
  onVer,
  onOcultar,
}: {
  ultimoCierre: string;
  onVer: () => void;
  onOcultar: () => void;
}) {
  return (
    <div className="resumen-listo" role="status">
      <CalendarCheckIcon aria-hidden size={22} />
      <p>
        <strong>Tu resumen de la semana está listo.</strong> {semanaDel(ultimoCierre)}.
      </p>
      <button type="button" className="btn secondary" onClick={onVer}>
        Ver resumen
      </button>
      <button type="button" className="objetivos-textual" onClick={onOcultar}>
        Listo
      </button>
    </div>
  );
}
