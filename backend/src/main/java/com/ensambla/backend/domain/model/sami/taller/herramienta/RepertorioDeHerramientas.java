package com.ensambla.backend.domain.model.sami.taller.herramienta;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.ArgumentosDeHerramienta;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Todas las herramientas que el modelo puede invocar, por su nombre. El modelo nunca escribe el sistema: pide
 * herramientas con vocabulario cerrado ({@link CatalogoComponentes}, {@link KitsRubro}) y cada una descarta lo que no
 * exista, esté repetido o se pase de los topes.
 */
public final class RepertorioDeHerramientas {

    private final Map<HerramientaTaller, HerramientaDeTaller> todas = new EnumMap<>(HerramientaTaller.class);
    private final Map<HerramientaTaller, HerramientaSobreElSistema> sobreElSistema = new EnumMap<>(HerramientaTaller.class);

    public RepertorioDeHerramientas(CatalogoComponentes catalogo, KitsRubro kits) {
        AgregarPieza agregarPieza = new AgregarPieza(catalogo);
        Conectar conectar = new Conectar(catalogo);
        EditarCampo editarCampo = new EditarCampo();
        List.of(agregarPieza, new QuitarPieza(), new RenombrarPieza(), new RenombrarSistema(), new AgregarCampo(),
                new QuitarCampo(catalogo), editarCampo, new CargarRegistros(), new EditarRegistros(),
                new BorrarRegistros(), conectar, new SepararEnSeccion(), new CambiarDistribucion())
                .forEach(herramienta -> sobreElSistema.put(herramienta.herramienta(), herramienta));
        todas.putAll(sobreElSistema);
        List.of(new UsarKit(kits, agregarPieza, conectar, editarCampo), new OfrecerOpciones())
                .forEach(herramienta -> todas.put(herramienta.herramienta(), herramienta));
    }

    /** La herramienta con ese nombre; una que no existe se ignora. */
    public Optional<HerramientaDeTaller> de(String nombre) {
        return HerramientaTaller.deNombre(nombre).map(todas::get);
    }

    /** Cuenta una acción ya validada en una frase. */
    public String describir(AccionTaller accion, NombresDePiezas nombres) {
        HerramientaTaller herramienta = HerramientaTaller.deNombre(accion.herramienta()).orElseThrow();
        return sobreElSistema.get(herramienta).describir(new ArgumentosDeHerramienta(accion.argumentos()), nombres);
    }
}
