import { HttpResponse, delay, http } from 'msw';
import { plantillasMock } from '../db/estado';
import { LATENCIA_BACKEND_MS, api } from './comun';

// El backend ya sirve GET /api/plantillas; este mock sólo se usa con VITE_API_MOCKS=completo.
export const handlersPlantillas = [
  http.get(api('/plantillas'), async () => {
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(plantillasMock);
  }),
];
