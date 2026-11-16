// Cuántos negocios deja tener un plan, dicho como lo lee la persona (ENS-284 · T-206). Cada negocio es un cuaderno,
// así que el límite es el de proyectos del plan (`limiteProyectos`).

/** «1 negocio», «Tu negocio y hasta 5 más» o «Sin límite»; `null` en el límite es que no lo tiene. */
export const textoDeNegocios = (limite: number | null) => {
  if (limite === null) return 'Sin límite';
  return limite <= 1 ? '1 negocio' : `Tu negocio y hasta ${limite - 1} más`;
};
