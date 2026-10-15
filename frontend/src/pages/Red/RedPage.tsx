import { CatalogoMaestro } from '@/components/Red/CatalogoMaestro';
import { CompararProveedores } from '@/components/Red/CompararProveedores';
import { ComoFunciona } from '@/components/Red/ComoFunciona';
import { ComprasCaro } from '@/components/Red/ComprasCaro';
import { Neutralidad } from '@/components/Red/Neutralidad';
import { OfertasQueSirven } from '@/components/Red/OfertasQueSirven';
import { PageHeading } from '@/components/ui';
import { useBarrio } from '@/hooks/red/useRed';

// «Mi red»: de «app para un comercio» a «la red que conecta comercios y proveedores y avisa sola».
// Muestra el valor de la red y, a la vez, es explícita con la privacidad y la neutralidad.
export function RedPage() {
  const barrio = useBarrio();

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="La red de comercios y proveedores"
        title="Mi red"
        lead={`Ensambla cruza los precios de ${barrio.comercios} comercios y ${barrio.proveedores} proveedores de ${barrio.zona} para avisarte lo que te conviene.`}
      />
      <ComoFunciona />
      <OfertasQueSirven />
      <CompararProveedores />
      <ComprasCaro />
      <CatalogoMaestro />
      <Neutralidad />
    </div>
  );
}
