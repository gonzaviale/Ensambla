import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Plan } from '@/types/planes';
import { textoDeNegocios } from './negocios';
import { ILIMITADO, presentacionDelPlan } from './presentacionDelPlan';

export type FilaDelPlan = {
  icono: NombreDeIcono;
  titulo: string;
  valor: string;
  excluida?: boolean;
};

type FuncionDelPlan = {
  icono: NombreDeIcono;
  titulo: string;
  incluidaEn: (plan: Plan) => boolean;
};

const POR_DEFINIR = 'Por definir';
const INCLUIDO = 'Incluido';
const NO_INCLUIDO = 'No incluido';

const FUNCIONES: FuncionDelPlan[] = [
  { icono: 'edit', titulo: 'Carga manual, edición y consulta', incluidaEn: () => true },
  {
    icono: 'download',
    titulo: 'Resúmenes en PDF',
    incluidaEn: (plan) => plan.funciones.includes('REPORTES_FILTRADOS'),
  },
];

export const filasDelPlan = (plan: Plan): FilaDelPlan[] => [
  ...filasDeLimites(plan),
  ...FUNCIONES.map((funcion) => filaDeFuncion(funcion, plan)),
];

function filasDeLimites(plan: Plan): FilaDelPlan[] {
  const { avisosPorMes } = presentacionDelPlan(plan);
  return [
    filaDeNegocios(plan),
    filaConLimite('note', 'Datos guardados en total', plan.limiteRegistros, plan),
    filaConLimite('camera', 'Análisis de foto por mes', plan.limitePaginasIaMensuales, plan),
    ...filaDeObjetivosConIa(plan),
    { icono: 'bell', titulo: 'Avisos por mes', valor: avisosPorMes },
    { icono: 'ubicacion', titulo: 'Recomendaciones de zona por mes', valor: avisosPorMes },
  ];
}

function filaDeNegocios(plan: Plan): FilaDelPlan {
  const valor = plan.limitesConfigurados ? textoDeNegocios(plan.limiteProyectos) : POR_DEFINIR;
  return { icono: 'folder', titulo: 'Negocios', valor };
}

function filaDeObjetivosConIa(plan: Plan): FilaDelPlan[] {
  if (plan.limiteObjetivosIaMensuales === undefined) return [];
  return [filaConLimite('chart', 'Objetivos con IA por mes', plan.limiteObjetivosIaMensuales, plan)];
}

function filaConLimite(
  icono: NombreDeIcono,
  titulo: string,
  limite: number | null,
  plan: Plan,
): FilaDelPlan {
  return { icono, titulo, valor: textoDelLimite(limite, plan.limitesConfigurados) };
}

function textoDelLimite(limite: number | null, configurado: boolean) {
  if (!configurado) return POR_DEFINIR;
  return limite === null ? ILIMITADO : limite.toLocaleString('es-AR');
}

function filaDeFuncion(funcion: FuncionDelPlan, plan: Plan): FilaDelPlan {
  const incluida = funcion.incluidaEn(plan);
  const valor = incluida ? INCLUIDO : NO_INCLUIDO;
  return { icono: funcion.icono, titulo: funcion.titulo, valor, excluida: !incluida };
}
