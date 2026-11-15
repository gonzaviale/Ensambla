import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { SelectorClienteCaja } from './SelectorClienteCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import { dineroCaja, leerPrecio } from '@/lib/caja';
import { Icon } from '@/components/sistema/Icon';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { MedioCaja, PagoCaja, SolicitudCobro } from '@/types/caja';

const medios: MedioCaja[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA'];
const nombre = (medio: MedioCaja) =>
  medio === 'EFECTIVO' ? 'Efectivo' : medio === 'TARJETA' ? 'Tarjeta' : 'Transferencia';

export function CobroCaja({
  total,
  sistemaId,
  ventasId,
  ocupada,
  incierta,
  error,
  onConfirmar,
  onCerrar,
}: {
  total: number;
  sistemaId: string;
  ventasId: string;
  ocupada: boolean;
  incierta: boolean;
  error: string;
  onConfirmar: (
    medio: SolicitudCobro['medio'],
    recibido: number | null,
    pagos?: PagoCaja[],
    clienteId?: string,
  ) => void;
  onCerrar: () => void;
}) {
  const [medio, setMedio] = useState<MedioCaja>('EFECTIVO');
  const [segundo, setSegundo] = useState<MedioCaja>('TRANSFERENCIA');
  const [dividido, setDividido] = useState(false);
  const [primeraParte, setPrimeraParte] = useState('');
  const [recibido, setRecibido] = useState('');
  const [tipo, setTipo] = useState<'pagada' | 'parcial' | 'fiada'>('pagada');
  const [importeParcial, setImporteParcial] = useState('');
  const [clienteId, setClienteId] = useState('');
  const [asociarCliente, setAsociarCliente] = useState(false);
  const pendiente = useSistemaStore((s) => s.pendientes.includes(sistemaId));
  const cambios = useConfirmarCambios();
  const solicitarCierre = async () => {
    if (ocupada || incierta) return;
    if (
      !(dividido || recibido || medio !== 'EFECTIVO' || tipo !== 'pagada' || clienteId) ||
      (await cambios.confirmar(
        'Se descartarán los datos de este pago. La compra seguirá en Caja para que puedas cobrarla después.',
      ))
    )
      onCerrar();
  };
  let aCobrar = total;
  let importeValido = true;
  try {
    if (tipo === 'parcial') aCobrar = leerPrecio(importeParcial);
    if (tipo === 'fiada') aCobrar = 0;
  } catch {
    importeValido = false;
  }
  if (tipo === 'parcial' && aCobrar >= total) importeValido = false;
  let parte = aCobrar,
    entregado = 0,
    valido = importeValido && (tipo === 'pagada' || !!clienteId);
  try {
    if (dividido && tipo !== 'fiada') parte = leerPrecio(primeraParte);
  } catch {
    valido = false;
  }
  if (dividido && tipo !== 'fiada' && (parte >= aCobrar || segundo === medio)) valido = false;
  const restante = aCobrar - parte;
  const efectivo = medio === 'EFECTIVO' ? parte : dividido && segundo === 'EFECTIVO' ? restante : 0;
  try {
    entregado = recibido ? leerPrecio(recibido) : efectivo;
  } catch {
    if (efectivo > 0) valido = false;
  }
  if (efectivo > 0 && entregado < efectivo) valido = false;
  const bloqueado = ocupada || incierta;
  const confirmar = () => {
    const pagos: PagoCaja[] =
      tipo === 'fiada'
        ? []
        : dividido
          ? [
              { medio, importe: parte / 100 },
              { medio: segundo, importe: restante / 100 },
            ]
          : [{ medio, importe: aCobrar / 100 }];
    onConfirmar(
      dividido && tipo !== 'fiada' ? 'MIXTO' : medio,
      efectivo > 0 && recibido ? entregado / 100 : null,
      pagos,
      clienteId || undefined,
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
        <fieldset disabled={bloqueado}>
          <legend>Cómo queda la venta</legend>
          <div className="caja-opciones">
            {(
              [
                ['pagada', 'Pagada'],
                ['parcial', 'A medias'],
                ['fiada', 'Fiada'],
              ] as const
            ).map(([valor, texto]) => (
              <button
                key={valor}
                type="button"
                aria-pressed={tipo === valor}
                onClick={() => {
                  setTipo(valor);
                  setRecibido('');
                  setDividido(false);
                }}
              >
                {texto}
              </button>
            ))}
          </div>
        </fieldset>
        {tipo === 'parcial' && (
          <label>
            Paga ahora ($)
            <input
              inputMode="decimal"
              value={importeParcial}
              disabled={bloqueado}
              onChange={(e) => setImporteParcial(e.target.value)}
              placeholder="0,00"
            />
          </label>
        )}
        {tipo !== 'pagada' && (
          <p className="caja-saldo" role="status">
            Queda pendiente:{' '}
            <strong>{importeValido ? dineroCaja(total - aCobrar) : 'Revisá el importe'}</strong>
          </p>
        )}
        {(tipo !== 'pagada' || asociarCliente) && (
          <SelectorClienteCaja
            sistemaId={sistemaId}
            ventasId={ventasId}
            clienteId={clienteId}
            bloqueado={bloqueado}
            onCliente={setClienteId}
          />
        )}
        {tipo === 'pagada' && !asociarCliente && (
          <button
            type="button"
            className="btn secondary"
            disabled={bloqueado}
            onClick={() => setAsociarCliente(true)}
          >
            Agregar cliente (opcional)
          </button>
        )}
        {tipo !== 'fiada' && (
          <>
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
                        valor === 'EFECTIVO'
                          ? 'wallet'
                          : valor === 'TRANSFERENCIA'
                            ? 'arrow'
                            : 'orders'
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
          </>
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
              : tipo === 'fiada'
                ? 'Se guardará la deuda a nombre del cliente.'
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
            disabled={ocupada || pendiente || (!incierta && !valido)}
            onClick={confirmar}
          >
            {ocupada
              ? 'Confirmando…'
              : incierta
                ? 'Verificar misma operación'
                : tipo === 'fiada'
                  ? 'Registrar fiado'
                  : tipo === 'parcial'
                    ? 'Registrar venta y pago'
                    : 'Confirmar cobro'}
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
