import { useMemo, useState } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { Cifra, DiceSami, MensajeDeWhatsapp, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { PEDIDOS_DE_HOY } from '@/lib/gestion/datosDeEjemplo';
import { pesos } from '@/lib/gestion/formato';
import {
  armarPedido,
  respuestaAlCliente,
  type EstadoDeLinea,
  type PedidoArmado,
} from '@/lib/gestion/pedidos';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
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
  const pedidos = useMemo(
    () => PEDIDOS_DE_HOY.map((mensaje) => armarPedido(mensaje, negocio.datos)),
    [negocio.datos],
  );
  const [confirmados, setConfirmados] = useState<string[]>([]);
  const incompletos = pedidos.filter((pedido) => pedido.incompleto).length;
  const total = pedidos.reduce((suma, pedido) => suma + pedido.total, 0);
  const aFiado = pedidos
    .filter((pedido) => pedido.mensaje.pago === 'fiado')
    .reduce((suma, pedido) => suma + pedido.total, 0);
  const noTenes = pedidos.flatMap((pedido) =>
    pedido.lineas.filter((linea) => linea.estado === 'no-trabajas'),
  );

  return (
    <>
      <DiceSami>
        <p>
          Hoy llegaron <strong>{pedidos.length} pedidos</strong> por WhatsApp. Los armé contra tu
          stock
          {incompletos > 0 && (
            <>
              : <strong>{incompletos}</strong> tienen algo que falta y ya te dejé la explicación
              para el cliente
            </>
          )}
          .
        </p>
      </DiceSami>

      <div className="gn-cifras">
        <Cifra
          etiqueta="Pedidos de hoy"
          valor={String(pedidos.length)}
          detalle={`${confirmados.length} confirmados`}
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
              confirmado={confirmados.includes(pedido.mensaje.id)}
              onConfirmar={() => setConfirmados((lista) => [...lista, pedido.mensaje.id])}
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
        <p className="gn-enviado">
          <Icon name="check-circle" /> Respondido. Descontado del stock y anotado en el cierre de
          hoy.
        </p>
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
