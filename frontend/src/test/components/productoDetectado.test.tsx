import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ProductoDetectado } from '@/components/escaneo/ProductoDetectado';

const propiedades = {
  resultado: { codigo: '7790957000101', fuentes: [] },
  sugerencia: { nombre: 'Alfajor Jorgito', imagen: 'https://images.openfoodfacts.org/alfajor.jpg' },
  nombre: 'Alfajor Jorgito',
  existente: false,
  buscando: false,
  onFuentes: vi.fn(),
};

type Vista = ReturnType<typeof render>;

let vista: Vista;

describe('foto de referencia del producto', () => {
  it('si la imagen no carga muestra que no hay foto de referencia', () => {
    dadoUnProductoDetectadoCuyaFotoFalla();

    entoncesNoHayImagenYSeAvisaQueNoHayFoto();
  });

  it('si la imagen no carga conserva la descripción y permite seguir revisando', () => {
    dadoUnProductoDetectadoCuyaFotoFalla();

    entoncesSeConservaLaDescripcionYElOrigenDeLosDatos();
  });

  it('muestra la foto del siguiente producto aunque la anterior haya fallado', () => {
    dadoUnProductoDetectadoCuyaFotoFalla();

    cuandoSeDetectaOtroProductoConFoto();

    entoncesSeVeLaFotoYElNombreDeGalletitas();
  });
});

function dadoUnProductoDetectadoCuyaFotoFalla() {
  vista = render(<ProductoDetectado {...propiedades} />);
  fireEvent.error(screen.getByRole('img', { name: 'Foto de referencia del producto' }));
}

function cuandoSeDetectaOtroProductoConFoto() {
  vista.rerender(
    <ProductoDetectado
      {...propiedades}
      nombre="Galletitas"
      sugerencia={{
        nombre: 'Galletitas',
        imagen: 'https://images.openfoodfacts.org/galletitas.jpg',
      }}
    />,
  );
}

function entoncesNoHayImagenYSeAvisaQueNoHayFoto() {
  expect(screen.queryByRole('img')).toBeNull();
  expect(screen.getByText('Sin foto de referencia')).toBeVisible();
}

function entoncesSeConservaLaDescripcionYElOrigenDeLosDatos() {
  expect(screen.getByRole('heading', { name: 'Alfajor Jorgito' })).toBeVisible();
  expect(screen.getByRole('button', { name: 'Ver origen de los datos' })).toBeEnabled();
}

function entoncesSeVeLaFotoYElNombreDeGalletitas() {
  expect(screen.getByRole('img')).toHaveAttribute(
    'src',
    'https://images.openfoodfacts.org/galletitas.jpg',
  );
  expect(screen.getByRole('heading', { name: 'Galletitas' })).toBeVisible();
}
