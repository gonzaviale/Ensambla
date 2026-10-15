// Validaciones del flujo de recuperación de contraseña (simulado en el cliente: no hay endpoints).

const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MAX_BYTES_CONTRASENIA = 72;

export function validarEmail(valor: string): string | undefined {
  const email = valor.trim();
  if (!email) return 'Ingresá tu email.';
  if (email.length > 254) return 'El email es demasiado largo.';
  if (!FORMATO_EMAIL.test(email)) return 'Ingresá un email válido, por ejemplo nombre@correo.com.';
  return undefined;
}

export type ErroresClave = { contrasenia?: string; repetida?: string };

export function validarClaveNueva(contrasenia: string, repetida: string): ErroresClave {
  const errores: ErroresClave = {};
  if (contrasenia.length < 8) errores.contrasenia = 'Usá al menos 8 caracteres.';
  else if (new TextEncoder().encode(contrasenia).length > MAX_BYTES_CONTRASENIA) {
    errores.contrasenia = 'La contraseña es demasiado larga (máximo 72 caracteres).';
  }
  if (!repetida) errores.repetida = 'Repetí la contraseña nueva.';
  else if (contrasenia !== repetida) errores.repetida = 'Las contraseñas no coinciden.';
  return errores;
}
