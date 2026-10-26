import { PanelDeAvisos } from '@/components/sistema/avisos/PanelDeAvisos';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useTituloPagina } from '@/hooks/useTituloPagina';

// La pestaña «Avisos»: lo que Sami encontró en tus datos, con más lugar que el panel lateral de antes.
export function AvisosPage() {
  useTituloPagina('Avisos de Sami');
  return (
    <>
      <CabeceraComun active="avisos" />
      <main id="main" tabIndex={-1} className="page-wrap avisos-page reveal">
        <PanelDeAvisos />
      </main>
    </>
  );
}
