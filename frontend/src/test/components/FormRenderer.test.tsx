import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FormRenderer } from '@/components/FormRenderer/FormRenderer';
import { campoPorTipo, camposFormulario } from '@/test/formularioTestData';

describe('ENS-52: formulario dinámico', () => {
  it('renderiza los cinco tipos con controles accesibles y valores iniciales', () => {
    render(<FormRenderer campos={camposFormulario} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText('Nombre')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Nombre')).toHaveValue('Producto');
    expect(screen.getByLabelText('Precio')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Precio')).toHaveAttribute('inputmode', 'decimal');
    expect(screen.getByLabelText('Precio')).toHaveValue('0');
    expect(screen.getByLabelText('Entrega')).toHaveAttribute('type', 'date');
    expect(screen.getByLabelText('Entrega')).toHaveValue('2026-10-10');
    expect(screen.getByRole('combobox', { name: 'Rubro' })).toHaveValue('Insumos');
    expect(screen.getByRole('radio', { name: 'No' })).toBeChecked();
  });

  it('respeta la posición de los campos sin modificar la definición recibida', () => {
    const campos = [...camposFormulario].reverse();
    render(<FormRenderer campos={campos} onSubmit={vi.fn()} />);

    const nombres = [...document.querySelectorAll('[name]')].map((el) => el.getAttribute('name'));
    expect([...new Set(nombres)]).toEqual(camposFormulario.map((campo) => campo.id));
    expect(campos[0]?.tipo).toBe('SI_NO');
  });

  it('envía valores por id de campo con números y booleanos reales', async () => {
    const guardar = vi.fn(async () => ({ exito: true as const }));
    const user = userEvent.setup();
    render(<FormRenderer campos={camposFormulario} onSubmit={guardar} />);

    await user.clear(screen.getByLabelText('Precio'));
    await user.type(screen.getByLabelText('Precio'), '12.5');
    await user.selectOptions(screen.getByLabelText('Rubro'), 'Terminados');
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));

    expect(guardar).toHaveBeenCalledWith({
      [campoPorTipo('TEXTO').id]: 'Producto',
      [campoPorTipo('NUMERO').id]: 12.5,
      [campoPorTipo('FECHA').id]: '2026-10-10',
      [campoPorTipo('OPCION').id]: 'Terminados',
      [campoPorTipo('SI_NO').id]: false,
    });
  });

  it('deja vacíos los campos sin valor inicial y omite los opcionales vacíos', async () => {
    const guardar = vi.fn(async () => ({ exito: true as const }));
    const campo = { ...campoPorTipo('TEXTO'), obligatorio: false, valorInicial: null };
    render(<FormRenderer campos={[campo]} onSubmit={guardar} />);

    expect(screen.getByLabelText('Nombre')).toHaveValue('');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar fila' }));

    expect(guardar).toHaveBeenCalledWith({});
  });

  it('explica un bloque sin campos y no permite guardar un formulario vacío', () => {
    render(<FormRenderer campos={[]} onSubmit={vi.fn()} />);

    expect(screen.getByText(/todavía no tiene columnas/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar fila' })).not.toBeInTheDocument();
  });

  it('muestra el error junto al campo, lo enfoca y conserva el resto de los valores', async () => {
    const guardar = vi.fn(async () => ({ exito: true as const }));
    const user = userEvent.setup();
    render(<FormRenderer campos={camposFormulario} onSubmit={guardar} />);
    const nombre = screen.getByLabelText('Nombre');
    await user.clear(nombre);
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));

    expect(guardar).not.toHaveBeenCalled();
    expect(nombre).toHaveFocus();
    expect(nombre).toHaveAttribute('aria-invalid', 'true');
    expect(nombre).toHaveAccessibleDescription('Este dato es obligatorio');
    expect(screen.getByLabelText('Precio')).toHaveValue('0');
    expect(screen.getByRole('radio', { name: 'No' })).toBeChecked();
    await user.type(nombre, 'Corregido');
    expect(nombre).not.toHaveAttribute('aria-invalid');
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));
    expect(guardar).toHaveBeenCalledOnce();
  });

  it('exige responder sí o no sin confundir No con un campo vacío', async () => {
    const campo = { ...campoPorTipo('SI_NO'), valorInicial: null };
    const guardar = vi.fn(async () => ({ exito: true as const }));
    const user = userEvent.setup();
    render(<FormRenderer campos={[campo]} onSubmit={guardar} />);
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));
    expect(guardar).not.toHaveBeenCalled();
    expect(screen.getByRole('group', { name: /Entregado/ })).toHaveAccessibleDescription(
      'Este dato es obligatorio',
    );
    await user.click(screen.getByRole('radio', { name: 'No' }));
    await user.click(screen.getByRole('button', { name: 'Guardar fila' }));
    expect(guardar).toHaveBeenCalledWith({ [campo.id]: false });
  });
});
