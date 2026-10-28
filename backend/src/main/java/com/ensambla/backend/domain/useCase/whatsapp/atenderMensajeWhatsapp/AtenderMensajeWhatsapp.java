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
import com.ensambla.backend.domain.model.registro.MovimientoDeStock;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.CoincidenciaDeFilas;
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
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
    private static final int MAX_NOMBRES_POR_TABLA = 100;
    private static final Set<TipoCampo> NUMERICOS = Set.of(TipoCampo.NUMERO, TipoCampo.MONEDA);

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
        List<FilaPropuesta> filas = armar(filasDeTexto(interpretacion, tabla), tabla, tablas, negocioId);
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
                new ConsultaInterpretacion(texto, tablas, dia, destino, nombresConocidos(tablas, negocioId)));
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
        proponerDeFoto(mensaje, negocioId, tablaDe(tablas, elegida.get()), tablas, propuesta, ahora);
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
                                List<TablaDisponible> tablas, PropuestaLectura propuesta, OffsetDateTime ahora) {
        List<FilaPropuesta> filas = armar(filasDeFoto(propuesta, tabla), tabla, tablas, negocioId);
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
        List<FilaPropuesta> nuevas = propuesta.getOrigen() == OrigenPropuestaWhatsapp.FOTO
                ? filasDeFoto(PropuestaLectura.proponer(propuesta.getLectura(),
                        tablas.stream().map(TablaDisponible::bloque).toList(), destino), tabla)
                : filasDeTexto(interpretar(negocioId, propuesta.getTexto(), tablas, destino, ahora), tabla);
        List<FilaPropuesta> filas = armar(nuevas, tabla, tablas, negocioId);
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
        Guardado guardado = transaccionPort.ejecutar(() -> guardar(propuesta, negocioId, ahora));
        responder(mensaje, TextosWhatsapp.guardado(guardado.nuevas(), guardado.actualizadas(), guardado.tabla(),
                guardado.sumados(), guardado.stock()));
    }

    private record Guardado(String tabla, int nuevas, int actualizadas, List<String> sumados,
                            List<MovimientoDeStock> stock) {
    }

    /**
     * Las filas que nombran algo que ya existía lo actualizan; el resto se suma al final de la tabla. Antes se suman
     * los productos o clientes nuevos que nombran sus vínculos y, si la tabla es de movimientos, después se mueve el
     * stock de cada producto.
     */
    private Guardado guardar(PropuestaWhatsapp propuesta, NegocioId negocioId, OffsetDateTime ahora) {
        propuesta.exigirPendiente();
        Bloque bloque = bloqueRepository.findByIdAndNegocioId(propuesta.getBloqueId(), negocioId)
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        Vinculados vinculados = vincularTodo(propuesta.getFilas(), negocioId, ahora);
        List<Map<String, Object>> nuevas = new ArrayList<>();
        List<Registro> actualizados = new ArrayList<>();
        for (FilaPropuesta fila : propuesta.getFilas()) {
            Optional<Registro> existente = existente(fila, bloque.getId(), negocioId);
            if (existente.isPresent()) {
                Registro registro = existente.get();
                Map<String, Object> cambios = vinculados.ponerIds(fila, fila.cambiosTipados(campos,
                        registro.getValores()));
                registro.reemplazarValores(validar(campos, registro.combinarCon(cambios)), ahora);
                actualizados.add(registro);
            } else {
                nuevas.add(validar(campos, vinculados.ponerIds(fila, fila.tipados(campos))));
            }
        }
        if (!nuevas.isEmpty()) {
            controlPlanService.validarAltaRegistros(negocioId, nuevas.size(), ahora);
            int primero = registroRepository.siguienteOrden(bloque.getId(), negocioId);
            registroRepository.saveAll(IntStream.range(0, nuevas.size())
                    .mapToObj(i -> Registro.deWhatsapp(negocioId, bloque.getId(), nuevas.get(i), primero + i, ahora))
                    .toList());
        }
        if (!actualizados.isEmpty()) {
            registroRepository.saveAll(actualizados);
        }
        List<MovimientoDeStock> stock = moverStock(bloque, campos, nuevas, negocioId, ahora);
        propuesta.confirmar(ahora);
        propuestaRepository.save(propuesta);
        return new Guardado(bloque.getNombre(), nuevas.size(), actualizados.size(), vinculados.sumados(), stock);
    }

    // --- vínculos al guardar ---

    /**
     * El id de cada fila que nombran los vínculos de la propuesta, por tabla y nombre.
     *
     * @param sumados las filas que se crearon para poder vincularlas («Marta en Clientes»)
     */
    private record Vinculados(Map<String, String> ids, List<String> sumados) {

        private static String clave(VinculoPropuesto vinculo) {
            return vinculo.bloqueId() + "|" + TextoNormalizado.de(vinculo.nombre());
        }

        /** Los valores de la fila con el id de la fila vinculada en lugar del nombre que escribió la persona. */
        Map<String, Object> ponerIds(FilaPropuesta fila, Map<String, Object> valores) {
            fila.vinculos().forEach((campo, vinculo) -> valores.put(campo, ids.get(clave(vinculo))));
            return valores;
        }
    }

    /**
     * Resuelve los vínculos de todas las filas: la fila que se encontró al proponer, si sigue estando, o una nueva con
     * ese nombre (una sola por nombre, aunque varias ventas la nombren).
     */
    private Vinculados vincularTodo(List<FilaPropuesta> filas, NegocioId negocioId, OffsetDateTime ahora) {
        Map<String, VinculoPropuesto> distintos = new LinkedHashMap<>();
        filas.forEach(fila -> fila.vinculos().values()
                .forEach(vinculo -> distintos.putIfAbsent(Vinculados.clave(vinculo), vinculo)));
        Map<String, String> ids = new LinkedHashMap<>();
        List<VinculoPropuesto> nuevos = new ArrayList<>();
        distintos.forEach((clave, vinculo) -> vigente(vinculo, negocioId)
                .ifPresentOrElse(id -> ids.put(clave, id), () -> nuevos.add(vinculo)));
        if (!nuevos.isEmpty()) {
            controlPlanService.validarAltaRegistros(negocioId, nuevos.size(), ahora);
        }
        List<String> sumados = new ArrayList<>();
        for (VinculoPropuesto vinculo : nuevos) {
            ids.put(Vinculados.clave(vinculo), sumar(vinculo, negocioId, ahora).getId().toString());
            sumados.add("%s en %s".formatted(vinculo.nombre(), vinculo.tabla()));
        }
        return new Vinculados(ids, sumados);
    }

    /** El id de la fila vinculada, si todavía está en su tabla. */
    private Optional<String> vigente(VinculoPropuesto vinculo, NegocioId negocioId) {
        if (vinculo.esNuevo()) {
            return Optional.empty();
        }
        return registroRepository.findByIdAndNegocioId(RegistroId.de(vinculo.registroId()), negocioId)
                .filter(registro -> registro.getBloqueId().equals(BloqueId.de(vinculo.bloqueId())))
                .map(registro -> registro.getId().toString());
    }

    /**
     * Suma al final de su tabla la fila que nombra el vínculo, con su nombre y los valores iniciales de la tabla; un
     * número obligatorio sin valor inicial (el stock de un producto) empieza en 0.
     */
    private Registro sumar(VinculoPropuesto vinculo, NegocioId negocioId, OffsetDateTime ahora) {
        BloqueId bloqueId = BloqueId.de(vinculo.bloqueId());
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
        Campo identificador = campos.stream().filter(campo -> campo.getTipo() == TipoCampo.TEXTO).findFirst()
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
        Map<String, String> valores = new LinkedHashMap<>();
        for (Campo campo : campos) {
            if (campo.getValorInicial() != null && !campo.getValorInicial().isBlank()) {
                valores.put(campo.getId().toString(), campo.getValorInicial());
            } else if (campo.esObligatorio() && NUMERICOS.contains(campo.getTipo())) {
                valores.put(campo.getId().toString(), "0");
            }
        }
        valores.put(identificador.getId().toString(), vinculo.nombre());
        Map<String, Object> validos = validar(campos, new FilaPropuesta(valores, Set.of()).tipados(campos));
        int orden = registroRepository.siguienteOrden(bloqueId, negocioId);
        return registroRepository.save(Registro.deWhatsapp(negocioId, bloqueId, validos, orden, ahora));
    }

    // --- stock ---

    /**
     * Una tabla de movimientos (ventas, salidas, entradas) mueve el stock del producto de cada fila nueva, como en el
     * taller. Varias filas del mismo producto se acumulan.
     */
    private List<MovimientoDeStock> moverStock(Bloque bloque, List<Campo> campos, List<Map<String, Object>> nuevas,
                                               NegocioId negocioId, OffsetDateTime ahora) {
        Optional<Campo> producto = conClave(campos, MovimientoDeStock.CLAVE_PRODUCTO).filter(Campo::esRelacion);
        Optional<Campo> cantidad = conClave(campos, MovimientoDeStock.CLAVE_CANTIDAD);
        if (bloque.getDireccionStock() == null || producto.isEmpty() || cantidad.isEmpty()) {
            return List.of();
        }
        Map<String, Registro> productos = new LinkedHashMap<>();
        List<MovimientoDeStock> movimientos = new ArrayList<>();
        for (Map<String, Object> fila : nuevas) {
            Object productoId = fila.get(producto.get().getId().toString());
            Object pedido = fila.get(cantidad.get().getId().toString());
            if (productoId == null || pedido == null) {
                continue;
            }
            Optional<Registro> vinculado = Optional.ofNullable(productos.computeIfAbsent(productoId.toString(),
                    id -> registroRepository.findByIdAndNegocioId(RegistroId.de(id), negocioId).orElse(null)));
            vinculado.flatMap(registro -> mover(registro, new BigDecimal(pedido.toString()),
                    bloque.getDireccionStock(), negocioId, ahora)).ifPresent(movimientos::add);
        }
        if (!movimientos.isEmpty()) {
            registroRepository.saveAll(List.copyOf(productos.values()));
        }
        return movimientos;
    }

    private Optional<MovimientoDeStock> mover(Registro producto, BigDecimal pedido, int direccion,
                                              NegocioId negocioId, OffsetDateTime ahora) {
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(producto.getBloqueId(), negocioId);
        Optional<Campo> stock = conClave(campos, MovimientoDeStock.CLAVE_CANTIDAD);
        if (stock.isEmpty()) {
            return Optional.empty();
        }
        String clave = stock.get().getId().toString();
        String nombre = campos.stream().filter(campo -> campo.getTipo() == TipoCampo.TEXTO).findFirst()
                .map(campo -> producto.getValores().get(campo.getId().toString()))
                .map(Object::toString).orElse("el producto");
        MovimientoDeStock movimiento = MovimientoDeStock.aplicar(nombre,
                FilaPropuesta.sumado(producto.getValores().get(clave), BigDecimal.ZERO), pedido, direccion);
        Map<String, Object> valores = new LinkedHashMap<>(producto.getValores());
        valores.put(clave, movimiento.despues());
        producto.reemplazarValores(valores, ahora);
        return Optional.of(movimiento);
    }

    private static Optional<Campo> conClave(List<Campo> campos, String clave) {
        return campos.stream().filter(campo -> clave.equals(campo.getClave())).findFirst();
    }

    /** La fila que la propuesta iba a actualizar, si todavía está en esa tabla; si se borró, se agrega como nueva. */
    private Optional<Registro> existente(FilaPropuesta fila, BloqueId bloqueId, NegocioId negocioId) {
        if (!fila.actualizaUnaFila()) {
            return Optional.empty();
        }
        return registroRepository.findByIdAndNegocioId(RegistroId.de(fila.registroId()), negocioId)
                .filter(registro -> registro.getBloqueId().equals(bloqueId));
    }

    private static Map<String, Object> validar(List<Campo> campos, Map<String, Object> valores) {
        try {
            return Registro.validar(campos, valores);
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

    /** Los campos donde se puede cargar un dato: no los calculados. */
    private static Set<CampoId> cargables(TablaDisponible tabla) {
        Set<CampoId> ids = new HashSet<>();
        for (CampoConocido campo : tabla.bloque().campos()) {
            if (campo.tipo() != TipoCampo.FORMULA) {
                ids.add(campo.id());
            }
        }
        return ids;
    }

    /** Las filas listas para proponer: emparejadas con las que ya existen y con sus vínculos resueltos. */
    private List<FilaPropuesta> armar(List<FilaPropuesta> filas, TablaDisponible tabla, List<TablaDisponible> tablas,
                                      NegocioId negocioId) {
        return conVinculos(conExistentes(filas, tabla, negocioId), tabla, tablas, negocioId);
    }

    /** Las filas de una tabla que tienen nombre, cada una con el suyo en la misma posición. */
    private record Nombradas(List<Registro> registros, List<String> nombres) {
    }

    private Nombradas nombradas(TablaDisponible tabla, CampoConocido identificador, NegocioId negocioId) {
        String clave = identificador.id().toString();
        List<Registro> registros = registroRepository.findAllByBloqueIdAndNegocioId(tabla.bloque().id(), negocioId)
                .stream().filter(registro -> registro.getValores().get(clave) != null).toList();
        return new Nombradas(registros,
                registros.stream().map(registro -> registro.getValores().get(clave).toString()).toList());
    }

    /**
     * Los nombres que la IA tiene que conocer para no inventar otro: los de las tablas de cosas y los de las tablas a
     * las que apunta algún vínculo.
     */
    private Map<BloqueId, List<String>> nombresConocidos(List<TablaDisponible> tablas, NegocioId negocioId) {
        Set<BloqueId> vinculadas = new HashSet<>();
        tablas.forEach(tabla -> tabla.bloque().campos().stream()
                .filter(campo -> campo.tipo() == TipoCampo.RELACION && campo.bloqueRelacionadoId() != null)
                .forEach(campo -> vinculadas.add(campo.bloqueRelacionadoId())));
        Map<BloqueId, List<String>> nombres = new LinkedHashMap<>();
        for (TablaDisponible tabla : tablas) {
            Optional<CampoConocido> identificador = tabla.campoIdentificador();
            if (identificador.isPresent()
                    && (tabla.actualizaFilasExistentes() || vinculadas.contains(tabla.bloque().id()))) {
                List<String> deLaTabla = nombradas(tabla, identificador.get(), negocioId).nombres().stream()
                        .distinct().limit(MAX_NOMBRES_POR_TABLA).toList();
                if (!deLaTabla.isEmpty()) {
                    nombres.put(tabla.bloque().id(), deLaTabla);
                }
            }
        }
        return nombres;
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
        Nombradas existentes = nombradas(tabla, identificador.get(), negocioId);
        return filas.stream().map(fila -> emparejar(fila, clave, existentes)).toList();
    }

    private static FilaPropuesta emparejar(FilaPropuesta fila, String clave, Nombradas existentes) {
        String nombre = fila.valores().get(clave);
        if (nombre == null || !fila.cambiaAlgoAdemasDelNombre(clave)) {
            return fila;
        }
        return CoincidenciaDeFilas.buscar(nombre, existentes.nombres())
                .map(posicion -> {
                    Registro registro = existentes.registros().get(posicion);
                    return fila.actualizando(registro.getId().toString(), existentes.nombres().get(posicion), clave,
                            registro.getValores());
                })
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
            String clave = campo.id().toString();
            if (campo.tipo() != TipoCampo.RELACION
                    || resultado.stream().noneMatch(fila -> fila.valores().containsKey(clave))) {
                continue;
            }
            Optional<TablaDisponible> destino = tablas.stream()
                    .filter(otra -> otra.bloque().id().equals(campo.bloqueRelacionadoId()))
                    .findFirst();
            Optional<CampoConocido> identificador = destino.flatMap(TablaDisponible::campoIdentificador);
            if (identificador.isEmpty()) {
                resultado = resultado.stream().map(fila -> fila.sinValor(clave)).toList();
                continue;
            }
            Nombradas nombradas = nombradas(destino.get(), identificador.get(), negocioId);
            resultado = resultado.stream().map(fila -> vincular(fila, clave, destino.get(), nombradas)).toList();
        }
        return resultado;
    }

    private static FilaPropuesta vincular(FilaPropuesta fila, String clave, TablaDisponible destino,
                                          Nombradas nombradas) {
        String nombre = fila.valores().get(clave);
        if (nombre == null) {
            return fila;
        }
        String bloqueId = destino.bloque().id().toString();
        String tabla = destino.bloque().nombre();
        VinculoPropuesto vinculo = CoincidenciaDeFilas.buscar(nombre, nombradas.nombres())
                .map(posicion -> new VinculoPropuesto(nombradas.nombres().get(posicion),
                        nombradas.registros().get(posicion).getId().toString(), bloqueId, tabla))
                .orElseGet(() -> new VinculoPropuesto(nombre.strip(), null, bloqueId, tabla));
        return fila.vinculando(clave, vinculo);
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
