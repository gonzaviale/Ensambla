import { PanelDeAvisos } from '@/components/sistema/avisos/PanelDeAvisos';
import { SamiFlotante } from '@/components/sistema/avisos/SamiFlotante';
import { TarjetaDeImpacto } from '@/components/sistema/avisos/TarjetaDeImpacto';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';

// La pestaña «Avisos»: lo que Sami encontró en tus datos, con más lugar que el panel lateral de antes.
export function AvisosPage() {
  useTituloPagina('Avisos de Sami');
  // El chat flotante habla del cuaderno que se estaba usando (o, si no hay, del primero).
  const sistemaDelChat = useSistemaStore(
    (estado) =>
      estado.sistemas.find((item) => item.id === estado.activeId)?.id ?? estado.sistemas[0]?.id,
  );
  return (
    <>
      <CabeceraComun active="avisos" />
      <main id="main" tabIndex={-1} className="page-wrap avisos-page reveal">
        <TarjetaDeImpacto />
        <PanelDeAvisos />
      </main>
      {/* Fuera del <main>: su animación de entrada haría que «fixed» se mueva con la página. */}
      {sistemaDelChat && <SamiFlotante sistemaId={sistemaDelChat} />}
    </>
  );
}
