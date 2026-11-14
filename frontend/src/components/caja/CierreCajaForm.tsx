import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import { dineroCaja, leerMontoCaja } from '@/lib/caja';
import type { ResumenVentasCaja, SolicitudCierreCaja } from '@/types/caja';

export function CierreCajaForm({
  resumen,
  ocupada,
  incierta,
  desactualizada,
  error,
  compraPendiente,
  onCerrar,
  onConfirmar,
}: {
  resumen: ResumenVentasCaja;
  ocupada: boolean;
  incierta: boolean;
  desactualizada: boolean;
  error: string;
  compraPendiente: boolean;
  onCerrar: () => void;
  onConfirmar: (datos: Omit<SolicitudCierreCaja, 'id' | 'proyectoId' | 'inventarioId'>) => void;
}) {
  const [fondo, setFondo] = useState(''),
    [entradas, setEntradas] = useState(''),
    [retiros, setRetiros] = useState('');
  const [contado, setContado] = useState(''),
    [nota, setNota] = useState('');
  const cambios = useConfirmarCambios();
  const solicitarCierre = async () => {
    if (ocupada || incierta) return;
    if (
      !(fondo || entradas || retiros || contado || nota) ||
      (await cambios.confirmar(
        'El recuento y la nota todavía no se guardaron. El período seguirá abierto.',
      ))
    )
      onCerrar();
  };
  let base = 0,
    ingresos = 0,
    salidas = 0,
    efectivoContado: number | null = null,
    valido = true;
  try {
    base = leerMontoCaja(fondo);
    ingresos = leerMontoCaja(entradas);
    salidas = leerMontoCaja(retiros);
    efectivoContado = contado.trim() ? leerMontoCaja(contado) : null;
  } catch {
    valido = false;
  }
  const esperado = Math.round(resumen.efectivo * 100) + base + ingresos - salidas;
  if (esperado < 0) valido = false;
  const bloqueada = ocupada || incierta;
  return (
    <>
      <DialogoCaja
        titulo={incierta ? 'Verificar el cierre' : 'Cerrar caja'}
        onCerrar={() => void solicitarCierre()}
        bloqueado={bloqueada}
      >
        <div className="caja-total-resumen">
          <span>Total del período</span>
          <strong>{dineroCaja(Math.round(resumen.total * 100))}</strong>
          <span>{resumen.ventas.length} ventas</span>
        </div>
        <p className="caja-nota">
          El resumen quedará guardado. El próximo cobro abrirá un nuevo período, incluso si vendés
          hoy.
        </p>
        {compraPendiente && (
          <p className="caja-nota">
            La compra que estás armando todavía no se cobró y no entra en este cierre.
          </p>
        )}
        <details className="caja-recuento">
          <summary>Contar efectivo (opcional)</summary>
          <p className="caja-ayuda">
            Completá los movimientos de este período. Transferencias y tarjetas no forman parte del
            cajón.
          </p>
          <label>
            Efectivo al comenzar ($)
            <input
              inputMode="decimal"
              value={fondo}
              disabled={bloqueada}
              placeholder="0"
              onChange={(e) => setFondo(e.target.value)}
            />
          </label>
          <label>
            Otras entradas de efectivo ($)
            <input
              inputMode="decimal"
              value={entradas}
              disabled={bloqueada}
              placeholder="0"
              onChange={(e) => setEntradas(e.target.value)}
            />
          </label>
          <label>
            Retiros de efectivo ($)
            <input
              inputMode="decimal"
              value={retiros}
              disabled={bloqueada}
              placeholder="0"
              onChange={(e) => setRetiros(e.target.value)}
            />
          </label>
          <p>
            Efectivo esperado:{' '}
            <strong>{valido ? dineroCaja(esperado) : 'Revisá los importes'}</strong>
          </p>
          <label>
            Efectivo contado al cerrar ($)
            <input
              inputMode="decimal"
              value={contado}
              disabled={bloqueada}
              placeholder="Sin recuento"
              onChange={(e) => setContado(e.target.value)}
            />
          </label>
          {efectivoContado !== null && valido && (
            <p className="caja-diferencia" role="status">
              Diferencia: <strong>{dineroCaja(efectivoContado - esperado)}</strong>
              {efectivoContado === esperado
                ? ' · La caja coincide'
                : efectivoContado > esperado
                  ? ' · Sobrante'
                  : ' · Faltante'}
            </p>
          )}
        </details>
        <label className="caja-nota-label">
          Nota del cierre (opcional)
          <textarea
            value={nota}
            maxLength={1000}
            disabled={bloqueada}
            onChange={(e) => setNota(e.target.value)}
            placeholder="Algo que quieras recordar de este período…"
          />
        </label>
        {error && (
          <p className="caja-error" role="alert">
            {error}
          </p>
        )}
        {incierta && (
          <p className="caja-nota">
            Consultá la misma operación para verificar si se guardó. El cierre no se repetirá.
          </p>
        )}
        {!valido && <p className="caja-error">Revisá los importes del recuento.</p>}
        <div className="caja-acciones">
          {!incierta && (
            <button
              type="button"
              className="btn secondary"
              disabled={ocupada}
              onClick={() => void solicitarCierre()}
            >
              Volver
            </button>
          )}
          {desactualizada ? (
            <button type="button" className="btn primary" onClick={onCerrar}>
              Revisar resumen actualizado
            </button>
          ) : (
            <button
              type="button"
              className="btn primary"
              disabled={ocupada || (!incierta && !valido)}
              onClick={() =>
                onConfirmar({
                  revision: resumen.revision,
                  fondoInicial: base / 100,
                  entradas: ingresos / 100,
                  retiros: salidas / 100,
                  contado: efectivoContado === null ? null : efectivoContado / 100,
                  nota: nota.trim(),
                })
              }
            >
              {ocupada ? 'Guardando…' : incierta ? 'Verificar mismo cierre' : 'Guardar cierre'}
            </button>
          )}
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
