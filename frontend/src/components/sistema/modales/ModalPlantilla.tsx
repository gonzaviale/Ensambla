import { useMemo, useState } from 'react';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconTile } from '../IconTile';
import { Modal } from '../Modal';
import { PreviewBoard } from '../PreviewBoard';
import { varsDeColor } from '../colores';
import { TEMPLATES } from '@/lib/sistema/catalogo';
import { makeTemplate } from '@/lib/sistema/core';
import type { ClavePlantilla, OpcionesPlantilla } from '@/lib/sistema/tipos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

// Las opciones que cada plantilla deja elegir antes de empezar.
const OPCIONES: Partial<
  Record<ClavePlantilla, { clave: keyof OpcionesPlantilla; titulo: string; detalle: string }[]>
> = {
  business: [
    {
      clave: 'movements',
      titulo: 'Entradas y salidas',
      detalle: 'Agregar movimientos que actualizan el stock.',
    },
    {
      clave: 'expenses',
      titulo: 'Insumos y gastos',
      detalle: 'Sumar un registro de lo que invertís.',
    },
  ],
  shop: [
    {
      clave: 'movements',
      titulo: 'Entradas de mercadería',
      detalle: 'Registrar lo que comprás para sumar al stock.',
    },
    {
      clave: 'expenses',
      titulo: 'Gastos del local',
      detalle: 'Sumar un registro de lo que gastás.',
    },
  ],
};

export function ModalPlantilla({ clave }: { clave: ClavePlantilla }) {
  const definicion = TEMPLATES[clave];
  const { crearYAbrir } = useNavegacion();
  const [opciones, setOpciones] = useState<OpcionesPlantilla>({
    movements: false,
    expenses: false,
  });
  const vista = useMemo(() => makeTemplate(clave, opciones, true), [clave, opciones]);
  const lista = OPCIONES[clave];

  return (
    <Modal ancho>
      <div className="template-preview-header">
        <span className="icon-tile large" style={varsDeColor(definicion.color)}>
          <Icon name={definicion.icon} />
        </span>
        <div>
          <span className="eyebrow">UNA BASE DE BLOQUES CONECTADOS</span>
          <h2 id="modal-title">{definicion.name}, a tu manera.</h2>
          <p>{definicion.subtitle}</p>
        </div>
      </div>
      <div className="template-preview-options">
        {lista ? (
          lista.map((opcion) => (
            <label key={opcion.clave}>
              <input
                type="checkbox"
                checked={opciones[opcion.clave]}
                onChange={(evento) =>
                  setOpciones({ ...opciones, [opcion.clave]: evento.target.checked })
                }
              />
              <span>
                <strong>{opcion.titulo}</strong>
                <small>{opcion.detalle}</small>
              </span>
            </label>
          ))
        ) : (
          <div className="template-flow">
            <IconTile tipo="inventory" />
            Productos <Icon name="link" /> <IconTile tipo="entries" />
            Entradas <Icon name="link" /> <IconTile tipo="exits" />
            Salidas
          </div>
        )}
      </div>
      <div className="template-preview-stage">
        <PreviewBoard sistema={vista} compacto />
      </div>
      <p className="preview-example-label" style={{ marginTop: 12 }}>
        Datos de ejemplo · Todos los bloques, campos y anchos se pueden cambiar.
      </p>
      <div className="modal-footer spread">
        <Btn cls="secondary" onClick={() => useSistemaStore.getState().cerrarModal()}>
          Seguir mirando
        </Btn>
        <Btn cls="primary" onClick={() => void crearYAbrir(makeTemplate(clave, opciones, true))}>
          Empezar con este conjunto <Icon name="arrow" />
        </Btn>
      </div>
    </Modal>
  );
}
