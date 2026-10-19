package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * Valida lo que la IA pidió en un turno del taller. El modelo nunca escribe el sistema: pide herramientas con
 * vocabulario cerrado ({@link CatalogoTaller}) y acá se descarta todo lo que no exista, esté repetido o se pase de
 * los topes. Las llamadas se validan en orden sobre una copia del estado, así una herramienta puede apoyarse en una
 * pieza que se sumó un instante antes en el mismo turno.
 * <p>
 * En las acciones, {@code pieza} es el id de una pieza existente o, para una pieza nueva del turno, su tipo.
 */
@Service
public class HerramientasTallerService {

    private interface Manejador extends BiFunction<Map<String, Object>, List<PiezaTaller>, Optional<AccionTaller>> {
    }

    private final Map<HerramientaTaller, Manejador> manejadores = new EnumMap<>(HerramientaTaller.class);

    public HerramientasTallerService() {
        manejadores.put(HerramientaTaller.AGREGAR_PIEZA, this::agregarPieza);
        manejadores.put(HerramientaTaller.QUITAR_PIEZA, this::quitarPieza);
        manejadores.put(HerramientaTaller.RENOMBRAR_PIEZA, this::renombrarPieza);
        manejadores.put(HerramientaTaller.RENOMBRAR_SISTEMA, this::renombrarSistema);
        manejadores.put(HerramientaTaller.AGREGAR_CAMPO, this::agregarCampo);
        manejadores.put(HerramientaTaller.QUITAR_CAMPO, this::quitarCampo);
        manejadores.put(HerramientaTaller.SEPARAR_EN_SECCION, this::separarEnSeccion);
        manejadores.put(HerramientaTaller.CAMBIAR_DISTRIBUCION, this::cambiarDistribucion);
    }

    public ValidacionTaller validar(List<LlamadaHerramienta> llamadas, EstadoTaller estado) {
        List<PiezaTaller> piezas = new ArrayList<>(estado.piezas());
        List<AccionTaller> acciones = new ArrayList<>();
        OpcionesTaller opciones = null;
        int descartadas = 0;
        for (LlamadaHerramienta llamada : llamadas) {
            Optional<HerramientaTaller> herramienta = HerramientaTaller.deNombre(llamada.nombre());
            if (herramienta.isEmpty()) {
                descartadas++;
            } else if (herramienta.get() == HerramientaTaller.OFRECER_OPCIONES) {
                Optional<OpcionesTaller> pregunta = opciones(llamada.argumentos());
                opciones = pregunta.orElse(opciones);
                descartadas += pregunta.isPresent() ? 0 : 1;
            } else {
                Optional<AccionTaller> accion = acciones.size() < CatalogoTaller.MAX_ACCIONES
                        ? manejadores.get(herramienta.get()).apply(llamada.argumentos(), piezas)
                        : Optional.empty();
                accion.ifPresent(acciones::add);
                descartadas += accion.isPresent() ? 0 : 1;
            }
        }
        return new ValidacionTaller(List.copyOf(acciones), opciones, descartadas);
    }

    private Optional<AccionTaller> agregarPieza(Map<String, Object> args, List<PiezaTaller> piezas) {
        return clave(args.get("tipo"))
                .filter(CatalogoTaller.TIPOS_DE_PIEZA::containsKey)
                .filter(tipo -> piezas.stream().noneMatch(pieza -> pieza.tipo().equals(tipo)))
                .map(tipo -> {
                    String nombre = CatalogoTaller.TIPOS_DE_PIEZA.get(tipo);
                    piezas.add(new PiezaTaller(null, tipo, nombre, null, List.of()));
                    return accion(HerramientaTaller.AGREGAR_PIEZA, "tipo", tipo);
                });
    }

    private Optional<AccionTaller> quitarPieza(Map<String, Object> args, List<PiezaTaller> piezas) {
        return indiceDe(args.get("pieza"), piezas).map(indice -> {
            String referencia = referencia(piezas.remove((int) indice));
            return accion(HerramientaTaller.QUITAR_PIEZA, "pieza", referencia);
        });
    }

    private Optional<AccionTaller> renombrarPieza(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> nombre = texto(args.get("nombre"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> nombre.map(nuevo -> {
            PiezaTaller pieza = piezas.get(indice);
            piezas.set(indice, new PiezaTaller(pieza.id(), pieza.tipo(), nuevo, pieza.seccion(), pieza.campos()));
            return accionConPieza(HerramientaTaller.RENOMBRAR_PIEZA, pieza, "nombre", nuevo);
        }));
    }

    private Optional<AccionTaller> renombrarSistema(Map<String, Object> args, List<PiezaTaller> piezas) {
        return texto(args.get("nombre"), CatalogoTaller.MAX_LARGO_NOMBRE)
                .map(nombre -> accion(HerramientaTaller.RENOMBRAR_SISTEMA, "nombre", nombre));
    }

    private Optional<AccionTaller> agregarCampo(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> etiqueta = texto(args.get("etiqueta"), CatalogoTaller.MAX_LARGO_NOMBRE);
        Optional<String> tipo = clave(args.get("tipo")).filter(CatalogoTaller.TIPOS_DE_CAMPO::contains);
        List<String> opciones = textos(args.get("opciones"));
        boolean opcionesValidas = tipo.filter("select"::equals).isEmpty() || opciones.size() >= 2;
        return indiceDe(args.get("pieza"), piezas)
                .filter(indice -> etiqueta.isPresent() && tipo.isPresent() && opcionesValidas)
                .filter(indice -> hayLugar(piezas.get(indice)) && !tieneCampo(piezas.get(indice), etiqueta.get()))
                .map(indice -> {
                    PiezaTaller pieza = piezas.get(indice);
                    List<CampoTaller> campos = new ArrayList<>(pieza.campos());
                    campos.add(new CampoTaller(etiqueta.get(), tipo.get()));
                    piezas.set(indice, conCampos(pieza, campos));
                    AccionTaller accion = accionConPieza(
                            HerramientaTaller.AGREGAR_CAMPO, pieza, "etiqueta", etiqueta.get());
                    accion.argumentos().put("tipo", tipo.get());
                    if (tipo.get().equals("select")) {
                        accion.argumentos().put("opciones", opciones);
                    }
                    return accion;
                });
    }

    private Optional<AccionTaller> quitarCampo(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> campo = texto(args.get("campo"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> campo.flatMap(buscado -> {
            PiezaTaller pieza = piezas.get(indice);
            return pieza.campos().stream()
                    .filter(candidato -> candidato.etiqueta().equalsIgnoreCase(buscado))
                    .findFirst()
                    .map(existente -> {
                        List<CampoTaller> campos = new ArrayList<>(pieza.campos());
                        campos.remove(existente);
                        piezas.set(indice, conCampos(pieza, campos));
                        return accionConPieza(HerramientaTaller.QUITAR_CAMPO, pieza, "campo", existente.etiqueta());
                    });
        }));
    }

    private Optional<AccionTaller> separarEnSeccion(Map<String, Object> args, List<PiezaTaller> piezas) {
        Optional<String> seccion = texto(args.get("seccion"), CatalogoTaller.MAX_LARGO_NOMBRE);
        return indiceDe(args.get("pieza"), piezas).flatMap(indice -> seccion.map(nueva -> {
            PiezaTaller pieza = piezas.get(indice);
            piezas.set(indice, new PiezaTaller(pieza.id(), pieza.tipo(), pieza.nombre(), nueva, pieza.campos()));
            return accionConPieza(HerramientaTaller.SEPARAR_EN_SECCION, pieza, "seccion", nueva);
        }));
    }

    private Optional<AccionTaller> cambiarDistribucion(Map<String, Object> args, List<PiezaTaller> piezas) {
        return clave(args.get("layout"))
                .filter(CatalogoTaller.DISTRIBUCIONES::contains)
                .map(layout -> accion(HerramientaTaller.CAMBIAR_DISTRIBUCION, "layout", layout));
    }

    private Optional<OpcionesTaller> opciones(Map<String, Object> args) {
        List<String> opciones = textos(args.get("opciones")).stream()
                .limit(CatalogoTaller.MAX_OPCIONES)
                .toList();
        return texto(args.get("pregunta"), CatalogoTaller.MAX_LARGO_PREGUNTA)
                .filter(pregunta -> opciones.size() >= CatalogoTaller.MIN_OPCIONES)
                .map(pregunta -> new OpcionesTaller(pregunta, opciones));
    }

    private Optional<Integer> indiceDe(Object referencia, List<PiezaTaller> piezas) {
        return texto(referencia, CatalogoTaller.MAX_LARGO_NOMBRE).flatMap(buscada -> IntStream.range(0, piezas.size())
                .boxed()
                .filter(indice -> coincide(piezas.get(indice), buscada))
                .findFirst());
    }

    private boolean coincide(PiezaTaller pieza, String buscada) {
        return buscada.equals(pieza.id())
                || buscada.equalsIgnoreCase(pieza.nombre())
                || buscada.toLowerCase(Locale.ROOT).equals(pieza.tipo());
    }

    private boolean hayLugar(PiezaTaller pieza) {
        return pieza.campos().size() < CatalogoTaller.MAX_CAMPOS_POR_PIEZA;
    }

    private boolean tieneCampo(PiezaTaller pieza, String etiqueta) {
        return pieza.campos().stream().anyMatch(campo -> campo.etiqueta().equalsIgnoreCase(etiqueta));
    }

    private PiezaTaller conCampos(PiezaTaller pieza, List<CampoTaller> campos) {
        return new PiezaTaller(pieza.id(), pieza.tipo(), pieza.nombre(), pieza.seccion(), campos);
    }

    private String referencia(PiezaTaller pieza) {
        return pieza.id() == null ? pieza.tipo() : pieza.id();
    }

    private AccionTaller accion(HerramientaTaller herramienta, String argumento, String valor) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put(argumento, valor);
        return new AccionTaller(herramienta.nombre(), argumentos);
    }

    private AccionTaller accionConPieza(HerramientaTaller herramienta, PiezaTaller pieza, String argumento,
                                        String valor) {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("pieza", referencia(pieza));
        argumentos.put(argumento, valor);
        return new AccionTaller(herramienta.nombre(), argumentos);
    }

    private Optional<String> texto(Object valor, int maximo) {
        return Optional.ofNullable(valor)
                .filter(String.class::isInstance)
                .map(texto -> ((String) texto).strip())
                .filter(texto -> !texto.isEmpty() && texto.length() <= maximo);
    }

    private Optional<String> clave(Object valor) {
        return texto(valor, CatalogoTaller.MAX_LARGO_NOMBRE).map(texto -> texto.toLowerCase(Locale.ROOT));
    }

    private List<String> textos(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        return lista.stream()
                .flatMap(elemento -> texto(elemento, CatalogoTaller.MAX_LARGO_NOMBRE).stream())
                .distinct()
                .toList();
    }
}
