package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Campo que guarda un número. */
public abstract class CampoNumerico extends Campo {

    private static final Pattern NUMERO = Pattern.compile("-?\\d+(\\.\\d+)?");

    protected CampoNumerico(CampoBuilder datos) {
        super(datos);
    }

    /** El número que dice el texto, con coma o punto decimal; si no es un número devuelve el mismo texto. */
    public static Object interpretar(String texto) {
        try {
            return new BigDecimal(texto.replace(',', '.').replaceAll("\\s", ""));
        } catch (NumberFormatException e) {
            return texto;
        }
    }

    @Override
    public boolean esNumerico() {
        return true;
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        return errorSi(!esNumeroFinito(valor), "Tiene que ser un número");
    }

    @Override
    public Optional<String> errorDeValorLeido(String valor) {
        return errorSi(!esNumeroLeido(valor), "Tiene que ser un número");
    }

    @Override
    public Optional<ClaseDeDatoInterpretable> claseInterpretable() {
        return Optional.of(ClaseDeDatoInterpretable.NUMERO);
    }

    @Override
    public Object tipar(String valor) {
        return interpretar(valor);
    }

    @Override
    public Object celdaDeReporte(Object valor, Function<RegistroId, String> etiquetaDe) {
        return new BigDecimal(valor.toString());
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        return errorSi(!NUMERO.matcher(valor).matches(), "Tiene que ser un número, por ejemplo 12.5");
    }

    private static boolean esNumeroLeido(String valor) {
        try {
            new BigDecimal(valor);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean esNumeroFinito(Object valor) {
        return valor instanceof Number numero && Double.isFinite(numero.doubleValue());
    }
}
