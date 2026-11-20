import { DialogoCaja } from './DialogoCaja';

export function ConfirmacionCambios({
  mensaje,
  onConservar,
  onDescartar,
}: {
  mensaje: string;
  onConservar: () => void;
  onDescartar: () => void;
}) {
  return (
    <DialogoCaja titulo="Hay cambios sin guardar" rol="alertdialog" onCerrar={onConservar}>
      <p className="caja-nota">{mensaje}</p>
      <p className="caja-ayuda">Podés seguir completando los datos o descartar estos cambios.</p>
      <div className="caja-acciones">
        <button type="button" className="btn secondary" onClick={onConservar}>
          Seguir editando
        </button>
        <button type="button" className="btn danger" onClick={onDescartar}>
          Descartar y continuar
        </button>
      </div>
    </DialogoCaja>
  );
}
