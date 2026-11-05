import { useMemo } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { useNegocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { faltantes } from '@/lib/gestion/compras';
import { pesos } from '@/lib/gestion/formato';

// «Tu negocio hoy» en el inicio: la ganancia de verdad, cuándo se cubren los gastos fijos, el fiado y lo que se
// está por acabar, con la entrada a cada pantalla de «Mi negocio». Sin un cuaderno con ventas no se muestra.
export function ResumenDeNegocio() {
  const negocio = useNegocio();
  const navegacion = useNavegacion();
  const urgentes = useMemo(
    () =>
      negocio
        ? faltantes(negocio.datos, negocio.hoy).filter((faltante) => faltante.urgencia !== 'pronto')
            .length
        : 0,
    [negocio],
  );
  if (!negocio) return null;
  const { resultado, fiado } = negocio;
  const enLaCalle = fiado.reduce((total, cuenta) => total + cuenta.debe, 0);

  return (
    <section className="gn-inicio" aria-labelledby="gn-inicio-titulo">
      <header>
        <div>
          <span className="eyebrow">TU NEGOCIO · ÚLTIMOS 30 DÍAS</span>
          <h2 id="gn-inicio-titulo">
            Ganaste <strong>{pesos(resultado.ganancia)}</strong> de verdad.
          </h2>
          <p>
            Vendiste {pesos(resultado.ventas)}. Después de la mercadería, las comisiones, las
            pérdidas y los gastos fijos, eso es lo que te quedó.
          </p>
        </div>
      </header>
      <div className="gn-inicio-datos">
        <div>
          <span>Cubrís los gastos fijos</span>
          <strong>
            {resultado.diaDeEquilibrio === null
              ? 'Todavía no'
              : `el día ${resultado.diaDeEquilibrio}`}
          </strong>
        </div>
        <div>
          <span>Fiado en la calle</span>
          <strong>{pesos(enLaCalle)}</strong>
        </div>
        <div>
          <span>Para pedir ya</span>
          <strong>{urgentes === 1 ? '1 producto' : `${urgentes} productos`}</strong>
        </div>
      </div>
      <div className="gn-inicio-acciones">
        <button type="button" data-color="orange" onClick={() => void navegacion.irANegocio()}>
          <Icon name="camera" />
          <span>
            <strong>Cerrar el día</strong>
            <small>Con una foto de la hoja</small>
          </span>
        </button>
        <button type="button" data-color="petrol" onClick={() => void navegacion.irACompras()}>
          <Icon name="box" />
          <span>
            <strong>Compras</strong>
            <small>El pedido ya armado</small>
          </span>
        </button>
        <button type="button" data-color="coral" onClick={() => void navegacion.irAProyeccion()}>
          <Icon name="chart" />
          <span>
            <strong>¿Me conviene?</strong>
            <small>Proyectá una decisión</small>
          </span>
        </button>
      </div>
    </section>
  );
}
