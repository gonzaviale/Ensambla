#!/usr/bin/env python3
"""ENS-134 / ENS-192: evalúa anotaciones humanas usando el extractor Java configurado.

Además de la precisión de la lectura, mide la detección de columnas nuevas: un caso con `destino` (el bloque
existente al que van las filas) y `camposNuevos` en su anotación (las columnas que la foto trae y el bloque no
tiene) reporta aciertos, omisiones y falsos positivos."""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import math
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unicodedata
import zipfile
from decimal import Decimal

TIPOS = {"VENTAS", "GASTOS", "PEDIDOS", "STOCK", "FIADOS", "CLIENTES", "OTRO"}
CAMPOS = {"TEXTO", "NUMERO", "FECHA", "OPCION", "SI_NO"}
REPO = Path(__file__).resolve().parents[1]


def ruta_privada(raiz: Path, relativa: str) -> Path:
    ruta = (raiz / relativa).resolve(strict=True)
    if not ruta.is_relative_to(raiz.resolve()):
        raise ValueError("El manifest incluye una ruta fuera del set")
    return ruta


def normalizar_nombre(nombre: str) -> str:
    """Compara nombres de columnas sin tildes, mayúsculas ni puntuación: «¿Pagó?» y «pago» son lo mismo."""
    sin_tildes = "".join(c for c in unicodedata.normalize("NFD", nombre) if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", " ", sin_tildes.lower()).strip()


def validar_destino(destino: dict) -> None:
    if not isinstance(destino, dict) or not str(destino.get("nombre", "")).strip():
        raise ValueError("El destino del caso necesita el nombre del bloque")
    campos = destino.get("campos")
    if not campos:
        raise ValueError("El destino del caso necesita al menos un campo")
    nombres = [normalizar_nombre(campo["nombre"]) for campo in campos]
    if "" in nombres or len(nombres) != len(set(nombres)):
        raise ValueError("El destino tiene campos repetidos o sin nombre")
    if any(campo["tipo"] not in CAMPOS for campo in campos):
        raise ValueError("El destino tiene un tipo de campo inválido")


def validar_columnas_nuevas(nuevas: list[dict]) -> None:
    nombres = [normalizar_nombre(columna["nombre"]) for columna in nuevas]
    if "" in nombres or len(nombres) != len(set(nombres)):
        raise ValueError("La anotación tiene columnas nuevas repetidas o sin nombre")
    if any(columna.get("tipo") is not None and columna["tipo"] not in CAMPOS for columna in nuevas):
        raise ValueError("La anotación tiene una columna nueva con tipo inválido")


def validar_esperado(esperado: dict) -> None:
    if esperado.get("tipoRegistro") not in TIPOS:
        raise ValueError("La anotación tiene un tipo de registro inválido")
    campos = esperado["bloque"]["campos"]
    claves = [campo["clave"] for campo in campos]
    if not claves or len(claves) != len(set(claves)):
        raise ValueError("La anotación debe tener campos con claves únicas")
    if any(campo["tipo"] not in CAMPOS for campo in campos):
        raise ValueError("La anotación tiene un tipo de campo inválido")
    posiciones = set()
    for fila in esperado["filas"]:
        posicion = (fila["pagina"], fila["renglon"])
        if posicion[0] != 1 or posicion[1] < 1 or posicion in posiciones:
            raise ValueError("La anotación debe tener renglones únicos de la página 1")
        posiciones.add(posicion)
        if not set(fila["datos"]).issubset(claves):
            raise ValueError("La anotación usa un campo inexistente")
        for dato in fila["datos"].values():
            valor = dato["valor"]
            if valor is not None and not isinstance(valor, str):
                raise ValueError("El valor esperado debe ser texto o null")


def cargar_set(raiz: Path, permitir_muestra: bool = False) -> tuple[dict, list[dict]]:
    fotos = raiz / "fotos"
    if not fotos.exists() or not any(p.is_file() and p.name != ".gitkeep" for p in fotos.iterdir()):
        raise ValueError(f"El set está vacío. Cargá fotos reales en {fotos} y sus JSON en {raiz / 'esperados'}")
    manifest = json.loads((raiz / "manifest.json").read_text(encoding="utf-8-sig"))
    if not manifest.get("version") or manifest.get("consentimiento") is not True or manifest.get("anonimizado") is not True:
        raise ValueError("El manifest debe declarar versión, consentimiento y anonimización")
    casos = manifest["casos"]
    if not casos or (len(casos) < 30 and not permitir_muestra):
        raise ValueError("Se necesitan al menos 30 fotos reales anotadas para la evaluación")
    ids = set()
    imagenes = set()
    resultados = []
    for caso in casos:
        if caso["id"] in ids:
            raise ValueError("Hay identificadores de caso repetidos")
        ids.add(caso["id"])
        foto = ruta_privada(raiz, caso["foto"])
        anotacion = ruta_privada(raiz, caso["esperado"])
        if foto in imagenes:
            raise ValueError("Una misma foto no puede contarse dos veces")
        imagenes.add(foto)
        if not foto.is_relative_to(fotos.resolve()) or not anotacion.is_relative_to((raiz / "esperados").resolve()):
            raise ValueError("Fotos y anotaciones deben estar en sus carpetas respectivas")
        tipo = mimetypes.guess_type(foto)[0]
        if tipo not in {"image/jpeg", "image/png", "image/webp", "image/heic"}:
            raise ValueError("La evaluación de precisión requiere archivos de imagen admitidos")
        esperado = json.loads(anotacion.read_text(encoding="utf-8-sig"))
        validar_esperado(esperado)
        destino = caso.get("destino")
        if destino is not None:
            validar_destino(destino)
            if "camposNuevos" not in esperado:
                raise ValueError("Un caso con destino necesita «camposNuevos» en su anotación (puede ir vacío)")
        elif "camposNuevos" in esperado:
            raise ValueError("«camposNuevos» sólo tiene sentido en un caso con destino")
        if "camposNuevos" in esperado:
            validar_columnas_nuevas(esperado["camposNuevos"])
        resultados.append({
            "id": caso["id"], "archivo": str(foto), "tipoContenido": tipo, "destino": destino,
            "esperado": esperado, "sha256Foto": hashlib.sha256(foto.read_bytes()).hexdigest(),
            "sha256Esperado": hashlib.sha256(anotacion.read_bytes()).hexdigest(),
        })
    return manifest, resultados


def celdas(lectura: dict) -> dict:
    return {
        (fila["pagina"], fila["renglon"], clave): dato["valor"]
        for fila in lectura["filas"] for clave, dato in fila["datos"].items()
    }


def medir(esperado: dict, lectura: dict | None) -> dict:
    datos_esperados = celdas(esperado)
    datos_leidos = celdas(lectura) if lectura else {}
    posiciones = datos_esperados.keys() | datos_leidos.keys()
    correctos = sum(posicion in datos_esperados and posicion in datos_leidos
                    and datos_esperados[posicion] == datos_leidos[posicion] for posicion in posiciones)
    campos_esperados = {campo["clave"]: campo["tipo"] for campo in esperado["bloque"]["campos"]}
    campos_leidos = {campo["clave"]: campo["tipo"] for campo in lectura["bloque"]["campos"]} if lectura else {}
    campos = campos_esperados.keys() | campos_leidos.keys()
    tipos_correctos = sum(clave in campos_esperados and clave in campos_leidos
                          and campos_esperados[clave] == campos_leidos[clave] for clave in campos)
    return {
        "clasificacionCorrecta": bool(lectura and esperado["tipoRegistro"] == lectura["tipoRegistro"]),
        "datosCorrectos": correctos, "datosEvaluados": len(posiciones),
        "camposCorrectos": tipos_correctos, "camposEvaluados": len(campos),
    }


def medir_columnas_nuevas(esperado: dict, respuesta: dict) -> dict:
    """Columnas nuevas esperadas contra las detectadas, por nombre normalizado (la clave la elige el modelo)."""
    esperadas = {normalizar_nombre(columna["nombre"]): columna.get("tipo") for columna in esperado["camposNuevos"]}
    leyo = respuesta["lectura"] is not None
    detectadas = {normalizar_nombre(columna["nombre"]): columna["tipo"]
                  for columna in respuesta.get("camposNuevos") or []}
    aciertos = esperadas.keys() & detectadas.keys()
    return {
        "esperadas": len(esperadas), "detectadas": len(detectadas), "aciertos": len(aciertos),
        "tiposCorrectos": sum(1 for nombre in aciertos if esperadas[nombre] in (None, detectadas[nombre])),
        "falsosPositivos": len(detectadas.keys() - esperadas.keys()),
        "omitidas": len(esperadas.keys() - detectadas.keys()),
        "exacto": leyo and esperadas.keys() == detectadas.keys(),
    }


def resumir_columnas_nuevas(mediciones: list[dict]) -> dict | None:
    if not mediciones:
        return None
    total = {clave: sum(medicion[clave] for medicion in mediciones)
             for clave in ("esperadas", "detectadas", "aciertos", "tiposCorrectos", "falsosPositivos", "omitidas")}
    total["casos"] = len(mediciones)
    total["casosExactos"] = sum(1 for medicion in mediciones if medicion["exacto"])
    total["precisionPct"] = porcentaje(total["aciertos"], total["detectadas"])
    total["recallPct"] = porcentaje(total["aciertos"], total["esperadas"])
    total["tipoCorrectoPct"] = porcentaje(total["tiposCorrectos"], total["aciertos"])
    total["casosExactosPct"] = porcentaje(total["casosExactos"], total["casos"])
    return total


def porcentaje(correctos: int, total: int) -> float | None:
    return round(100 * correctos / total, 4) if total else None


def informe(manifest: dict, casos: list[dict], respuestas: list[dict], meta: float | None,
            precio_entrada: Decimal | None = None, precio_salida: Decimal | None = None) -> dict:
    por_id = {respuesta["id"]: respuesta for respuesta in respuestas}
    if len(por_id) != len(respuestas) or set(por_id) != {caso["id"] for caso in casos}:
        raise ValueError("El extractor devolvió casos faltantes, repetidos o inesperados")
    categorias = {}
    detalles = []
    columnas_nuevas = []
    simulacion = False
    for caso in casos:
        respuesta = por_id[caso["id"]]
        lectura = respuesta["lectura"]
        medicion = medir(caso["esperado"], lectura)
        consumo = lectura["consumo"] if lectura else None
        simulacion |= bool(consumo and consumo["proveedor"] == "simulado")
        tipo = caso["esperado"]["tipoRegistro"]
        categoria = categorias.setdefault(tipo, {
            "casos": 0, "clasificacionesCorrectas": 0, "datosCorrectos": 0, "datosEvaluados": 0,
            "camposCorrectos": 0, "camposEvaluados": 0, "erroresExtraccion": 0,
        })
        categoria["casos"] += 1
        categoria["clasificacionesCorrectas"] += int(medicion["clasificacionCorrecta"])
        categoria["erroresExtraccion"] += int(respuesta["error"] is not None)
        for clave in ("datosCorrectos", "datosEvaluados", "camposCorrectos", "camposEvaluados"):
            categoria[clave] += medicion[clave]
        detalle = {
            "id": caso["id"], "tipo": tipo, **medicion, "error": respuesta["error"], "consumo": consumo,
            "sha256Foto": caso["sha256Foto"], "sha256Esperado": caso["sha256Esperado"],
        }
        if caso.get("destino") is not None:
            detalle["columnasNuevas"] = medir_columnas_nuevas(caso["esperado"], respuesta)
            columnas_nuevas.append(detalle["columnasNuevas"])
        detalles.append(detalle)
    for categoria in categorias.values():
        categoria["precisionDatosPct"] = porcentaje(categoria["datosCorrectos"], categoria["datosEvaluados"])
        categoria["precisionCamposPct"] = porcentaje(categoria["camposCorrectos"], categoria["camposEvaluados"])
        categoria["precisionClasificacionPct"] = porcentaje(categoria["clasificacionesCorrectas"], categoria["casos"])
    columnas = resumir_columnas_nuevas(columnas_nuevas)
    validacion_real = not simulacion and len(casos) >= 30
    cumple_meta = None
    if meta is not None and validacion_real:
        cumple_meta = all(categoria[metrica] is not None and categoria[metrica] >= meta
                          for categoria in categorias.values()
                          for metrica in ("precisionDatosPct", "precisionCamposPct", "precisionClasificacionPct"))
        if columnas is not None:
            # Una métrica sin base (por ejemplo, sin columnas nuevas esperadas) no se exige.
            cumple_meta &= all(columnas[metrica] >= meta for metrica in ("precisionPct", "recallPct", "casosExactosPct")
                               if columnas[metrica] is not None)
    consumos = [detalle["consumo"] for detalle in detalles if detalle["consumo"]]
    latencias = sorted(consumo["duracionMs"] for consumo in consumos)
    costo = None
    if simulacion and len(consumos) == len(casos):
        costo = "0.000000"
    elif precio_entrada is not None and precio_salida is not None and len(consumos) == len(casos):
        costo = str(sum((precio_entrada * consumo["tokensEntrada"] + precio_salida * consumo["tokensSalida"])
                        / Decimal(1_000_000) for consumo in consumos).quantize(Decimal("0.000001")))
    return {
        "versionSet": manifest["version"], "simulado": simulacion,
        "evidenciaReal": validacion_real, "metaPrecisionPct": meta, "cumpleMeta": cumple_meta,
        "porTipo": categorias, "columnasNuevas": columnas, "casos": detalles,
        "costoEstimadoUsd": costo,
        "costoPorPaginaUsd": str(Decimal(costo) / len(casos)) if costo is not None else None,
        "latenciaPromedioMs": sum(latencias) / len(latencias) if latencias else None,
        "latenciaP95Ms": latencias[math.ceil(0.95 * len(latencias)) - 1] if latencias else None,
        "tokensEntrada": sum(consumo["tokensEntrada"] for consumo in consumos),
        "tokensSalida": sum(consumo["tokensSalida"] for consumo in consumos),
        "precioEntradaMillon": str(precio_entrada) if precio_entrada is not None else None,
        "precioSalidaMillon": str(precio_salida) if precio_salida is not None else None,
        "notaCosto": "El costo es estimado; los fallos sin datos del proveedor dejan el costo desconocido.",
    }


def construir_jar() -> Path:
    backend = REPO / "backend"
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=backend, suffix=".xml",
                                     prefix=".pom-evaluacion-", delete=False) as temporal:
        pom = Path(temporal.name)
        temporal.write((backend / "pom.xml").read_text(encoding="utf-8").replace(
            "<build>", "<build><directory>${project.basedir}/target/evaluacion-cli</directory>"))
    try:
        wrapper = backend / ("mvnw.cmd" if os.name == "nt" else "mvnw")
        subprocess.run([str(wrapper), "-q", "-f", str(pom), "-DskipTests", "package"], cwd=backend, check=True)
    finally:
        pom.unlink()
    return backend / "target/evaluacion-cli/backend-0.0.1-SNAPSHOT.jar"


def ejecutar(args: argparse.Namespace) -> Path:
    raiz = args.set.resolve()
    manifest, casos = cargar_set(raiz, args.permitir_simulado)
    if args.proveedor == "simulado" and not args.permitir_simulado:
        raise ValueError("El modelo real todavía no está elegido. Usá --permitir-simulado sólo para verificar el circuito.")
    jar = args.jar or construir_jar()
    with zipfile.ZipFile(jar) as paquete:
        if "BOOT-INF/classes/com/ensambla/evaluacion/EvaluacionIaApplication.class" not in paquete.namelist():
            raise ValueError("El jar no contiene la herramienta de evaluación; recompilá esta rama")
    with tempfile.TemporaryDirectory(prefix="ensambla-evaluacion-") as carpeta:
        entrada, salida = Path(carpeta) / "entrada.json", Path(carpeta) / "salida.json"
        entrada.write_text(json.dumps({"casos": [
            {**{clave: caso[clave] for clave in ("id", "archivo", "tipoContenido")},
             **({"destino": caso["destino"]} if caso.get("destino") is not None else {})}
            for caso in casos]}), encoding="utf-8")
        comando = [
            "java", "-Dloader.main=com.ensambla.evaluacion.EvaluacionIaApplication",
            "-cp", str(jar), "org.springframework.boot.loader.launch.PropertiesLauncher",
            f"--evaluacion.entrada={entrada}", f"--evaluacion.salida={salida}",
            f"--ensambla.ia.proveedor={args.proveedor}", f"--ensambla.ia.modelo={args.modelo}",
            "--spring.main.banner-mode=off", "--spring.config.import=",
        ]
        subprocess.run(comando, cwd=REPO / "backend", check=True, timeout=args.timeout)
        respuestas = json.loads(salida.read_text(encoding="utf-8"))
    reporte = informe(manifest, casos, respuestas, args.meta, args.precio_entrada_millon, args.precio_salida_millon)
    reporte["proveedorSolicitado"], reporte["modeloSolicitado"] = args.proveedor, args.modelo
    reporte["simulado"] |= args.proveedor == "simulado"
    if reporte["simulado"]:
        reporte["evidenciaReal"], reporte["cumpleMeta"] = False, None
    reporte["sha256Manifest"] = hashlib.sha256((raiz / "manifest.json").read_bytes()).hexdigest()
    destino = args.reporte or raiz / "reportes" / "evaluacion.json"
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(json.dumps(reporte, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Reporte guardado en {destino}")
    if reporte["simulado"]:
        print("Ejecución simulada: verifica el circuito; no demuestra precisión real.")
    if reporte["cumpleMeta"] is False:
        raise ValueError("La precisión no alcanza la meta configurada; revisá el reporte")
    return destino


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--set", type=Path, default=REPO / "datos/ia/cuadernos")
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--proveedor", default=os.getenv("ENSAMBLA_IA_PROVEEDOR", "simulado"))
    parser.add_argument("--modelo", default=os.getenv("ENSAMBLA_IA_MODELO", ""))
    parser.add_argument("--permitir-simulado", action="store_true")
    parser.add_argument("--meta", type=float)
    parser.add_argument("--reporte", type=Path)
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--precio-entrada-millon", type=Decimal)
    parser.add_argument("--precio-salida-millon", type=Decimal)
    args = parser.parse_args()
    try:
        if args.meta is not None and not 0 <= args.meta <= 100:
            raise ValueError("La meta debe estar entre 0 y 100")
        if any(precio is not None and (not precio.is_finite() or precio < 0)
               for precio in (args.precio_entrada_millon, args.precio_salida_millon)):
            raise ValueError("Las tarifas deben ser números finitos no negativos")
        ejecutar(args)
        return 0
    except (ValueError, KeyError, TypeError, OSError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        print(f"No se pudo evaluar: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
