import type { ReactElement } from 'react';
import type { Modal } from '@/stores/sistemaStore';
import { ModalPruebaPremium } from '@/components/planes/ModalPruebaPremium';
import { ModalRenovacion } from '@/components/planes/ModalRenovacion';
import { ModalAgregarNegocio } from './ModalAgregarNegocio';
import { ModalAgregarSeccion } from './ModalAgregarSeccion';
import { ModalAyuda } from './ModalAyuda';
import { ModalBienvenida } from './ModalBienvenida';
import { ModalBlanco } from './ModalBlanco';
import { ModalCampo } from './ModalCampo';
import { ModalConfirmar } from './ModalConfirmar';
import { ModalConsejosDeFoto } from './ModalConsejosDeFoto';
import { ModalDetalleDeAviso } from './ModalDetalleDeAviso';
import { ModalElegirBloque } from './ModalElegirBloque';
import { ModalEliminarNegocio } from './ModalEliminarNegocio';
import { ModalEliminarSistema } from './ModalEliminarSistema';
import { ModalImportar } from './ModalImportar';
import { ModalPlantilla } from './ModalPlantilla';
import { ModalProductosDelAviso } from './ModalProductosDelAviso';
import { ModalRegistro } from './ModalRegistro';
import { ModalRenombrar } from './ModalRenombrar';
import { ModalRenombrarPieza } from './ModalRenombrarPieza';
import { ModalResumen } from './ModalResumen';
import { ModalSecciones } from './ModalSecciones';

type TipoDeModal = Modal['tipo'];
type ModalDeTipo<T extends TipoDeModal> = Extract<Modal, { tipo: T }>;
type ContenidoPorTipo = { [T in TipoDeModal]: (modal: ModalDeTipo<T>) => ReactElement };

const contenidos: ContenidoPorTipo = {
  agregarNegocio: () => <ModalAgregarNegocio />,
  eliminarNegocio: (modal) => <ModalEliminarNegocio negocioId={modal.negocioId} nombre={modal.nombre} />,
  bienvenida: () => <ModalBienvenida />,
  blanco: () => <ModalBlanco />,
  plantilla: (modal) => <ModalPlantilla clave={modal.clave} />,
  ayuda: (modal) => <ModalAyuda respuesta={modal.respuesta} />,
  pruebaPremium: () => <ModalPruebaPremium />,
  contratarPremium: () => <ModalRenovacion />,
  cancelarRenovacion: () => <ModalRenovacion cancelar />,
  renombrar: () => <ModalRenombrar />,
  resumen: () => <ModalResumen />,
  eliminarSistema: () => <ModalEliminarSistema />,
  renombrarPieza: (modal) => <ModalRenombrarPieza bloqueId={modal.bloqueId} />,
  registro: (modal) => (
    <ModalRegistro bloqueId={modal.bloqueId} filaId={modal.filaId} avisoId={modal.avisoId} />
  ),
  consejosDeFoto: (modal) => <ModalConsejosDeFoto onTomarFoto={modal.onTomarFoto} />,
  registroDelBorrador: (modal) => (
    <ModalRegistro bloqueId={modal.bloqueId} filaId={modal.filaId} enBorrador />
  ),
  campo: (modal) => <ModalCampo bloqueId={modal.bloqueId} clave={modal.clave} />,
  secciones: (modal) => <ModalSecciones enBorrador={modal.enBorrador} />,
  agregarSeccion: (modal) => <ModalAgregarSeccion enBorrador={modal.enBorrador} />,
  elegirBloque: (modal) => <ModalElegirBloque enBorrador={modal.enBorrador} />,
  importar: (modal) => (
    <ModalImportar sistemaId={modal.sistemaId} bloqueId={modal.bloqueId} contexto={modal.contexto} />
  ),
  productosDelAviso: (modal) => (
    <ModalProductosDelAviso cambios={modal.cambios} categoria={modal.categoria} titulo={modal.titulo} />
  ),
  detalleDeAviso: (modal) => <ModalDetalleDeAviso avisoId={modal.avisoId} onAccion={modal.onAccion} />,
  confirmar: (modal) => <ModalConfirmar {...modal} />,
};

/** Cada tipo de modal sabe cómo se muestra: agregar un tipo sin su contenido no compila. */
export function contenidoDeModal(modal: Modal): ReactElement {
  const contenido = contenidos[modal.tipo] as (modal: Modal) => ReactElement;
  return contenido(modal);
}
