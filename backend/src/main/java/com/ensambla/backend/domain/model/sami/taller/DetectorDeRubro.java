package com.ensambla.backend.domain.model.sami.taller;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reconoce el rubro de la persona sin gastar IA: busca los términos de cada kit en el mensaje, la charla reciente y el
 * nombre del sistema (con {@link TextoNormalizado}, así «Velas» y «vela» dan lo mismo). Un término de varias palabras
 * pesa más que uno suelto y lo que dice el mensaje pesa el doble que el resto; ante un empate gana el kit que figura
 * primero. Así a la IA sólo le llega el kit que importa.
 */
public final class DetectorDeRubro {

    private static final int PESO_DEL_MENSAJE = 2;

    /** Un kit con sus términos ya normalizados. */
    private record TerminosDeKit(KitRubro kit, List<List<String>> frases) {
    }

    private record KitPuntuado(KitRubro kit, int puntaje) {
    }

    private final List<TerminosDeKit> kits;

    public DetectorDeRubro(KitsRubro kitsRubro) {
        this.kits = kitsRubro.kits().stream()
                .map(kit -> new TerminosDeKit(kit, kit.terminos().stream().map(TextoNormalizado::palabras).toList()))
                .toList();
    }

    public Optional<KitRubro> detectar(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
        List<String> delMensaje = TextoNormalizado.palabras(Objects.requireNonNullElse(mensaje, ""));
        List<String> delResto = TextoNormalizado.palabras(restoDeLaCharla(estado, historial));
        return kits.stream()
                .map(kit -> new KitPuntuado(kit.kit(), puntaje(kit, delMensaje, delResto)))
                .filter(puntuado -> puntuado.puntaje() > 0)
                .reduce((mejor, otro) -> otro.puntaje() > mejor.puntaje() ? otro : mejor)
                .map(KitPuntuado::kit);
    }

    private String restoDeLaCharla(EstadoTaller estado, List<MensajeTaller> historial) {
        return historial.stream().map(MensajeTaller::texto).collect(Collectors.joining(" "))
                + " " + Objects.requireNonNullElse(estado.nombre(), "");
    }

    private int puntaje(TerminosDeKit kit, List<String> delMensaje, List<String> delResto) {
        return aciertos(kit.frases(), delMensaje) * PESO_DEL_MENSAJE + aciertos(kit.frases(), delResto);
    }

    /** Cada término encontrado suma sus palabras: «almacen natural» le gana a «almacen». */
    private int aciertos(List<List<String>> frases, List<String> palabras) {
        return frases.stream()
                .filter(frase -> TextoNormalizado.contiene(palabras, frase))
                .mapToInt(List::size)
                .sum();
    }
}
