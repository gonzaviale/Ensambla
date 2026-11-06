import { useMemo, useState } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { DiceSami, MensajeDeWhatsapp, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { TOPE_DE_OFERTAS_POR_SEMANA } from '@/lib/gestion/datosDeEjemplo';
import { pesos, porcentaje } from '@/lib/gestion/formato';
import {
  conDescuento,
  DIAS_DE_SOBRA,
  mensajeDeOferta,
  ofertasPosibles,
  type OfertaPosible,
} from '@/lib/gestion/ofertas';
import { pedidosDeOferta } from '@/lib/gestion/elDia';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useNegocioStore } from '@/stores/negocioStore';
import { useSistemaStore } from '@/stores/sistemaStore';

// Ofertas por WhatsApp: qué ofrecer, hasta qué descuento se sigue ganando y a
// quién. Se manda sólo con el OK del comerciante y se mide qué pasó.
export function OfertasPage() {
  return (
    <MarcoDeNegocio
      pantalla="ofertas"
      titulo={
        <>
          Lo que no se mueve, ofrecelo.
          <br />A quien lo compra y sin perder plata.
        </>
      }
      bajada="Sami elige qué ofrecer, el descuento que tu margen aguanta y a qué clientes mandárselo. Después te cuenta cómo salió."
    >
      {(negocio) => <Ofertas negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

function Ofertas({ negocio }: { negocio: Negocio }) {
  const enviadas = useNegocioStore((estado) => estado.ofertas);
  // Las enviadas quedan como salieron, aunque después baje el stock con los pedidos que trajeron.
  const ofertas = useMemo(() => {
    const posibles = ofertasPosibles(negocio.datos, negocio.hoy).map(
      (oferta) => enviadas[oferta.producto.id]?.oferta ?? oferta,
    );
    const yaListadas = new Set(posibles.map((oferta) => oferta.producto.id));
    const otras = Object.values(enviadas)
      .map((enviada) => enviada.oferta)
      .filter((oferta) => !yaListadas.has(oferta.producto.id));
    return [...posibles, ...otras];
  }, [negocio.datos, negocio.hoy, enviadas]);
  const sinMandar = ofertas.filter((oferta) => !enviadas[oferta.producto.id]);
  const pendientes = sinMandar.length;
  const parada = sinMandar.reduce((total, oferta) => total + oferta.plataParada, 0);

  if (ofertas.length === 0)
    return (
      <DiceSami pose="wink">
        <p>Todo lo que tenés se está moviendo bien. Por ahora no hace falta ofertar nada.</p>
      </DiceSami>
    );

  return (
    <>
      <DiceSami lado="derecha">
        {pendientes === 0 ? (
          <p>
            Ya mandaste todas las ofertas. Las respuestas entran como pedidos: confirmalos en
            Pedidos y acá te cuento cuánto se vendió.
          </p>
        ) : (
          <p>
            Tenés <strong>{pesos(parada)}</strong> parados en{' '}
            {pendientes === 1 ? 'este producto' : `estos ${pendientes} productos`}. Te propongo una
            oferta para cada uno: <strong>todas te siguen dejando ganancia</strong>, y sólo les
            llegan a quienes compran eso o algo parecido.
          </p>
        )}
      </DiceSami>

      <div className="gn-ofertas">
        {ofertas.map((oferta) => (
          <Oferta key={oferta.producto.id} base={oferta} negocio={negocio.sistema.name} />
        ))}
      </div>

      <Seccion
        titulo="Para no molestar"
        bajada="Las reglas que respeta Sami antes de mandar cualquier oferta."
      >
        <ul className="gn-reglas">
          <li>
            <Icon name="check" /> Sólo a clientes que aceptaron recibir ofertas la primera vez que
            Sami se lo preguntó.
          </li>
          <li>
            <Icon name="check" /> Como mucho {TOPE_DE_OFERTAS_POR_SEMANA} oferta por semana a cada
            cliente.
          </li>
          <li>
            <Icon name="check" /> Cada mensaje dice cómo darse de baja: responder BAJA.
          </li>
          <li>
            <Icon name="bulb" /> Con el puente actual (Evolution API, no oficial) mandar a muchos
            números puede hacer que WhatsApp bloquee el número. En producción se usaría la API
            oficial, donde cada mensaje de oferta tiene costo.
          </li>
        </ul>
      </Seccion>
    </>
  );
}

function Oferta({ base, negocio }: { base: OfertaPosible; negocio: string }) {
  const [descuento, setDescuento] = useState(base.descuentoSugerido);
  const [verMensaje, setVerMensaje] = useState(false);
  const enviadaAntes = useNegocioStore((estado) => estado.ofertas[base.producto.id]);
  const confirmados = useNegocioStore((estado) => estado.confirmados);
  const enviarOferta = useNegocioStore((estado) => estado.enviarOferta);
  const avisar = useSistemaStore((estado) => estado.avisar);
  const navegacion = useNavegacion();
  const enviada = Boolean(enviadaAntes);
  const oferta = enviadaAntes?.oferta ?? conDescuento(base, descuento);
  const pierde = oferta.dejaPorUnidad <= 0;
  const maximoEnPasos = Math.max(1, Math.floor(base.descuentoMaximo * 20) + 1);
  const primero = oferta.destinatarios[0]?.nombre ?? 'cliente';

  // Lo que ya pasó: las respuestas que llegaron y, de esas, las que se confirmaron como venta.
  const respuestas = enviadaAntes?.pedidos ?? [];
  const vendidos = respuestas
    .map((mensaje) => confirmados[mensaje.id])
    .filter((pedido) => pedido !== undefined);
  const unidades = vendidos
    .flatMap((pedido) => pedido.lineas)
    .reduce((total, linea) => total + linea.entrega, 0);
  const vendido = vendidos.reduce((total, pedido) => total + pedido.total, 0);

  const enviar = () => {
    const pedidos = pedidosDeOferta(
      oferta,
      new Date().toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' }),
    );
    enviarOferta({ oferta, pedidos });
    setVerMensaje(false);
    avisar(
      `Mandamos la oferta de ${oferta.producto.nombre} a ${oferta.destinatarios.length} clientes. ${pedidos.length} ya respondieron: están en Pedidos.`,
    );
  };

  return (
    <article className="gn-oferta" data-enviada={enviada || undefined}>
      <header>
        <div>
          <h3>{oferta.producto.nombre}</h3>
          <span className="tag" data-riesgo={oferta.motivo === 'parado' ? 'alto' : 'medio'}>
            {oferta.motivo === 'parado'
              ? oferta.diasSinVenta === null
                ? 'Nunca se vendió'
                : `Sin ventas hace ${oferta.diasSinVenta} días`
              : `Te dura más de ${Math.round(DIAS_DE_SOBRA / 30)} meses`}
          </span>
        </div>
        <span className="gn-oferta-parada">
          <small>Parado</small>
          <strong>{pesos(oferta.plataParada)}</strong>
          <small>{oferta.producto.stock} unidades</small>
        </span>
      </header>

      <div className="gn-oferta-precio">
        <span>
          <s>{pesos(oferta.producto.precio)}</s> <strong>{pesos(oferta.precioOferta)}</strong>
        </span>
        <span data-tono={pierde ? 'mal' : 'bien'}>
          {pierde
            ? `Perdés ${pesos(-oferta.dejaPorUnidad)} por unidad`
            : `Te deja ${pesos(oferta.dejaPorUnidad)} por unidad`}
        </span>
      </div>

      {!enviada && (
        <label className="gn-oferta-descuento">
          <span>
            Descuento: <strong>{porcentaje(descuento)}</strong>
            <small className="gn-gris">
              Hasta {porcentaje(oferta.descuentoMaximo)} seguís ganando
            </small>
          </span>
          <input
            type="range"
            min={1}
            max={maximoEnPasos}
            step={1}
            value={Math.round(descuento * 20)}
            onChange={(evento) => setDescuento(Number(evento.target.value) / 20)}
          />
        </label>
      )}

      <p className="gn-oferta-a-quien">
        <Icon name="users" /> A <strong>{oferta.destinatarios.length} clientes</strong>:{' '}
        {oferta.destinatarios.map((cliente) => cliente.nombre).join(', ')}.
        {oferta.sinPermiso > 0 && (
          <small className="gn-gris">
            {oferta.sinPermiso === 1
              ? '1 cliente que lo compra pidió no recibir ofertas.'
              : `${oferta.sinPermiso} clientes que lo compran pidieron no recibir ofertas.`}
          </small>
        )}
      </p>

      {enviada ? (
        <div className="gn-oferta-resultado" role="status">
          <span className="gn-enviado">
            <Icon name="check-circle" /> Enviada. Así viene:
          </span>
          <ul>
            <li>
              <strong>{respuestas.length}</strong>{' '}
              {respuestas.length === 1 ? 'respondió' : 'respondieron'} «LA QUIERO» y{' '}
              {respuestas.length === 1 ? 'entró como pedido' : 'entraron como pedidos'}
              {vendidos.length < respuestas.length &&
                `: ${respuestas.length - vendidos.length} sin confirmar`}
              .
            </li>
            <li>
              {vendidos.length === 0 ? (
                'Cuando confirmes los pedidos, acá vas a ver cuánto vendiste y cuánta plata parada volvió.'
              ) : (
                <>
                  Vendiste <strong>{unidades}</strong> por {pesos(vendido)}: volvieron{' '}
                  {pesos(unidades * oferta.producto.costo)} que estaban parados y ganaste{' '}
                  {pesos(unidades * oferta.dejaPorUnidad)}.
                </>
              )}
            </li>
          </ul>
          <Btn cls="ghost small" onClick={() => void navegacion.irAPedidos()}>
            Ver los pedidos
          </Btn>
        </div>
      ) : (
        <>
          {verMensaje && (
            <MensajeDeWhatsapp
              para={`${primero} y ${Math.max(0, oferta.destinatarios.length - 1)} más`}
              texto={mensajeDeOferta(oferta, primero, negocio)}
            />
          )}
          <div className="gn-orden-acciones">
            <Btn
              cls="ghost small"
              aria-expanded={verMensaje}
              onClick={() => setVerMensaje(!verMensaje)}
            >
              <Icon name="eye" /> {verMensaje ? 'Ocultar el mensaje' : 'Ver el mensaje'}
            </Btn>
            <Btn
              cls="primary small"
              disabled={pierde || oferta.destinatarios.length === 0}
              onClick={enviar}
            >
              Mandar a {oferta.destinatarios.length}
            </Btn>
          </div>
        </>
      )}
    </article>
  );
}
