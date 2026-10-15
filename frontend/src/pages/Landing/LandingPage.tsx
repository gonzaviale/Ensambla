import { ArmadorBloques } from '@/components/Landing/ArmadorBloques';
import { Avisos } from '@/components/Landing/Avisos';
import { CintaRubros } from '@/components/Landing/CintaRubros';
import { Comentarios } from '@/components/Landing/Comentarios';
import { ComoFunciona } from '@/components/Landing/ComoFunciona';
import { DeLaFoto } from '@/components/Landing/DeLaFoto';
import { Dolor } from '@/components/Landing/Dolor';
import { Hero } from '@/components/Landing/Hero';
import { LandingFooter } from '@/components/Landing/LandingFooter';
import { LlamadaFinal } from '@/components/Landing/LlamadaFinal';
import { ListaLlega } from '@/components/Landing/ListaLlega';
import { Pilares } from '@/components/Landing/Pilares';
import { Planes } from '@/components/Landing/Planes';
import { Plantillas } from '@/components/Landing/Plantillas';
import { RedQueTrabaja } from '@/components/Landing/RedQueTrabaja';
import { useRevelado } from '@/hooks/landing/useRevelado';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import './landing.css';

// Relato: el dolor → llega la lista → avisos → la red → otros caminos → precios → cierre.
export function LandingPage() {
  useTituloPagina('Tu sistema, un bloque a la vez');
  const raiz = useRevelado<HTMLDivElement>();

  return (
    <div className="landing" ref={raiz}>
      <Hero />
      <CintaRubros />
      <Dolor />
      <ListaLlega />
      <Avisos />
      <RedQueTrabaja />
      <DeLaFoto />
      <ComoFunciona />
      <ArmadorBloques />
      <Plantillas />
      <Pilares />
      <Comentarios />
      <Planes />
      <LlamadaFinal />
      <LandingFooter />
    </div>
  );
}
