package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.util.Optional;
import java.util.function.Function;

/** Campo que apunta a una fila de otra tabla; su valor es el id de esa fila. */
public class CampoRelacion extends Campo {

    private static final String FILA_ELIMINADA = "Fila eliminada";

    private final BloqueId bloqueRelacionadoId;

    public CampoRelacion(CampoBuilder datos) {
        super(datos);
        this.bloqueRelacionadoId = datos.bloqueRelacionadoId;
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.RELACION;
    }

    @Override
    public BloqueId getBloqueRelacionadoId() {
        return bloqueRelacionadoId;
    }

    @Override
    public boolean esRelacion() {
        return true;
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        return errorSi(!esIdDeFila(valor), "Elegí una fila de la tabla relacionada");
    }

    @Override
    public Object celdaDeReporte(Object valor, Function<RegistroId, String> etiquetaDe) {
        return Optional.ofNullable(etiquetaDe.apply(RegistroId.de(valor.toString()))).orElse(FILA_ELIMINADA);
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        return Optional.of("Una conexión no tiene valor inicial");
    }

    @Override
    protected Optional<String> errorDeBloqueRelacionado() {
        return errorSi(bloqueRelacionadoId == null, "Elegí la tabla con la que se conecta");
    }

    /** El valor de una conexión es el id del registro referenciado, en su forma canónica de 36 caracteres. */
    private static boolean esIdDeFila(Object valor) {
        if (!(valor instanceof String texto)) {
            return false;
        }
        try {
            return RegistroId.de(texto).toString().equalsIgnoreCase(texto);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
