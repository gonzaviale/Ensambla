import { useRef, useState } from 'react';
import { vi } from 'vitest';
import { LectorDeCodigos } from '@/components/escaneo/LectorDeCodigos';
import { useCamaraCodigo } from '@/hooks/escaneo/useCamaraCodigo';

export function EscenarioLectorCamara() {
  const camara = useCamaraCodigo(vi.fn());
  const [panel, setPanel] = useState<'codigo' | 'ajustes' | null>(null);
  const entrada = useRef<HTMLInputElement>(null);
  return (
    <LectorDeCodigos
      panel={panel}
      onPanel={setPanel}
      codigo=""
      buscando={false}
      abierta={camara.abierta}
      iniciando={camara.iniciando}
      sinLectura={camara.sinLectura}
      sesion={camara.sesion}
      camaras={camara.camaras}
      camaraId={camara.camaraId}
      camaraActiva={camara.camaraActiva}
      camaraActualId={camara.camaraActualId}
      errorCamara={camara.error}
      video={camara.video}
      entradaCodigo={entrada}
      onCodigo={vi.fn()}
      onBuscar={vi.fn()}
      onAbrir={camara.abrir}
      onCerrar={camara.cerrar}
      onReiniciar={camara.reiniciar}
      onCamara={camara.seleccionarCamara}
    />
  );
}
