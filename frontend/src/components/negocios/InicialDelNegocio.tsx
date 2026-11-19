import { varsDeColor } from '@/components/sistema/colores';
import { colorDelNegocio, inicialDelNegocio } from '@/lib/negocios/presentacion';

type Props = { nombre: string; posicion: number; className: string };

// El cuadrado con la letra de un negocio, en su color de la paleta: el mismo en la barra, en su menú y al entrar.
export function InicialDelNegocio({ nombre, posicion, className }: Props) {
  const color = colorDelNegocio(posicion);
  return (
    <span className={className} style={varsDeColor(color)} data-color={color} aria-hidden="true">
      {inicialDelNegocio(nombre)}
    </span>
  );
}
