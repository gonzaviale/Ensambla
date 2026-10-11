import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Menu } from '@/components/ui/Menu';
import { Sheet } from '@/components/ui/Sheet';

function Demo() {
  const [abierto, setAbierto] = useState(false);
  return (
    <>
      <button onClick={() => setAbierto(true)}>Abrir panel</button>
      <Sheet open={abierto} onClose={() => setAbierto(false)} title="Editar columna">
        <label>
          Nombre de la columna
          <input />
        </label>
      </Sheet>
    </>
  );
}

describe('Sheet', () => {
  it('lleva el foco al primer campo, se cierra con Esc y devuelve el foco', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    const boton = screen.getByRole('button', { name: 'Abrir panel' });

    await user.click(boton);
    expect(screen.getByRole('dialog', { name: 'Editar columna' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre de la columna')).toHaveFocus();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(boton).toHaveFocus();
  });

  it('se cierra con el botón Cerrar', async () => {
    const user = userEvent.setup();
    render(<Demo />);

    await user.click(screen.getByRole('button', { name: 'Abrir panel' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).toBeNull();
  });
});

describe('Menu', () => {
  it('abre con el botón, recorre con flechas y cierra con Esc devolviendo el foco', async () => {
    const user = userEvent.setup();
    let elegido = '';
    render(
      <Menu
        label="Más acciones de Ventas"
        items={[
          { label: 'Cambiar nombre', onSelect: () => (elegido = 'nombre') },
          { label: 'Eliminar', tone: 'danger', onSelect: () => (elegido = 'eliminar') },
        ]}
      />,
    );
    const disparador = screen.getByRole('button', { name: 'Más acciones de Ventas' });

    await user.click(disparador);
    expect(screen.getByRole('menuitem', { name: 'Cambiar nombre' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Eliminar' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).toBeNull();
    expect(disparador).toHaveFocus();

    await user.click(disparador);
    await user.click(screen.getByRole('menuitem', { name: 'Eliminar' }));
    expect(elegido).toBe('eliminar');
  });
});
