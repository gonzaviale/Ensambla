import type { ReactNode } from 'react';
import { AjusteUbicacion } from '@/components/sistema/ubicacion/AjusteUbicacion';
import { BuscadorDireccion } from '@/components/sistema/ubicacion/BuscadorDireccion';
import {
  DatosDelComercio,
  LocalidadDelComercio,
} from '@/components/sistema/ubicacion/DatosDelComercio';
import { SelectorRubro } from '@/components/sistema/ubicacion/SelectorRubro';
import type { EstadoDeUbicacion } from '@/hooks/ubicacion/useConfiguracionUbicacion';
import type { SeccionUbicacion } from '@/types/ubicacion';

type Props = { estado: EstadoDeUbicacion; pie: ReactNode };

function SolapaDelComercio({ estado, pie }: Props) {
  return (
    <DatosDelComercio
      nombre={estado.nombre}
      comparteDatosZona={estado.comparteDatosZona}
      zona={estado.zona}
      ubicacionGuardada={estado.ubicacionGuardada}
      onNombre={estado.cambiarNombre}
      onCompartir={estado.cambiarCompartir}
      pie={pie}
    />
  );
}

function LocalidadDeLaDireccion({ estado }: { estado: EstadoDeUbicacion }) {
  return (
    <LocalidadDelComercio
      localidad={estado.localidad}
      localidadDelMapa={estado.localidadDelMapa}
      errorLocalidad={estado.errorLocalidad}
      verificandoLocalidad={estado.verificandoLocalidad}
      onUsarLocalidadDelMapa={estado.usarLocalidadDelMapa}
      onReintentarLocalidad={estado.reintentarLocalidad}
    />
  );
}

function SolapaDeLaUbicacion({ estado, pie }: Props) {
  return (
    <div className="ubicacion-grid">
      <BuscadorDireccion
        consulta={estado.consulta}
        onConsulta={estado.cambiarConsulta}
        localidad={<LocalidadDeLaDireccion estado={estado} />}
        onBuscar={estado.buscar}
        buscando={estado.buscando}
        resultados={estado.resultados}
        elegida={estado.direccion}
        onElegir={estado.elegirDireccion}
        error={estado.errorBusqueda}
        pie={pie}
      />
      <AjusteUbicacion punto={estado.punto} onMover={estado.moverPunto} />
    </div>
  );
}

function SolapaDelRubro({ estado, pie }: Props) {
  return (
    <SelectorRubro
      rubros={estado.rubros}
      valor={estado.rubro}
      onCambiar={estado.cambiarRubro}
      cargando={estado.cargandoRubros}
      error={estado.errorRubros}
      onReintentar={estado.reintentarRubros}
      pie={pie}
    />
  );
}

// Sólo se monta la solapa visible: el mapa no se dibuja bien estando oculto.
const CONTENIDO: Record<SeccionUbicacion, (props: Props) => ReactNode> = {
  comercio: SolapaDelComercio,
  ubicacion: SolapaDeLaUbicacion,
  rubro: SolapaDelRubro,
};

export function ContenidoDeSolapa({ activa, ...props }: Props & { activa: SeccionUbicacion }) {
  const Contenido = CONTENIDO[activa];
  return <Contenido {...props} />;
}
