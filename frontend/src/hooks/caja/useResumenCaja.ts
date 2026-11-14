import { useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { consultarCaja, cerrarCaja, obtenerCierreCaja } from '@/api/caja';
import { ApiError } from '@/api/apiError';
import { idCaja } from '@/lib/caja';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { CierreCaja, SolicitudCierreCaja } from '@/types/caja';

export function useResumenCaja(sistemaId: string, inventarioId: string) {
  const usuario = useSistemaStore((s) => s.usuarioId);
  const clave = `ensambla:cierre:${usuario}:${sistemaId}:${inventarioId}`;
  const [fecha, setFecha] = useState('');
  const [pagina, setPagina] = useState(0);
  const [solicitud, setSolicitud] = useState<SolicitudCierreCaja | null>(() => {
    try {
      return JSON.parse(sessionStorage.getItem(clave) ?? 'null') as SolicitudCierreCaja | null;
    } catch {
      return null;
    }
  });
  const [ocupada, setOcupada] = useState(false);
  const [error, setError] = useState('');
  const [desactualizada, setDesactualizada] = useState(false);
  const [detalle, setDetalle] = useState<CierreCaja | null>(null);
  const bloqueo = useRef(false);
  const consulta = useQuery({
    queryKey: ['caja-resumen', usuario, sistemaId, inventarioId, fecha, pagina],
    queryFn: () => consultarCaja(idCaja(sistemaId), idCaja(inventarioId), fecha, pagina),
    staleTime: 0,
    refetchInterval: 30_000,
  });
  const cerrar = async (datos: Omit<SolicitudCierreCaja, 'id' | 'proyectoId' | 'inventarioId'>) => {
    if (bloqueo.current || !consulta.data) return;
    const request = solicitud ?? {
      ...datos,
      id: crypto.randomUUID(),
      proyectoId: idCaja(sistemaId),
      inventarioId: idCaja(inventarioId),
    };
    bloqueo.current = true;
    setOcupada(true);
    setError('');
    setDesactualizada(false);
    setSolicitud(request);
    sessionStorage.setItem(clave, JSON.stringify(request));
    try {
      const respuesta = await cerrarCaja(request);
      setDetalle(respuesta);
      setSolicitud(null);
      sessionStorage.removeItem(clave);
      await consulta.refetch();
    } catch (e) {
      if (e instanceof ApiError && e.status >= 400 && e.status < 500) {
        setSolicitud(null);
        sessionStorage.removeItem(clave);
        await consulta.refetch();
        setDesactualizada(e.status === 409);
      }
      setError(
        e instanceof Error
          ? e.message
          : 'No pudimos verificar el cierre. Consultá la misma operación.',
      );
    } finally {
      bloqueo.current = false;
      setOcupada(false);
    }
  };
  const abrirDetalle = async (id: string) => {
    if (bloqueo.current) return;
    bloqueo.current = true;
    setOcupada(true);
    setError('');
    try {
      setDetalle(await obtenerCierreCaja(id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No pudimos cargar el cierre.');
    } finally {
      bloqueo.current = false;
      setOcupada(false);
    }
  };
  return {
    consulta,
    fecha,
    setFecha,
    pagina,
    setPagina,
    solicitud,
    ocupada,
    error,
    detalle,
    setDetalle,
    cerrar,
    abrirDetalle,
    desactualizada,
    revisar: () => {
      setDesactualizada(false);
      setError('');
    },
  };
}
