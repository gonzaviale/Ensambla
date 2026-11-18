package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class CampoFecha extends Campo {

    private static final Pattern FORMATO_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public CampoFecha(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.FECHA;
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        return errorSi(!esFechaIso(valor), "Tiene que ser una fecha válida");
    }

    @Override
    public Optional<String> errorDeValorLeido(String valor) {
        return errorSi(!esFechaIso(valor), "Tiene que ser una fecha");
    }

    @Override
    public Optional<ClaseDeDatoInterpretable> claseInterpretable() {
        return Optional.of(ClaseDeDatoInterpretable.FECHA);
    }

    @Override
    public Object celdaDeReporte(Object valor, Function<RegistroId, String> etiquetaDe) {
        return LocalDate.parse(valor.toString());
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        boolean valida = FORMATO_ISO.matcher(valor).matches() && esFechaIso(valor);
        return errorSi(!valida, "Tiene que ser una fecha con el formato AAAA-MM-DD");
    }

    private static boolean esFechaIso(Object valor) {
        if (!(valor instanceof String texto)) {
            return false;
        }
        try {
            LocalDate.parse(texto);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
