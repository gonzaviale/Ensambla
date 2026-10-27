import type { ComponentType } from 'react';
import { getRouteApi, Link } from '@tanstack/react-router';
import { SeccionNotificaciones } from '@/components/cuenta/SeccionNotificaciones';
import { SeccionPerfil } from '@/components/cuenta/SeccionPerfil';
import { SeccionSesion } from '@/components/cuenta/SeccionSesion';
import { SeccionSuscripcion } from '@/components/cuenta/SeccionSuscripcion';
import { SeccionWhatsapp } from '@/components/cuenta/SeccionWhatsapp';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { SECCION_DE_CUENTA_POR_DEFECTO, type SeccionDeCuenta } from '@/lib/cuenta';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

const ruta = getRouteApi('/_app/cuenta');

const SECCIONES: { clave: SeccionDeCuenta; titulo: string; icono: NombreDeIcono }[] = [
  { clave: 'perfil', titulo: 'Perfil', icono: 'users' },
  { clave: 'notificaciones', titulo: 'Notificaciones', icono: 'bell' },
  { clave: 'suscripcion', titulo: 'Suscripción', icono: 'wallet' },
  { clave: 'whatsapp', titulo: 'WhatsApp', icono: 'spark' },
  { clave: 'sesion', titulo: 'Cerrar sesión', icono: 'arrow' },
];

const CONTENIDO: Record<SeccionDeCuenta, ComponentType> = {
  perfil: SeccionPerfil,
  notificaciones: SeccionNotificaciones,
  suscripcion: SeccionSuscripcion,
  whatsapp: SeccionWhatsapp,
  sesion: SeccionSesion,
};

// Tu cuenta: lo personal que no es de un sistema en particular. Se llega desde «Configuración», en el menú del avatar.
export function CuentaPage() {
  useTituloPagina('Tu cuenta');
  const { seccion = SECCION_DE_CUENTA_POR_DEFECTO } = ruta.useSearch();
  const Contenido = CONTENIDO[seccion];

  return (
    <>
      <CabeceraComun active="account" />
      <main id="main" tabIndex={-1} className="page-wrap cuenta-page reveal">
        <div className="page-title">
          <div className="eyebrow">TU ESPACIO PERSONAL</div>
          <h1>Tu cuenta</h1>
          <p>Tus datos, los avisos que recibís y tu plan, en un solo lugar.</p>
        </div>
        <div className="cuenta-layout">
          <nav className="cuenta-menu" aria-label="Secciones de tu cuenta">
            {SECCIONES.map(({ clave, titulo, icono }) => (
              <Link
                key={clave}
                to="/cuenta"
                search={{ seccion: clave }}
                className={clave === seccion ? 'activa' : ''}
                aria-current={clave === seccion ? 'page' : undefined}
              >
                <Icon name={icono} />
                {titulo}
              </Link>
            ))}
          </nav>
          <div className="cuenta-contenido">
            <Contenido />
          </div>
        </div>
        <PageFooter />
      </main>
    </>
  );
}
