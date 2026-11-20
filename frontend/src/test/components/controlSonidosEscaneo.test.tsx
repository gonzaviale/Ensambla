import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ControlSonidosEscaneo } from '@/components/escaneo/ControlSonidosEscaneo';

type Vista = ReturnType<typeof render>;

let vista: Vista;
const usuario = () => userEvent.setup();

describe('controles de sonido del lector', () => {
  it('permite silenciar los sonidos y entonces no se pueden probar', async () => {
    dadoElControlMostrado();

    await cuandoSeSilencian();

    entoncesNoSePuedenProbarLosSonidos();
  });

  it('recuerda la preferencia de silencio al volver', async () => {
    dadoElControlMostrado();
    await cuandoSeSilencian();

    cuandoSeVuelveAMostrarElControl();

    entoncesElBotonDeActivarEstaSinPresionar();
  });

  it('permite reactivar los sonidos después de silenciarlos', async () => {
    dadoElControlMostrado();
    await cuandoSeSilencian();
    cuandoSeVuelveAMostrarElControl();

    await cuandoSeReactivan();

    entoncesLosSonidosEstanActivosYSePuedenProbar();
  });
});

function dadoElControlMostrado() {
  vista = render(<ControlSonidosEscaneo />);
}

async function cuandoSeSilencian() {
  await usuario().click(screen.getByRole('button', { name: 'Silenciar sonidos del lector' }));
}

function cuandoSeVuelveAMostrarElControl() {
  vista.unmount();
  vista = render(<ControlSonidosEscaneo />);
}

async function cuandoSeReactivan() {
  await usuario().click(screen.getByRole('button', { name: 'Activar sonidos del lector' }));
}

function entoncesNoSePuedenProbarLosSonidos() {
  expect(screen.getByRole('button', { name: 'Probar sonidos' })).toBeDisabled();
}

function entoncesElBotonDeActivarEstaSinPresionar() {
  const activar = screen.getByRole('button', { name: 'Activar sonidos del lector' });
  expect(activar).toHaveAttribute('aria-pressed', 'false');
}

function entoncesLosSonidosEstanActivosYSePuedenProbar() {
  expect(screen.getByRole('button', { name: 'Probar sonidos' })).toBeEnabled();
  expect(screen.getByRole('button', { name: 'Silenciar sonidos del lector' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
}
