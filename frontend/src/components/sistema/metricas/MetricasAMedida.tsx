import { useState } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { AgregarMetrica } from '@/components/sistema/metricas/AgregarMetrica';
import { useMetricasAMedida } from '@/hooks/metricas/queries';
import { useQuitarMetrica } from '@/hooks/metricas/mutations';
import {
  formatoDelValor,
  resumenDeLaReceta,
  textoDeDondeSale,
} from '@/lib/metricas/metricasAMedida';
import type { Metrica, ResultadoDeMetrica } from '@/types/metricas';

// «Tus métricas»: las que la persona eligió para su cuaderno. Cada una muestra el número que calcula el servidor (o
// por qué no se pudo, nunca un cero inventado) y de dónde sale. Desde acá también se agregan y se quitan.
export function MetricasAMedida({ sistemaId, cuaderno }: { sistemaId: string; cuaderno: string }) {
  const { metricas, fallo, reintentar } = useMetricasAMedida(sistemaId);
  const [agregando, setAgregando] = useState(false);

  return (
    <section className="metricas-medida" aria-label="Tus métricas">
      <header className="metricas-medida-cabecera">
        <div>
          <h2>Tus métricas</h2>
          <p>Lo que elegiste mirar de «{cuaderno}». Los números salen de lo que cargaste.</p>
        </div>
        {!agregando && (
          <button type="button" className="metricas-medida-agregar" onClick={() => setAgregando(true)}>
            <Icon name="plus" />
            Agregar métrica
          </button>
        )}
      </header>

      {agregando && <AgregarMetrica sistemaId={sistemaId} onCerrar={() => setAgregando(false)} />}

      {fallo ? (
        <p className="metricas-medida-aviso" role="status">
          No pudimos traer tus métricas ahora.{' '}
          <button type="button" onClick={() => void reintentar()}>
            Probar de nuevo
          </button>
        </p>
      ) : !metricas ? (
        <p className="metricas-medida-aviso" role="status">
          Mirando tus métricas…
        </p>
      ) : metricas.length === 0 ? (
        <p className="metricas-medida-aviso">
          Todavía no elegiste ninguna. Tocá «Agregar métrica» y probá con las que te sugerimos.
        </p>
      ) : (
        <ul className="metricas-medida-lista">
          {metricas.map((metrica) => (
            <TarjetaDeMetrica key={metrica.id} metrica={metrica} sistemaId={sistemaId} />
          ))}
        </ul>
      )}
    </section>
  );
}

function TarjetaDeMetrica({ metrica, sistemaId }: { metrica: Metrica; sistemaId: string }) {
  const quitar = useQuitarMetrica(sistemaId);
  const [conDetalle, setConDetalle] = useState(false);
  const { definicion, resultado } = metrica;
  return (
    <li className="metrica-medida">
      <div className="metrica-medida-titulo">
        <h3>{definicion.nombre}</h3>
        <button
          type="button"
          className="metrica-medida-quitar"
          aria-label={`Quitar ${definicion.nombre}`}
          disabled={quitar.isPending}
          onClick={() => quitar.mutate(metrica.id)}
        >
          <Icon name="trash" />
        </button>
      </div>
      <p className="metrica-medida-receta">{resumenDeLaReceta(definicion)}</p>
      <ResultadoDeLaMetrica resultado={resultado} />
      <button
        type="button"
        className="metrica-medida-origen"
        aria-expanded={conDetalle}
        onClick={() => setConDetalle(!conDetalle)}
      >
        ¿De dónde sale?
      </button>
      {conDetalle && <p className="metrica-medida-detalle">{textoDeDondeSale(resultado)}</p>}
      {quitar.isError && (
        <p className="metrica-medida-error" role="alert">
          No pudimos quitarla. Probá de nuevo.
        </p>
      )}
    </li>
  );
}

/** El número grande y, si la métrica se reparte, las barras; si no se pudo calcular, el motivo en palabras. */
export function ResultadoDeLaMetrica({ resultado }: { resultado: ResultadoDeMetrica }) {
  if (resultado.valor == null)
    return (
      <p className="metrica-medida-motivo" role="status">
        {resultado.motivo ?? 'Todavía no se puede calcular.'}
      </p>
    );
  const maximo = Math.max(...resultado.filas.map((fila) => fila.valor), 0);
  return (
    <>
      <p className="metrica-medida-valor">{formatoDelValor(resultado.valor, resultado.formato)}</p>
      {resultado.filas.length > 0 && (
        <ul className="metrica-medida-reparto">
          {resultado.filas.map((fila) => (
            <li key={fila.etiqueta}>
              <span className="metrica-medida-etiqueta">{fila.etiqueta}</span>
              <span className="metrica-medida-barra" aria-hidden="true">
                <span style={{ width: `${maximo > 0 ? Math.max(4, (fila.valor / maximo) * 100) : 0}%` }} />
              </span>
              <span className="metrica-medida-numero">{formatoDelValor(fila.valor, resultado.formato)}</span>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
