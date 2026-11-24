import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Plan } from '@/types/planes';

export const ILIMITADO = 'Ilimitado';

type PresentacionDelPlan = {
  icono: NombreDeIcono;
  color: 'blue' | 'petrol';
  descripcion: string;
  avisosPorMes: string;
  destacado: boolean;
};

const PRESENTACIONES: Record<Plan['codigo'], PresentacionDelPlan> = {
  GRATUITO: {
    icono: 'grid',
    color: 'blue',
    descripcion: 'Lo esencial para empezar a organizar tu negocio.',
    avisosPorMes: '6',
    destacado: false,
  },
  PREMIUM: {
    icono: 'spark',
    color: 'petrol',
    descripcion: 'Más espacio para tus datos. Más herramientas para decidir.',
    avisosPorMes: ILIMITADO,
    destacado: true,
  },
};

export const presentacionDelPlan = (plan: Plan) => PRESENTACIONES[plan.codigo];
