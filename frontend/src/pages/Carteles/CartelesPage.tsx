import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { PageFooter } from '@/components/sistema/PageFooter';
import { AccionesDelCartel } from '@/components/sistema/carteles/AccionesDelCartel';
import { EditorDeTextos } from '@/components/sistema/carteles/EditorDeTextos';
import { OpcionesDeCartel } from '@/components/sistema/carteles/OpcionesDeCartel';
import { PromocionesSugeridas } from '@/components/sistema/carteles/PromocionesSugeridas';
import { useCrearCartel } from '@/hooks/carteles/useCrearCartel';
import { usePromocionesSugeridas } from '@/hooks/carteles/queries';
import { useTituloPagina } from '@/hooks/useTituloPagina';

export function CartelesPage() {
  useTituloPagina('Carteles y promociones');
  return (
    <>
      <CabeceraComun active="carteles" />
      <main id="main" tabIndex={-1} className="carteles-page reveal">
        <h1>Carteles y promociones</h1>
        <p>Armá un cartel con tu marca y tus precios, listo para imprimir o compartir.</p>
        <CreadorDeCartel />
      </main>
      <PageFooter />
    </>
  );
}

function CreadorDeCartel() {
  const creador = useCrearCartel();
  const promociones = usePromocionesSugeridas();
  const sugeridas = promociones.data?.promociones ?? [];
  return (
    <>
      <QueSePromociona
        sugeridas={sugeridas}
        cargando={promociones.isPending}
        ocupado={creador.redactando}
        onElegir={(promocion) => void creador.empezarCon(promocion)}
      />
      {creador.error ? <p role="alert">{creador.error.message}</p> : null}
      {creador.cartel ? <CartelArmado creador={creador} /> : null}
    </>
  );
}

type Sugeridas = ReturnType<typeof usePromocionesSugeridas>['data'];
type PropsQueSePromociona = {
  sugeridas: NonNullable<Sugeridas>['promociones'];
  cargando: boolean;
  ocupado: boolean;
  onElegir: Parameters<typeof PromocionesSugeridas>[0]['onElegir'];
};

function QueSePromociona({ sugeridas, cargando, ocupado, onElegir }: PropsQueSePromociona) {
  if (cargando) return <p>Buscando qué conviene promocionar…</p>;
  if (sugeridas.length === 0) {
    return <p>Todavía no encontramos nada para promocionar. Cuando Sami lo detecte, lo vas a ver acá.</p>;
  }
  return <PromocionesSugeridas promociones={sugeridas} deshabilitado={ocupado} onElegir={onElegir} />;
}

function CartelArmado({ creador }: { creador: ReturnType<typeof useCrearCartel> }) {
  const cartel = creador.cartel;
  if (!cartel) return null;
  return (
    <section aria-label="Tu cartel">
      <OpcionesDeCartel
        opciones={cartel.opciones}
        elegida={cartel.variante}
        onElegir={creador.elegirVariante}
      />
      <EditorDeTextos
        contenido={cartel.contenido}
        ocupado={creador.redactando}
        onAplicar={(cambios) => void creador.actualizarContenido(cambios)}
      />
      <AccionesDelCartel
        textoPosteo={cartel.textoPosteo}
        descargando={creador.descargando}
        onDescargar={() => void creador.descargar()}
      />
    </section>
  );
}
