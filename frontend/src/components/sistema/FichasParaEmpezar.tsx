import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Color } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { varsDeColor } from './colores';
import { Icon } from './Icon';

// Las tres maneras de crear un cuaderno (con Sami, con una plantilla o de cero) como fichas de color.
// Están en el inicio y en «Mis cuadernos». `guia` marca las fichas que señala la guía de primeros pasos.
export function FichasParaEmpezar({ guia = false }: { guia?: boolean }) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const navegacion = useNavegacion();
  return (
    <div className="inicio-fichas" data-tour={guia ? 'choose' : undefined}>
      <Ficha
        color="orange"
        icono="spark"
        titulo="Crear con Sami"
        texto="Contale cómo trabajás y te propone una base."
        accion="guide"
        imagen="/mockup/sami-constructora.webp"
        onClick={() => void navegacion.irATaller()}
      />
      <Ficha
        color="blue"
        icono="grid"
        titulo="Elegir una plantilla"
        texto="Almacén, depósito o pedidos, listos para usar."
        accion="templates"
        onClick={() => void navegacion.irAPlantillas()}
      />
      <Ficha
        color="yellow"
        icono="plus"
        titulo="Empezar de cero"
        texto="Armá tus columnas y tus secciones a tu manera."
        accion="blank"
        onClick={() => abrirModal({ tipo: 'blanco' })}
      />
    </div>
  );
}

type PropsFicha = {
  color: Color;
  icono: NombreDeIcono;
  titulo: string;
  texto: string;
  /** Marca que usa la guía para saber qué camino eligió la persona. */
  accion: string;
  /** Imagen que reemplaza al ícono. */
  imagen?: string;
  onClick: () => void;
};

// Una de las tres maneras de empezar: ficha de color lleno con el ícono en blanco (o una imagen de Sami).
function Ficha({ color, icono, titulo, texto, accion, imagen, onClick }: PropsFicha) {
  return (
    <button
      type="button"
      className="inicio-ficha"
      style={varsDeColor(color)}
      data-color={color}
      data-accion={accion}
      onClick={onClick}
    >
      {imagen ? (
        <img className="inicio-ficha-imagen" src={imagen} alt="" />
      ) : (
        <span className="inicio-ficha-icono">
          <Icon name={icono} />
        </span>
      )}
      <span className="inicio-ficha-texto">
        <strong>{titulo}</strong>
        <span>{texto}</span>
      </span>
      <span className="inicio-ficha-flecha">
        <Icon name="arrow" />
      </span>
    </button>
  );
}
