package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reconoce el rubro de la persona sin gastar IA: busca los términos de cada kit en el mensaje, la charla reciente y el
 * nombre del sistema (con {@link NormalizadorTexto}, así «Velas» y «vela» dan lo mismo). Un término de varias palabras
 * pesa más que uno suelto y lo que dice el mensaje pesa el doble que el resto; ante un empate gana el kit que figura
 * primero. Así a la IA sólo le llega el kit que importa.
 */
public class RubroTallerService {

    static final int PESO_DEL_MENSAJE = 2;

    /** Un kit con sus términos ya normalizados. */
    private record TerminosDeKit(KitRubro kit, List<List<String>> frases) {
    }

    private final List<TerminosDeKit> kits;

    public RubroTallerService(KitsRubro kitsRubro) {
        this.kits = kitsRubro.kits().stream()
                .map(kit -> new TerminosDeKit(kit, kit.terminos().stream().map(NormalizadorTexto::palabras).toList()))
                .toList();
    }

    public Optional<KitRubro> detectar(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
        List<String> delMensaje = NormalizadorTexto.palabras(Objects.requireNonNullElse(mensaje, ""));
        String resto = historial.stream().map(MensajeTaller::texto).collect(Collectors.joining(" "))
                + " " + Objects.requireNonNullElse(estado.nombre(), "");
        List<String> delResto = NormalizadorTexto.palabras(resto);
        KitRubro elegido = null;
        int mejor = 0;
        for (TerminosDeKit kit : kits) {
            int puntaje = aciertos(kit.frases(), delMensaje) * PESO_DEL_MENSAJE + aciertos(kit.frases(), delResto);
            if (puntaje > mejor) {
                mejor = puntaje;
                elegido = kit.kit();
            }
        }
        return Optional.ofNullable(elegido);
    }

    /** Cada término encontrado suma sus palabras: «almacen natural» le gana a «almacen». */
    private int aciertos(List<List<String>> frases, List<String> palabras) {
        return frases.stream()
                .filter(frase -> NormalizadorTexto.contiene(palabras, frase))
                .mapToInt(List::size)
                .sum();
    }
}
