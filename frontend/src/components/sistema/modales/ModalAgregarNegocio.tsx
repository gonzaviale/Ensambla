import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { leerErrorServidor } from '@/hooks/errores';
import { useCrearNegocio } from '@/hooks/negocios/mutations';
import { useNegociosDeLaCuenta } from '@/hooks/negocios/queries';
import { useRubros } from '@/hooks/negocio/queries';
import { useUbicacionParaNegocio } from '@/hooks/ubicacion/useUbicacionParaNegocio';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { Direccion } from '@/types/ubicacion';
import { Icon } from '../Icon';
import { Modal } from '../Modal';

export function ModalAgregarNegocio() {
  const cerrar = useSistemaStore((estado) => estado.cerrarModal);
  const { data: cuenta } = useNegociosDeLaCuenta();
  const rubros = useRubros();
  const crear = useCrearNegocio();
  const ubicacionParaNegocio = useUbicacionParaNegocio();
  const [nombre, setNombre] = useState('');
  const [rubro, setRubro] = useState('OTRO');
  const [conUbicacion, setConUbicacion] = useState(false);
  const [consulta, setConsulta] = useState('');
  const [resultados, setResultados] = useState<Direccion[] | null>(null);
  const [elegida, setElegida] = useState<Direccion | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [preparando, setPreparando] = useState(false);
  const [errorUbicacion, setErrorUbicacion] = useState('');
  const error = leerErrorServidor(crear.error);
  const ocupado = crear.isPending || preparando;

  const buscar = async () => {
    const limpia = consulta.trim();
    if (!limpia) {
      setErrorUbicacion('Escribí una dirección para buscarla.');
      return;
    }
    setBuscando(true);
    setErrorUbicacion('');
    try {
      const resultadosEncontrados = await ubicacionParaNegocio.buscar(limpia);
      setResultados(resultadosEncontrados);
      setElegida(null);
      if (resultadosEncontrados.length === 0) setErrorUbicacion('No encontramos esa dirección.');
    } catch (causa) {
      setErrorUbicacion(leerErrorServidor(causa)?.mensaje ?? 'No pudimos buscar la dirección.');
    } finally {
      setBuscando(false);
    }
  };

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault();
    setErrorUbicacion('');
    if (!nombre.trim()) return;
    setPreparando(true);
    try {
      let ubicacion;
      if (conUbicacion) {
        if (!elegida) {
          setErrorUbicacion('Buscá y elegí una dirección, o continuá sin ubicación.');
          return;
        }
        const localidad = await ubicacionParaNegocio.completarLocalidad(elegida);
        if (!localidad) {
          setErrorUbicacion('No pudimos reconocer la localidad de esa dirección.');
          return;
        }
        ubicacion = {
          direccion: elegida.direccion,
          localidad,
          latitud: elegida.latitud,
          longitud: elegida.longitud,
        };
      }
      await crear.mutateAsync({ nombre: nombre.trim(), rubro, ubicacion });
      cerrar();
    } catch {
      // La mutación conserva el error para mostrarlo junto al formulario.
    } finally {
      setPreparando(false);
    }
  };

  if (!cuenta.puedeCrear) {
    return (
      <Modal clase="modal-negocio">
        <div className="icon-tile large"><Icon name="wallet" /></div>
        <h2 id="modal-title">Tu plan ya alcanzó el límite</h2>
        <p>
          Tenés {cuenta.negocios.length} de {cuenta.limite}{' '}
          {cuenta.limite === 1 ? 'negocio disponible' : 'negocios disponibles'}.
        </p>
        <div className="negocio-plan-aviso">
          Con Premium podés organizar hasta 6 negocios desde la misma cuenta.
        </div>
        <div className="modal-footer">
          <button type="button" className="btn ghost" onClick={cerrar}>Ahora no</button>
          <Link to="/planes" className="btn primary" onClick={cerrar}>Ver planes</Link>
        </div>
      </Modal>
    );
  }

  return (
    <Modal clase="modal-negocio">
      <div className="icon-tile large"><Icon name="plus" /></div>
      <h2 id="modal-title">Agregá otro negocio</h2>
      <p>Va a tener sus propias secciones, avisos, objetivos, zona y conversaciones con Sami.</p>
      <form onSubmit={(evento) => void guardar(evento)} noValidate>
        <fieldset disabled={ocupado}>
          <div className="field">
            <label htmlFor="nuevo-negocio-nombre">Nombre del negocio</label>
            <input id="nuevo-negocio-nombre" className="input" maxLength={120} required autoFocus
              value={nombre} onChange={(evento) => setNombre(evento.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="nuevo-negocio-rubro">Rubro</label>
            <select id="nuevo-negocio-rubro" value={rubro}
              onChange={(evento) => setRubro(evento.target.value)} disabled={rubros.isPending}>
              {(rubros.data?.rubros ?? []).map((item) =>
                <option key={item.codigo} value={item.codigo}>{item.nombre}</option>)}
            </select>
          </div>
          <label className="check-row negocio-ubicacion-opcional">
            <input type="checkbox" checked={conUbicacion}
              onChange={(evento) => setConUbicacion(evento.target.checked)} />
            <span><strong>Agregar la ubicación ahora</strong><small>Es opcional. También podés cargarla después desde Zona.</small></span>
          </label>
          {conUbicacion && (
            <div className="negocio-buscador-ubicacion">
              <div className="field">
                <label htmlFor="nuevo-negocio-direccion">Dirección y localidad</label>
                <div className="negocio-busqueda-fila">
                  <input id="nuevo-negocio-direccion" className="input" value={consulta}
                    placeholder="Ej.: Florencio Varela 1903, San Justo"
                    onChange={(evento) => setConsulta(evento.target.value)} />
                  <button type="button" className="btn secondary" disabled={buscando}
                    onClick={() => void buscar()}>{buscando ? 'Buscando…' : 'Buscar'}</button>
                </div>
              </div>
              {!!resultados?.length && (
                <fieldset className="negocio-direcciones">
                  <legend>Elegí la dirección correcta</legend>
                  {resultados.map((direccion, indice) => (
                    <label key={`${direccion.latitud}-${direccion.longitud}-${indice}`}>
                      <input type="radio" name="direccion-nuevo-negocio"
                        checked={elegida === direccion} onChange={() => setElegida(direccion)} />
                      <span>{direccion.direccion}</span>
                    </label>
                  ))}
                </fieldset>
              )}
            </div>
          )}
        </fieldset>
        {errorUbicacion && <p className="form-error" role="alert">{errorUbicacion}</p>}
        {error && <p className="form-error" role="alert">{error.mensaje}</p>}
        <p className="negocio-cupo-nota">
          <strong>Plan Premium</strong> · Usás {cuenta.negocios.length} de {cuenta.limite} negocios.
        </p>
        <div className="modal-footer">
          <button type="button" className="btn ghost" disabled={ocupado} onClick={cerrar}>Cancelar</button>
          <button type="submit" className="btn primary" disabled={ocupado || !nombre.trim()}>
            {ocupado ? 'Creando…' : 'Crear y entrar al negocio'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
