import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { useDescargarResumen } from '@/hooks/reportes/mutations';
import { useFuncionDePlan } from '@/hooks/planes/queries';
import {
  PERIODOS_DEL_RESUMEN,
  errorDelPeriodo,
  ultimosDias,
  type ClaveDePeriodo,
} from '@/lib/sistema/periodoResumen';
import { seleccionarSistemaActivo, useSistemaStore } from '@/stores/sistemaStore';

const QUE_INCLUYE = [
  'Cuánto vendiste, ganaste y gastaste, comparado con el período anterior.',
  'Lo que más se vende y lo que está parado.',
  'El stock que falta y lo que vale tu inventario.',
  'Tus clientes, el fiado y los gastos por categoría.',
  'Tus precios frente a la zona y lo que te avisó Sami.',
];

// El resumen del negocio en PDF: se elige el período y se baja un documento listo para guardar o imprimir.
// Es una función Premium: sin ella el modal lo explica y lleva a los planes.
export function ModalResumen() {
  const sistema = useSistemaStore(seleccionarSistemaActivo);
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const { cargando, incluida } = useFuncionDePlan('REPORTES_FILTRADOS');
  const descarga = useDescargarResumen();
  const [periodo, setPeriodo] = useState<ClaveDePeriodo>('30');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [intento, setIntento] = useState(false);

  const dias = PERIODOS_DEL_RESUMEN.find((opcion) => opcion.clave === periodo)?.dias;
  const errorDelRango = dias === undefined ? errorDelPeriodo(desde, hasta) : null;

  const descargar = (evento: FormEvent) => {
    evento.preventDefault();
    setIntento(true);
    if (!sistema || errorDelRango) return;
    descarga.descargar(
      { sistemaId: sistema.id, periodo: dias === undefined ? { desde, hasta } : ultimosDias(dias) },
      { onSuccess: cerrar },
    );
  };

  return (
    <Modal clase="modal-resumen">
      <h2 id="modal-title">Resumen de tu negocio en PDF</h2>
      <p>Todo lo que te sirve para decidir, en un solo documento.</p>
      <ul className="resumen-incluye">
        {QUE_INCLUYE.map((texto) => (
          <li key={texto}>{texto}</li>
        ))}
      </ul>
      {!cargando && !incluida ? (
        <>
          <p role="status" className="resumen-premium">
            <Icon name="spark" /> El resumen en PDF es parte de Premium.
          </p>
          <div className="modal-footer">
            <Btn cls="secondary" onClick={cerrar}>
              Ahora no
            </Btn>
            <Link to="/planes" className="btn primary" onClick={cerrar}>
              Ver planes
            </Link>
          </div>
        </>
      ) : (
        <form onSubmit={descargar}>
          <div className="field">
            <label htmlFor="resumen-periodo">Período</label>
            <select
              className="input"
              id="resumen-periodo"
              value={periodo}
              onChange={(evento) => setPeriodo(evento.target.value as ClaveDePeriodo)}
            >
              {PERIODOS_DEL_RESUMEN.map((opcion) => (
                <option key={opcion.clave} value={opcion.clave}>
                  {opcion.etiqueta}
                </option>
              ))}
              <option value="otro">Elegir fechas</option>
            </select>
          </div>
          {periodo === 'otro' ? (
            <div className="resumen-fechas">
              <div className="field">
                <label htmlFor="resumen-desde">Desde</label>
                <input
                  className="input"
                  id="resumen-desde"
                  type="date"
                  value={desde}
                  onChange={(evento) => setDesde(evento.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="resumen-hasta">Hasta</label>
                <input
                  className="input"
                  id="resumen-hasta"
                  type="date"
                  value={hasta}
                  onChange={(evento) => setHasta(evento.target.value)}
                />
              </div>
            </div>
          ) : null}
          {intento && errorDelRango ? (
            <p role="alert" className="field-error">
              {errorDelRango}
            </p>
          ) : null}
          {descarga.isError ? (
            <p role="alert" className="field-error">
              {descarga.mensajeError}
            </p>
          ) : null}
          <div className="modal-footer">
            <Btn cls="secondary" onClick={cerrar}>
              Cancelar
            </Btn>
            <button
              className="btn primary"
              type="submit"
              disabled={cargando || descarga.isPending}
              aria-busy={descarga.isPending}
            >
              <Icon name="download" /> {descarga.isPending ? 'Armando el PDF…' : 'Descargar PDF'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
