import { ArmadorBloques } from '@/components/Landing/ArmadorBloques';
import { CintaRubros } from '@/components/Landing/CintaRubros';
import { Comentarios } from '@/components/Landing/Comentarios';
import { ComoFunciona } from '@/components/Landing/ComoFunciona';
import { DeLaFoto } from '@/components/Landing/DeLaFoto';
import { Hero } from '@/components/Landing/Hero';
import { LandingFooter } from '@/components/Landing/LandingFooter';
import { LlamadaFinal } from '@/components/Landing/LlamadaFinal';
import { Pilares } from '@/components/Landing/Pilares';
import { Plantillas } from '@/components/Landing/Plantillas';
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
      <DeLaFoto />
      <ComoFunciona />
      <ArmadorBloques />
      <Plantillas />
      <Pilares />
      <Comentarios />
      <LlamadaFinal />
      <LandingFooter />
    </div>
  );
}
