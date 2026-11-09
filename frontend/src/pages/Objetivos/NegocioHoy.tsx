import type { Negocio } from '@/lib/objetivosNegocio';
import { formatoPesos } from '@/lib/objetivos';
import { Metrica } from './piezas';

// Cómo viene el negocio en los últimos 30 días, con lo que la persona anotó, dicho en pesos y sin jerga.
export function NegocioHoy({ negocio }: { negocio: Negocio }) {
  if (!negocio.hayVentas) return null;
  return (
    <section className="objetivos-negocio" aria-labelledby="objetivos-negocio-titulo">
      <h2 id="objetivos-negocio-titulo">Cómo viene tu negocio</h2>
      <p>Los últimos 30 días, con lo que anotaste en tus cuadernos.</p>
      <div className="objetivos-negocio-cifras">
        <Metrica etiqueta="Vendiste" valor={formatoPesos(negocio.vendiste)} />
        <Metrica etiqueta="La mercadería te costó" valor={formatoPesos(negocio.mercaderia)} />
        {negocio.gastos > 0 && (
          <Metrica etiqueta="Otros gastos" valor={formatoPesos(negocio.gastos)} />
        )}
        <Metrica
          etiqueta="Te queda"
          valor={formatoPesos(negocio.queda)}
          alerta={negocio.queda < 0}
        />
        {negocio.teDeben > 0 && (
          <Metrica etiqueta="Te deben (fiado)" valor={formatoPesos(negocio.teDeben)} />
        )}
      </div>
    </section>
  );
}
