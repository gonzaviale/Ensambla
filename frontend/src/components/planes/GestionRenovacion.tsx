import { Btn } from '@/components/sistema/Btn';
import { useRenovacion } from '@/hooks/pagos/queries';
import { useOperacionPago, useOperacionPagoPendiente } from '@/hooks/pagos/mutations';
import { useSistemaStore } from '@/stores/sistemaStore';

const fecha = (valor: string) =>
  new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'long',
    timeStyle: 'short',
    hour12: false,
  }).format(new Date(valor));

export function GestionRenovacion() {
  const consulta = useRenovacion();
  const actualizar = useOperacionPago('actualizar');
  const pendiente = useOperacionPagoPendiente();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const datos = consulta.data;
  const contrato = datos?.contrato;
  const cancelando = contrato?.cancelacionSolicitada && contrato.estado !== 'CANCELADA';
  const abierta = contrato && contrato.estado !== 'CANCELADA';
  const importe = contrato
    ? new Intl.NumberFormat('es-AR', {
        style: 'currency',
        currency: 'ARS',
        maximumFractionDigits: 0,
      }).format(contrato.importe)
    : null;

  return (
    <div className="plan-renewal">
      <h3>Tu renovación</h3>
      {consulta.isPending ? <p role="status">Consultando renovación…</p> : null}
      {consulta.isError ? (
        <div role="alert">
          <p>{consulta.mensajeError}</p>
          <Btn
            cls="secondary small"
            disabled={consulta.isFetching}
            onClick={() => void consulta.refetch()}
          >
            Reintentar renovación
          </Btn>
        </div>
      ) : null}
      {datos ? (
        <>
          {contrato ? (
            <>
              <p>
                {cancelando
                  ? 'Cancelación pendiente de confirmación.'
                  : {
                      PREPARANDO: 'Estamos confirmando la contratación con Mercado Pago.',
                      PENDIENTE: 'Completá la autorización en Mercado Pago.',
                      AUTORIZADA: 'Renovación automática autorizada.',
                      PAUSADA: 'La renovación está en pausa.',
                      CANCELADA: 'La renovación automática está cancelada.',
                    }[contrato.estado]}
              </p>
              <p>{importe} cada 30 días.</p>
              {abierta && !cancelando ? (
                <p>
                  Primer cobro programado: {fecha(contrato.primerCobroEn)}.
                  {contrato.proximoCobroEn ? (
                    <> Próximo cobro informado: {fecha(contrato.proximoCobroEn)}.</>
                  ) : null}
                </p>
              ) : null}
              {contrato.estado === 'AUTORIZADA' ? (
                <p>El acceso pago se habilita al confirmar cada cobro aprobado.</p>
              ) : null}
              {contrato.estado === 'CANCELADA' ? (
                <p>Conservás la prueba o el período que ya pagaste hasta su vencimiento.</p>
              ) : null}
              {contrato.estado === 'PENDIENTE' &&
              contrato.enlace &&
              !cancelando &&
              datos.habilitada ? (
                <a className="btn primary full" href={contrato.enlace}>
                  Continuar en Mercado Pago
                </a>
              ) : null}
            </>
          ) : null}
          {!abierta ? (
            <>
              <Btn
                cls="primary full"
                disabled={!datos.habilitada || pendiente || consulta.isError}
                onClick={() => abrirModal({ tipo: 'contratarPremium' })}
              >
                Contratar Premium
              </Btn>
              {!datos.habilitada ? (
                <p>La contratación estará disponible próximamente.</p>
              ) : (
                <p>El primer cobro respeta los días de prueba que te queden.</p>
              )}
            </>
          ) : null}
          {abierta ? (
            <div className="plan-renewal-actions">
              <Btn
                cls="secondary small"
                disabled={pendiente || !datos.habilitada}
                onClick={() => actualizar.mutate()}
              >
                {actualizar.isPending ? 'Actualizando pagos…' : 'Actualizar pagos'}
              </Btn>
              {!cancelando ? (
                <Btn
                  cls="secondary small"
                  disabled={pendiente}
                  onClick={() => abrirModal({ tipo: 'cancelarRenovacion' })}
                >
                  Cancelar renovación
                </Btn>
              ) : null}
            </div>
          ) : null}
        </>
      ) : null}
      {actualizar.isError ? <p role="alert">{actualizar.mensajeError}</p> : null}
    </div>
  );
}
