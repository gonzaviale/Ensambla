import { useState } from 'react';
import { QrDeSami } from '@/components/pedidos/QrDeSami';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { usePedidos } from '@/hooks/pedidos/queries';
import { FILTROS, filtrarPedidos, type FiltroDePedidos } from '@/lib/pedidos';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { OrdenCard } from './OrdenCard';

// Lo que los clientes le piden a Sami por WhatsApp llega acá: el dueño lo acepta, lo prepara y avisa que está listo.
export function PedidosPage() {
  useTituloPagina('Pedidos');
  const { pedidos, cargando, fallo } = usePedidos();
  const [filtro, setFiltro] = useState<FiltroDePedidos>('todos');
  const visibles = filtrarPedidos(pedidos, filtro);

  return (
    <>
      <CabeceraComun active="orders" />
      <main id="main" tabIndex={-1} className="page-wrap pedidos-page reveal">
        <header className="pedidos-encabezado">
          <p className="pedidos-etiqueta">Pedidos</p>
          <h1>Pedidos de tus clientes</h1>
          <p>
            Lo que tus clientes le piden a Sami por WhatsApp llega acá. Aceptalo, prepará el pedido
            y Sami le avisa a tu cliente cuando ya puede pasar a buscarlo.
          </p>
        </header>

        {cargando ? (
          <p role="status" className="pedidos-aviso">
            Cargando tus pedidos…
          </p>
        ) : fallo ? (
          <p role="alert" className="pedidos-aviso">
            No pudimos cargar tus pedidos. Probá de nuevo en unos minutos.
          </p>
        ) : pedidos.length === 0 ? (
          <section className="pedidos-vacio" aria-labelledby="pedidos-vacio-titulo">
            <h2 id="pedidos-vacio-titulo">Todavía no te llegaron pedidos</h2>
            <p>
              Cuando un cliente le pida a Sami por WhatsApp y confirme su lista, el pedido aparece
              acá para que lo gestiones.
            </p>
          </section>
        ) : (
          <>
            <div className="pedidos-filtros" role="group" aria-label="Filtrar pedidos">
              {FILTROS.map(({ clave, etiqueta }) => (
                <button
                  key={clave}
                  type="button"
                  className="pedidos-filtro"
                  aria-pressed={filtro === clave}
                  onClick={() => setFiltro(clave)}
                >
                  {etiqueta} ({filtrarPedidos(pedidos, clave).length})
                </button>
              ))}
            </div>
            {visibles.length === 0 ? (
              <p className="pedidos-aviso">No hay pedidos en esta etapa.</p>
            ) : (
              <div className="pedidos-lista">
                {visibles.map((orden) => (
                  <OrdenCard key={orden.id} orden={orden} />
                ))}
              </div>
            )}
          </>
        )}

        <QrDeSami />
      </main>
    </>
  );
}
