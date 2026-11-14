import { useState } from 'react';
import { useResumenCaja } from '@/hooks/caja/useResumenCaja';
import { dineroCaja, fechaCaja } from '@/lib/caja';
import { ResumenVentasCaja } from './ResumenVentasCaja';
import { CierreCajaForm } from './CierreCajaForm';
import { DialogoCaja } from './DialogoCaja';
import type { ResumenVentasCaja as Resumen } from '@/types/caja';

export function GestionCaja({
  sistemaId,
  inventarioId,
  vista,
  compraPendiente,
}: {
  sistemaId: string;
  inventarioId: string;
  vista: 'dia' | 'abierta' | 'cierres';
  compraPendiente: boolean;
}) {
  const caja = useResumenCaja(sistemaId, inventarioId);
  const [cerrando, setCerrando] = useState<Resumen | null>(null);
  const [fecha, setFecha] = useState('');
  const datos = caja.consulta.data;
  const volver = () => {
    setCerrando(null);
    caja.revisar();
  };
  return (
    <section className="caja-gestion" aria-label="Ventas y cierres">
      <div className="caja-entre">
        <div>
          <span className="caja-antetitulo">LAS CUENTAS CLARAS</span>
          <h2>
            {vista === 'dia'
              ? 'Resumen del día'
              : vista === 'abierta'
                ? 'Caja abierta'
                : 'Cierres guardados'}
          </h2>
        </div>
        <button
          type="button"
          className="btn secondary small"
          disabled={caja.consulta.isFetching || caja.ocupada}
          onClick={() => void caja.consulta.refetch()}
        >
          Actualizar
        </button>
      </div>
      {vista === 'dia' && (
        <form
          className="caja-fecha"
          onSubmit={(e) => {
            e.preventDefault();
            caja.setFecha(fecha);
          }}
        >
          <label>
            Consultar fecha
            <input
              type="date"
              value={fecha || datos?.fecha || ''}
              onChange={(e) => setFecha(e.target.value)}
            />
          </label>
          <button type="submit" className="btn secondary">
            Ver fecha
          </button>
          <button
            type="button"
            className="btn secondary"
            onClick={() => {
              setFecha('');
              caja.setFecha('');
            }}
          >
            Hoy
          </button>
        </form>
      )}
      {caja.consulta.isPending && <p role="status">Cargando ventas guardadas…</p>}
      {caja.consulta.error && (
        <p className="caja-error" role="alert">
          No pudimos cargar el resumen. Tocá Actualizar para volver a intentar.
        </p>
      )}
      {caja.error && !cerrando && !caja.solicitud && (
        <p className="caja-error" role="alert">
          {caja.error}
        </p>
      )}
      {datos && (
        <>
          {vista === 'dia' && (
            <>
              <p className="caja-ayuda">
                Ventas del {datos.fecha.split('-').reverse().join('/')}, de 00:00 a 23:59, hora de
                Argentina. Incluye ventas de períodos ya cerrados.
              </p>
              <ResumenVentasCaja resumen={datos.dia} titulo="Total vendido en el día" />
            </>
          )}
          {vista === 'abierta' && (
            <>
              <p className="caja-estado-periodo">
                {datos.abierta.desde
                  ? `Abierta desde ${fechaCaja(datos.abierta.desde)}. Se mantiene hasta que la cierres.`
                  : 'Sin ventas pendientes. El próximo cobro abrirá un período nuevo.'}
              </p>
              <ResumenVentasCaja resumen={datos.abierta} titulo="Acumulado del período abierto" />
              <div className="caja-acciones">
                <button
                  type="button"
                  className="btn primary"
                  disabled={!datos.abierta.ventas.length || caja.ocupada}
                  onClick={() => {
                    caja.revisar();
                    setCerrando(datos.abierta);
                  }}
                >
                  Cerrar caja
                </button>
              </div>
            </>
          )}
          {vista === 'cierres' && (
            <>
              <p className="caja-ayuda">
                Cada cierre conserva sus ventas, precios y medios de pago originales. Tocá uno para
                verlo.
              </p>
              {!datos.cierres.length && (
                <p className="caja-vacio">Todavía no hay cierres en esta página.</p>
              )}
              <div className="caja-historial">
                {datos.cierres.map((c) => (
                  <button
                    type="button"
                    key={c.id}
                    disabled={caja.ocupada}
                    onClick={() => void caja.abrirDetalle(c.id)}
                  >
                    <span>
                      <strong>{fechaCaja(c.cerradoEn)}</strong>
                      <small>
                        {c.cantidadVentas} ventas · Desde {fechaCaja(c.desde)}
                      </small>
                    </span>
                    <span>
                      <strong>{dineroCaja(Math.round(c.total * 100))}</strong>
                      <small>
                        {c.diferencia === null
                          ? 'Sin recuento'
                          : `Diferencia: ${dineroCaja(Math.round(c.diferencia * 100))}`}
                      </small>
                    </span>
                    <span aria-hidden="true">→</span>
                  </button>
                ))}
              </div>
              <div className="caja-acciones">
                <button
                  type="button"
                  className="btn secondary"
                  disabled={caja.pagina === 0 || caja.ocupada}
                  onClick={() => caja.setPagina(caja.pagina - 1)}
                >
                  Anteriores
                </button>
                <span>Página {caja.pagina + 1}</span>
                <button
                  type="button"
                  className="btn secondary"
                  disabled={!datos.hayMas || caja.ocupada}
                  onClick={() => caja.setPagina(caja.pagina + 1)}
                >
                  Más cierres
                </button>
              </div>
            </>
          )}
          {(cerrando || caja.solicitud) && !caja.detalle && (
            <CierreCajaForm
              resumen={cerrando ?? datos.abierta}
              ocupada={caja.ocupada}
              incierta={!!caja.solicitud && !caja.ocupada}
              desactualizada={caja.desactualizada}
              error={caja.error}
              compraPendiente={compraPendiente}
              onCerrar={volver}
              onConfirmar={(d) => void caja.cerrar(d)}
            />
          )}
        </>
      )}
      {caja.detalle && (
        <DialogoCaja
          titulo="Cierre guardado"
          onCerrar={() => {
            caja.setDetalle(null);
            setCerrando(null);
          }}
        >
          <p className="caja-estado-periodo">Cerrado el {fechaCaja(caja.detalle.cerradoEn)}</p>
          <ResumenVentasCaja resumen={caja.detalle.resumen} titulo="Total del cierre" />
          <div className="caja-arqueo-guardado">
            <p>
              Fondo inicial:{' '}
              <strong>{dineroCaja(Math.round(caja.detalle.fondoInicial * 100))}</strong>
            </p>
            <p>
              Otras entradas: <strong>{dineroCaja(Math.round(caja.detalle.entradas * 100))}</strong>
            </p>
            <p>
              Retiros: <strong>{dineroCaja(Math.round(caja.detalle.retiros * 100))}</strong>
            </p>
            <p>
              Efectivo esperado:{' '}
              <strong>{dineroCaja(Math.round(caja.detalle.esperado * 100))}</strong>
            </p>
            <p>
              Efectivo contado:{' '}
              <strong>
                {caja.detalle.contado === null
                  ? 'Sin recuento'
                  : dineroCaja(Math.round(caja.detalle.contado * 100))}
              </strong>
            </p>
            {caja.detalle.diferencia !== null && (
              <p>
                Diferencia: <strong>{dineroCaja(Math.round(caja.detalle.diferencia * 100))}</strong>
              </p>
            )}
          </div>
          {caja.detalle.nota && <p className="caja-nota">{caja.detalle.nota}</p>}
          <button
            type="button"
            className="btn primary full"
            onClick={() => {
              caja.setDetalle(null);
              setCerrando(null);
            }}
          >
            Listo
          </button>
        </DialogoCaja>
      )}
    </section>
  );
}
