package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Configuración de un campo tal como la manda la persona (sin validar). {@link #validar()} aplica las reglas de
 * T-018 y devuelve la versión normalizada, o lanza {@link CampoInvalidoException} con un mensaje por dato.
 */
public record ConfiguracionCampo(
        String nombre,
        String tipo,
        Boolean obligatorio,
        String valorInicial,
        List<String> opciones
) {

    static final int MAX_CARACTERES_NOMBRE = 120;

    private static final Pattern NUMERO = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** Configuración ya validada: nombre y opciones sin espacios en los extremos, valor inicial vacío como null. */
    public record Validada(
            String nombre,
            TipoCampo tipo,
            boolean obligatorio,
            String valorInicial,
            List<String> opciones
    ) {
    }

    public Validada validar() {
        String nombreLimpio = limpiar(nombre);
        TipoCampo tipoCampo = tipoDe(tipo);
        List<String> opcionesLimpias = tipoCampo == TipoCampo.OPCION ? limpiarOpciones() : List.of();
        String valorLimpio = limpiar(valorInicial);
        Map<String, String> errores = new LinkedHashMap<>();
        errorDeNombre(nombreLimpio).ifPresent(error -> errores.put("nombre", error));
        if (tipoCampo == null) {
            errores.put("tipo", "Elegí un tipo válido: texto, número, fecha, opción o sí/no");
        } else {
            errorDeOpciones(tipoCampo, opcionesLimpias).ifPresent(error -> errores.put("opciones", error));
            errorDeValorInicial(tipoCampo, valorLimpio, opcionesLimpias)
                    .ifPresent(error -> errores.put("valorInicial", error));
        }
        if (!errores.isEmpty()) {
            throw new CampoInvalidoException(errores);
        }
        return new Validada(nombreLimpio, tipoCampo, Boolean.TRUE.equals(obligatorio), valorLimpio, opcionesLimpias);
    }

    private List<String> limpiarOpciones() {
        if (opciones == null) {
            return List.of();
        }
        return opciones.stream().map(ConfiguracionCampo::limpiar).toList();
    }

    private static Optional<String> errorDeNombre(String nombre) {
        if (nombre == null) {
            return Optional.of("Escribí un nombre");
        }
        return errorSi(nombre.length() > MAX_CARACTERES_NOMBRE, "Máximo " + MAX_CARACTERES_NOMBRE + " caracteres");
    }

    private static Optional<String> errorDeOpciones(TipoCampo tipo, List<String> opciones) {
        if (tipo != TipoCampo.OPCION) {
            return Optional.empty();
        }
        if (opciones.isEmpty()) {
            return Optional.of("Agregá al menos una opción");
        }
        if (opciones.stream().anyMatch(Objects::isNull)) {
            return Optional.of("Las opciones no pueden quedar vacías");
        }
        long distintas = opciones.stream().map(opcion -> opcion.toLowerCase(Locale.ROOT)).distinct().count();
        return errorSi(distintas < opciones.size(), "Hay opciones repetidas");
    }

    private static Optional<String> errorDeValorInicial(TipoCampo tipo, String valor, List<String> opciones) {
        if (valor == null) {
            return Optional.empty();
        }
        return switch (tipo) {
            case TEXTO -> errorSi(valor.length() > Campo.MAX_CARACTERES_TEXTO,
                    "Máximo " + Campo.MAX_CARACTERES_TEXTO + " caracteres");
            case NUMERO -> errorSi(!NUMERO.matcher(valor).matches(), "Tiene que ser un número, por ejemplo 12.5");
            case FECHA -> errorSi(!esFecha(valor), "Tiene que ser una fecha con el formato AAAA-MM-DD");
            case OPCION -> errorSi(!opciones.contains(valor), "Tiene que ser una de las opciones");
            case SI_NO -> errorSi(!"true".equals(valor) && !"false".equals(valor),
                    "Tiene que ser true (sí) o false (no)");
        };
    }

    private static boolean esFecha(String valor) {
        if (!FECHA.matcher(valor).matches()) {
            return false;
        }
        try {
            LocalDate.parse(valor);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static TipoCampo tipoDe(String tipo) {
        return Arrays.stream(TipoCampo.values())
                .filter(valor -> valor.name().equals(tipo))
                .findFirst()
                .orElse(null);
    }

    /** Quita los espacios de los extremos; un texto en blanco cuenta como ausente (null). */
    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }

    private static Optional<String> errorSi(boolean invalido, String mensaje) {
        return invalido ? Optional.of(mensaje) : Optional.empty();
    }
}
