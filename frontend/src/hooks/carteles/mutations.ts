import { useMutation } from '@tanstack/react-query';
import { armarOpcionesDeCartel, exportarCartel, redactarTextosDeCartel } from '@/api/carteles';

export const useRedactarTextos = () => useMutation({ mutationFn: redactarTextosDeCartel });

export const useArmarOpciones = () => useMutation({ mutationFn: armarOpcionesDeCartel });

export const useExportarCartel = () => useMutation({ mutationFn: exportarCartel });
