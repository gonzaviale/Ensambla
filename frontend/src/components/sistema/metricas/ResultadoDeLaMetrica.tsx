import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type { ResultadoDeMetrica } from '@/types/metricas';

/** El número grande y, si la métrica se reparte, las barras; si no se pudo calcular, el motivo en palabras. */
export function ResultadoDeLaMetrica({ resultado }: { resultado: ResultadoDeMetrica }) {
  if (resultado.valor == null)
    return (
      <p className="metrica-medida-motivo" role="status">
        {resultado.motivo ?? 'Todavía no se puede calcular.'}
      </p>
    );
  return (
    <>
      <p className="metrica-medida-valor">{formatoDelValor(resultado.valor, resultado.formato)}</p>
      {resultado.filas.length > 0 && <RepartoDeLaMetrica resultado={resultado} />}
    </>
  );
}

function RepartoDeLaMetrica({ resultado }: { resultado: ResultadoDeMetrica }) {
  const maximo = Math.max(...resultado.filas.map((fila) => fila.valor), 0);
  return (
    <ul className="metrica-medida-reparto">
      {resultado.filas.map((fila) => (
        <li key={fila.etiqueta}>
          <span className="metrica-medida-etiqueta">{fila.etiqueta}</span>
          <span className="metrica-medida-barra" aria-hidden="true">
            <span style={{ width: anchoDeLaBarra(fila.valor, maximo) }} />
          </span>
          <span className="metrica-medida-numero">
            {formatoDelValor(fila.valor, resultado.formato)}
          </span>
        </li>
      ))}
    </ul>
  );
}

const anchoDeLaBarra = (valor: number, maximo: number) =>
  `${maximo > 0 ? Math.max(4, (valor / maximo) * 100) : 0}%`;
