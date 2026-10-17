import { ArmadorBloques } from '@/components/Landing/ArmadorBloques';
import { CintaRubros } from '@/components/Landing/CintaRubros';
import { Comentarios } from '@/components/Landing/Comentarios';
import { ComoFunciona } from '@/components/Landing/ComoFunciona';
import { EntradaDatos } from '@/components/Landing/EntradaDatos';
import { Hero } from '@/components/Landing/Hero';
import { LandingFooter } from '@/components/Landing/LandingFooter';
import { LlamadaFinal } from '@/components/Landing/LlamadaFinal';
import { MotorMonitoreo } from '@/components/Landing/MotorMonitoreo';
import { Pilares } from '@/components/Landing/Pilares';
import { SamiAgente } from '@/components/Landing/SamiAgente';
import { useRevelado } from '@/hooks/landing/useRevelado';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import './landing.css';

export function LandingPage() {
  useTituloPagina('Tu sistema, un bloque a la vez');
  const raiz = useRevelado<HTMLDivElement>();

  return (
    <div className="landing" ref={raiz}>
      <Hero />
      <CintaRubros />
      <EntradaDatos />
      <SamiAgente />
      <MotorMonitoreo />
      <ComoFunciona />
      <ArmadorBloques />
      <Pilares />
      <Comentarios />
      <LlamadaFinal />
      <LandingFooter />
    </div>
  );
}
