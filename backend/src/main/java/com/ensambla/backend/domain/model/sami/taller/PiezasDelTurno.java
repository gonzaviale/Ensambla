package com.ensambla.backend.domain.model.sami.taller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** Las piezas del sistema tal como van quedando durante un turno, para que cada herramienta se apoye en las anteriores. */
public final class PiezasDelTurno {

    private final List<PiezaTaller> piezas;

    public PiezasDelTurno(List<PiezaTaller> piezas) {
        this.piezas = new ArrayList<>(piezas);
    }

    /** Una pieza se busca por id, después por tipo y por último por nombre (sin importar mayúsculas, tildes ni plural). */
    public Optional<Integer> indiceDe(ValorDeArgumento referencia) {
        return referencia.texto(CatalogoTaller.MAX_LARGO_NOMBRE).flatMap(this::indiceDeLaPieza);
    }

    public PiezaTaller en(int indice) {
        return piezas.get(indice);
    }

    public void reemplazar(int indice, PiezaTaller pieza) {
        piezas.set(indice, pieza);
    }

    public void sumar(PiezaTaller pieza) {
        piezas.add(pieza);
    }

    public PiezaTaller quitar(int indice) {
        return piezas.remove(indice);
    }

    public boolean tieneDelTipo(String tipo) {
        return piezas.stream().anyMatch(pieza -> pieza.tipo().equals(tipo));
    }

    public boolean esLaUnicaDelTipo(PiezaTaller pieza) {
        return piezas.stream().filter(otra -> otra.tipo().equals(pieza.tipo())).count() == 1;
    }

    public boolean tieneNombre(String nombre) {
        return piezas.stream().anyMatch(pieza -> pieza.seLlama(nombre));
    }

    public List<PiezaTaller> comoLista() {
        return List.copyOf(piezas);
    }

    private Optional<Integer> indiceDeLaPieza(String buscada) {
        return primera(pieza -> buscada.equals(pieza.id()))
                .or(() -> primera(pieza -> buscada.toLowerCase(Locale.ROOT).equals(pieza.tipo())))
                .or(() -> primera(pieza -> pieza.seLlama(buscada)));
    }

    private Optional<Integer> primera(Predicate<PiezaTaller> criterio) {
        return IntStream.range(0, piezas.size())
                .boxed()
                .filter(indice -> criterio.test(piezas.get(indice)))
                .findFirst();
    }
}
