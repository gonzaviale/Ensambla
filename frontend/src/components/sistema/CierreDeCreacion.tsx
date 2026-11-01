import { allBlocks } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { Btn } from './Btn';
import { Icon } from './Icon';
import { IconBtn } from './IconBtn';

const MAXIMO_DE_NOMBRES = 4;

const SIN_REGISTROS = ['summary', 'note'];

/** «Productos, Clientes y Pedidos»; con muchas tablas, las primeras y cuántas más hay. */
function listaDeTablas(nombres: string[]) {
  if (nombres.length <= MAXIMO_DE_NOMBRES) {
    return new Intl.ListFormat('es', { type: 'conjunction' }).format(nombres);
  }
  const resto = nombres.length - (MAXIMO_DE_NOMBRES - 1);
  return `${nombres.slice(0, MAXIMO_DE_NOMBRES - 1).join(', ')} y ${resto} más`;
}

type Props = {
  sistema: Sistema;
  onRegistrar: (bloqueId: string) => void;
  onPersonalizar: () => void;
  onAjustarConSami: () => void;
  onCerrar: () => void;
};

// El cierre al terminar de armar un sistema: qué quedó creado y qué se puede hacer ahora.
export function CierreDeCreacion({
  sistema,
  onRegistrar,
  onPersonalizar,
  onAjustarConSami,
  onCerrar,
}: Props) {
  const bloques = allBlocks(sistema);
  const paraRegistrar = bloques.find(
    (bloque) => bloque.fields.length > 0 && !SIN_REGISTROS.includes(bloque.type),
  );

  return (
    <section className="system-ready" aria-labelledby="sistema-listo">
      <div className="system-ready-head">
        <Icon name="check-circle" />
        <div>
          <strong id="sistema-listo">¡Listo! «{sistema.name}» ya está armado.</strong>
          <p>
            Quedaron {bloques.length} {bloques.length === 1 ? 'tabla' : 'tablas'}:{' '}
            {listaDeTablas(bloques.map((bloque) => bloque.name))}. Ya podés empezar a usarlo.
          </p>
        </div>
        <IconBtn name="close" label="Cerrar aviso" onClick={onCerrar} />
      </div>
      <div className="system-ready-actions">
        {paraRegistrar && (
          <Btn cls="primary small" onClick={() => onRegistrar(paraRegistrar.id)}>
            <Icon name="plus" /> Registrar mi primer dato
          </Btn>
        )}
        <Btn cls="secondary small" onClick={onPersonalizar}>
          <Icon name="edit" /> Personalizar las tablas
        </Btn>
        <Btn cls="ghost small" onClick={onAjustarConSami}>
          <Icon name="spark" /> Pedirle un ajuste a Sami
        </Btn>
      </div>
    </section>
  );
}
