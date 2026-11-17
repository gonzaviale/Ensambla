package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Un dato de una tabla. Cada tipo de campo es una subclase que sabe validar y tipar sus propios valores. */
@Getter
@Setter
public abstract class Campo {

    private CampoId id;

    private NegocioId negocioId;

    private BloqueId bloqueId;

    private String nombre;

    private Boolean obligatorio;

    private String valorInicial;

    private String clave;

    private BigDecimal minimo;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    protected Campo(CampoBuilder datos) {
        this.id = datos.id;
        this.negocioId = datos.negocioId;
        this.bloqueId = datos.bloqueId;
        this.nombre = datos.nombre;
        this.obligatorio = datos.obligatorio;
        this.valorInicial = datos.valorInicial;
        this.clave = datos.clave;
        this.minimo = datos.minimo;
        this.posicion = datos.posicion;
        this.creadoEn = datos.creadoEn;
        this.actualizadoEn = datos.actualizadoEn;
    }

    public static CampoBuilder builder() {
        return new CampoBuilder();
    }

    public abstract TipoCampo tipo();

    public abstract Optional<String> errorDe(Object valor);

    protected abstract Optional<String> errorDeValorInicial(String valor);

    public TipoCampo getTipo() {
        return tipo();
    }

    public List<String> getOpciones() {
        return List.of();
    }

    public BloqueId getBloqueRelacionadoId() {
        return null;
    }

    public String getFormula() {
        return null;
    }

    public boolean esObligatorio() {
        return Boolean.TRUE.equals(obligatorio);
    }

    public boolean esNumerico() {
        return false;
    }

    public boolean esRelacion() {
        return false;
    }

    public boolean esFormula() {
        return false;
    }

    /** Si el texto que leyó la IA no sirve para este tipo de campo; vacío si sirve. */
    public Optional<String> errorDeValorLeido(String valor) {
        return Optional.empty();
    }

    /** Qué clase de dato de un mensaje libre puede completar este campo; vacío si ninguna. */
    public Optional<ClaseDeDatoInterpretable> claseInterpretable() {
        return Optional.empty();
    }

    /** El valor como se guarda en un registro; lo que no se puede convertir queda como texto. */
    public Object tipar(String valor) {
        return valor;
    }

    /** El valor como va en una celda del reporte; {@code etiquetaDe} nombra la fila a la que apunta una conexión. */
    public Object celdaDeReporte(Object valor, Function<RegistroId, String> etiquetaDe) {
        return valor.toString();
    }

    public Campo ubicadoEn(NegocioId negocioId, BloqueId bloqueId, short posicion, OffsetDateTime ahora) {
        this.negocioId = negocioId;
        this.bloqueId = bloqueId;
        this.posicion = posicion;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
        return this;
    }

    /** Este campo toma el lugar de {@code anterior}: conserva su id, su bloque, su posición y su fecha de creación. */
    public Campo reemplazandoA(Campo anterior, OffsetDateTime ahora) {
        this.id = anterior.id;
        this.negocioId = anterior.negocioId;
        this.bloqueId = anterior.bloqueId;
        this.posicion = anterior.posicion;
        this.clave = anterior.clave;
        this.minimo = anterior.minimo;
        this.creadoEn = anterior.creadoEn;
        this.actualizadoEn = ahora;
        return this;
    }

    public Map<String, String> erroresDeConfiguracion() {
        Map<String, String> errores = new LinkedHashMap<>();
        errorDeOpciones().ifPresent(error -> errores.put("opciones", error));
        errorDelValorInicialConfigurado().ifPresent(error -> errores.put("valorInicial", error));
        errorDeBloqueRelacionado().ifPresent(error -> errores.put("bloqueRelacionadoId", error));
        errorDeFormula().ifPresent(error -> errores.put("formula", error));
        return errores;
    }

    public void validarReferenciasDeFormula(List<Campo> camposDelBloque) {
    }

    /** Al reemplazar el campo por otro tipo no puede quedar una fórmula apoyada en él. */
    public void validarCambioDeTipo(List<Campo> camposDelBloque) {
        validarSinFormulasQueLoUsen(camposDelBloque, "cambiar el tipo de");
    }

    /** No se puede dejar sin uso o cambiar un campo numérico del que depende una fórmula. */
    public void validarSinFormulasQueLoUsen(List<Campo> camposDelBloque, String accion) {
        camposDelBloque.stream()
                .filter(Campo::esFormula)
                .filter(otro -> Formula.parsear(otro.getFormula()).referencias().contains(id))
                .findFirst()
                .ifPresent(otro -> {
                    throw new BusinessRuleException("No se puede " + accion + " «" + nombre
                            + "»: la fórmula «" + otro.getNombre() + "» lo usa");
                });
    }

    protected Optional<String> errorDeOpciones() {
        return Optional.empty();
    }

    protected Optional<String> errorDeBloqueRelacionado() {
        return Optional.empty();
    }

    protected Optional<String> errorDeFormula() {
        return Optional.empty();
    }

    protected static Optional<String> errorSi(boolean invalido, String mensaje) {
        return invalido ? Optional.of(mensaje) : Optional.empty();
    }

    private Optional<String> errorDelValorInicialConfigurado() {
        return Optional.ofNullable(valorInicial).flatMap(this::errorDeValorInicial);
    }
}
