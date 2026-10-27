import { describe, expect, it } from 'vitest';
import { actualizarNegocio, obtenerConfiguracionNegocio } from '@/api/negocio';
import { resolverLocalidad } from '@/api/ubicacion';
import { LOCALIDAD_FUERA_DE_LA_ZONA } from '@/mocks/db/ubicacion';
import { handlersPara } from '@/mocks/handlers';
import { setupUsuarioConSesion } from '@/test/testHelpers';

describe('Configuración del negocio', () => {
  it('guarda con sesión y CSRF y conserva los campos de un PATCH parcial', async () => {
    setupUsuarioConSesion();
    await actualizarNegocio({ nombre: 'Almacén nuevo', rubro: 'KIOSCO', comparteDatosZona: false });
    const respuesta = await actualizarNegocio({ nombre: 'Almacén editado' });
    expect(respuesta).toEqual({
      nombre: 'Almacén editado',
      rubro: 'KIOSCO',
      ubicacion: null,
      comparteDatosZona: false,
      zona: null,
    });
    expect(await obtenerConfiguracionNegocio()).toEqual(respuesta);
  });

  it('asigna la zona que cubre la ubicación y la quita si queda fuera de toda zona', async () => {
    setupUsuarioConSesion();
    const ubicacion = { direccion: 'Calle 123', localidad: 'San Justo' };
    const dentro = await actualizarNegocio({
      ubicacion: { ...ubicacion, latitud: -34.6701, longitud: -58.5625 },
    });
    expect(dentro.zona).toBe('San Justo - UNLaM');
    const fuera = await actualizarNegocio({
      ubicacion: {
        ...ubicacion,
        localidad: LOCALIDAD_FUERA_DE_LA_ZONA,
        latitud: -34.6037,
        longitud: -58.3816,
      },
    });
    expect(fuera.zona).toBeNull();
    expect((await obtenerConfiguracionNegocio()).zona).toBeNull();
  });

  it('rechaza una localidad que no coincide con la del mapa y conserva lo guardado', async () => {
    setupUsuarioConSesion();
    const ubicacion = { direccion: 'Calle 123', latitud: -34.6701, longitud: -58.5625 };
    await actualizarNegocio({ ubicacion: { ...ubicacion, localidad: ' san JUSTO ' } });
    await expect(
      actualizarNegocio({ ubicacion: { ...ubicacion, localidad: 'Ramos Mejía' } }),
    ).rejects.toMatchObject({ status: 400 });
    expect((await obtenerConfiguracionNegocio()).ubicacion?.localidad).toBe('san JUSTO');
  });

  it('resuelve la localidad que el mapa reconoce en un punto', async () => {
    setupUsuarioConSesion();
    expect(await resolverLocalidad({ latitud: -34.6701, longitud: -58.5625 })).toEqual({
      localidad: 'San Justo',
      localidades: ['San Justo'],
    });
    expect((await resolverLocalidad({ latitud: -34.6037, longitud: -58.3816 })).localidad).toBe(
      LOCALIDAD_FUERA_DE_LA_ZONA,
    );
  });

  it('no mezcla la configuración entre dos cuentas', async () => {
    setupUsuarioConSesion({ email: 'primera@example.com' });
    await actualizarNegocio({ nombre: 'Primera cuenta' });
    setupUsuarioConSesion({ email: 'segunda@example.com' });
    expect((await obtenerConfiguracionNegocio()).nombre).not.toBe('Primera cuenta');
  });

  it('rechaza un rubro inexistente', async () => {
    setupUsuarioConSesion();
    await expect(actualizarNegocio({ rubro: 'INEXISTENTE' })).rejects.toMatchObject({
      status: 400,
    });
  });

  it('requiere una sesión para consultar el negocio', async () => {
    await expect(obtenerConfiguracionNegocio()).rejects.toMatchObject({ status: 401 });
  });

  it('simula la configuración sólo en modo completo', () => {
    const esNegocio = (path: unknown) => String(path).endsWith('/business');
    expect(handlersPara('completo').filter((handler) => esNegocio(handler.info.path))).toHaveLength(
      2,
    );
    expect(handlersPara('parcial').filter((handler) => esNegocio(handler.info.path))).toHaveLength(
      0,
    );
    expect(handlersPara('ninguno')).toHaveLength(0);
  });
});
