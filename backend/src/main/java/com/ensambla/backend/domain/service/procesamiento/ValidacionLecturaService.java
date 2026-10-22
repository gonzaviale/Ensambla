package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ValidacionLecturaService {

    public void validar(LecturaCuaderno lectura) {
        nombre(lectura.bloque().nombre());
        Map<String, CampoPropuesto> campos = lectura.bloque().campos().stream()
                .collect(Collectors.toMap(CampoPropuesto::clave, Function.identity()));
        campos.values().forEach(this::campo);
        Set<String> renglones = new HashSet<>();
        lectura.filas().forEach(fila -> {
            if (!renglones.add(fila.pagina() + ":" + fila.renglon())) {
                throw invalida("Hay un renglón repetido");
            }
            fila.datos().forEach((clave, dato) -> dato(campos.get(clave), dato));
        });
    }

    private void campo(CampoPropuesto campo) {
        nombre(campo.nombre());
        if (!campo.clave().matches("[a-z][a-z0-9_]*")) {
            throw invalida("La clave del campo no respeta el formato esperado");
        }
        if (campo.opciones().stream().anyMatch(String::isBlank)
                || new HashSet<>(campo.opciones()).size() != campo.opciones().size()) {
            throw invalida("Las opciones deben ser únicas y no estar vacías");
        }
    }

    private void nombre(String valor) {
        if (valor.length() > 120) {
            throw invalida("El nombre del bloque o campo supera los 120 caracteres");
        }
    }

    private void dato(CampoPropuesto campo, DatoLeido dato) {
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

    private void comprobar(boolean valido) {
        if (!valido) {
            throw new IllegalArgumentException("Valor incompatible");
        }
    }

    private LecturaCuadernoException invalida(String mensaje) {
        return new LecturaCuadernoException("La lectura de la IA no es válida: " + mensaje, null, false);
    }
}
