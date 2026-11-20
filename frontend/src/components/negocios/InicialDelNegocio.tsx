import { inicialDelNegocio } from '@/lib/negocios/presentacion';
import { tonoPorPosicion } from '@/lib/sistema/tonos';

type Props = { nombre: string; posicion: number; className: string };

// El círculo con la letra de un negocio, en su color sólido: el mismo en la barra, en su menú y al entrar.
export function InicialDelNegocio({ nombre, posicion, className }: Props) {
  return (
    <span
      className={`${className} solido`}
      data-tono={tonoPorPosicion(posicion)}
      aria-hidden="true"
    >
      {inicialDelNegocio(nombre)}
    </span>
  );
}
