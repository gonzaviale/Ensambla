import { BotonSubir } from '@/components/Landing/BotonSubir';
import { CintaRubros } from '@/components/Landing/CintaRubros';
import { Comentarios } from '@/components/Landing/Comentarios';
import { ComoFunciona } from '@/components/Landing/ComoFunciona';
import { EntradaDatos } from '@/components/Landing/EntradaDatos';
import { Hero } from '@/components/Landing/Hero';
import { LandingFooter } from '@/components/Landing/LandingFooter';
import { LlamadaFinal } from '@/components/Landing/LlamadaFinal';
import { MotorMonitoreo } from '@/components/Landing/MotorMonitoreo';
import { Pilares } from '@/components/Landing/Pilares';
import { Problema } from '@/components/Landing/Problema';
import { SamiAgente } from '@/components/Landing/SamiAgente';
import { useDesplazamientoSuave } from '@/hooks/landing/useDesplazamientoSuave';
import { useRevelado } from '@/hooks/landing/useRevelado';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import './landing.css';

export function LandingPage() {
  useTituloPagina('Tu sistema, un bloque a la vez');
  const raiz = useRevelado<HTMLDivElement>();
  useDesplazamientoSuave();

  return (
    <div className="landing" ref={raiz}>
      <Hero />
      <CintaRubros />
      <Problema />
      <SamiAgente />
      <ComoFunciona />
      <MotorMonitoreo />
      <EntradaDatos />
      <Pilares />
      <Comentarios />
      <LlamadaFinal />
      <LandingFooter />
      <BotonSubir />
    </div>
  );
}
