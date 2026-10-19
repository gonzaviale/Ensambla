import { describe, expect, it } from 'vitest';
import { actualizarNegocio, obtenerConfiguracionNegocio } from '@/api/negocio';
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
    });
    expect(await obtenerConfiguracionNegocio()).toEqual(respuesta);
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
