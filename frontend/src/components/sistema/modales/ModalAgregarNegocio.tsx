import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { useCrearNegocio } from '@/hooks/negocios/mutations';
import { useNegociosDeLaCuenta } from '@/hooks/negocios/queries';
import { useSistemaStore } from '@/stores/sistemaStore';
import { NOMBRE_NEGOCIO_MAX } from '@/types/negocios';
import { Icon } from '../Icon';
import { Modal } from '../Modal';

// Un negocio nuevo es un cuaderno vacío con ese nombre: se arma después, desde su Inicio. El rubro y la ubicación son
// del perfil de la cuenta, no de cada negocio, así que acá no se piden.
export function ModalAgregarNegocio() {
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const { data: cuenta } = useNegociosDeLaCuenta();
  const crear = useCrearNegocio();
  const [nombre, setNombre] = useState('');

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault();
    if (!nombre.trim()) return;
    await crear.mutateAsync(nombre.trim());
    cerrar();
  };

  if (!cuenta.puedeCrear) {
    return (
      <Modal clase="modal-negocio">
        <div className="icon-tile large">
          <Icon name="wallet" />
        </div>
        <h2 id="modal-title">Tu plan ya alcanzó el límite</h2>
        <p>
          Tenés {cuenta.negocios.length} de {cuenta.limite}{' '}
          {cuenta.limite === 1 ? 'negocio disponible' : 'negocios disponibles'}.
        </p>
        <div className="negocio-plan-aviso">
          Con Premium podés organizar más negocios desde la misma cuenta.
        </div>
        <div className="modal-footer">
          <button type="button" className="btn ghost" onClick={cerrar}>
            Ahora no
          </button>
          <Link to="/planes" className="btn primary" onClick={cerrar}>
            Ver planes
          </Link>
        </div>
      </Modal>
    );
  }

  return (
    <Modal clase="modal-negocio">
      <div className="icon-tile large">
        <Icon name="plus" />
      </div>
      <h2 id="modal-title">Agregá otro negocio</h2>
      <p>Arranca vacío y con sus propias tablas. Lo armás después, desde su Inicio.</p>
      <form onSubmit={(evento) => void guardar(evento)} noValidate>
        <fieldset disabled={crear.isPending}>
          <div className="field">
            <label htmlFor="nuevo-negocio-nombre">Nombre del negocio</label>
            <input
              id="nuevo-negocio-nombre"
              className="input"
              maxLength={NOMBRE_NEGOCIO_MAX}
              required
              autoFocus
              value={nombre}
              onChange={(evento) => setNombre(evento.target.value)}
            />
          </div>
        </fieldset>
        {cuenta.limite !== null && (
          <p className="negocio-cupo-nota">
            Usás {cuenta.negocios.length} de {cuenta.limite} negocios de tu plan.
          </p>
        )}
        <div className="modal-footer">
          <button type="button" className="btn ghost" disabled={crear.isPending} onClick={cerrar}>
            Cancelar
          </button>
          <button
            type="submit"
            className="btn primary"
            disabled={crear.isPending || !nombre.trim()}
          >
            {crear.isPending ? 'Creando…' : 'Crear y entrar al negocio'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
