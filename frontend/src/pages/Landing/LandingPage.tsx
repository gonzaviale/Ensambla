import { BotonSubir } from '@/components/Landing/BotonSubir';
import { EntradaDatos } from '@/components/Landing/EntradaDatos';
import { FranjaCotidiana } from '@/components/Landing/FranjaCotidiana';
import { Hero } from '@/components/Landing/Hero';
import { LandingFooter } from '@/components/Landing/LandingFooter';
import { LlamadaFinal } from '@/components/Landing/LlamadaFinal';
import { Pilares } from '@/components/Landing/Pilares';
import { Preguntas } from '@/components/Landing/Preguntas';
import { Problema } from '@/components/Landing/Problema';
import { SamiAgente } from '@/components/Landing/SamiAgente';
import { useDesplazamientoSuave } from '@/hooks/landing/useDesplazamientoSuave';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import './landing.css';
import './landing-ejemplo.css';
import './landing-recorrido.css';

export function LandingPage() {
  useTituloPagina('Tu cuaderno digital, tus cuentas más claras');
  useDesplazamientoSuave();

  return (
    <div className="landing">
      <Hero />
      <FranjaCotidiana />
      <Problema />
      <EntradaDatos />
      <Pilares />
      <SamiAgente />
      <Preguntas />
      <LlamadaFinal />
      <LandingFooter />
      <BotonSubir />
    </div>
  );
}
