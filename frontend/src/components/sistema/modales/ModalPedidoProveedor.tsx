import { useMemo, useState } from 'react';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { FilaDelPedido } from '../pedido/FilaDelPedido';
import { SelectorDeProveedor } from '../pedido/SelectorDeProveedor';
import { useArmarPedidoProveedor } from '@/hooks/reposicion/mutations';
import { useReposicion } from '@/hooks/reposicion/queries';
import { guardarArchivo } from '@/lib/descargas';
import { compartir } from '@/lib/sistema/compartir';
import {
  decisionDe,
  decisionesIniciales,
  errorDelPedido,
  lineasDelPedido,
  nombreDelGrupo,
  textoDelPedido,
  type DecisionDelProducto,
  type DecisionesDelPedido,
} from '@/lib/sistema/pedidoProveedor';
import { recordarTelefono, telefonoDe } from '@/lib/sistema/telefonosDeProveedores';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';
import type { GrupoDeReposicion } from '@/types/reposicion';

const AYUDA_SIN_PROVEEDOR =
  'Estos productos no tienen proveedor. Agregá la columna «Proveedor» a tu inventario y se agrupan solos.';
const MENSAJE_ERROR_DE_ENVIO = 'No pudimos abrir WhatsApp. Bajá el PDF y mandáselo vos.';

// El pedido a un proveedor: lo que falta, agrupado por proveedor, con las cantidades para ajustar. Se baja en PDF
// o se manda por WhatsApp.
export function ModalPedidoProveedor() {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const reposicion = useReposicion(sistema?.id ?? '');

  return (
    <Modal clase="modal-pedido">
      <h2 id="modal-title">Pedido a tu proveedor</h2>
      <p>Esto es lo que te falta. Ajustá las cantidades y mandáselo.</p>
      {reposicion.isPending ? <p role="status">Buscando lo que te falta…</p> : null}
      {reposicion.isError ? (
        <>
          <p role="alert" className="field-error">
            No pudimos ver tu stock. Probá nuevamente.
          </p>
          <div className="modal-footer">
            <Btn cls="secondary" onClick={cerrar}>
              Cerrar
            </Btn>
            <Btn cls="primary" onClick={() => void reposicion.refetch()}>
              Reintentar
            </Btn>
          </div>
        </>
      ) : null}
      {reposicion.data ? <PedidoPorProveedor sistemaId={sistema?.id ?? ''} grupos={reposicion.data.grupos} /> : null}
    </Modal>
  );
}

function PedidoPorProveedor({ sistemaId, grupos }: { sistemaId: string; grupos: GrupoDeReposicion[] }) {
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const armado = useArmarPedidoProveedor();
  const iniciales = useMemo(() => decisionesIniciales(grupos), [grupos]);
  const [cambios, setCambios] = useState<DecisionesDelPedido>({});
  const [posicion, setPosicion] = useState(0);
  const [telefonos, setTelefonos] = useState<Record<string, string>>({});
  const [intento, setIntento] = useState(false);
  const [errorDeEnvio, setErrorDeEnvio] = useState<string | null>(null);

  const grupo = grupos[posicion];
  if (!grupo) return <SinFaltantes onCerrar={cerrar} />;

  const decisiones = { ...iniciales, ...cambios };
  const proveedor = nombreDelGrupo(grupo);
  const telefono = telefonos[proveedor] ?? telefonoDe(proveedor);
  const error = errorDelPedido(grupo, decisiones);

  const cambiar = (productoId: string, cambio: Partial<DecisionDelProducto>) =>
    setCambios((actuales) => ({
      ...actuales,
      [productoId]: { ...decisionDe({ ...iniciales, ...actuales }, productoId), ...cambio },
    }));

  const pedirArchivo = async () => {
    setIntento(true);
    setErrorDeEnvio(null);
    if (error) return null;
    return armado.armar({
      sistemaId,
      pedido: { proveedor: grupo.proveedor, productos: lineasDelPedido(grupo, decisiones) },
    });
  };

  const descargar = async () => {
    const archivo = await pedirArchivo();
    if (!archivo) return;
    guardarArchivo(archivo);
    cerrar();
  };

  const enviar = async () => {
    const archivo = await pedirArchivo();
    if (!archivo) return;
    recordarTelefono(proveedor, telefono);
    try {
      await compartir({
        archivo,
        texto: textoDelPedido(grupo, decisiones),
        telefono,
        titulo: 'Pedido de reposición',
      });
      cerrar();
    } catch {
      setErrorDeEnvio(MENSAJE_ERROR_DE_ENVIO);
    }
  };

  return (
    <>
      <SelectorDeProveedor
        grupos={grupos}
        elegido={posicion}
        onElegir={(elegida) => {
          setPosicion(elegida);
          setIntento(false);
        }}
      />
      {grupo.proveedor === null ? <p className="pedido-ayuda">{AYUDA_SIN_PROVEEDOR}</p> : null}
      <ul className="pedido-lista">
        {grupo.productos.map((producto) => (
          <FilaDelPedido
            key={producto.id}
            producto={producto}
            decision={decisionDe(decisiones, producto.id)}
            onCambio={(cambio) => cambiar(producto.id, cambio)}
          />
        ))}
      </ul>
      <div className="field">
        <label htmlFor="pedido-telefono">WhatsApp de {proveedor} (opcional)</label>
        <input
          className="input"
          id="pedido-telefono"
          type="tel"
          inputMode="tel"
          placeholder="Ej.: 11 5555 0101"
          value={telefono}
          onChange={(evento) => setTelefonos((actuales) => ({ ...actuales, [proveedor]: evento.target.value }))}
        />
      </div>
      {intento && error ? (
        <p role="alert" className="field-error">
          {error}
        </p>
      ) : null}
      {armado.isError ? (
        <p role="alert" className="field-error">
          {armado.mensajeError}
        </p>
      ) : null}
      {errorDeEnvio ? (
        <p role="alert" className="field-error">
          {errorDeEnvio}
        </p>
      ) : null}
      <div className="modal-footer">
        <Btn cls="secondary" onClick={cerrar}>
          Cancelar
        </Btn>
        <Btn cls="secondary" disabled={armado.isPending} onClick={() => void descargar()}>
          <Icon name="download" /> Descargar PDF
        </Btn>
        <Btn cls="primary" disabled={armado.isPending} aria-busy={armado.isPending} onClick={() => void enviar()}>
          {armado.isPending ? 'Armando el pedido…' : 'Enviar por WhatsApp'}
        </Btn>
      </div>
    </>
  );
}

function SinFaltantes({ onCerrar }: { onCerrar: () => void }) {
  return (
    <>
      <p role="status" className="pedido-vacio">
        <Icon name="check-circle" /> Por ahora no te falta nada: ningún producto está agotado ni por debajo de su
        mínimo.
      </p>
      <div className="modal-footer">
        <Btn cls="primary" onClick={onCerrar}>
          Listo
        </Btn>
      </div>
    </>
  );
}
