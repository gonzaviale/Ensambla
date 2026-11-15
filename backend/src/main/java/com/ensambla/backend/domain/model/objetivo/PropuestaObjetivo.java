package com.ensambla.backend.domain.model.objetivo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lo que Sami le muestra a la persona antes de guardar: qué respondió, cómo entendió el objetivo y hasta 3
 * mini-objetivos. Es lo que queda de la respuesta de la IA después de validarla: una métrica fuera del catálogo se
 * descarta, dos que miden lo mismo también, una acción con cifras de plata no se acepta y no pasan de
 * {@link MiniObjetivo#MAXIMO_EN_CURSO}.
 */
public record PropuestaObjetivo(
        String respuesta, String entendido, boolean esMonetario, List<MiniObjetivoPropuesto> minis,
        OrigenPropuesta origen) {

    public static final int LARGO_MAXIMO_RESPUESTA = 600;

    public static final int LARGO_MAXIMO_ENTENDIDO = 300;

    /**
     * @throws IllegalStateException si la IA no dio una respuesta para la persona (vacía o más larga de lo permitido)
     */
    public static PropuestaObjetivo de(PropuestaObjetivoIa ia, ConsultaObjetivoIa consulta) {
        if (ia.respuesta() == null || ia.respuesta().isBlank()
                || ia.respuesta().strip().length() > LARGO_MAXIMO_RESPUESTA) {
            throw new IllegalStateException("La IA no dio una respuesta válida");
        }
        return new PropuestaObjetivo(
                ia.respuesta().strip(),
                entendido(ia.entendido()),
                ia.esMonetario(),
                ia.esMonetario() ? List.of() : validar(ia.elecciones(), consulta),
                OrigenPropuesta.IA);
    }

    /** Sin IA: un solo mini-objetivo manual para que la persona cuente cada semana cómo viene. */
    public static PropuestaObjetivo respaldo(String objetivoDeLaPersona) {
        return new PropuestaObjetivo(
                "Ahora no puedo armar los mini-objetivos con Sami. Podés guardar tu objetivo como seguimiento: "
                        + "una vez por semana me contás cómo te fue.",
                null,
                false,
                List.of(MiniObjetivoPropuesto.manual(AccionManual.seguimiento(objetivoDeLaPersona), false)),
                OrigenPropuesta.RESPALDO);
    }

    /** El tipo que tendría el objetivo al guardarlo; vacío si no hay mini-objetivos o es una meta en pesos. */
    public Optional<TipoObjetivo> tipo() {
        return puedeGuardar() ? Optional.of(TipoObjetivo.de(minis)) : Optional.empty();
    }

    /** Si hay algo para guardar: una meta en pesos va por el formulario de siempre. */
    public boolean puedeGuardar() {
        return !esMonetario && !minis.isEmpty();
    }

    private static String entendido(String texto) {
        boolean sirve = texto != null && !texto.isBlank() && texto.strip().length() <= LARGO_MAXIMO_ENTENDIDO;
        return sirve ? texto.strip() : null;
    }

    private static List<MiniObjetivoPropuesto> validar(List<EleccionIa> elecciones, ConsultaObjetivoIa consulta) {
        List<MiniObjetivoPropuesto> propuestos = new ArrayList<>();
        for (EleccionIa eleccion : elecciones) {
            if (propuestos.size() == MiniObjetivo.MAXIMO_EN_CURSO) {
                break;
            }
            resolver(eleccion, consulta, propuestos).ifPresent(propuestos::add);
        }
        return List.copyOf(propuestos);
    }

    private static Optional<MiniObjetivoPropuesto> resolver(
            EleccionIa eleccion, ConsultaObjetivoIa consulta, List<MiniObjetivoPropuesto> yaElegidos) {
        if (eleccion.modo() == ModoMiniObjetivo.MANUAL) {
            return AccionManual.de(eleccion.titulo(), eleccion.justificacion())
                    .map(accion -> MiniObjetivoPropuesto.manual(accion, true));
        }
        return consulta.candidato(eleccion.codigo())
                .filter(candidato -> yaElegidos.stream().noneMatch(elegido -> elegido.esMedido()
                        && candidato.chocaCon(elegido.candidato())))
                .map(MiniObjetivoPropuesto::medido);
    }
}
