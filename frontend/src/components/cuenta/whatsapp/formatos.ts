export const fecha = (valor: string) =>
  new Intl.DateTimeFormat('es-AR', { dateStyle: 'long' }).format(new Date(valor));

export const enlaceDeSami = (numeroBot: string) => `https://wa.me/${numeroBot}`;
