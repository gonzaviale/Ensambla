import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearRegistro } from '@/api/registros';
import { obtenerProyecto } from '@/api/proyectos';
import type { PiezaId, RolCampo, RubroId } from '@/types/negocio';
import type { ValoresRegistro } from '@/types/registro';
import { registrosKeys } from '../registros/queries';
import { proyectosKeys } from '../proyectos/queries';
import { ubicarPiezas } from './useMiNegocio';

// «Cargar datos de ejemplo»: unas filas de muestra, según el rubro, en las tablas recién armadas.
// Cada texto lleva «(ejemplo)» para que la persona los reconozca y los pueda borrar.

type Fila = Partial<Record<RolCampo, string | number | boolean>>;

const EJ = ' (ejemplo)';

type Contexto = { fecha: (diasAtras: number) => string; rubro: RubroId };

/** Fecha de este mes (nunca antes del día 1) en formato AAAA-MM-DD. */
export function fechaDelMes(diasAtras: number, hoy: Date = new Date()): string {
  const dia = Math.max(1, hoy.getDate() - diasAtras);
  const mes = String(hoy.getMonth() + 1).padStart(2, '0');
  return `${hoy.getFullYear()}-${mes}-${String(dia).padStart(2, '0')}`;
}

const PRODUCTOS: Record<RubroId, Array<[string, number, number, number]>> = {
  comercio: [
    ['Yerba 1 kg', 2800, 3600, 12],
    ['Aceite 900 ml', 2100, 2700, 8],
    ['Fideos 500 g', 900, 1250, 20],
  ],
  gastronomia: [
    ['Vianda de milanesa', 3200, 5500, 0],
    ['Tarta de verdura', 2600, 4800, 0],
    ['Porción de torta', 900, 1800, 0],
  ],
  otro: [
    ['Producto A', 1500, 2500, 10],
    ['Producto B', 2000, 3200, 6],
    ['Producto C', 800, 1400, 15],
  ],
};

export function filasDeEjemplo(id: PiezaId, { fecha, rubro }: Contexto): Fila[] {
  const productos = PRODUCTOS[rubro];
  switch (id) {
    case 'productos':
      return productos.map(([nombre, costo, precio, stock]) => ({
        nombre: nombre + EJ,
        costo,
        precio,
        stock,
      }));
    case 'ventas':
      return productos.map(([nombre, , precio], i) => ({
        fecha: fecha(i),
        producto: nombre + EJ,
        cantidad: i + 1,
        monto: precio * (i + 1),
      }));
    case 'gastos':
      return [
        { fecha: fecha(4), concepto: 'Mercadería' + EJ, monto: 18000 },
        { fecha: fecha(2), concepto: 'Luz' + EJ, monto: 9500 },
      ];
    case 'fiados':
      return [
        { fecha: fecha(3), cliente: 'Marta' + EJ, monto: 6200, pagado: false },
        { fecha: fecha(1), cliente: 'Carlos' + EJ, monto: 3400, pagado: false },
      ];
    case 'pedidos': {
      const [plato = 'Plato', , precio = 0] = productos[0] ?? [];
      return [
        {
          fecha: fecha(0),
          cliente: 'Laura' + EJ,
          producto: plato + EJ,
          cantidad: 4,
          monto: precio * 4,
          sena: 5000,
          entregado: false,
        },
      ];
    }
    case 'insumos':
      return [
        { nombre: 'Harina' + EJ, precio: 1100, unidad: 'kg' },
        { nombre: 'Aceite' + EJ, precio: 2400, unidad: 'litro' },
      ];
  }
}

/** Crea filas de muestra en las tablas del sistema; devuelve cuántas cargó. */
export function useDatosDeEjemplo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ sistemaId, rubro }: { sistemaId: string; rubro: RubroId }) => {
      const sistema = await obtenerProyecto(sistemaId);
      const contexto: Contexto = { rubro, fecha: (dias) => fechaDelMes(dias) };
      const bloques: string[] = [];
      let cargadas = 0;
      for (const [id, presente] of ubicarPiezas(sistema)) {
        for (const fila of filasDeEjemplo(id, contexto)) {
          const valores: ValoresRegistro = {};
          for (const [rol, valor] of Object.entries(fila) as [
            RolCampo,
            string | number | boolean,
          ][]) {
            const campo = presente.campo(rol);
            if (campo) valores[campo.id] = valor;
          }
          await crearRegistro(presente.bloque.id, valores);
          cargadas += 1;
        }
        bloques.push(presente.bloque.id);
      }
      return { cargadas, bloques };
    },
    onSuccess: ({ bloques }, { sistemaId }) =>
      Promise.all([
        ...bloques.map((id) =>
          queryClient.invalidateQueries({ queryKey: registrosKeys.bloque(id) }),
        ),
        queryClient.invalidateQueries({ queryKey: proyectosKeys.detalle(sistemaId), exact: true }),
      ]),
  });
}
