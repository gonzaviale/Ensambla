import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { AjusteUbicacion } from '@/components/sistema/ubicacion/AjusteUbicacion';
import { BuscadorDireccion } from '@/components/sistema/ubicacion/BuscadorDireccion';
import { RevisionUbicacion } from '@/components/sistema/ubicacion/RevisionUbicacion';
import { SelectorRubro } from '@/components/sistema/ubicacion/SelectorRubro';
import {
  DatosDelComercio,
  LocalidadDelComercio,
} from '@/components/sistema/ubicacion/DatosDelComercio';
import { useConfiguracionUbicacion } from '@/hooks/ubicacion/useConfiguracionUbicacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';

const PASOS = ['Tu comercio', 'Ubicación', 'Rubro'] as const;

// La configuración del comercio de a un paso por vez, con el camino arriba para saber por dónde vas.
// Se puede ir y volver entre pasos: lo cargado queda en el estado y se guarda todo junto al final.
export function UbicacionPage() {
  useTituloPagina('Ubicación y rubro');
  const estado = useConfiguracionUbicacion();
  const [paso, setPaso] = useState(0);
  const ultimo = paso === PASOS.length - 1;
  // Un paso está completo cuando tiene lo mínimo para guardar; no se avanza con uno incompleto.
  const completo = [
    !!estado.nombre.trim(),
    !!estado.punto && !!estado.localidad.trim() && !estado.localidadBloquea,
    !!estado.rubro,
  ];
  const alcanzable = (indice: number) => completo.slice(0, indice).every(Boolean);

  return (
    <>
      <CabeceraComun active="zone" />
      <main id="main" tabIndex={-1} className="page-wrap ubicacion-page reveal">
        <Link to="/zona" className="text-link">
          ← Volver a Mi zona
        </Link>
        <div className="page-title">
          <h1>Ubicación y rubro</h1>
          <p>Así comparamos tu comercio con los de tu zona.</p>
        </div>
        {estado.busquedaSimulada && (
          <p className="ubicacion-aviso">
            Modo de prueba: direcciones de ejemplo de San Justo y guardado simulado.
          </p>
        )}
        <nav className="ubicacion-camino" aria-label="Pasos de la configuración">
          <ol>
            {PASOS.map((nombre, indice) => (
              <li
                key={nombre}
                className={indice === paso ? 'actual' : completo[indice] ? 'hecho' : ''}
              >
                <button
                  type="button"
                  aria-current={indice === paso ? 'step' : undefined}
                  disabled={!alcanzable(indice)}
                  onClick={() => setPaso(indice)}
                >
                  <span className="ubicacion-camino-punto" aria-hidden="true">
                    {indice !== paso && completo[indice] ? <Icon name="check" /> : indice + 1}
                  </span>
                  {nombre}
                </button>
              </li>
            ))}
          </ol>
        </nav>
        {estado.cargandoConfiguracion && (
          <p role="status">Cargando la configuración del comercio…</p>
        )}
        {estado.errorConfiguracion && (
          <div role="alert">
            <p>{estado.errorConfiguracion}</p>
            <button className="btn secondary" onClick={estado.reintentarConfiguracion}>
              Reintentar configuración
            </button>
          </div>
        )}
        <fieldset
          className="ubicacion-formulario"
          disabled={estado.cargandoConfiguracion || !!estado.errorConfiguracion || estado.guardando}
        >
          <legend className="sr-only">
            Configuración del comercio: {PASOS[paso]}, paso {paso + 1} de {PASOS.length}
          </legend>
          {paso === 0 && (
            <DatosDelComercio
              nombre={estado.nombre}
              comparteDatosZona={estado.comparteDatosZona}
              zona={estado.zona}
              ubicacionGuardada={estado.ubicacionGuardada}
              onNombre={estado.cambiarNombre}
              onCompartir={estado.cambiarCompartir}
            />
          )}
          {paso === 1 && (
            <div className="ubicacion-grid">
              <BuscadorDireccion
                calle={estado.calle}
                altura={estado.altura}
                onCalle={estado.cambiarCalle}
                onAltura={estado.cambiarAltura}
                localidad={
                  <LocalidadDelComercio
                    localidad={estado.localidad}
                    localidadDelMapa={estado.localidadDelMapa}
                    errorLocalidad={estado.errorLocalidad}
                    verificandoLocalidad={estado.verificandoLocalidad}
                    onLocalidad={estado.cambiarLocalidad}
                    onUsarLocalidadDelMapa={estado.usarLocalidadDelMapa}
                    onReintentarLocalidad={estado.reintentarLocalidad}
                  />
                }
                onBuscar={estado.buscar}
                buscando={estado.buscando}
                resultados={estado.resultados}
                elegida={estado.direccion}
                onElegir={estado.elegirDireccion}
                error={estado.errorBusqueda}
              />
              <AjusteUbicacion punto={estado.punto} onMover={estado.moverPunto} />
            </div>
          )}
          {paso === 2 && (
            <SelectorRubro
              rubros={estado.rubros}
              valor={estado.rubro}
              onCambiar={estado.cambiarRubro}
              cargando={estado.cargandoRubros}
              error={estado.errorRubros}
              onReintentar={estado.reintentarRubros}
            />
          )}
          <div className="ubicacion-acciones">
            {paso > 0 && (
              <button type="button" className="btn secondary" onClick={() => setPaso(paso - 1)}>
                <Icon name="back" /> Atrás
              </button>
            )}
            {!ultimo && (
              <button
                type="button"
                className="btn primary"
                disabled={!completo[paso]}
                aria-describedby={completo[paso] ? undefined : 'falta-en-paso'}
                onClick={() => setPaso(paso + 1)}
              >
                Siguiente <Icon name="arrow" />
              </button>
            )}
            {ultimo && (
              <>
                <button
                  type="button"
                  className="btn secondary"
                  disabled={estado.buscando || estado.cargandoRubros || !!estado.errorRubros}
                  onClick={estado.revisar}
                >
                  Revisar configuración
                </button>
                <button
                  type="button"
                  className="btn primary"
                  disabled={
                    estado.buscando ||
                    estado.cargandoRubros ||
                    !!estado.errorRubros ||
                    estado.localidadBloquea
                  }
                  onClick={() => void estado.guardar()}
                >
                  {estado.guardando ? 'Guardando…' : 'Guardar configuración'}
                </button>
              </>
            )}
          </div>
          {!ultimo && !completo[paso] && (
            <p id="falta-en-paso" className="ubicacion-falta">
              {paso === 0
                ? 'Escribí el nombre del comercio para seguir.'
                : 'Buscá y elegí tu dirección, y revisá que la localidad coincida con el mapa.'}
            </p>
          )}
          <p className="form-error" role="alert">
            {estado.errorRevision}
          </p>
          <p className="form-error" role="alert">
            {estado.errorGuardado}
          </p>
        </fieldset>
        {estado.guardado && (
          <p className="ubicacion-revision" role="status">
            La configuración de tu comercio se guardó correctamente.
          </p>
        )}
        {estado.revision && <RevisionUbicacion configuracion={estado.revision} />}
      </main>
      <PageFooter />
    </>
  );
}
