import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ElegirNegocio, type EstadoDeLaEleccion } from '@/components/negocios/ElegirNegocio';
import type { NegocioParaElegir } from '@/lib/negocios/presentacion';

type Usuario = ReturnType<typeof userEvent.setup>;

const ALMACEN: NegocioParaElegir = {
  id: 'almacen',
  nombre: 'Almacén Don Pepe',
  avisosPendientes: 3,
  estado: 'BIEN',
};

const KIOSCO: NegocioParaElegir = {
  id: 'kiosco',
  nombre: 'Kiosco de la esquina',
  avisosPendientes: 1,
  estado: 'ATENCION',
};

const FERIA: NegocioParaElegir = {
  id: 'feria',
  nombre: 'Puesto de la feria',
  avisosPendientes: 0,
  estado: 'SIN_DATOS',
};

const listo = (
  activoId: string | null = null,
  negocios: NegocioParaElegir[] = [ALMACEN, KIOSCO, FERIA],
): EstadoDeLaEleccion => ({ tipo: 'listo', negocios, activoId });

let user: Usuario;
let onElegir: ReturnType<typeof vi.fn>;
let reintentar: ReturnType<typeof vi.fn>;

describe('ElegirNegocio', () => {
  it('ofrece cada negocio por su nombre, en el orden del servidor', () => {
    dadaLaEleccion(listo());

    entoncesLosNegociosSeOfrecenEnOrden([
      'Almacén Don Pepe',
      'Kiosco de la esquina',
      'Puesto de la feria',
    ]);
  });

  it('dice de cada negocio cómo viene', () => {
    dadaLaEleccion(listo());

    entoncesElNegocioDice('Kiosco de la esquina', 'Atención');
    entoncesElNegocioDice('Almacén Don Pepe', 'Viene bien');
  });

  it('dice de cada negocio sus avisos', () => {
    dadaLaEleccion(listo());

    entoncesElNegocioSeDescribeComo('Kiosco de la esquina', '1 aviso pendiente');
    entoncesElNegocioSeDescribeComo('Puesto de la feria', 'Sin avisos pendientes');
  });

  it('no muestra montos', () => {
    dadaLaEleccion(listo());

    entoncesElNegocioNoDice('Kiosco de la esquina', '$');
  });

  it.each([
    ['Almacén Don Pepe', 'petrol'],
    ['Kiosco de la esquina', 'orange'],
    ['Puesto de la feria', 'coral'],
  ])('le da a %s el color %s según su lugar en la lista', (nombre, color) => {
    dadaLaEleccion(listo());

    entoncesElNegocioTieneElAtributo(nombre, 'data-color', color);
  });

  it('marca el estado para que se vea', () => {
    dadaLaEleccion(listo());

    entoncesElNegocioTieneElAtributo('Kiosco de la esquina', 'data-estado', 'ATENCION');
  });

  it('resalta los avisos pendientes y no los que no hay', () => {
    dadaLaEleccion(listo());

    entoncesLosAvisosSeResaltanSoloEnElKiosco();
  });

  it('señala el último negocio que se usó', () => {
    dadaLaEleccion(listo('kiosco'));

    entoncesElNegocioSeDescribeComo(
      'Kiosco de la esquina',
      '1 aviso pendiente · El último que usaste',
    );
  });

  it('no señala como último a los otros negocios', () => {
    dadaLaEleccion(listo('kiosco'));

    entoncesElNegocioNoDice('Almacén Don Pepe', 'El último que usaste');
  });

  it('si no se sabe cuántos avisos tiene un negocio, no dice nada de ellos', () => {
    dadaLaEleccion(listo('kiosco', [{ ...KIOSCO, avisosPendientes: null }]));

    entoncesElNegocioSeDescribeComo('Kiosco de la esquina', 'El último que usaste');
    entoncesElNegocioNoDice('Kiosco de la esquina', 'aviso');
  });

  it('tocar un negocio avisa cuál se eligió', async () => {
    dadaLaEleccion(listo());

    await cuandoSeTocaElNegocio('Kiosco de la esquina');

    entoncesSeEligio('kiosco');
  });

  it('mientras carga lo anuncia', () => {
    dadaLaEleccion({ tipo: 'cargando' });

    entoncesSeAnuncia('Buscando tus negocios…');
  });

  it('mientras carga no ofrece negocios', () => {
    dadaLaEleccion({ tipo: 'cargando' });

    entoncesNoSeOfreceNingunNegocio();
  });

  it('si falla lo avisa', () => {
    dadaUnaEleccionQueFallo();

    entoncesSeAnuncia('No pudimos traer tus negocios ahora.');
  });

  it('si falla deja probar de nuevo', async () => {
    dadaUnaEleccionQueFallo();

    await cuandoSePruebaDeNuevo();

    entoncesSeReintentoUnaVez();
  });
});

const opcion = (nombre: string) => screen.getByRole('button', { name: nombre });

function dadaLaEleccion(estado: EstadoDeLaEleccion) {
  onElegir = vi.fn();
  render(<ElegirNegocio estado={estado} onElegir={onElegir} />);
  user = userEvent.setup();
}

function dadaUnaEleccionQueFallo() {
  reintentar = vi.fn();
  dadaLaEleccion({ tipo: 'error', reintentar });
}

async function cuandoSeTocaElNegocio(nombre: string) {
  await user.click(opcion(nombre));
}

async function cuandoSePruebaDeNuevo() {
  await user.click(screen.getByRole('button', { name: 'Probar de nuevo' }));
}

function entoncesLosNegociosSeOfrecenEnOrden(nombres: string[]) {
  const ofrecidos = screen.getAllByRole('button').map((boton) => boton.textContent);
  expect(ofrecidos).toEqual(nombres.map((nombre) => expect.stringContaining(nombre)));
}

function entoncesElNegocioDice(nombre: string, texto: string) {
  expect(opcion(nombre)).toHaveTextContent(texto);
}

function entoncesElNegocioNoDice(nombre: string, texto: string) {
  expect(opcion(nombre)).not.toHaveTextContent(texto);
}

function entoncesElNegocioSeDescribeComo(nombre: string, descripcion: string) {
  expect(opcion(nombre)).toHaveAccessibleDescription(descripcion);
}

function entoncesElNegocioTieneElAtributo(nombre: string, atributo: string, valor: string) {
  expect(opcion(nombre)).toHaveAttribute(atributo, valor);
}

function entoncesLosAvisosSeResaltanSoloEnElKiosco() {
  const kiosco = within(opcion('Kiosco de la esquina')).getByText('1 aviso pendiente');
  expect(kiosco).toHaveAttribute('data-hay', 'true');
  const feria = within(opcion('Puesto de la feria')).getByText('Sin avisos pendientes');
  expect(feria).not.toHaveAttribute('data-hay');
}

function entoncesSeEligio(id: string) {
  expect(onElegir).toHaveBeenCalledWith(id);
}

function entoncesSeAnuncia(texto: string) {
  expect(screen.getByRole('status')).toHaveTextContent(texto);
}

function entoncesNoSeOfreceNingunNegocio() {
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
}

function entoncesSeReintentoUnaVez() {
  expect(reintentar).toHaveBeenCalledOnce();
}
