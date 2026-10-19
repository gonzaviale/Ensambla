import { useMutation } from '@tanstack/react-query';
import { pedirLectura } from '@/api/importaciones';

// La lectura simulada de una foto: devuelve registros de ejemplo para revisar antes de incorporarlos.
export const useLecturaDeImagen = () => useMutation({ mutationFn: pedirLectura });
