import { formatoPesos } from '@/lib/objetivos';
import type { SemanaDeCaja } from '@/types/panel';

/** «2026-10-05» → «05/10». */
const diaYMes = (fecha: string) => `${fecha.slice(8, 10)}/${fecha.slice(5, 7)}`;

/** Un saldo con su signo, para que se vea si la semana dejó plata o la sacó: «+$110.000», «-$2.000». */
const conSigno = (valor: number) =>
  `${valor > 0 ? '+' : valor < 0 ? '-' : ''}${formatoPesos(Math.abs(valor))}`;

/** El ancho de una barra respecto de la más larga; lo que no es cero siempre se alcanza a ver. */
const ancho = (valor: number, maximo: number) =>
  valor <= 0 || maximo <= 0 ? 0 : Math.max(2, (valor / maximo) * 100);

// El flujo de caja, una fila por semana: lo que entró, lo que salió y lo que dejó, con los importes escritos.
// Todas las barras comparten escala, así que se pueden comparar entre semanas. Sin hover ni tooltips: en el
// celular no existen, y acá no hace falta tocar nada para leer los números.
export function FlujoDeCajaPorSemana({ semanas }: { semanas: SemanaDeCaja[] }) {
  const maximo = Math.max(0, ...semanas.flatMap((semana) => [semana.entradas, semana.salidas]));
  const queda = semanas.at(-1)?.saldoAcumulado ?? 0;

  return (
    <>
      <ol className="panel-semanas" aria-label="Flujo de caja por semana">
        {semanas.map((semana) => (
          <li key={semana.desde} className="panel-semana">
            <h4>
              {diaYMes(semana.desde)} al {diaYMes(semana.hasta)}
            </h4>
            <FilaDeSemana tipo="entra" etiqueta="Entró" importe={semana.entradas} maximo={maximo} />
            <FilaDeSemana tipo="sale" etiqueta="Salió" importe={semana.salidas} maximo={maximo} />
            <p className="panel-semana-deja" data-signo={Math.sign(semana.saldo)}>
              <span>Dejó</span>
              <strong>{conSigno(semana.saldo)}</strong>
            </p>
          </li>
        ))}
      </ol>
      <p className="panel-flujo-total" data-signo={Math.sign(queda)}>
        <span>Te va quedando</span>
        <strong>{conSigno(queda)}</strong>
      </p>
    </>
  );
}

type PropsDeFila = { tipo: 'entra' | 'sale'; etiqueta: string; importe: number; maximo: number };

function FilaDeSemana({ tipo, etiqueta, importe, maximo }: PropsDeFila) {
  return (
    <div className="panel-semana-fila" data-tipo={tipo}>
      <span className="panel-semana-etiqueta">{etiqueta}</span>
      <span className="panel-semana-barra" aria-hidden="true">
        <i style={{ width: `${ancho(importe, maximo)}%` }} />
      </span>
      <strong className="panel-semana-importe">{formatoPesos(importe)}</strong>
    </div>
  );
}
