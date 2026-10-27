package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resultado validado de leer un lote de páginas: estructura propuesta + filas + avisos, sin guardar nada todavía. */
public record LecturaCuaderno(TipoRegistroCuaderno tipoRegistro, BloquePropuesto bloque, List<FilaLeida> filas,
                              List<String> advertencias, ConsumoIa consumo) {

    public LecturaCuaderno {
        filas = List.copyOf(filas);
        advertencias = List.copyOf(advertencias);
    }

    /**
     * Comprueba que lo que devolvió la IA respete el formato esperado: nombres acotados, claves y opciones válidas,
     * renglones sin repetir y valores compatibles con el tipo de su campo.
     *
     * @throws LecturaCuadernoException no recuperable si algo no cumple
     */
    public void validar() {
        validarNombre(bloque.nombre());
        Map<String, CampoPropuesto> campos = bloque.campos().stream()
                .collect(Collectors.toMap(CampoPropuesto::clave, Function.identity()));
        campos.values().forEach(LecturaCuaderno::validarCampo);
        Set<String> renglones = new HashSet<>();
        filas.forEach(fila -> {
            if (!renglones.add(fila.pagina() + ":" + fila.renglon())) {
                throw invalida("Hay un renglón repetido");
            }
            fila.datos().forEach((clave, dato) -> validarDato(campos.get(clave), dato));
        });
    }

    private static void validarCampo(CampoPropuesto campo) {
        validarNombre(campo.nombre());
        if (!campo.clave().matches("[a-z][a-z0-9_]*")) {
            throw invalida("La clave del campo no respeta el formato esperado");
        }
        if (campo.opciones().stream().anyMatch(String::isBlank)
                || new HashSet<>(campo.opciones()).size() != campo.opciones().size()) {
            throw invalida("Las opciones deben ser únicas y no estar vacías");
        }
    }

    private static void validarNombre(String valor) {
        if (valor.length() > 120) {
            throw invalida("El nombre del bloque o campo supera los 120 caracteres");
        }
    }

    private static void validarDato(CampoPropuesto campo, DatoLeido dato) {
        if (!Double.isFinite(dato.confianza())) {
            throw invalida("La confianza debe ser un número finito");
        }
        if (dato.valor() == null) {
            return;
        }
        try {
            switch (campo.tipo()) {
                case NUMERO, MONEDA -> new BigDecimal(dato.valor());
                case FECHA -> LocalDate.parse(dato.valor());
                case SI_NO -> comprobar("true".equals(dato.valor()) || "false".equals(dato.valor()));
                case OPCION -> comprobar(campo.opciones().contains(dato.valor()));
                case FORMULA -> throw new IllegalArgumentException("La lectura no propone fórmulas");
                default -> { } // TEXTO y RELACION: cualquier texto sirve (la conexión se resuelve a una fila después)
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw invalida("El valor del campo " + campo.clave() + " no coincide con su tipo");
        }
    }

    private static void comprobar(boolean valido) {
        if (!valido) {
            throw new IllegalArgumentException("Valor incompatible");
        }
    }

    private static LecturaCuadernoException invalida(String mensaje) {
        return new LecturaCuadernoException("La lectura de la IA no es válida: " + mensaje, null, false);
    }
}
