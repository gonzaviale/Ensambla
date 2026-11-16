package com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.OpcionesDeCartel;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.DibujanteDeCartelPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.cartel.OpcionDeCartelItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ArmarOpcionesDeCartel {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    private final DibujanteDeCartelPort dibujanteDeCartelPort;

    private final Clock clock;

    public ArmarOpcionesDeCartelResponse execute(ArmarOpcionesDeCartelRequest request) {
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        KitDeMarca kit = obtenerKitDelNegocio(negocio);
        List<ComposicionDeCartel> opciones = OpcionesDeCartel.armar(
                kit, request.tipo(), request.formato(), request.contenidoDe(negocio.getNombre()));
        return new ArmarOpcionesDeCartelResponse(opciones.stream().map(this::dibujar).toList());
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private KitDeMarca obtenerKitDelNegocio(Negocio negocio) {
        return kitDeMarcaRepository.obtenerOPredeterminado(negocio.getId(), OffsetDateTime.now(clock));
    }

    private OpcionDeCartelItem dibujar(ComposicionDeCartel composicion) {
        return new OpcionDeCartelItem(composicion.variante(), dibujarComposicion(composicion));
    }

    private String dibujarComposicion(ComposicionDeCartel composicion) {
        return dibujanteDeCartelPort.dibujar(composicion);
    }
}
