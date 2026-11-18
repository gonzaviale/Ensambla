const MS_POR_DIA = 86_400_000;

const fechaDelNegocio = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Argentina/Buenos_Aires',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

export function diaDelNegocio(instante: Date) {
  const partes = fechaDelNegocio.formatToParts(instante);
  const leer = (tipo: Intl.DateTimeFormatPartTypes) =>
    Number(partes.find((parte) => parte.type === tipo)?.value);
  return Date.UTC(leer('year'), leer('month') - 1, leer('day'));
}

export function diasDeCalendarioEntre(desde: Date, hasta: Date) {
  return (diaDelNegocio(hasta) - diaDelNegocio(desde)) / MS_POR_DIA;
}
