import { useState, type ComponentType } from 'react';
import type { Bloque, Sistema, TipoBloque } from '@/lib/sistema/tipos';
import { CONTENIDOS_SIN_TABLA } from '../contenidoSinTabla';
import type { ContenidoDeBloqueProps } from '../tiposDeContenido';
import { BarraDelBloque } from './BarraDelBloque';
import { FormularioEnLinea } from './FormularioEnLinea';
import { RegistrosDelBloque } from './RegistrosDelBloque';

type ContenidoDelLienzoProps = ContenidoDeBloqueProps & { onEscanear?: () => void };

function ContenidoDeFormulario({ bloque, sistema }: { bloque: Bloque; sistema: Sistema }) {
  return (
    <>
      <FormularioEnLinea bloque={bloque} sistema={sistema} />
      <RegistrosDelBloque bloque={bloque} sistema={sistema} consulta="" />
    </>
  );
}

function ContenidoDeTabla({ bloque, sistema, onEscanear }: ContenidoDelLienzoProps) {
  const [consulta, setConsulta] = useState('');
  return (
    <>
      <BarraDelBloque
        bloque={bloque}
        consulta={consulta}
        onConsulta={setConsulta}
        onEscanear={onEscanear}
      />
      <RegistrosDelBloque bloque={bloque} sistema={sistema} consulta={consulta} />
    </>
  );
}

// Lo que dibuja cada tipo de bloque en el lienzo; los que no tienen contenido propio son una tabla.
const CONTENIDOS_DEL_LIENZO: Partial<Record<TipoBloque, ComponentType<ContenidoDelLienzoProps>>> = {
  ...CONTENIDOS_SIN_TABLA,
  form: ContenidoDeFormulario,
};

export function ContenidoDelBloque(props: ContenidoDelLienzoProps) {
  const Contenido = CONTENIDOS_DEL_LIENZO[props.bloque.type] ?? ContenidoDeTabla;
  return <Contenido {...props} />;
}
