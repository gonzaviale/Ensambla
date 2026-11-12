import type { Bloque, Campo, ErroresFila, Sistema, Valor } from '@/lib/sistema/tipos';
import type { BorradorProductoInventario } from '@/types/escaneo';
import type { RefObject } from 'react';
import { CampoProductoInventario } from './CampoProductoInventario';
import { Icon } from '@/components/sistema/Icon';

type Props = {
  borrador: BorradorProductoInventario;
  inventario: Bloque;
  sistema: Sistema;
  grupos: { titulo: string; ayuda: string; campos: Campo[] }[];
  productosVinculables: { id: string; nombre: string }[];
  formulario: RefObject<HTMLFormElement | null>;
  errores: ErroresFila;
  bloqueado: boolean;
  onCambiar: (clave: string, valor: Valor) => void;
  onElegir: (id: string) => void;
  onGuardar: () => void;
  onCancelar: () => void;
};

export function FormularioProductoInventario({
  borrador,
  inventario,
  sistema,
  grupos,
  productosVinculables,
  formulario,
  errores,
  bloqueado,
  onCambiar,
  onElegir,
  onGuardar,
  onCancelar,
}: Props) {
  return (
    <section className="escaneo-panel escaneo-formulario" aria-label="Revisión del producto">
      <div className="escaneo-panel-titulo">
        <span className="escaneo-numero-paso">2</span>
        <div>
          <h2>Revisá el producto</h2>
          <p>Completá los campos antes de confirmar.</p>
        </div>
        <span className="escaneo-tipo-producto">
          {borrador.productoId ? 'Ya registrado' : 'Nuevo producto'}
        </span>
      </div>
      <div className="escaneo-codigo-leido">
        <span>Código leído</span>
        <strong>{borrador.codigo}</strong>
        <small>Solo lectura</small>
      </div>
      <form
        ref={formulario}
        onSubmit={(evento) => {
          evento.preventDefault();
          onGuardar();
        }}
        noValidate
      >
        <fieldset className="escaneo-campos" disabled={bloqueado}>
          {productosVinculables.length > 0 && (
            <div className="escaneo-vinculo">
              <div className="escaneo-etiqueta-campo">
                <label htmlFor="escaneo-vincular">Vincular con un producto existente</label>
                <span>Desplegable</span>
              </div>
              <p>Si ya lo cargaste sin código, elegilo para evitar duplicados.</p>
              <div className="escaneo-control escaneo-control-elegir">
                <select
                  id="escaneo-vincular"
                  value={borrador.productoId ?? ''}
                  onChange={(evento) => onElegir(evento.target.value)}
                >
                  <option value="">Crear un producto nuevo</option>
                  {productosVinculables.map((producto) => (
                    <option key={producto.id} value={producto.id}>
                      {producto.nombre}
                    </option>
                  ))}
                </select>
                <Icon name="down" />
              </div>
            </div>
          )}
          {grupos.map((grupo) => (
            <section
              className={`escaneo-grupo ${grupo.campos.some((campo) => campo.key === 'quantity') ? 'escaneo-grupo-existencias' : ''}`}
              key={grupo.titulo}
              aria-label={grupo.titulo}
            >
              <h3>{grupo.titulo}</h3>
              <p>{grupo.ayuda}</p>
              <div className="escaneo-grilla-campos">
                {grupo.campos.map((campo) => (
                  <CampoProductoInventario
                    key={campo.key}
                    campo={campo}
                    valor={borrador.valores[campo.key]}
                    sistema={sistema}
                    error={errores[campo.key]}
                    onCambiar={(valor) => onCambiar(campo.key, valor)}
                  />
                ))}
              </div>
            </section>
          ))}
          <p className="escaneo-ayuda">
            La cantidad es el conteo actual de tu negocio. Escanear no suma ni resta unidades. El
            costo y el precio no vienen del código de barras.
          </p>
          <div className="escaneo-confirmacion">
            <div className="escaneo-panel-titulo">
              <span className="escaneo-numero-paso">3</span>
              <div>
                <h3>Confirmá la carga</h3>
                <p>Se guarda en {inventario.name}.</p>
              </div>
            </div>
            <button type="submit" className="btn primary">
              <Icon name="check-circle" />{' '}
              {borrador.productoId
                ? 'Guardar cambios del producto'
                : 'Agregar producto al inventario'}
            </button>
          </div>
        </fieldset>
        <button type="button" className="btn escaneo-descartar" onClick={onCancelar}>
          <Icon name="close" /> Descartar carga
        </button>
      </form>
    </section>
  );
}
