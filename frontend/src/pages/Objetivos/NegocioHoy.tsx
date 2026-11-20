import { formatoPesos } from '@/lib/objetivos';
import { tonoDe, type ConceptoDeColor } from '@/lib/sistema/tonos';
import type { ComoVieneTuNegocio } from '@/types/comoVieneTuNegocio';

// Cómo viene el negocio en los últimos 30 días, con lo que la persona anotó, dicho en pesos y sin jerga. Lo calcula
// el servidor; mientras carga o sin ventas no se muestra ningún número.
export function NegocioHoy({ negocio }: { negocio: ComoVieneTuNegocio | undefined }) {
  if (!negocio?.hayVentas) return null;
  return (
    <section className="objetivos-negocio" aria-labelledby="objetivos-negocio-titulo">
      <h2 id="objetivos-negocio-titulo">Cómo viene tu negocio</h2>
      <p>Los últimos 30 días, con lo que anotaste en tus secciones.</p>
      <CifrasDelNegocio negocio={negocio} />
    </section>
  );
}

function CifrasDelNegocio({ negocio }: { negocio: ComoVieneTuNegocio }) {
  return (
    <div className="objetivos-negocio-cifras">
      <CifraDelNegocio etiqueta="Vendiste" valor={negocio.vendiste} concepto="ventas" />
      <CifraDelNegocio
        etiqueta="La mercadería te costó"
        valor={negocio.mercaderia}
        concepto="costoDeMercaderia"
      />
      {negocio.gastos > 0 && (
        <CifraDelNegocio etiqueta="Otros gastos" valor={negocio.gastos} concepto="otrosGastos" />
      )}
      <CifraDelNegocio etiqueta="Te queda" valor={negocio.queda} concepto="ganancia" />
      {negocio.teDeben > 0 && (
        <CifraDelNegocio etiqueta="Te deben (fiado)" valor={negocio.teDeben} concepto="fiado" />
      )}
    </div>
  );
}

type PropsDeCifra = { etiqueta: string; valor: number; concepto: ConceptoDeColor };

function CifraDelNegocio({ etiqueta, valor, concepto }: PropsDeCifra) {
  return (
    <div className="objetivos-metrica solido" data-tono={tonoDe(concepto)}>
      <span>{etiqueta}</span>
      <strong className="solido-cifra">{formatoPesos(valor)}</strong>
    </div>
  );
}
