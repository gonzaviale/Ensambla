package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuración de un campo tal como la manda la persona (sin validar). {@link #validar()} aplica las reglas de
 * T-018 y devuelve el campo del tipo elegido, o lanza {@link CampoInvalidoException} con un mensaje por dato.
 */
public record ConfiguracionCampo(
        String nombre,
        String tipo,
        Boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        BloqueId bloqueRelacionadoId,
        String formula
) {

    static final int MAX_CARACTERES_NOMBRE = 120;

    private static final String TIPO_INVALIDO =
            "Elegí un tipo válido: texto, número, fecha, opción, sí/no, conexión o fórmula";

    public Campo validar() {
        Map<String, String> errores = new LinkedHashMap<>();
        errorDeNombre().ifPresent(error -> errores.put("nombre", error));
        Optional<Campo> campo = tipoElegido().map(this::aCampo);
        campo.ifPresentOrElse(
                elegido -> errores.putAll(elegido.erroresDeConfiguracion()),
                () -> errores.put("tipo", TIPO_INVALIDO));
        exigirSinErrores(errores);
        return campo.orElseThrow();
    }

    private Campo aCampo(TipoCampo tipoElegido) {
        return Campo.builder()
                .nombre(limpiar(nombre))
                .tipo(tipoElegido)
                .obligatorio(Boolean.TRUE.equals(obligatorio))
                .valorInicial(limpiar(valorInicial))
                .opciones(opcionesLimpias())
                .bloqueRelacionadoId(bloqueRelacionadoId)
                .formula(limpiar(formula))
                .build();
    }

    private Optional<TipoCampo> tipoElegido() {
        return Arrays.stream(TipoCampo.values())
                .filter(valor -> valor.name().equals(tipo))
                .findFirst();
    }

    private Optional<String> errorDeNombre() {
        String limpio = limpiar(nombre);
        if (limpio == null) {
            return Optional.of("Escribí un nombre");
        }
        boolean demasiadoLargo = limpio.length() > MAX_CARACTERES_NOMBRE;
        return demasiadoLargo ? Optional.of("Máximo " + MAX_CARACTERES_NOMBRE + " caracteres") : Optional.empty();
    }

    private List<String> opcionesLimpias() {
        if (opciones == null) {
            return List.of();
        }
        return opciones.stream().map(ConfiguracionCampo::limpiar).toList();
    }

    private static void exigirSinErrores(Map<String, String> errores) {
        if (!errores.isEmpty()) {
            throw new CampoInvalidoException(errores);
        }
    }

    /** Quita los espacios de los extremos; un texto en blanco cuenta como ausente (null). */
    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
