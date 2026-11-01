import { useImpactoDeAvisos } from '@/hooks/avisos/queries';
import { formatoPesos } from '@/lib/sistema/formato';

const unidades = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 });

// Qué pasó con las ventas después de aplicar un aviso. Es una comparación entre los días anteriores y los posteriores:
// no se dice que el aviso fue la causa, porque otras cosas también mueven las ventas. Sin avisos medidos no se muestra.
export function TarjetaDeImpacto() {
  const impacto = useImpactoDeAvisos();
  if (!impacto || impacto.medidos.length === 0) return null;

  const delta = impacto.ingresoSemanalDelta;
  const titular =
    delta === 0
      ? 'Tus ventas por semana están igual que antes de aplicar los avisos.'
      : `Después de aplicar los avisos, vendés ${formatoPesos(Math.abs(delta))} ${
          delta > 0 ? 'más' : 'menos'
        } por semana que antes.`;

  return (
    <section className="impacto-avisos" aria-labelledby="impacto-avisos-titulo">
      <h2 id="impacto-avisos-titulo">Qué pasó con tus ventas</h2>
      <p className="impacto-avisos-titular">{titular}</p>
      <ul className="impacto-avisos-lista">
        {impacto.medidos.map((medido) => (
          <li key={medido.avisoId}>
            <strong>{medido.titulo}</strong>
            <span>
              {unidades.format(medido.unidadesPorDiaAntes)} → {unidades.format(medido.unidadesPorDiaDespues)}{' '}
              unidades por día, comparando {medido.ventanaDias} días antes y después de aplicarlo
            </span>
          </li>
        ))}
      </ul>
      <p className="impacto-avisos-nota">
        Es una comparación entre antes y después: otras cosas también cambian las ventas.
      </p>
    </section>
  );
}
