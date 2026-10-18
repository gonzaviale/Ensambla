import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';

// Provisorio hasta la etapa de «Mi zona».
export function ZonaPage() {
  return (
    <>
      <CabeceraComun active="zone" />
      <main id="main" tabIndex={-1} className="page-wrap reveal">
        <h1>Mi zona</h1>
        <PageFooter />
      </main>
    </>
  );
}
