import { useState } from 'react';

export interface Comentario {
  nombre: string;
  negocio: string;
  puntaje: number;
  texto: string;
}

const CLAVE = 'ensambla-comentarios';
const MAXIMO_GUARDADOS = 12;

function esComentario(c: unknown): c is Comentario {
  if (typeof c !== 'object' || c === null) return false;
  const { nombre, texto } = c as Partial<Comentario>;
  return typeof nombre === 'string' && nombre !== '' && typeof texto === 'string';
}

// El almacenamiento puede no estar disponible (modo privado): la landing funciona igual.
function leer(): Comentario[] {
  try {
    const guardado: unknown = JSON.parse(localStorage.getItem(CLAVE) ?? '[]');
    return Array.isArray(guardado) ? guardado.filter(esComentario) : [];
  } catch {
    return [];
  }
}

function guardar(comentarios: Comentario[]) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(comentarios));
  } catch {
    /* sin almacenamiento */
  }
}

// Los comentarios se guardan en este navegador hasta que exista un endpoint en el backend.
export function useComentarios() {
  const [propios, setPropios] = useState<Comentario[]>(leer);

  const agregar = (comentario: Comentario) => {
    const siguientes = [...propios, comentario].slice(-MAXIMO_GUARDADOS);
    guardar(siguientes);
    setPropios(siguientes);
  };

  return { propios, agregar };
}
