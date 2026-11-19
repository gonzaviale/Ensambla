package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Empareja lo que la persona nombró con las filas que el negocio ya tiene, para actualizarlas o vincularlas. */
@RequiredArgsConstructor
final class EmparejamientoDeFilas {

    private static final int MAX_NOMBRES_POR_TABLA = 100;

    private final RegistroRepository registroRepository;

    /** Las filas listas para proponer: emparejadas con las que ya existen y con sus vínculos resueltos. */
    List<FilaPropuesta> armar(List<FilaPropuesta> filas, TablaDisponible tabla, List<TablaDisponible> tablas,
                              NegocioId negocioId) {
        return conVinculos(conExistentes(filas, tabla, negocioId), tabla, tablas, negocioId);
    }

    /**
     * Los nombres que la IA tiene que conocer para no inventar otro: los de las tablas de cosas y los de las tablas a
     * las que apunta algún vínculo.
     */
    Map<BloqueId, List<String>> nombresConocidos(List<TablaDisponible> tablas, NegocioId negocioId) {
        Set<BloqueId> vinculadas = tablasConVinculosHacia(tablas);
        Map<BloqueId, List<String>> nombres = new LinkedHashMap<>();
        for (TablaDisponible tabla : tablas) {
            List<String> deLaTabla = nombresDe(tabla, vinculadas, negocioId);
            if (!deLaTabla.isEmpty()) {
                nombres.put(tabla.bloque().id(), deLaTabla);
            }
        }
        return nombres;
    }

    private List<String> nombresDe(TablaDisponible tabla, Set<BloqueId> vinculadas, NegocioId negocioId) {
        Optional<CampoConocido> identificador = tabla.campoIdentificador();
        if (identificador.isEmpty() || !seNombraEnElChat(tabla, vinculadas)) {
            return List.of();
        }
        return nombradas(tabla, identificador.get(), negocioId).nombres().stream()
                .distinct().limit(MAX_NOMBRES_POR_TABLA).toList();
    }

    private static boolean seNombraEnElChat(TablaDisponible tabla, Set<BloqueId> vinculadas) {
        return tabla.actualizaFilasExistentes() || vinculadas.contains(tabla.bloque().id());
    }

    private static Set<BloqueId> tablasConVinculosHacia(List<TablaDisponible> tablas) {
        Set<BloqueId> vinculadas = new HashSet<>();
        tablas.forEach(tabla -> tabla.bloque().campos().stream()
                .filter(CampoConocido::vinculaConOtraTabla)
                .forEach(campo -> vinculadas.add(campo.bloqueRelacionadoId())));
        return vinculadas;
    }

    private FilasNombradas nombradas(TablaDisponible tabla, CampoConocido identificador, NegocioId negocioId) {
        String clave = identificador.id().toString();
        List<Registro> registros = buscarFilasConNombre(tabla, clave, negocioId);
        return new FilasNombradas(registros,
                registros.stream().map(registro -> registro.getValores().get(clave).toString()).toList());
    }

    private List<Registro> buscarFilasConNombre(TablaDisponible tabla, String clave, NegocioId negocioId) {
        return registroRepository.findAllByBloqueIdAndNegocioId(tabla.bloque().id(), negocioId).stream()
                .filter(registro -> registro.getValores().get(clave) != null)
                .toList();
    }

    /**
     * En las tablas de cosas (productos, clientes) una fila que nombra algo que ya existe lo actualiza, aunque la
     * persona no lo haya escrito tal cual. Si no encuentra una fila clara, queda como fila nueva.
     */
    private List<FilaPropuesta> conExistentes(List<FilaPropuesta> filas, TablaDisponible tabla, NegocioId negocioId) {
        Optional<CampoConocido> identificador = tabla.campoIdentificador();
        if (!tabla.actualizaFilasExistentes() || identificador.isEmpty()) {
            return filas;
        }
        String clave = identificador.get().id().toString();
        FilasNombradas existentes = nombradas(tabla, identificador.get(), negocioId);
        return filas.stream().map(fila -> emparejar(fila, clave, existentes)).toList();
    }

    private static FilaPropuesta emparejar(FilaPropuesta fila, String clave, FilasNombradas existentes) {
        String nombre = fila.valores().get(clave);
        if (nombre == null || !fila.cambiaAlgoAdemasDelNombre(clave)) {
            return fila;
        }
        return existentes.buscar(nombre)
                .map(posicion -> existentes.actualizando(fila, posicion, clave))
                .orElse(fila);
    }

    /**
     * Cada campo que se vincula (el Producto o el Cliente de una venta) trae un nombre: se busca en la tabla a la que
     * apunta, aunque no esté escrito tal cual. Si no está, se propone sumarlo. Si esa tabla no tiene con qué nombrar
     * sus filas, el valor no se carga.
     */
    private List<FilaPropuesta> conVinculos(List<FilaPropuesta> filas, TablaDisponible tabla,
                                            List<TablaDisponible> tablas, NegocioId negocioId) {
        List<FilaPropuesta> resultado = filas;
        for (CampoConocido campo : tabla.bloque().campos()) {
            if (campo.esRelacion()) {
                resultado = vincularCampo(resultado, campo, tablas, negocioId);
            }
        }
        return resultado;
    }

    private List<FilaPropuesta> vincularCampo(List<FilaPropuesta> filas, CampoConocido campo,
                                              List<TablaDisponible> tablas, NegocioId negocioId) {
        String clave = campo.id().toString();
        if (filas.stream().noneMatch(fila -> fila.valores().containsKey(clave))) {
            return filas;
        }
        Optional<TablaDisponible> destino = tablaDestino(tablas, campo);
        Optional<CampoConocido> identificador = destino.flatMap(TablaDisponible::campoIdentificador);
        if (identificador.isEmpty()) {
            return filas.stream().map(fila -> fila.sinValor(clave)).toList();
        }
        FilasNombradas nombradas = nombradas(destino.get(), identificador.get(), negocioId);
        return filas.stream().map(fila -> vincular(fila, clave, destino.get(), nombradas)).toList();
    }

    private static Optional<TablaDisponible> tablaDestino(List<TablaDisponible> tablas, CampoConocido campo) {
        return tablas.stream()
                .filter(otra -> otra.bloque().id().equals(campo.bloqueRelacionadoId()))
                .findFirst();
    }

    private static FilaPropuesta vincular(FilaPropuesta fila, String clave, TablaDisponible destino,
                                          FilasNombradas nombradas) {
        String nombre = fila.valores().get(clave);
        if (nombre == null) {
            return fila;
        }
        return fila.vinculando(clave, vinculoCon(nombre, destino, nombradas));
    }

    private static VinculoPropuesto vinculoCon(String nombre, TablaDisponible destino, FilasNombradas nombradas) {
        return nombradas.buscar(nombre)
                .map(posicion -> nombradas.vinculoEn(posicion, destino))
                .orElseGet(() -> vinculoNuevo(nombre, destino));
    }

    private static VinculoPropuesto vinculoNuevo(String nombre, TablaDisponible destino) {
        return new VinculoPropuesto(nombre.strip(), null, destino.bloque().id().toString(), destino.bloque().nombre());
    }
}
