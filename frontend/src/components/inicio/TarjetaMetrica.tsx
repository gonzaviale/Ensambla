import { useId, useRef, useState, type FormEvent } from 'react';
import { useMetaInicio } from '@/hooks/inicio/useMetaInicio';
import { validarMeta, type MetricaInicio } from '@/lib/sistema/metricasInicio';
import { Icon } from '@/components/sistema/Icon';

export function TarjetaMetrica({
  metrica,
  usuarioId,
  sistemaId,
}: {
  metrica: MetricaInicio;
  usuarioId: string;
  sistemaId: string;
}) {
  const { meta, guardar, aviso } = useMetaInicio(
    usuarioId,
    sistemaId,
    metrica.clave,
    metrica.moneda,
  );
  const [editando, setEditando] = useState(false);
  const [texto, setTexto] = useState('');
  const [error, setError] = useState('');
  const id = useId();
  const devolverFoco = useRef(false);
  const formato = (n: number) =>
    new Intl.NumberFormat(
      'es-AR',
      metrica.moneda
        ? { style: 'currency', currency: 'ARS', maximumFractionDigits: 2 }
        : { maximumFractionDigits: 0 },
    ).format(n);
  const alcanzada =
    meta !== null &&
    metrica.valor !== null &&
    (metrica.direccion === 'minimo' ? metrica.valor >= meta : metrica.valor <= meta);
  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    const valor = validarMeta(texto, metrica.moneda);
    if (valor === null) {
      setError(
        metrica.moneda
          ? 'Ingresá un importe válido, igual o mayor a cero.'
          : 'Ingresá un número entero, igual o mayor a cero.',
      );
      return;
    }
    guardar(valor);
    devolverFoco.current = true;
    setEditando(false);
    setError('');
  };
  return (
    <article
      className={`inicio-metrica ${alcanzada ? 'inicio-metrica-lograda' : ''}`}
      aria-labelledby={`${id}-titulo`}
    >
      <div className="inicio-metrica-cabecera">
        <Icon name={metrica.icono} />
        <h3 id={`${id}-titulo`}>{metrica.titulo}</h3>
      </div>
      <p className="inicio-metrica-valor">
        {metrica.valor === null ? 'Sin datos completos' : formato(metrica.valor)}
      </p>
      <p className="inicio-metrica-detalle">{metrica.detalle}</p>
      <div className="inicio-metrica-meta">
        <p>
          <strong>Objetivo:</strong> {metrica.objetivo}.
        </p>
        {meta !== null ? (
          <>
            <p className="inicio-meta-estado">
              {alcanzada ? '✓ Meta alcanzada' : 'Meta en curso'} ·{' '}
              {metrica.direccion === 'maximo' ? 'Máximo' : 'Mínimo'} {formato(meta)}
            </p>
            {metrica.valor !== null && metrica.direccion === 'minimo' ? (
              <progress
                aria-label={`Progreso de ${metrica.titulo}`}
                max={Math.max(meta, 1)}
                value={alcanzada ? Math.max(meta, 1) : Math.min(metrica.valor, meta)}
              />
            ) : null}
            {!alcanzada && metrica.valor !== null ? (
              <p>
                {metrica.direccion === 'maximo' ? 'Reducí' : 'Faltan'}{' '}
                {formato(Math.abs(meta - metrica.valor))} para alcanzar tu meta.
              </p>
            ) : null}
          </>
        ) : (
          <p className="inicio-meta-sin">Definí una meta para ver cómo vas.</p>
        )}
        {editando ? (
          <form onSubmit={enviar} noValidate>
            <label htmlFor={`${id}-meta`}>
              {metrica.direccion === 'maximo' ? 'Máximo' : 'Mínimo'} para{' '}
              {metrica.titulo.toLowerCase()}
            </label>
            <input
              id={`${id}-meta`}
              type="number"
              inputMode={metrica.moneda ? 'decimal' : 'numeric'}
              min="0"
              step={metrica.moneda ? '0.01' : '1'}
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              aria-invalid={!!error}
              aria-describedby={error ? `${id}-error` : undefined}
              autoFocus
            />
            {error ? (
              <p id={`${id}-error`} role="alert">
                {error}
              </p>
            ) : null}
            <div className="inicio-meta-acciones">
              <button className="btn primary small" type="submit">
                Guardar meta
              </button>
              <button
                className="btn ghost small"
                type="button"
                onClick={() => {
                  devolverFoco.current = true;
                  setEditando(false);
                }}
              >
                Cancelar
              </button>
            </div>
          </form>
        ) : (
          <div className="inicio-meta-acciones">
            <button
              type="button"
              className="text-link"
              ref={(nodo) => {
                if (nodo && devolverFoco.current) {
                  devolverFoco.current = false;
                  nodo.focus();
                }
              }}
              onClick={() => {
                setTexto(meta === null ? '' : String(meta));
                setError('');
                setEditando(true);
              }}
            >
              {meta === null ? 'Definir meta' : 'Cambiar meta'}
            </button>
            {meta !== null ? (
              <button type="button" className="text-link" onClick={() => guardar(null)}>
                Quitar meta
              </button>
            ) : null}
          </div>
        )}
        {aviso ? <p role="status">{aviso}</p> : null}
      </div>
    </article>
  );
}
