package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.EstadoPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.RespuestaChat;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Atiende, de a uno y en orden, los mensajes que llegaron por WhatsApp. Un texto o una foto se convierte en una
 * propuesta de filas que se muestra por el chat y recién se guarda cuando la persona responde que sí. Los mismos
 * límites que en la app: las fotos descuentan del cupo de páginas del plan y los textos de las consultas diarias de
 * Sami. Tiene muchas dependencias a propósito: ningún caso de uso llama a otro, así que los pasos viven acá y la
 * lógica pura en el modelo.
 */
@RequiredArgsConstructor
public class AtenderMensajeWhatsapp {

    private static final int MAX_OPCIONES = 9;
    private static final String MOTIVO_PERDIDO = "Se perdió mientras se atendía";
    private static final Set<TipoCampo> NO_CARGABLES = Set.of(TipoCampo.FORMULA, TipoCampo.RELACION);

    private final MensajeWhatsappRepository mensajeRepository;
    private final VinculoWhatsappRepository vinculoRepository;
    private final PropuestaWhatsappRepository propuestaRepository;
    private final ProcesamientoRepository procesamientoRepository;
    private final CampoRepository campoRepository;
    private final BloqueRepository bloqueRepository;
    private final RegistroRepository registroRepository;
    private final ControlPlanService controlPlanService;
    private final ConsumoSamiRepository consumoSamiRepository;
    private final InterpreteRegistrosIaPort interpreteIa;
    private final LectorCuadernoPort lectorCuadernoPort;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;
    private final PoliticaAtencionWhatsapp politica;

    /** @return si había un mensaje esperando; quien lo llama insiste mientras haya */
    public boolean execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        recuperarPerdidos(ahora);
        Optional<MensajeWhatsapp> reclamado = mensajeRepository.reclamarSiguiente(ahora, politica.vigenciaReclamo());
        reclamado.ifPresent(this::atender);
        return reclamado.isPresent();
    }

    /** Un mensaje reclamado que nunca terminó (se cayó el proceso): no se reintenta, para no cobrar dos veces. */
    private void recuperarPerdidos(OffsetDateTime ahora) {
        for (MensajeWhatsapp perdido : mensajeRepository.vencidos(ahora)) {
            perdido.fallar(MOTIVO_PERDIDO, ahora);
            mensajeRepository.save(perdido);
            mensajeria.enviarTexto(perdido.getTelefono(), TextosWhatsapp.ERROR_TEMPORAL);
        }
    }

    private void atender(MensajeWhatsapp mensaje) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        try {
            Optional<VinculoWhatsapp> vinculo = vinculoRepository.findByTelefono(mensaje.getTelefono());
            if (vinculo.isPresent()) {
                despachar(mensaje, vinculo.get().getNegocioId(), ahora);
            } else {
                responder(mensaje, TextosWhatsapp.NO_VINCULADO);
            }
            mensaje.atender(ahora);
        } catch (BusinessRuleException e) {
            responder(mensaje, e.getMessage());
            mensaje.atender(ahora);
        } catch (RuntimeException e) {
            responder(mensaje, TextosWhatsapp.ERROR_TEMPORAL);
            mensaje.fallar(e.getClass().getSimpleName() + ": " + e.getMessage(), ahora);
        }
        mensajeRepository.save(mensaje);
    }

    private void despachar(MensajeWhatsapp mensaje, NegocioId negocioId, OffsetDateTime ahora) {
        Optional<PropuestaWhatsapp> abierta = propuestaRepository.findAbiertaByTelefono(mensaje.getTelefono());
        if (abierta.isPresent() && !abierta.get().estaVigente(ahora)) {
            abierta.get().vencer(ahora);
            propuestaRepository.save(abierta.get());
            if (esRespuesta(mensaje)) {
                responder(mensaje, TextosWhatsapp.VENCIDA);
                return;
            }
            abierta = Optional.empty();
        }
        if (mensaje.getTipo() == TipoMensajeWhatsapp.AUDIO) {
            responder(mensaje, TextosWhatsapp.SOLO_TEXTO_Y_FOTOS);
        } else if (mensaje.getTipo() == TipoMensajeWhatsapp.IMAGEN) {
            abierta.ifPresent(propuesta -> descartarAnterior(propuesta, mensaje, ahora));
            leerFoto(mensaje, negocioId, ahora);
        } else {
            atenderTexto(mensaje, negocioId, abierta, ahora);
        }
    }

    private static boolean esRespuesta(MensajeWhatsapp mensaje) {
        return mensaje.getTipo() == TipoMensajeWhatsapp.TEXTO
                && RespuestaChat.de(mensaje.getTexto()) != RespuestaChat.OTRO;
    }

    // --- texto ---

    private void atenderTexto(MensajeWhatsapp mensaje, NegocioId negocioId, Optional<PropuestaWhatsapp> abierta,
                              OffsetDateTime ahora) {
        RespuestaChat respuesta = RespuestaChat.de(mensaje.getTexto());
        if (abierta.isPresent()) {
            PropuestaWhatsapp propuesta = abierta.get();
            if (respuesta == RespuestaChat.NO) {
                descartar(propuesta, mensaje, ahora);
                return;
            }
            if (respuesta == RespuestaChat.SI) {
                confirmar(propuesta, mensaje, negocioId, ahora);
                return;
            }
            if (respuesta == RespuestaChat.NUMERO && propuesta.getEstado() == EstadoPropuestaWhatsapp.ELIGIENDO_TABLA) {
                elegirTabla(propuesta, mensaje, negocioId, ahora);
                return;
            }
            descartarAnterior(propuesta, mensaje, ahora);
        }
        interpretarTexto(mensaje, negocioId, ahora);
    }

    private void interpretarTexto(MensajeWhatsapp mensaje, NegocioId negocioId, OffsetDateTime ahora) {
        List<TablaDisponible> tablas = tablas(negocioId);
        InterpretacionRegistros interpretacion = interpretar(negocioId, mensaje.getTexto(), tablas, null, ahora);
        if (interpretacion.bloqueId() == null) {
            responder(mensaje, TextosWhatsapp.NO_ENTENDI);
            return;
        }
        TablaDisponible tabla = tablaDe(tablas, interpretacion.bloqueId());
        List<FilaPropuesta> filas = filasDeTexto(interpretacion, tabla);
        if (filas.isEmpty()) {
            responder(mensaje, TextosWhatsapp.NO_ENTENDI);
        } else if (interpretacion.entendida(politica.umbralConfianza())) {
            PropuestaWhatsapp propuesta = propuestaRepository.save(PropuestaWhatsapp.pendiente(negocioId,
                    mensaje.getTelefono(), OrigenPropuestaWhatsapp.TEXTO, mensaje.getTexto(),
                    tabla.bloque().id(), filas, ahora));
            responder(mensaje, TextosWhatsapp.resumen(propuesta, tabla));
        } else {
            preguntarTabla(mensaje, negocioId, OrigenPropuestaWhatsapp.TEXTO, null, tablas, tabla, ahora);
        }
    }

    private InterpretacionRegistros interpretar(NegocioId negocioId, String texto, List<TablaDisponible> tablas,
                                                BloqueId destino, OffsetDateTime ahora) {
        LocalDate dia = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!consumoSamiRepository.reservar(negocioId, dia, politica.limiteDiarioSami(), ahora)) {
            throw new BusinessRuleException(TextosWhatsapp.LIMITE_SAMI);
        }
        InterpretacionRegistros interpretacion = interpreteIa.interpretar(
                new ConsultaInterpretacion(texto, tablas, dia, destino));
        if (interpretacion.consumo() != null) {
            consumoSamiRepository.registrar(negocioId, dia, interpretacion.consumo(), ahora);
        }
        return interpretacion;
    }

    // --- foto ---

    private void leerFoto(MensajeWhatsapp mensaje, NegocioId negocioId, OffsetDateTime ahora) {
        List<TablaDisponible> tablas = tablas(negocioId);
        ImagenCuaderno imagen = ImagenCuaderno.de(descargar(mensaje).contenido());
        controlPlanService.reservarPaginaIaInmediata(negocioId, politica.limiteDiarioPaginas(), ahora);
        responder(mensaje, TextosWhatsapp.LEYENDO_FOTO);
        List<BloqueLectura> bloques = tablas.stream().map(TablaDisponible::bloque).toList();
        BloqueId destino = destinoDelPie(mensaje.getTexto(), tablas);
        List<BloqueConocido> conocidos = bloques.stream()
                .filter(bloque -> destino == null || bloque.id().equals(destino))
                .map(bloque -> bloque.paraElLector(bloques))
                .toList();
        LecturaCuaderno lectura = lectorCuadernoPort.leer(List.of(imagen.comoPagina()), conocidos, destino);
        PropuestaLectura propuesta = PropuestaLectura.proponer(lectura, bloques, destino);
        Optional<BloqueId> elegida = propuesta.lectura().bloque().bloqueExistente();
        if (elegida.isEmpty()) {
            preguntarTabla(mensaje, negocioId, OrigenPropuestaWhatsapp.FOTO, lectura, tablas, null, ahora);
            return;
        }
        proponerDeFoto(mensaje, negocioId, tablaDe(tablas, elegida.get()), propuesta, ahora);
    }

    /** Se baja antes de reservar la página: si no se puede bajar, no se cobra nada. */
    private ArchivoWhatsapp descargar(MensajeWhatsapp mensaje) {
        try {
            return mensajeria.descargarMedia(mensaje.getIdExterno());
        } catch (RuntimeException e) {
            throw new BusinessRuleException(TextosWhatsapp.FOTO_NO_DESCARGADA);
        }
    }

    private void proponerDeFoto(MensajeWhatsapp mensaje, NegocioId negocioId, TablaDisponible tabla,
                                PropuestaLectura propuesta, OffsetDateTime ahora) {
        List<FilaPropuesta> filas = filasDeFoto(propuesta, tabla);
        if (filas.isEmpty()) {
            responder(mensaje, TextosWhatsapp.NO_ENTENDI);
            return;
        }
        PropuestaWhatsapp pendiente = propuestaRepository.save(PropuestaWhatsapp.pendiente(negocioId,
                mensaje.getTelefono(), OrigenPropuestaWhatsapp.FOTO, mensaje.getTexto(), tabla.bloque().id(), filas,
                ahora));
        responder(mensaje, TextosWhatsapp.resumen(pendiente, tabla));
    }

    /** Si el pie de la foto nombra una sola tabla, va a esa. */
    private static BloqueId destinoDelPie(String pie, List<TablaDisponible> tablas) {
        if (pie == null || pie.isBlank()) {
            return null;
        }
        String buscado = TextoNormalizado.de(pie);
        List<TablaDisponible> nombradas = tablas.stream()
                .filter(tabla -> buscado.contains(TextoNormalizado.de(tabla.bloque().nombre())))
                .toList();
        return nombradas.size() == 1 ? nombradas.getFirst().bloque().id() : null;
    }

    // --- elegir tabla ---

    private void preguntarTabla(MensajeWhatsapp mensaje, NegocioId negocioId, OrigenPropuestaWhatsapp origen,
                                LecturaCuaderno lectura, List<TablaDisponible> tablas, TablaDisponible sugerida,
                                OffsetDateTime ahora) {
        List<TablaDisponible> ofrecidas = Stream.concat(Stream.ofNullable(sugerida),
                        tablas.stream().filter(tabla -> tabla != sugerida))
                .limit(MAX_OPCIONES)
                .toList();
        propuestaRepository.save(PropuestaWhatsapp.eligiendoTabla(negocioId, mensaje.getTelefono(), origen,
                mensaje.getTexto(), lectura, ofrecidas.stream().map(tabla -> tabla.bloque().id()).toList(), ahora));
        responder(mensaje, TextosWhatsapp.elegirTabla(ofrecidas));
    }

    private void elegirTabla(PropuestaWhatsapp propuesta, MensajeWhatsapp mensaje, NegocioId negocioId,
                             OffsetDateTime ahora) {
        BloqueId destino = propuesta.elegir(RespuestaChat.numero(mensaje.getTexto()));
        List<TablaDisponible> tablas = tablas(negocioId);
        TablaDisponible tabla = tablaDe(tablas, destino);
        List<FilaPropuesta> filas = propuesta.getOrigen() == OrigenPropuestaWhatsapp.FOTO
                ? filasDeFoto(PropuestaLectura.proponer(propuesta.getLectura(),
                        tablas.stream().map(TablaDisponible::bloque).toList(), destino), tabla)
                : filasDeTexto(interpretar(negocioId, propuesta.getTexto(), tablas, destino, ahora), tabla);
        if (filas.isEmpty()) {
            propuesta.descartar(ahora);
            propuestaRepository.save(propuesta);
            responder(mensaje, TextosWhatsapp.NO_ENTENDI);
            return;
        }
        propuesta.aPendiente(destino, filas);
        responder(mensaje, TextosWhatsapp.resumen(propuestaRepository.save(propuesta), tabla));
    }

    // --- confirmar y descartar ---

    private void confirmar(PropuestaWhatsapp propuesta, MensajeWhatsapp mensaje, NegocioId negocioId,
                           OffsetDateTime ahora) {
        Bloque bloque = transaccionPort.ejecutar(() -> guardar(propuesta, negocioId, ahora));
        responder(mensaje, TextosWhatsapp.guardado(propuesta.getFilas().size(), bloque.getNombre()));
    }

    private Bloque guardar(PropuestaWhatsapp propuesta, NegocioId negocioId, OffsetDateTime ahora) {
        propuesta.exigirPendiente();
        Bloque bloque = bloqueRepository.findByIdAndNegocioId(propuesta.getBloqueId(), negocioId)
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        List<Map<String, Object>> validas = propuesta.getFilas().stream()
                .map(fila -> validar(campos, fila))
                .toList();
        controlPlanService.validarAltaRegistros(negocioId, validas.size(), ahora);
        int primero = registroRepository.siguienteOrden(bloque.getId(), negocioId);
        registroRepository.saveAll(IntStream.range(0, validas.size())
                .mapToObj(i -> Registro.deWhatsapp(negocioId, bloque.getId(), validas.get(i), primero + i, ahora))
                .toList());
        propuesta.confirmar(ahora);
        propuestaRepository.save(propuesta);
        return bloque;
    }

    private static Map<String, Object> validar(List<Campo> campos, FilaPropuesta fila) {
        try {
            return Registro.validar(campos, fila.tipados(campos));
        } catch (RegistroInvalidoException e) {
            Map<String, String> nombres = new LinkedHashMap<>();
            campos.forEach(campo -> nombres.put(campo.getId().toString(), campo.getNombre()));
            throw new BusinessRuleException(TextosWhatsapp.datosInvalidos(e.getErrores(), nombres));
        }
    }

    private void descartar(PropuestaWhatsapp propuesta, MensajeWhatsapp mensaje, OffsetDateTime ahora) {
        propuesta.descartar(ahora);
        propuestaRepository.save(propuesta);
        responder(mensaje, TextosWhatsapp.DESCARTADO);
    }

    private void descartarAnterior(PropuestaWhatsapp propuesta, MensajeWhatsapp mensaje, OffsetDateTime ahora) {
        propuesta.descartar(ahora);
        propuestaRepository.save(propuesta);
        responder(mensaje, TextosWhatsapp.DESCARTE_ANTERIOR);
    }

    // --- armado de filas ---

    private List<TablaDisponible> tablas(NegocioId negocioId) {
        List<TablaDisponible> tablas = procesamientoRepository.tablas(negocioId);
        if (tablas.isEmpty()) {
            throw new BusinessRuleException(TextosWhatsapp.SIN_TABLAS);
        }
        return tablas;
    }

    private static TablaDisponible tablaDe(List<TablaDisponible> tablas, BloqueId bloqueId) {
        return tablas.stream()
                .filter(tabla -> tabla.bloque().id().equals(bloqueId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
    }

    /** Los campos donde se puede cargar un dato: no los calculados ni los que apuntan a otra tabla. */
    private static Set<CampoId> cargables(TablaDisponible tabla) {
        Set<CampoId> ids = new HashSet<>();
        for (CampoConocido campo : tabla.bloque().campos()) {
            if (!NO_CARGABLES.contains(campo.tipo())) {
                ids.add(campo.id());
            }
        }
        return ids;
    }

    private static List<FilaPropuesta> filasDeTexto(InterpretacionRegistros interpretacion, TablaDisponible tabla) {
        Set<CampoId> cargables = cargables(tabla);
        return interpretacion.filas().stream()
                .map(fila -> {
                    Map<String, String> valores = new LinkedHashMap<>();
                    fila.forEach((campoId, valor) -> {
                        if (cargables.contains(campoId) && valor != null && !valor.isBlank()) {
                            valores.put(campoId.toString(), valor);
                        }
                    });
                    return new FilaPropuesta(valores, Set.of());
                })
                .filter(fila -> !fila.valores().isEmpty())
                .toList();
    }

    private List<FilaPropuesta> filasDeFoto(PropuestaLectura propuesta, TablaDisponible tabla) {
        Set<CampoId> cargables = cargables(tabla);
        return propuesta.lectura().filas().stream()
                .map(fila -> {
                    Map<String, String> valores = new LinkedHashMap<>();
                    Set<String> dudosos = new HashSet<>();
                    fila.datos().forEach((clave, dato) -> {
                        CampoId campoId = propuesta.mapeoCampos().get(clave);
                        if (campoId != null && cargables.contains(campoId) && dato.valor() != null
                                && !dato.valor().isBlank()) {
                            valores.put(campoId.toString(), dato.valor());
                            if (dato.esDudoso(politica.umbralConfianza())) {
                                dudosos.add(campoId.toString());
                            }
                        }
                    });
                    return new FilaPropuesta(valores, dudosos);
                })
                .filter(fila -> !fila.valores().isEmpty())
                .toList();
    }

    private void responder(MensajeWhatsapp mensaje, String texto) {
        mensajeria.enviarTexto(mensaje.getTelefono(), texto);
    }
}
