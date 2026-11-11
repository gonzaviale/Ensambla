import { useId, useState, type FormEvent } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import { useCambiarEstadoDePedido } from '@/hooks/pedidos/mutations';
import { formatoPesos } from '@/lib/objetivos';
import {
  clienteLegible,
  ETIQUETA_DE_ESTADO,
  formatoCantidad,
  formatoFechaYHora,
  pasosPara,
  type PasoDelDueno,
} from '@/lib/pedidos';
import type { OrdenDelServidor } from '@/types/pedidos';

const MAX_MOTIVO = 300;

/** Una orden: quién pidió, qué y cuánto, y el paso siguiente que puede dar el dueño. */
export function OrdenCard({ orden }: { orden: OrdenDelServidor }) {
  const titulo = useId();
  const motivoId = useId();
  const cambiar = useCambiarEstadoDePedido(orden.id);
  const [rechazando, setRechazando] = useState(false);
  const [motivo, setMotivo] = useState('');
  const pasos = pasosPara(orden.estado);
  const error = leerErrorServidor(cambiar.error)?.mensaje;

  const dar = (paso: PasoDelDueno) => {
    if (paso.rechaza) setRechazando(true);
    else cambiar.mutate({ estado: paso.estado });
  };

  const rechazar = (evento: FormEvent) => {
    evento.preventDefault();
    cambiar.mutate({ estado: 'RECHAZADA', motivo: motivo.trim() || undefined });
  };

  return (
    <article className="pedido-card" aria-labelledby={titulo}>
      <header>
        <div>
          <h2 id={titulo}>{clienteLegible(orden.clienteTelefono)}</h2>
          <p>{formatoFechaYHora(orden.creadoEn)}</p>
        </div>
        <span className={`pedido-estado ${orden.estado.toLowerCase().replace('_', '-')}`}>
          {ETIQUETA_DE_ESTADO[orden.estado]}
        </span>
      </header>

      <ul className="pedido-items">
        {orden.items.map((item) => (
          <li key={item.nombre}>
            <span>
              {formatoCantidad(item.cantidad)} × {item.nombre}
            </span>
            <b>{formatoPesos(item.subtotal)}</b>
          </li>
        ))}
      </ul>
      <p className="pedido-total">
        <span>Total</span>
        <strong>{formatoPesos(orden.total)}</strong>
      </p>

      {orden.estado === 'RECHAZADA' && orden.motivoRechazo && (
        <p className="pedido-motivo">Motivo: {orden.motivoRechazo}</p>
      )}

      {error && (
        <p role="alert" className="pedido-error">
          {error}
        </p>
      )}

      {pasos.length > 0 &&
        (rechazando ? (
          <form className="pedido-rechazo" onSubmit={rechazar}>
            <label htmlFor={motivoId}>Motivo del rechazo (opcional)</label>
            <textarea
              id={motivoId}
              value={motivo}
              maxLength={MAX_MOTIVO}
              rows={2}
              placeholder="Por ejemplo: hoy no me queda stock"
              onChange={(evento) => setMotivo(evento.target.value)}
            />
            <div className="pedido-acciones">
              <button type="submit" className="btn secondary" disabled={cambiar.isPending}>
                {cambiar.isPending ? 'Rechazando…' : 'Confirmar rechazo'}
              </button>
              <button
                type="button"
                className="btn secondary"
                disabled={cambiar.isPending}
                onClick={() => setRechazando(false)}
              >
                Volver
              </button>
            </div>
          </form>
        ) : (
          <div className="pedido-acciones">
            {pasos.map((paso, posicion) => (
              <button
                key={paso.estado}
                type="button"
                className={posicion === 0 ? 'btn primary' : 'btn secondary'}
                disabled={cambiar.isPending}
                onClick={() => dar(paso)}
              >
                {paso.etiqueta}
              </button>
            ))}
          </div>
        ))}
    </article>
  );
}
