package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta sin IA, con una regla simple, para desarrollar y probar sin gastar: la tabla es la elegida, la única
 * que hay o la que el texto nombra; el primer número va a la primera columna numérica, la fecha de hoy a la primera
 * columna de fecha y el resto del texto a la primera columna de texto.
 */
public class InterpreteRegistrosSimulado implements InterpreteRegistrosIaPort {

    private static final Pattern NUMERO = Pattern.compile("[0-9]+([.,][0-9]+)?");
    private static final double CONFIANZA = 0.9;

    @Override
    public InterpretacionRegistros interpretar(ConsultaInterpretacion consulta) {
        Optional<TablaDisponible> tabla = tablaDe(consulta);
        if (tabla.isEmpty()) {
            return InterpretacionRegistros.sinResultado(null);
        }
        Map<CampoId, String> fila = fila(consulta, tabla.get().bloque().campos());
        return new InterpretacionRegistros(tabla.get().bloque().id(), List.of(fila), CONFIANZA, null);
    }

    private static Optional<TablaDisponible> tablaDe(ConsultaInterpretacion consulta) {
        List<TablaDisponible> tablas = consulta.tablas();
        if (consulta.destino() != null) {
            return tablas.stream().filter(tabla -> tabla.bloque().id().equals(consulta.destino())).findFirst();
        }
        String texto = TextoNormalizado.de(consulta.texto());
        List<TablaDisponible> nombradas = tablas.stream()
                .filter(tabla -> texto.contains(TextoNormalizado.de(tabla.bloque().nombre())))
                .toList();
        if (nombradas.size() == 1) {
            return Optional.of(nombradas.getFirst());
        }
        return tablas.size() == 1 ? Optional.of(tablas.getFirst()) : Optional.empty();
    }

    private static Map<CampoId, String> fila(ConsultaInterpretacion consulta, List<CampoConocido> campos) {
        Matcher numero = NUMERO.matcher(consulta.texto());
        String importe = numero.find() ? numero.group().replace(',', '.') : null;
        String resto = NUMERO.matcher(consulta.texto()).replaceAll("").strip();
        Map<CampoId, String> fila = new LinkedHashMap<>();
        Set<String> completados = new HashSet<>();
        for (CampoConocido campo : campos) {
            switch (campo.tipo()) {
                case NUMERO, MONEDA -> poner(fila, completados, "numero", campo, importe);
                case FECHA -> poner(fila, completados, "fecha", campo, consulta.hoy().toString());
                case TEXTO, TEXTO_LARGO -> poner(fila, completados, "texto", campo, resto);
                default -> { }
            }
        }
        return fila;
    }

    /** Sólo la primera columna de cada clase recibe el valor. */
    private static void poner(Map<CampoId, String> fila, Set<String> completados, String clase, CampoConocido campo,
                              String valor) {
        if (valor != null && !valor.isBlank() && completados.add(clase)) {
            fila.put(campo.id(), valor);
        }
    }
}
