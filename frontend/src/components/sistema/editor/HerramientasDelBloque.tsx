import type { DragEvent } from 'react';
import type { CargaDeArrastre } from '@/hooks/sistemas/useArrastreDelEditor';
import type { Bloque } from '@/lib/sistema/tipos';
import { duplicarBloque, moverBloque, pedirQuitarBloque } from '@/stores/accionesDelEditor';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';

type HerramientasDelBloqueProps = {
  bloque: Bloque;
  primero: boolean;
  ultimo: boolean;
  onArrastrar: (evento: DragEvent<HTMLElement>, carga: CargaDeArrastre) => void;
  onFinArrastre: () => void;
};

// Mover, personalizar, duplicar y quitar un bloque, en modo Construir.
export function HerramientasDelBloque({
  bloque,
  primero,
  ultimo,
  onArrastrar,
  onFinArrastre,
}: HerramientasDelBloqueProps) {
  const seleccionar = useSistemaStore((estado) => estado.seleccionar);
  return (
    <div className="block-tools" data-tour="move" data-tour-id={bloque.id}>
          <button
            type="button"
            className="icon-btn"
            title="Arrastrar bloque"
            aria-label="Arrastrar bloque"
            draggable
            data-accion="noop"
            data-drag-kind="block"
            data-value={bloque.id}
            onDragStart={(evento) => onArrastrar(evento, { tipo: 'bloque', valor: bloque.id })}
            onDragEnd={onFinArrastre}
          >
            <Icon name="grip" />
          </button>
          <IconBtn
            name="up"
            label="Mover arriba"
            data-accion="move-up"
            disabled={primero}
            onClick={() => moverBloque(bloque.id, 'arriba')}
          />
          <IconBtn
            name="down"
            label="Mover abajo"
            data-accion="move-down"
            disabled={ultimo}
            onClick={() => moverBloque(bloque.id, 'abajo')}
          />
          <IconBtn
            name="settings"
            label="Personalizar bloque"
            data-accion="select-block"
            data-tour="customize"
            data-tour-id={bloque.id}
            onClick={() => seleccionar(bloque.id)}
          />
          <IconBtn
            name="copy"
            label="Duplicar bloque"
            data-accion="duplicate-block"
            onClick={() => duplicarBloque(bloque.id)}
          />
          <IconBtn
            name="trash"
            label="Quitar bloque"
            data-accion="delete-block"
            onClick={() => pedirQuitarBloque(bloque.id)}
          />
        </div>
  );
}
