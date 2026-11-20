package com.ensambla.backend.domain.model.metrica;

import java.time.LocalDate;

/**
 * La explicación de un número: de qué tabla salió, cuántas filas entraron en la cuenta y cuántas quedaron afuera por
 * no tener el dato que hacía falta (por ejemplo, ventas sin precio).
 *
 * @param desde primer día mirado, o {@code null} si la métrica mira todo lo cargado
 * @param hasta último día mirado (hoy), o {@code null} si la métrica mira todo lo cargado
 */
public record DeDondeSale(FuenteMetrica fuente, long filasUsadas, long filasSinDato, LocalDate desde, LocalDate hasta) {
}
