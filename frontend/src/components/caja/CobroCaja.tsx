import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import { dineroCaja, leerPrecio } from '@/lib/caja';
import { Icon } from '@/components/sistema/Icon';
import type { MedioCaja, PagoCaja, SolicitudCobro } from '@/types/caja';

const medios: MedioCaja[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA'];
const nombre = (medio: MedioCaja) =>
  medio === 'EFECTIVO' ? 'Efectivo' : medio === 'TARJETA' ? 'Tarjeta' : 'Transferencia';

export function CobroCaja({
  total,
  ocupada,
  incierta,
  error,
  onConfirmar,
  onCerrar,
}: {
  total: number;
  ocupada: boolean;
  incierta: boolean;
  error: string;
  onConfirmar: (
    medio: SolicitudCobro['medio'],
    recibido: number | null,
    pagos?: PagoCaja[],
  ) => void;
  onCerrar: () => void;
}) {
  const [medio, setMedio] = useState<MedioCaja>('EFECTIVO');
  const [segundo, setSegundo] = useState<MedioCaja>('TRANSFERENCIA');
  const [dividido, setDividido] = useState(false);
  const [primeraParte, setPrimeraParte] = useState('');
  const [recibido, setRecibido] = useState('');
  const cambios = useConfirmarCambios();
  const solicitarCierre = async () => {
    if (ocupada || incierta) return;
    if (
      !(dividido || recibido || medio !== 'EFECTIVO') ||
      (await cambios.confirmar(
        'Se descartarán los datos de este pago. La compra seguirá en Caja para que puedas cobrarla después.',
      ))
    )
      onCerrar();
  };
  let parte = total,
    entregado = 0,
    valido = true;
  try {
    if (dividido) parte = leerPrecio(primeraParte);
  } catch {
    valido = false;
  }
  if (dividido && (parte >= total || segundo === medio)) valido = false;
  const restante = total - parte;
  const efectivo = medio === 'EFECTIVO' ? parte : dividido && segundo === 'EFECTIVO' ? restante : 0;
  try {
    entregado = recibido ? leerPrecio(recibido) : efectivo;
  } catch {
    if (efectivo > 0) valido = false;
  }
  if (efectivo > 0 && entregado < efectivo) valido = false;
  const bloqueado = ocupada || incierta;
  const confirmar = () => {
    const pagos: PagoCaja[] = dividido
      ? [
          { medio, importe: parte / 100 },
          { medio: segundo, importe: restante / 100 },
        ]
      : [{ medio, importe: total / 100 }];
    onConfirmar(
      dividido ? 'MIXTO' : medio,
      efectivo > 0 && recibido ? entregado / 100 : null,
      pagos,
    );
  };
  return (
    <>
      <DialogoCaja
        titulo={incierta ? 'Verificar la venta' : 'Registrar el cobro'}
        onCerrar={() => void solicitarCierre()}
        bloqueado={bloqueado}
      >
        <div className="caja-cobro-importe">
          <span className="caja-antetitulo">03 · TOTAL A COBRAR</span>
          <p className="caja-importe-previo">{dineroCaja(total)}</p>
        </div>
        <p className="caja-ayuda">Elegí cómo recibiste el pago.</p>
        <fieldset disabled={bloqueado}>
          <legend>{dividido ? 'Primer medio' : 'Medio de pago'}</legend>
          <div className="caja-opciones">
            {medios.map((valor) => (
              <button
                key={valor}
                type="button"
                disabled={bloqueado}
                aria-pressed={medio === valor}
                onClick={() => {
                  setMedio(valor);
                  if (segundo === valor)
                    setSegundo(medios.find((m) => m !== valor) ?? 'TRANSFERENCIA');
                  setRecibido('');
                }}
              >
                <Icon
                  name={
                    valor === 'EFECTIVO' ? 'wallet' : valor === 'TRANSFERENCIA' ? 'arrow' : 'orders'
                  }
                />
                {nombre(valor)}
              </button>
            ))}
          </div>
        </fieldset>
        <button
          type="button"
          className="btn secondary caja-dividir"
          aria-pressed={dividido}
          disabled={bloqueado}
          onClick={() => {
            setDividido(!dividido);
            setRecibido('');
          }}
        >
          {dividido ? 'Usar un solo medio' : 'Dividir pago'}
        </button>
        {dividido && (
          <div className="caja-pago-dividido">
            <label>
              Importe en {nombre(medio).toLowerCase()} ($)
              <input
                inputMode="decimal"
                value={primeraParte}
                disabled={bloqueado}
                onChange={(e) => setPrimeraParte(e.target.value)}
                placeholder="0,00"
              />
            </label>
            <fieldset disabled={bloqueado}>
              <legend>El resto se paga con</legend>
              <div className="caja-opciones">
                {medios
                  .filter((m) => m !== medio)
                  .map((valor) => (
                    <button
                      type="button"
                      key={valor}
                      disabled={bloqueado}
                      aria-pressed={segundo === valor}
                      onClick={() => {
                        setSegundo(valor);
                        setRecibido('');
                      }}
                    >
                      {nombre(valor)}
                    </button>
                  ))}
              </div>
            </fieldset>
            <p className="caja-restante" role="status">
              {restante > 0
                ? `${nombre(segundo)}: ${dineroCaja(restante)}`
                : 'Revisá el importe: las dos partes deben ser mayores a cero.'}
            </p>
          </div>
        )}
        {efectivo > 0 && (
          <label>
            Efectivo recibido ($)
            <input
              inputMode="decimal"
              value={recibido}
              disabled={bloqueado}
              placeholder="Importe exacto"
              onChange={(e) => setRecibido(e.target.value)}
            />
          </label>
        )}
        <p role="status">
          {valido
            ? efectivo > 0
              ? `Vuelto: ${dineroCaja(entregado - efectivo)}`
              : 'Confirmá que ya recibiste el pago.'
            : 'Revisá los importes antes de confirmar.'}
        </p>
        {error && (
          <p className="caja-error" role="alert">
            {error}
          </p>
        )}
        {incierta && (
          <p className="caja-nota">
            Estamos verificando si se registró. Consultar de nuevo conserva la misma operación; no
            vuelvas a cobrarle a la persona.
          </p>
        )}
        <div className="caja-acciones">
          <button
            type="button"
            className="btn secondary"
            disabled={bloqueado}
            onClick={() => void solicitarCierre()}
          >
            Volver
          </button>
          <button
            type="button"
            className="btn primary"
            disabled={ocupada || (!incierta && !valido)}
            onClick={confirmar}
          >
            {ocupada ? 'Confirmando…' : incierta ? 'Verificar misma operación' : 'Confirmar cobro'}
          </button>
        </div>
      </DialogoCaja>
      {cambios.confirmacion && (
        <ConfirmacionCambios
          mensaje={cambios.confirmacion}
          onConservar={cambios.conservar}
          onDescartar={cambios.aceptar}
        />
      )}
    </>
  );
}
