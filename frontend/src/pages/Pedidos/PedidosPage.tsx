import { useMemo, useState } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { Cifra, DiceSami, MensajeDeWhatsapp, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { pesos } from '@/lib/gestion/formato';
import {
  armarPedido,
  respuestaAlCliente,
  type EstadoDeLinea,
  type PedidoArmado,
} from '@/lib/gestion/pedidos';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useNegocioStore } from '@/stores/negocioStore';
import { useSistemaStore } from '@/stores/sistemaStore';

const ESTADOS: Record<EstadoDeLinea, string> = {
  hay: 'Hay',
  poco: 'Falta una parte',
  agotado: 'Agotado',
  'no-trabajas': 'No lo tenés',
};

// Pedidos por WhatsApp: los mensajes de los clientes, armados por Sami contra
// el stock real. El comerciante revisa, confirma y responde; la venta entra al cierre del día.
export function PedidosPage() {
  return (
    <MarcoDeNegocio
      pantalla="pedidos"
      titulo={
        <>
          Tus clientes piden por WhatsApp.
          <br />
          Sami arma el pedido con tu stock.
        </>
      }
      bajada="Entiende «la yerba de siempre», avisa lo que no hay y te deja la respuesta lista. Nada sale sin tu OK."
    >
      {(negocio) => <Pedidos negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

function Pedidos({ negocio }: { negocio: Negocio }) {
  const confirmados = useNegocioStore((estado) => estado.confirmados);
  const confirmarPedido = useNegocioStore((estado) => estado.confirmarPedido);
  // Un pedido confirmado queda como se confirmó; los demás se arman con el stock de ahora, que ya descuenta lo
  // confirmado.
  const pedidos = useMemo(
    () =>
      negocio.pedidos.map(
        (mensaje) => confirmados[mensaje.id] ?? armarPedido(mensaje, negocio.datos),
      ),
    [negocio.pedidos, negocio.datos, confirmados],
  );
  const cuantosConfirmados = pedidos.filter((pedido) => confirmados[pedido.mensaje.id]).length;
  const deOfertas = pedidos.filter((pedido) => pedido.mensaje.id.startsWith('oferta-')).length;
  const incompletos = pedidos.filter(
    (pedido) => pedido.incompleto && !confirmados[pedido.mensaje.id],
  ).length;
  const total = pedidos.reduce((suma, pedido) => suma + pedido.total, 0);
  const aFiado = pedidos
    .filter((pedido) => pedido.mensaje.pago === 'fiado')
    .reduce((suma, pedido) => suma + pedido.total, 0);
  const noTenes = pedidos.flatMap((pedido) =>
    pedido.lineas.filter((linea) => linea.estado === 'no-trabajas'),
  );

  return (
    <>
      <DiceSami lado="derecha">
        <p>
          Hoy llegaron <strong>{pedidos.length} pedidos</strong> por WhatsApp
          {deOfertas > 0 && <> ({deOfertas} respondiendo a tus ofertas)</>}. Los armé contra tu
          stock
          {incompletos > 0 && (
            <>
              : <strong>{incompletos}</strong>{' '}
              {incompletos === 1 ? 'tiene algo que falta' : 'tienen algo que falta'} y ya te dejé la
              explicación para el cliente
            </>
          )}
          .
        </p>
      </DiceSami>

      <div className="gn-cifras">
        <Cifra
          etiqueta="Pedidos de hoy"
          valor={String(pedidos.length)}
          detalle={`${cuantosConfirmados} confirmados`}
        />
        <Cifra etiqueta="Suman" valor={pesos(total)} detalle={`${pesos(aFiado)} a fiado`} />
        <Cifra
          etiqueta="Te pidieron y no tenés"
          valor={String(noTenes.length)}
          detalle={noTenes.map((linea) => linea.linea.busca).join(', ') || 'Nada'}
          tono={noTenes.length ? 'ojo' : 'neutro'}
        />
      </div>

      <Seccion
        titulo="La bandeja"
        bajada="Del más viejo al más nuevo. Confirmás y Sami le responde al cliente."
      >
        <div className="gn-pedidos">
          {pedidos.map((pedido) => (
            <Pedido
              key={pedido.mensaje.id}
              pedido={pedido}
              negocio={negocio.sistema.name}
              confirmado={Boolean(confirmados[pedido.mensaje.id])}
              onConfirmar={() => confirmarPedido(pedido)}
            />
          ))}
        </div>
      </Seccion>
    </>
  );
}

function Pedido({
  pedido,
  negocio,
  confirmado,
  onConfirmar,
}: {
  pedido: PedidoArmado;
  negocio: string;
  confirmado: boolean;
  onConfirmar: () => void;
}) {
  const [verRespuesta, setVerRespuesta] = useState(false);
  const avisar = useSistemaStore((estado) => estado.avisar);
  const navegacion = useNavegacion();
  const { mensaje } = pedido;

  const confirmar = () => {
    onConfirmar();
    setVerRespuesta(false);
    avisar(
      `Le respondimos a ${mensaje.cliente}. La venta entra al cierre del día ${mensaje.pago === 'fiado' ? 'como fiado' : 'como cobrada'}.`,
    );
  };

  return (
    <article className="gn-pedido" data-confirmado={confirmado || undefined}>
      <header>
        <div>
          <h3>{mensaje.cliente}</h3>
          <span className="gn-etiquetas">
            {mensaje.id.startsWith('oferta-') && <span className="tag">Por tu oferta</span>}
            <span className="tag outline">{mensaje.entrega === 'envio' ? 'Envío' : 'Retira'}</span>
            <span className="tag" data-riesgo={mensaje.pago === 'fiado' ? 'medio' : 'bajo'}>
              {mensaje.pago === 'fiado' ? 'A fiado' : 'Cobrado'}
            </span>
          </span>
        </div>
        <strong>{pesos(pedido.total)}</strong>
      </header>

      <MensajeDeWhatsapp de={mensaje.cliente} hora={mensaje.hora} texto={mensaje.texto} />

      <ul className="gn-lineas">
        {pedido.lineas.map((linea) => (
          <li key={linea.linea.texto} data-estado={linea.estado}>
            <span className="gn-lineas-que">
              <strong>{linea.producto?.nombre ?? linea.linea.texto}</strong>
              {linea.aviso && <small>{linea.aviso}</small>}
            </span>
            <span className="tag" data-linea={linea.estado}>
              {ESTADOS[linea.estado]}
            </span>
            <span className="gn-lineas-cuanto">
              {linea.entrega}/{linea.linea.cantidad}
              {linea.subtotal > 0 && <small>{pesos(linea.subtotal)}</small>}
            </span>
          </li>
        ))}
      </ul>

      {confirmado ? (
        <div className="gn-orden-acciones">
          <span className="gn-enviado">
            <Icon name="check-circle" /> Respondido. Descontado del stock y anotado en el cierre de
            hoy.
          </span>
          <Btn cls="ghost small" onClick={() => void navegacion.irANegocio()}>
            Ver el cierre
          </Btn>
          {pedido.total > 0 && (
            <Btn cls="ghost small" onClick={() => void navegacion.irAMonotributo()}>
              Facturar
            </Btn>
          )}
        </div>
      ) : (
        <>
          {verRespuesta && (
            <MensajeDeWhatsapp para={mensaje.cliente} texto={respuestaAlCliente(pedido, negocio)} />
          )}
          <div className="gn-orden-acciones">
            <Btn
              cls="ghost small"
              aria-expanded={verRespuesta}
              onClick={() => setVerRespuesta(!verRespuesta)}
            >
              <Icon name="eye" /> {verRespuesta ? 'Ocultar la respuesta' : 'Ver la respuesta'}
            </Btn>
            {pedido.lineas.some((linea) => linea.estado === 'agotado') && (
              <Btn cls="ghost small" onClick={() => void navegacion.irACompras()}>
                Ver la compra
              </Btn>
            )}
            <Btn cls="primary small" onClick={confirmar}>
              <Icon name="check" /> Confirmar y responder
            </Btn>
          </div>
        </>
      )}
    </article>
  );
}
