import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';

// Provisorio hasta la etapa del taller con Sami.
export function TallerPage() {
  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="creation-studio reveal">
        <h1>Lo armamos juntos.</h1>
        <PageFooter />
      </main>
    </>
  );
}
