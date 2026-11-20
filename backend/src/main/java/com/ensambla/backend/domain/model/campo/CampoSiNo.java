package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class CampoSiNo extends Campo {

    private static final Map<String, Object> RESPUESTAS = Map.of(
            "true", Boolean.TRUE,
            "si", Boolean.TRUE,
            "sí", Boolean.TRUE,
            "false", Boolean.FALSE,
            "no", Boolean.FALSE);

    public CampoSiNo(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.SI_NO;
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        return errorSi(!(valor instanceof Boolean), "Tiene que ser sí o no");
    }

    @Override
    public boolean admiteFiltroPorValor() {
        return true;
    }

    @Override
    public boolean admiteFiltrarPor(String valor) {
        return "true".equals(valor) || "false".equals(valor);
    }

    @Override
    public Optional<String> errorDeValorLeido(String valor) {
        return errorDeValorInicial(valor);
    }

    @Override
    public Object tipar(String valor) {
        return RESPUESTAS.getOrDefault(valor.toLowerCase(Locale.ROOT), valor);
    }

    @Override
    public Object celdaDeReporte(Object valor, Function<RegistroId, String> etiquetaDe) {
        return Boolean.TRUE.equals(valor) ? "Sí" : "No";
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        boolean valido = "true".equals(valor) || "false".equals(valor);
        return errorSi(!valido, "Tiene que ser true (sí) o false (no)");
    }
}
