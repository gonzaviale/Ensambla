package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BuscadorDeComerciosTest {

    private static final Comercio PEPE = comercio("Almacén Don Pepe");
    private static final Comercio ROSA = comercio("Almacén Doña Rosa");
    private static final Comercio KIOSCO = comercio("Kiosco Sol");
    private static final List<Comercio> COMERCIOS = List.of(PEPE, ROSA, KIOSCO);

    @Test
    @DisplayName("El nombre completo, sin importar tildes ni mayúsculas, encuentra sólo ese comercio")
    void buscarShouldFindTheExactName() {
        assertThat(BuscadorDeComercios.buscar("ALMACEN DON PEPE", COMERCIOS)).containsExactly(PEPE);
    }

    @Test
    @DisplayName("Parte del nombre que sólo comparte un comercio lo encuentra")
    void buscarShouldFindByPartOfTheName() {
        assertThat(BuscadorDeComercios.buscar("don pepe", COMERCIOS)).containsExactly(PEPE);
        assertThat(BuscadorDeComercios.buscar("kiosko sol", COMERCIOS)).containsExactly(KIOSCO);
    }

    @Test
    @DisplayName("Una palabra que comparten varios comercios los devuelve a todos para que elija")
    void buscarShouldReturnEverySimilarBusiness() {
        assertThat(BuscadorDeComercios.buscar("almacén", COMERCIOS)).containsExactly(PEPE, ROSA);
    }

    @Test
    @DisplayName("El nombre exacto gana aunque otros comercios se le parezcan")
    void buscarShouldPreferTheExactName() {
        Comercio sol = comercio("Sol");
        Comercio solNaciente = comercio("Sol Naciente");

        assertThat(BuscadorDeComercios.buscar("sol", List.of(solNaciente, sol))).containsExactly(sol);
    }

    @Test
    @DisplayName("Si ninguno se parece devuelve vacío")
    void buscarShouldReturnEmptyWhenNothingMatches() {
        assertThat(BuscadorDeComercios.buscar("panadería luna", COMERCIOS)).isEmpty();
        assertThat(BuscadorDeComercios.buscar("hola", List.of())).isEmpty();
    }

    @Test
    @DisplayName("Nunca devuelve más comercios de los que se pueden elegir con un número de una cifra")
    void buscarShouldCapTheOptions() {
        List<Comercio> muchos = IntStream.rangeClosed(1, 15).mapToObj(i -> comercio("Almacén " + i)).toList();

        assertThat(BuscadorDeComercios.buscar("almacén", muchos)).hasSize(BuscadorDeComercios.MAX_OPCIONES);
    }

    private static Comercio comercio(String nombre) {
        return new Comercio(NegocioId.de(UUID.randomUUID()), nombre);
    }
}
