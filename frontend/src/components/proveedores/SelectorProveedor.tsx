import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';

type Props = Pick<
  ProveedoresDemo,
  | 'datos'
  | 'proveedorId'
  | 'cambiarProveedor'
  | 'nuevoProveedor'
  | 'setNuevoProveedor'
  | 'guardarProveedor'
  | 'mostrarProveedor'
  | 'setMostrarProveedor'
>;
export function SelectorProveedor(p: Props) {
  return (
    <section className="prov-selector" aria-label="Elegir proveedor">
      <div className="prov-selector-fila">
        <label className="prov-campo">
          Proveedor
          <select value={p.proveedorId} onChange={(e) => p.cambiarProveedor(e.target.value)}>
            {p.datos.proveedores.map((v) => (
              <option key={v.id} value={v.id}>
                {v.nombre}
              </option>
            ))}
          </select>
        </label>
        <Btn
          cls="secondary"
          aria-expanded={p.mostrarProveedor}
          onClick={() => p.setMostrarProveedor(!p.mostrarProveedor)}
        >
          <Icon name="plus" /> Nuevo proveedor
        </Btn>
      </div>
      {p.mostrarProveedor && (
        <form
          className="prov-form-fila"
          onSubmit={(e) => {
            e.preventDefault();
            p.guardarProveedor();
          }}
        >
          <label className="prov-campo">
            Nombre del proveedor
            <input
              required
              maxLength={100}
              value={p.nuevoProveedor.nombre}
              onChange={(e) => p.setNuevoProveedor({ ...p.nuevoProveedor, nombre: e.target.value })}
              placeholder="Ej. Distribuidora Norte"
            />
          </label>
          <label className="prov-campo">
            Teléfono de contacto (opcional)
            <input
              type="tel"
              maxLength={30}
              value={p.nuevoProveedor.telefono}
              onChange={(e) =>
                p.setNuevoProveedor({ ...p.nuevoProveedor, telefono: e.target.value })
              }
              placeholder="Ej. +54 9 11…"
            />
          </label>
          <Btn type="submit" cls="primary">
            Guardar proveedor de prueba
          </Btn>
        </form>
      )}
    </section>
  );
}
