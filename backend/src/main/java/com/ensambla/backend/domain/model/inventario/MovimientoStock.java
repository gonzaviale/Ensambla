package com.ensambla.backend.domain.model.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Una entrada o una salida de un producto (un registro); el stock es la suma de sus movimientos. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    static final int MAX_CARACTERES_NOTA = 300;

    static final int MAX_DECIMALES = 3;

    /** Lo que entra en numeric(14, 3). */
    static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("99999999999.999");

    private UUID id;

    private UUID negocioId;

    /** Producto: el registro del que se mueve el stock. */
    private UUID registroId;

    private TipoMovimiento tipo;

    private BigDecimal cantidad;

    private LocalDate fecha;

    private String nota;

    private OffsetDateTime creadoEn;

    /**
     * Movimiento nuevo, todavía sin guardar. Una fecha vacía es hoy; el texto en blanco de la nota cuenta como vacío.
     *
     * @throws RegistroInvalidoException con un mensaje por cada dato inválido, indexado por nombre
     */
    public static MovimientoStock nuevo(
            UUID negocioId,
            UUID registroId,
            String tipo,
            BigDecimal cantidad,
            LocalDate fecha,
            String nota,
            LocalDate hoy,
            OffsetDateTime ahora) {
        Map<String, String> errores = new LinkedHashMap<>();
        TipoMovimiento tipoMovimiento = tipoDe(tipo);
        if (registroId == null) {
            errores.put("registroId", "Elegí el producto");
        }
        if (tipoMovimiento == null) {
            errores.put("tipo", "Elegí si es una entrada o una salida");
        }
        errorDeCantidad(cantidad).ifPresent(error -> errores.put("cantidad", error));
        LocalDate fechaFinal = fecha == null ? hoy : fecha;
        if (fechaFinal.isAfter(hoy)) {
            errores.put("fecha", "No puede ser una fecha futura");
        }
        String notaFinal = nota == null || nota.isBlank() ? null : nota.strip();
        if (notaFinal != null && notaFinal.length() > MAX_CARACTERES_NOTA) {
            errores.put("nota", "Máximo " + MAX_CARACTERES_NOTA + " caracteres");
        }
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
        return MovimientoStock.builder()
                .negocioId(negocioId)
                .registroId(registroId)
                .tipo(tipoMovimiento)
                .cantidad(cantidad)
                .fecha(fechaFinal)
                .nota(notaFinal)
                .creadoEn(ahora)
                .build();
    }

    public boolean esSalida() {
        return tipo == TipoMovimiento.SALIDA;
    }

    private static TipoMovimiento tipoDe(String tipo) {
        return Arrays.stream(TipoMovimiento.values())
                .filter(valor -> valor.name().equals(tipo))
                .findFirst()
                .orElse(null);
    }

    private static Optional<String> errorDeCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            return Optional.of("Escribí la cantidad");
        }
        if (cantidad.signum() <= 0) {
            return Optional.of("Tiene que ser mayor a cero");
        }
        if (cantidad.stripTrailingZeros().scale() > MAX_DECIMALES) {
            return Optional.of("Máximo " + MAX_DECIMALES + " decimales");
        }
        if (cantidad.compareTo(CANTIDAD_MAXIMA) > 0) {
            return Optional.of("Es una cantidad demasiado grande");
        }
        return Optional.empty();
    }
}
