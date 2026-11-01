/** Las secciones de la configuración de la cuenta, en el orden en que se muestran. */
export const SECCIONES_DE_CUENTA = ['perfil', 'notificaciones', 'suscripcion', 'whatsapp', 'sesion'] as const;

export type SeccionDeCuenta = (typeof SECCIONES_DE_CUENTA)[number];

export const SECCION_DE_CUENTA_POR_DEFECTO: SeccionDeCuenta = 'perfil';
