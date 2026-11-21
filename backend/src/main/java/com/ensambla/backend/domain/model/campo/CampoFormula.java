package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Campo que se calcula solo a partir de los campos numéricos de su tabla. */
public class CampoFormula extends Campo {

    private final String formula;

    public CampoFormula(CampoBuilder datos) {
        super(datos);
        this.formula = datos.formula;
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.FORMULA;
    }

    @Override
    public String getFormula() {
        return formula;
    }

    @Override
    public boolean esFormula() {
        return true;
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        return Optional.of("Este dato se calcula solo");
    }

    @Override
    public Object celdaDeReporte(Object valor, Function<RegistroId, String> etiquetaDe) {
        return new BigDecimal(valor.toString());
    }

    @Override
    public Optional<String> errorDeValorLeido(String valor) {
        return Optional.of("La lectura no propone fórmulas");
    }

    /** Valor con los datos de un registro; {@code null} si falta algún dato. */
    public BigDecimal calcular(Function<CampoId, BigDecimal> valorDe) {
        return Formula.parsear(formula).evaluar(valorDe);
    }

    /**
     * Una fórmula sólo puede usar campos numéricos del mismo bloque; como no puede citar a otra fórmula, tampoco puede
     * haber referencias circulares.
     */
    @Override
    public void validarReferenciasDeFormula(List<Campo> camposDelBloque) {
        if (!camposNumericosDe(camposDelBloque).containsAll(Formula.parsear(formula).referencias())) {
            throw new CampoInvalidoException(Map.of("formula", "Usá sólo campos numéricos de esta tabla"));
        }
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        return Optional.of("Una fórmula no tiene valor inicial");
    }

    /** Acá se valida la sintaxis; que los campos existan y sean numéricos lo controla la tabla. */
    @Override
    protected Optional<String> errorDeFormula() {
        try {
            Formula.parsear(formula);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(e.getMessage());
        }
    }

    private Set<CampoId> camposNumericosDe(List<Campo> camposDelBloque) {
        return camposDelBloque.stream()
                .filter(otro -> otro.getTipo() == TipoCampo.NUMERO && !otro.getId().equals(getId()))
                .map(Campo::getId)
                .collect(Collectors.toSet());
    }
}
