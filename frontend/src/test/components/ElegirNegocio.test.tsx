import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ElegirNegocio, type EstadoDeLaEleccion } from '@/components/negocios/ElegirNegocio';
import type { NegocioParaElegir } from '@/lib/negocios/presentacion';

const ALMACEN: NegocioParaElegir = {
  id: 'almacen',
  nombre: 'Almacén Don Pepe',
  rubro: 'ALMACEN',
  avisosPendientes: 3,
  estado: 'BIEN',
};

const KIOSCO: NegocioParaElegir = {
  id: 'kiosco',
  nombre: 'Kiosco de la esquina',
  rubro: 'KIOSCO',
  avisosPendientes: 1,
  estado: 'ATENCION',
};

const FERIA: NegocioParaElegir = {
  id: 'feria',
  nombre: 'Puesto de la feria',
  rubro: 'OTRO',
  avisosPendientes: 0,
  estado: 'SIN_DATOS',
};

const listo = (activoId: string | null = null): EstadoDeLaEleccion => ({
  tipo: 'listo',
  negocios: [ALMACEN, KIOSCO, FERIA],
  activoId,
});

const montar = (
  estado: EstadoDeLaEleccion,
  nombreDelRubro?: (codigo: string) => string | undefined,
) => {
  const onElegir = vi.fn();
  render(<ElegirNegocio estado={estado} onElegir={onElegir} nombreDelRubro={nombreDelRubro} />);
  return { user: userEvent.setup(), onElegir };
};

const opcion = (nombre: string) => screen.getByRole('button', { name: nombre });

describe('ElegirNegocio', () => {
  it('ofrece cada negocio por su nombre, en el orden del servidor', () => {
    montar(listo());

    expect(screen.getAllByRole('button').map((boton) => boton.textContent)).toEqual([
      expect.stringContaining('Almacén Don Pepe'),
      expect.stringContaining('Kiosco de la esquina'),
      expect.stringContaining('Puesto de la feria'),
    ]);
  });

  it('dice de cada negocio su rubro, cómo viene y sus avisos, sin montos', () => {
    montar(listo());

    const kiosco = opcion('Kiosco de la esquina');
    expect(kiosco).toHaveTextContent('Atención');
    expect(kiosco).toHaveAccessibleDescription('Kiosco · 1 aviso pendiente');
    expect(kiosco).not.toHaveTextContent('$');
    expect(opcion('Almacén Don Pepe')).toHaveTextContent('Viene bien');
    expect(opcion('Puesto de la feria')).toHaveAccessibleDescription(
      'Otro · Sin avisos pendientes',
    );
  });

  it('marca los avisos pendientes y el estado para que se vean', () => {
    montar(listo());

    const kiosco = opcion('Kiosco de la esquina');
    expect(kiosco).toHaveAttribute('data-estado', 'ATENCION');
    expect(within(kiosco).getByText('1 aviso pendiente')).toHaveAttribute('data-hay', 'true');
    expect(
      within(opcion('Puesto de la feria')).getByText('Sin avisos pendientes'),
    ).not.toHaveAttribute('data-hay');
  });

  it('señala el último negocio que se usó', () => {
    montar(listo('kiosco'));

    expect(opcion('Kiosco de la esquina')).toHaveAccessibleDescription(
      'Kiosco · 1 aviso pendiente · El último que usaste',
    );
    expect(opcion('Almacén Don Pepe')).not.toHaveTextContent('El último que usaste');
  });

  it('usa el nombre del rubro que da el servidor y, si no lo tiene, el código legible', () => {
    montar(listo(), (codigo) => (codigo === 'ALMACEN' ? 'Almacén de barrio' : undefined));

    expect(opcion('Almacén Don Pepe')).toHaveAccessibleDescription(
      'Almacén de barrio · 3 avisos pendientes',
    );
    expect(opcion('Kiosco de la esquina')).toHaveAccessibleDescription(
      'Kiosco · 1 aviso pendiente',
    );
  });

  it('tocar un negocio avisa cuál se eligió', async () => {
    const { user, onElegir } = montar(listo());

    await user.click(opcion('Kiosco de la esquina'));

    expect(onElegir).toHaveBeenCalledWith('kiosco');
  });

  it('mientras carga lo anuncia y no ofrece negocios', () => {
    montar({ tipo: 'cargando' });

    expect(screen.getByRole('status')).toHaveTextContent('Buscando tus negocios…');
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('si falla deja probar de nuevo', async () => {
    const reintentar = vi.fn();
    const { user } = montar({ tipo: 'error', reintentar });

    expect(screen.getByRole('status')).toHaveTextContent('No pudimos traer tus negocios ahora.');
    await user.click(screen.getByRole('button', { name: 'Probar de nuevo' }));

    expect(reintentar).toHaveBeenCalledOnce();
  });
});
