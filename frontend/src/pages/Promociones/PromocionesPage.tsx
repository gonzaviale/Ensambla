import { useMemo, useState } from 'react';
import { ElegirCanal } from '@/components/promociones/ElegirCanal';
import { ElegirPromocion } from '@/components/promociones/ElegirPromocion';
import { VistaPreviaDePromocion } from '@/components/promociones/VistaPreviaDePromocion';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { useRedactarPromocion } from '@/hooks/promociones/mutations';
import { usePromociones } from '@/hooks/promociones/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { promocionListaDePrecios, sistemaParaPromocionar } from '@/lib/promociones/sistemaParaPromocionar';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { Promocion } from '@/types/promociones';

const NEGOCIO_SIN_NOMBRE = 'Mi comercio';

// «Promocioná tu comercio»: Sami propone qué publicar a partir de lo que se vende, escribe el texto para el canal
// elegido y arma la imagen. La persona revisa y comparte: nada se publica solo.
export function PromocionesPage() {
  useTituloPagina('Promocioná tu comercio');
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const activoId = useSistemaStore((estado) => estado.activeId);
  const sistema = useMemo(() => sistemaParaPromocionar(sistemas, activoId), [sistemas, activoId]);

  return (
    <>
      <CabeceraComun active="promociones" />
      <main id="main" tabIndex={-1} className="page-wrap promociones-page reveal">
        <div className="page-title">
          <div className="eyebrow">PROMOCIONÁ</div>
          <h1>Promocioná tu comercio</h1>
          <p>Sami arma la publicación con lo que vendés. Vos la revisás y la compartís.</p>
        </div>
        {sistema ? <PromocionesDelSistema sistema={sistema} /> : <SinInventario />}
        <PageFooter />
      </main>
    </>
  );
}

function SinInventario() {
  const navegacion = useNavegacion();
  return (
    <div className="empty">
      <Icon name="box" />
      <p>Para promocionar, Sami necesita saber qué vendés: armá un cuaderno con tu inventario y volvé.</p>
      <Btn cls="primary" onClick={() => void navegacion.irAInicio()}>
        Armar mi cuaderno
      </Btn>
    </div>
  );
}

function PromocionesDelSistema({ sistema }: { sistema: Sistema }) {
  const sugerencias = usePromociones(sistema.id);
  const negocio = useConfiguracionNegocio().data?.nombre ?? NEGOCIO_SIN_NOMBRE;
  const redaccion = useRedactarPromocion();
  const [clave, setClave] = useState<string | null>(null);
  const [canal, setCanal] = useState<string | null>(null);
  const [texto, setTexto] = useState<string | null>(null);

  const promociones = useMemo(() => {
    const lista = promocionListaDePrecios(sistema);
    return [...(sugerencias.data?.promociones ?? []), ...(lista ? [lista] : [])];
  }, [sistema, sugerencias.data]);
  const elegida = promociones.find((promocion) => promocion.clave === clave) ?? null;

  const elegirPromocion = (nueva: string) => {
    setClave(nueva);
    setCanal(null);
    setTexto(null);
  };

  const elegirCanal = (nuevo: string, promocion: Promocion) => {
    setCanal(nuevo);
    setTexto(null);
    redaccion.redactar({ canal: nuevo, promocion }, { onSuccess: (respuesta) => setTexto(respuesta.texto) });
  };

  if (sugerencias.isPending) return <p role="status">Mirando lo que vendés…</p>;
  if (sugerencias.isError) {
    return (
      <div role="alert" className="empty">
        <p>No pudimos ver tus ventas. Probá nuevamente.</p>
        <Btn cls="secondary" onClick={() => void sugerencias.refetch()}>
          Reintentar
        </Btn>
      </div>
    );
  }
  if (promociones.length === 0) {
    return (
      <div className="empty">
        <Icon name="chart" />
        <p>
          Todavía no hay nada para promocionar. Cargá productos con precio y anotá algunas ventas, y Sami te
          propone qué publicar.
        </p>
      </div>
    );
  }

  return (
    <>
      <h2>1. ¿Qué querés promocionar?</h2>
      <ElegirPromocion promociones={promociones} elegida={clave} onElegir={elegirPromocion} />
      {elegida ? (
        <>
          <h2>2. ¿Dónde lo vas a compartir?</h2>
          <ElegirCanal
            canales={sugerencias.data.canales}
            elegido={canal}
            deshabilitado={redaccion.isPending}
            onElegir={(nuevo) => elegirCanal(nuevo, elegida)}
          />
          {redaccion.isPending ? <p role="status">Sami está escribiendo…</p> : null}
          {redaccion.isError ? (
            <p role="alert" className="field-error">
              {redaccion.mensajeError}
            </p>
          ) : null}
        </>
      ) : null}
      {elegida && canal && texto !== null ? (
        <>
          <h2>3. Revisala y compartila</h2>
          <VistaPreviaDePromocion
            negocio={negocio}
            canal={canal}
            promocion={elegida}
            texto={texto}
            onTexto={setTexto}
          />
        </>
      ) : null}
    </>
  );
}
