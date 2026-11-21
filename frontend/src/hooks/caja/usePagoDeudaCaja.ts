import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { registrarPagoDeudaCaja } from '@/api/caja';
import { ApiError } from '@/api/apiError';
import { idCaja } from '@/lib/caja';
import { clavePagoDeudaCaja } from '@/lib/cobrosDeudasCaja';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { SolicitudPagoDeudaCaja, CobroDeudaCaja } from '@/types/caja';

export function usePagoDeudaCaja(sistemaId: string, inventarioId: string, clienteId: string) {
  const usuarioId = useSistemaStore((s) => s.usuarioId);
  const clave = clavePagoDeudaCaja(usuarioId, sistemaId, inventarioId, clienteId);
  const [solicitud, setSolicitud] = useState<SolicitudPagoDeudaCaja | null>(() => {
    try {
      return JSON.parse(sessionStorage.getItem(clave) ?? 'null') as SolicitudPagoDeudaCaja | null;
    } catch {
      return null;
    }
  });
  const [ocupada, setOcupada] = useState(false);
  const [error, setError] = useState('');
  const [confirmado, setConfirmado] = useState<CobroDeudaCaja | null>(null);
  const bloqueo = useRef(false);
  const cliente = useQueryClient();
  const confirmar = async (
    aplicaciones: SolicitudPagoDeudaCaja['aplicaciones'],
    cobroQrId?: string,
  ) => {
    if (bloqueo.current) return;
    const request = solicitud ?? {
      id: crypto.randomUUID(),
      proyectoId: idCaja(sistemaId),
      inventarioId: idCaja(inventarioId),
      aplicaciones,
      ...(cobroQrId ? { cobroQrId } : {}),
    };
    bloqueo.current = true;
    setOcupada(true);
    setError('');
    setSolicitud(request);
    sessionStorage.setItem(clave, JSON.stringify(request));
    try {
      const respuesta = await registrarPagoDeudaCaja(request);
      setConfirmado(respuesta);
      setSolicitud(null);
      sessionStorage.removeItem(clave);
      await cliente.invalidateQueries();
    } catch (e) {
      if (e instanceof ApiError && e.status >= 400 && e.status < 500) {
        setSolicitud(null);
        sessionStorage.removeItem(clave);
        void cliente.invalidateQueries();
      }
      setError(e instanceof Error ? e.message : 'No pudimos verificar si se guardó el cobro.');
    } finally {
      bloqueo.current = false;
      setOcupada(false);
    }
  };
  return { solicitud, ocupada, error, confirmado, confirmar };
}
