import { useState } from 'react';
import { CobroConQrCaja } from './CobroConQrCaja';
import { DialogoCaja } from './DialogoCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { usePagoDeudaCaja } from '@/hooks/caja/usePagoDeudaCaja';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import { dineroCaja, fechaCaja, leerPrecio } from '@/lib/caja';
import { centavosPorTransferencia } from '@/lib/caja/cobroQr';
import { repartirCobroDeuda } from '@/lib/cobrosDeudasCaja';
import type { MedioCaja, PagoCaja, VentaCaja } from '@/types/caja';
import { tonoDe } from '@/lib/sistema/tonos';
import { Icon } from '@/components/sistema/Icon';
import { aspectoDelMedio, iconoDelMedio, nombreDelMedio } from '@/lib/caja/mediosDePago';

const medios: MedioCaja[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA'];

export function CobroDeudaCaja({
  sistemaId,
  inventarioId,
  clienteId,
  clienteNombre,
  ventas,
  onCerrar,
}: {
  sistemaId: string;
  inventarioId: string;
  clienteId: string;
  clienteNombre: string;
  ventas: VentaCaja[];
  onCerrar: () => void;
}) {
  const saldo = ventas.reduce((sum, v) => sum + Math.round((v.saldo ?? 0) * 100), 0);
  const [importe, setImporte] = useState((saldo / 100).toFixed(2).replace('.', ','));
  const [medio, setMedio] = useState<MedioCaja>('EFECTIVO');
  const [segundo, setSegundo] = useState<MedioCaja>('TRANSFERENCIA');
  const [dividido, setDividido] = useState(false);
  const [primeraParte, setPrimeraParte] = useState('');
  const [modificado, setModificado] = useState(false);
  const [cobrandoConQr, setCobrandoConQr] = useState(false);
  const pago = usePagoDeudaCaja(sistemaId, inventarioId, clienteId);
  const cambios = useConfirmarCambios();
  const bloqueado = pago.ocupada || !!pago.solicitud;
  let centavos = 0;
  let aplicaciones: ReturnType<typeof repartirCobroDeuda> = [];
  let errorImporte = '';
  try {
    centavos = leerPrecio(importe);
    const parte = dividido ? leerPrecio(primeraParte) : centavos;
    if (dividido && (parte >= centavos || medio === segundo))
      throw new Error('Revisá las dos partes del pago.');
    const pagos: PagoCaja[] = [{ medio, importe: parte / 100 }];
    if (dividido) pagos.push({ medio: segundo, importe: (centavos - parte) / 100 });
    aplicaciones = repartirCobroDeuda(ventas, pagos);
  } catch (e) {
    errorImporte = e instanceof Error ? e.message : 'Revisá el importe.';
  }
  const aTransferir = errorImporte
    ? 0
    : centavosPorTransferencia(aplicaciones.flatMap((aplicacion) => aplicacion.pagos));
  const cerrar = async () => {
    if (bloqueado) return;
    if (
      pago.confirmado ||
      !modificado ||
      (await cambios.confirmar('Los datos de este pago todavía no se guardaron.'))
    )
      onCerrar();
  };
  return (
    <>
      <DialogoCaja
        titulo={
          pago.confirmado
            ? 'Pago guardado'
            : pago.solicitud
              ? 'Verificar el pago'
              : 'Guardar pago del fiado'
        }
        bloqueado={bloqueado}
        tono={tonoDe('cobrado')}
        onCerrar={() => void cerrar()}
      >
        <p>
          Cliente: <strong>{clienteNombre}</strong>
        </p>
        {pago.confirmado ? (
          <>
            <p className="caja-saldo" role="status">
              Se guardaron{' '}
              <strong>{dineroCaja(Math.round(pago.confirmado.importe * 100))}</strong>.
            </p>
            <p>Fecha: {fechaCaja(pago.confirmado.creadoEn)}</p>
            <p className="caja-ayuda">
              El pago reduce la deuda y entra en la caja del período actual.
            </p>
            <button className="btn primary full" type="button" onClick={onCerrar}>
              Listo
            </button>
          </>
        ) : (
          <>
            {pago.solicitud ? (
              <section aria-label="Pago pendiente de verificar">
                <p>Esta operación conserva el importe y las ventas que elegiste originalmente.</p>
                {pago.solicitud.aplicaciones.map((a) => (
                  <p key={a.ventaId}>
                    Venta {a.ventaId.slice(0, 8)} ·{' '}
                    {dineroCaja(a.pagos.reduce((sum, p) => sum + Math.round(p.importe * 100), 0))}
                  </p>
                ))}
              </section>
            ) : (
              <>
                <p>
                  Saldo de las ventas elegidas: <strong>{dineroCaja(saldo)}</strong>
                </p>
                <label>
                  Importe del pago ($)
                  <input
                    inputMode="decimal"
                    value={importe}
                    disabled={bloqueado}
                    onChange={(e) => {
                      setImporte(e.target.value);
                      setModificado(true);
                    }}
                  />
                </label>
                <div className="caja-opciones" aria-label="Medio del pago del fiado">
                  {medios.map((valor) => (
                    <button
                      key={valor}
                      type="button"
                      disabled={bloqueado}
                      {...aspectoDelMedio(valor, medio === valor)}
                      aria-pressed={medio === valor}
                      onClick={() => {
                        setMedio(valor);
                        setModificado(true);
                        if (valor === segundo)
                          setSegundo(medios.find((m) => m !== valor) ?? 'TRANSFERENCIA');
                      }}
                    >
                      <Icon name={iconoDelMedio(valor)} />
                      {nombreDelMedio(valor)}
                    </button>
                  ))}
                </div>
                <button
                  type="button"
                  className="btn secondary"
                  disabled={bloqueado}
                  aria-pressed={dividido}
                  onClick={() => {
                    setDividido(!dividido);
                    setModificado(true);
                  }}
                >
                  {dividido ? 'Usar un solo medio' : 'Dividir pago'}
                </button>
                {dividido && (
                  <>
                    <label>
                      Importe en {nombreDelMedio(medio).toLowerCase()} ($)
                      <input
                        inputMode="decimal"
                        value={primeraParte}
                        disabled={bloqueado}
                        onChange={(e) => {
                          setPrimeraParte(e.target.value);
                          setModificado(true);
                        }}
                      />
                    </label>
                    <p>El resto se paga con</p>
                    <div className="caja-opciones">
                      {medios
                        .filter((m) => m !== medio)
                        .map((valor) => (
                          <button
                            key={valor}
                            type="button"
                            disabled={bloqueado}
                            aria-pressed={segundo === valor}
                            onClick={() => setSegundo(valor)}
                          >
                            {nombreDelMedio(valor)}
                          </button>
                        ))}
                    </div>
                  </>
                )}
                {errorImporte && <p className="caja-error">{errorImporte}</p>}
                {!!aplicaciones.length && (
                  <section aria-label="Cómo se aplica el pago">
                    <h3>Cómo se aplica el pago</h3>
                    <p className="caja-ayuda">Se completan primero las ventas más antiguas.</p>
                    {aplicaciones.map((a) => {
                      const aplicado = a.pagos.reduce(
                        (sum, p) => sum + Math.round(p.importe * 100),
                        0,
                      );
                      const restante =
                        Math.round((ventas.find((v) => v.id === a.ventaId)?.saldo ?? 0) * 100) -
                        aplicado;
                      return (
                        <p key={a.ventaId}>
                          Venta {a.ventaId.slice(0, 8)}: {dineroCaja(aplicado)} ·{' '}
                          {restante === 0 ? 'Queda pagada' : `Quedan ${dineroCaja(restante)}`}
                        </p>
                      );
                    })}
                    <p>
                      Saldo restante: <strong>{dineroCaja(saldo - centavos)}</strong>
                    </p>
                  </section>
                )}
              </>
            )}
            {pago.error && (
              <p role="alert" className="caja-error">
                {pago.error}
              </p>
            )}
            {pago.solicitud && (
              <p className="caja-nota">
                Verificá la misma operación para saber si se guardó. El importe y las ventas
                originales se conservan para evitar duplicados.
              </p>
            )}
            <div className="caja-acciones">
              <button
                type="button"
                className="btn secondary"
                disabled={bloqueado}
                onClick={() => void cerrar()}
              >
                Cancelar
              </button>
              {aTransferir > 0 && !pago.solicitud && (
                <button
                  type="button"
                  className="btn secondary"
                  disabled={pago.ocupada}
                  onClick={() => setCobrandoConQr(true)}
                >
                  Cobrar con QR
                </button>
              )}
              <button
                type="button"
                className="btn primary"
                disabled={pago.ocupada || (!pago.solicitud && !!errorImporte)}
                onClick={() => void pago.confirmar(aplicaciones)}
              >
                {pago.ocupada
                  ? 'Confirmando…'
                  : pago.solicitud
                    ? 'Verificar misma operación'
                    : 'Confirmar pago'}
              </button>
            </div>
          </>
        )}
      </DialogoCaja>
      {cobrandoConQr && !pago.confirmado && (
        <CobroConQrCaja
          importeCentavos={aTransferir}
          ocupada={pago.ocupada}
          incierta={!!pago.solicitud}
          error={pago.error}
          onYaTransfirieron={(cobroQrId) => void pago.confirmar(aplicaciones, cobroQrId)}
          onCerrar={() => setCobrandoConQr(false)}
        />
      )}
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
