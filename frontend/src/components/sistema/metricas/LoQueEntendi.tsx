import {
  lineasDeLoQueEntendi,
  palabrasQueNoEntendi,
  type LineaDeLoQueEntendi,
} from '@/lib/metricas/loQueEntendi';
import type { DefinicionDeMetrica, LecturaDelPedido, PedidoInterpretado } from '@/types/metricas';

// «Esto entendí»: antes de guardar, la persona ve en palabras simples qué se entendió de su pedido, qué se tomó por
// defecto porque no lo decía y qué palabras no se entendieron. Mismo recuadro que «Lo que entendió Sami» en Objetivos.
export function LoQueEntendi({ propuesta }: { propuesta: PedidoInterpretado }) {
  if (!propuesta.lectura) return null;
  return <RecuadroDeLoQueEntendi definicion={propuesta.definicion} lectura={propuesta.lectura} />;
}

type PropsDelRecuadro = { definicion: DefinicionDeMetrica; lectura: LecturaDelPedido };

function RecuadroDeLoQueEntendi({ definicion, lectura }: PropsDelRecuadro) {
  const lineas = lineasDeLoQueEntendi(definicion, lectura);
  return (
    <section className="objetivos-lectura agregar-metrica-entendi" aria-label="Esto entendí">
      <p className="objetivos-etiqueta">Esto entendí</p>
      <ul>
        {lineas.map((linea) => (
          <LineaEntendida key={linea.parte} linea={linea} />
        ))}
      </ul>
      <PalabrasQueNoEntendi lectura={lectura} />
    </section>
  );
}

function LineaEntendida({ linea }: { linea: LineaDeLoQueEntendi }) {
  return (
    <li>
      <b>{linea.titulo}:</b> {linea.valor}
      {linea.porDefecto && (
        <span className="agregar-metrica-por-defecto"> ({linea.porDefecto})</span>
      )}
    </li>
  );
}

function PalabrasQueNoEntendi({ lectura }: { lectura: LecturaDelPedido }) {
  if (lectura.noEntendidas.length === 0) return null;
  return (
    <p className="agregar-metrica-no-entendidas">No entendí: {palabrasQueNoEntendi(lectura)}</p>
  );
}
