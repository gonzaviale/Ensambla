import { Desbloquea } from '@/components/Landing/Desbloquea';
import { Diferencias } from '@/components/Landing/Diferencias';
import { Hero } from '@/components/Landing/Hero';
import { LandingFooter } from '@/components/Landing/LandingFooter';
import { LlamadaFinal } from '@/components/Landing/LlamadaFinal';
import { ParaQuien } from '@/components/Landing/ParaQuien';
import { Planes } from '@/components/Landing/Planes';
import { SeEnsamblaSolo } from '@/components/Landing/SeEnsamblaSolo';
import { TraeLoQueTenes } from '@/components/Landing/TraeLoQueTenes';
import { useRevelado } from '@/hooks/landing/useRevelado';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import './landing.css';

// Relato: contá tu negocio, para quién es, se ensambla solo, cada bloque responde, en qué nos
// diferenciamos, traé lo que ya tenés, planes y cierre.
export function LandingPage() {
  useTituloPagina('Contá tu negocio. Ensambla lo arma.');
  const raiz = useRevelado<HTMLDivElement>();

  return (
    <div className="landing" ref={raiz}>
      <Hero />
      <ParaQuien />
      <SeEnsamblaSolo />
      <Desbloquea />
      <Diferencias />
      <TraeLoQueTenes />
      <Planes />
      <LlamadaFinal />
      <LandingFooter />
    </div>
  );
}
