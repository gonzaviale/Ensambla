import { useState } from 'react';
import { validarMeta } from '@/lib/sistema/metricasInicio';

// El UUID de sistema identifica su negocio; la cuenta además separa metas entre usuarios.
export const claveMetaInicio = (usuarioId: string, sistemaId: string, metrica: string) =>
  `ensambla:metas:v1:${JSON.stringify([usuarioId, sistemaId, metrica])}`;

export function useMetaInicio(
  usuarioId: string,
  sistemaId: string,
  metrica: string,
  moneda = false,
) {
  const clave = claveMetaInicio(usuarioId, sistemaId, metrica);
  const [meta, setMeta] = useState<number | null>(() => {
    try {
      return validarMeta(localStorage.getItem(clave) ?? '', moneda);
    } catch {
      return null;
    }
  });
  const [aviso, setAviso] = useState('');
  const guardar = (valor: number | null) => {
    setMeta(valor);
    try {
      if (valor === null) localStorage.removeItem(clave);
      else localStorage.setItem(clave, String(valor));
      setAviso('');
    } catch {
      setAviso('La meta funciona en esta visita, pero este navegador no permite guardarla.');
    }
  };
  return { meta, guardar, aviso };
}
