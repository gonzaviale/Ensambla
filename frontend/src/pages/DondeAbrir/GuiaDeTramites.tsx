import { useMemo } from 'react';
import type { DatosDeZona, Rubro } from '@/lib/dondeAbrir';
import { guiaDeTramites } from '@/lib/tramitesParaAbrir';
import {
  AprobacionesNecesarias,
  CosasATenerEnCuenta,
  DocumentosAMano,
  DondeIrYConQuienHablar,
  FuentesDeLaGuia,
  NarracionDeSami,
  PasosDelTramite,
  ResumenDelTramite,
  TamanoDelLocal,
} from './PartesDeLaGuia';

type Props = {
  zona: DatosDeZona;
  rubro: Rubro;
  superficie: number;
  onSuperficie: (metros: number) => void;
};

// Lo que hay que tramitar para abrir en la zona elegida: qué aprobaciones, qué documentos y si se hace online o
// en la oficina. Sami lo cuenta primero en pocas palabras; el detalle está abajo.
export function GuiaDeTramites({ zona, rubro, superficie, onSuperficie }: Props) {
  const { jurisdiccion } = zona;
  const guia = useMemo(
    () => guiaDeTramites(jurisdiccion, rubro, superficie),
    [jurisdiccion, rubro, superficie],
  );

  return (
    <div className="donde-guia">
      <NarracionDeSami guia={guia} />
      <TamanoDelLocal superficie={superficie} onSuperficie={onSuperficie} />
      <ResumenDelTramite guia={guia} />
      <AprobacionesNecesarias guia={guia} />
      <PasosDelTramite guia={guia} />
      <DocumentosAMano guia={guia} />
      <DondeIrYConQuienHablar guia={guia} />
      <CosasATenerEnCuenta guia={guia} />
      <FuentesDeLaGuia guia={guia} jurisdiccion={jurisdiccion} />
    </div>
  );
}
