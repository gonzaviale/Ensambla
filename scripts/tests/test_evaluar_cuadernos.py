"""Datos sintéticos: estas pruebas no constituyen el set real de ENS-127."""
import copy
import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path
import tempfile
import unittest
from decimal import Decimal
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / "evaluar_cuadernos.py"
SPEC = importlib.util.spec_from_file_location("evaluar_cuadernos", SCRIPT)
evaluacion = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(evaluacion)


def esperado():
    return {"tipoRegistro": "VENTAS", "bloque": {"nombre": "Ventas", "bloqueExistenteId": None,
             "campos": [{"clave": "monto", "nombre": "Monto", "tipo": "NUMERO", "opciones": []}]},
            "filas": [{"pagina": 1, "renglon": 1, "datos": {"monto": {"valor": "100", "confianza": 1}}}],
            "advertencias": []}


class EvaluarCuadernosTest(unittest.TestCase):
    def setUp(self):
        self.temporal = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporal.cleanup)
        self.raiz = Path(self.temporal.name)
        for carpeta in ("fotos", "esperados", "reportes"):
            (self.raiz / carpeta).mkdir()

    def given_set(self, cantidad=1):
        manifest = {"version": "sintetico-test", "consentimiento": True, "anonimizado": True, "casos": []}
        for numero in range(cantidad):
            nombre = f"pagina-{numero:03d}"
            (self.raiz / "fotos" / f"{nombre}.jpg").write_bytes(b"sintetico")
            (self.raiz / "esperados" / f"{nombre}.json").write_text(json.dumps(esperado()), encoding="utf-8")
            manifest["casos"].append({"id": nombre, "foto": f"fotos/{nombre}.jpg",
                                     "esperado": f"esperados/{nombre}.json"})
        self.write_manifest(manifest)
        return manifest

    def write_manifest(self, manifest):
        (self.raiz / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")

    def given_report_inputs(self, cantidad=1, proveedor="real"):
        manifest = self.given_set(cantidad)
        _, casos = evaluacion.cargar_set(self.raiz, True)
        respuestas = []
        for caso in casos:
            lectura = esperado()
            lectura["consumo"] = {"proveedor": proveedor, "modelo": "prueba", "versionPrompt": "v1",
                                  "paginas": 1, "tokensEntrada": 1000, "tokensSalida": 100, "duracionMs": 50}
            respuestas.append({"id": caso["id"], "lectura": lectura, "error": None})
        return manifest, casos, respuestas

    def test_empty_set_returns_clear_error(self):
        with self.assertRaisesRegex(ValueError, "set está vacío"):
            evaluacion.cargar_set(self.raiz)

    def test_small_set_cannot_claim_real_evaluation(self):
        self.given_set()
        with self.assertRaisesRegex(ValueError, "30 fotos"):
            evaluacion.cargar_set(self.raiz)

    def test_manifest_requires_consent(self):
        manifest = self.given_set()
        manifest["consentimiento"] = False
        self.write_manifest(manifest)
        with self.assertRaisesRegex(ValueError, "consentimiento"):
            evaluacion.cargar_set(self.raiz, True)

    def test_duplicates_are_rejected(self):
        for campo, mensaje in (("id", "identificadores"), ("foto", "dos veces")):
            with self.subTest(campo=campo):
                manifest = self.given_set(2)
                manifest["casos"][1][campo] = manifest["casos"][0][campo]
                self.write_manifest(manifest)
                with self.assertRaisesRegex(ValueError, mensaje):
                    evaluacion.cargar_set(self.raiz, True)

    def test_annotations_are_hashed(self):
        self.given_set()
        _, casos = evaluacion.cargar_set(self.raiz, True)
        self.assertEqual(len(casos[0]["sha256Foto"]), 64)
        self.assertEqual(len(casos[0]["sha256Esperado"]), 64)

    def test_annotation_rejects_wrong_type(self):
        caso = esperado()
        caso["tipoRegistro"] = "DESCONOCIDO"
        with self.assertRaisesRegex(ValueError, "tipo de registro"):
            evaluacion.validar_esperado(caso)

    def test_annotation_rejects_duplicate_rows(self):
        caso = esperado()
        caso["filas"].append(copy.deepcopy(caso["filas"][0]))
        with self.assertRaisesRegex(ValueError, "renglones"):
            evaluacion.validar_esperado(caso)

    def test_correct_values_and_types_are_counted(self):
        resultado = evaluacion.medir(esperado(), esperado())
        self.assertTrue(resultado["clasificacionCorrecta"])
        self.assertEqual(resultado["datosCorrectos"], 1)
        self.assertEqual(resultado["camposCorrectos"], 1)

    def test_invented_rows_reduce_accuracy(self):
        respuesta = esperado()
        respuesta["filas"].append({"pagina": 1, "renglon": 2,
                                  "datos": {"monto": {"valor": "999", "confianza": 1}}})
        resultado = evaluacion.medir(esperado(), respuesta)
        self.assertEqual(resultado["datosCorrectos"], 1)
        self.assertEqual(resultado["datosEvaluados"], 2)

    def test_null_value_is_not_invented(self):
        anotacion = esperado()
        anotacion["filas"][0]["datos"]["monto"]["valor"] = None
        self.assertEqual(evaluacion.medir(anotacion, anotacion)["datosCorrectos"], 1)

    def test_failed_extraction_is_not_counted_as_correct(self):
        resultado = evaluacion.medir(esperado(), None)
        self.assertEqual(resultado["datosCorrectos"], 0)
        self.assertFalse(resultado["clasificacionCorrecta"])

    def test_simulation_never_passes_real_precision_gate(self):
        manifest, casos, respuestas = self.given_report_inputs(30, "simulado")
        reporte = evaluacion.informe(manifest, casos, respuestas, 90)
        self.assertFalse(reporte["evidenciaReal"])
        self.assertIsNone(reporte["cumpleMeta"])

    def test_target_also_checks_classification(self):
        manifest, casos, respuestas = self.given_report_inputs(30)
        respuestas[0]["lectura"]["tipoRegistro"] = "GASTOS"
        self.assertFalse(evaluacion.informe(manifest, casos, respuestas, 100)["cumpleMeta"])

    def test_unknown_price_is_not_zero(self):
        manifest, casos, respuestas = self.given_report_inputs()
        reporte = evaluacion.informe(manifest, casos, respuestas, None)
        self.assertIsNone(reporte["costoEstimadoUsd"])
        self.assertEqual(reporte["latenciaP95Ms"], 50)

    def test_cost_uses_prices_per_million(self):
        manifest, casos, respuestas = self.given_report_inputs()
        reporte = evaluacion.informe(manifest, casos, respuestas, None, Decimal("2"), Decimal("6"))
        self.assertEqual(reporte["costoEstimadoUsd"], "0.002600")

    def test_failed_attempt_keeps_cost_unknown(self):
        manifest, casos, respuestas = self.given_report_inputs()
        respuestas[0]["lectura"], respuestas[0]["error"] = None, "Timeout"
        reporte = evaluacion.informe(manifest, casos, respuestas, None, Decimal("2"), Decimal("6"))
        self.assertIsNone(reporte["costoEstimadoUsd"])
        self.assertEqual(reporte["porTipo"]["VENTAS"]["erroresExtraccion"], 1)

    def test_missing_results_are_rejected(self):
        manifest, casos, _ = self.given_report_inputs()
        with self.assertRaisesRegex(ValueError, "faltantes"):
            evaluacion.informe(manifest, casos, [], None)

    def test_empty_denominator_has_no_percentage(self):
        self.assertIsNone(evaluacion.porcentaje(0, 0))

    def test_command_fails_with_empty_set(self):
        with patch("sys.argv", [str(SCRIPT), "--set", str(self.raiz)]):
            self.assertEqual(evaluacion.main(), 1)

    @unittest.skipUnless(os.getenv("ENSAMBLA_EVAL_JAR") or os.getenv("ENSAMBLA_EVAL_BUILD"),
                         "Requiere jar compilado o autorización local para compilar la herramienta")
    def test_packaged_command_runs_same_extractor(self):
        self.given_set()
        reporte = self.raiz / "reportes" / "circuito.json"
        comando = [sys.executable, str(SCRIPT), "--set", str(self.raiz), "--permitir-simulado",
                   "--reporte", str(reporte)]
        if os.getenv("ENSAMBLA_EVAL_JAR"):
            comando += ["--jar", os.environ["ENSAMBLA_EVAL_JAR"]]

        resultado = subprocess.run(comando, capture_output=True, text=True, encoding="utf-8", errors="replace")

        self.assertEqual(resultado.returncode, 0, resultado.stdout + resultado.stderr)
        informe = json.loads(reporte.read_text(encoding="utf-8"))
        self.assertTrue(informe["simulado"])
        self.assertFalse(informe["evidenciaReal"])
        self.assertIsNone(informe["cumpleMeta"])
        self.assertEqual(informe["casos"][0]["consumo"]["proveedor"], "simulado")


DESTINO = {"nombre": "Clientes", "campos": [{"nombre": "Nombre", "tipo": "TEXTO"},
                                             {"nombre": "Celular", "tipo": "TEXTO", "opciones": []}]}


def esperado_con_columnas(*columnas):
    anotacion = esperado()
    anotacion["camposNuevos"] = [{"nombre": nombre, "tipo": tipo} for nombre, tipo in columnas]
    return anotacion


def detectada(nombre, tipo="TEXTO"):
    return {"clave": nombre.lower(), "nombre": nombre, "tipo": tipo, "opciones": []}


class ColumnasNuevasTest(unittest.TestCase):
    """ENS-192: la detección de columnas que el bloque destino no tiene (datos sintéticos)."""

    def setUp(self):
        self.temporal = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporal.cleanup)
        self.raiz = Path(self.temporal.name)
        for carpeta in ("fotos", "esperados", "reportes"):
            (self.raiz / carpeta).mkdir()

    def given_caso(self, anotacion, destino=DESTINO, nombre="pagina-000"):
        (self.raiz / "fotos" / f"{nombre}.jpg").write_bytes(b"sintetico")
        (self.raiz / "esperados" / f"{nombre}.json").write_text(json.dumps(anotacion), encoding="utf-8")
        caso = {"id": nombre, "foto": f"fotos/{nombre}.jpg", "esperado": f"esperados/{nombre}.json"}
        if destino is not None:
            caso["destino"] = destino
        return caso

    def write_manifest(self, *casos):
        manifest = {"version": "sintetico-test", "consentimiento": True, "anonimizado": True, "casos": list(casos)}
        (self.raiz / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")
        return manifest

    def respuesta(self, caso, nuevas, proveedor="real"):
        lectura = esperado()
        lectura["consumo"] = {"proveedor": proveedor, "modelo": "prueba", "versionPrompt": "v3",
                              "paginas": 1, "tokensEntrada": 1000, "tokensSalida": 100, "duracionMs": 50}
        return {"id": caso["id"], "lectura": lectura, "error": None, "camposNuevos": nuevas}

    def given_informe(self, caso_manifest, nuevas, meta=None, cantidad=30):
        manifest = self.write_manifest(caso_manifest)
        _, casos = evaluacion.cargar_set(self.raiz, True)
        casos = [dict(casos[0], id=f"{casos[0]['id']}-{n}") for n in range(cantidad)]
        respuestas = [self.respuesta(caso, nuevas) for caso in casos]
        return evaluacion.informe(manifest, casos, respuestas, meta)

    def test_names_are_compared_without_accents_case_or_punctuation(self):
        self.assertEqual(evaluacion.normalizar_nombre(" ¿Pagó? "), "pago")
        self.assertEqual(evaluacion.normalizar_nombre("Teléfono."), evaluacion.normalizar_nombre("TELEFONO"))

    def test_case_with_destination_loads_destination_and_expected_columns(self):
        caso = self.given_caso(esperado_con_columnas(("Dirección", "TEXTO")))
        self.write_manifest(caso)

        _, casos = evaluacion.cargar_set(self.raiz, True)

        self.assertEqual(casos[0]["destino"], DESTINO)
        self.assertEqual(casos[0]["esperado"]["camposNuevos"][0]["nombre"], "Dirección")

    def test_case_without_destination_has_none(self):
        self.write_manifest(self.given_caso(esperado(), destino=None))

        _, casos = evaluacion.cargar_set(self.raiz, True)

        self.assertIsNone(casos[0]["destino"])

    def test_destination_requires_expected_new_columns_even_if_empty(self):
        self.write_manifest(self.given_caso(esperado()))
        with self.assertRaisesRegex(ValueError, "camposNuevos"):
            evaluacion.cargar_set(self.raiz, True)

    def test_new_columns_require_a_destination(self):
        self.write_manifest(self.given_caso(esperado_con_columnas(("Dirección", "TEXTO")), destino=None))
        with self.assertRaisesRegex(ValueError, "sólo tiene sentido"):
            evaluacion.cargar_set(self.raiz, True)

    def test_invalid_destinations_are_rejected(self):
        invalidos = {
            "nombre del bloque": {"nombre": " ", "campos": DESTINO["campos"]},
            "al menos un campo": {"nombre": "Clientes", "campos": []},
            "repetidos": {"nombre": "Clientes", "campos": [{"nombre": "Celular", "tipo": "TEXTO"},
                                                          {"nombre": "CELULAR", "tipo": "TEXTO"}]},
            "tipo de campo": {"nombre": "Clientes", "campos": [{"nombre": "Celular", "tipo": "RELACION"}]},
        }
        for mensaje, destino in invalidos.items():
            with self.subTest(mensaje=mensaje):
                self.write_manifest(self.given_caso(esperado_con_columnas(), destino=destino))
                with self.assertRaisesRegex(ValueError, mensaje):
                    evaluacion.cargar_set(self.raiz, True)

    def test_invalid_expected_new_columns_are_rejected(self):
        for mensaje, columnas in (("repetidas", [("Dirección", "TEXTO"), ("direccion", "TEXTO")]),
                                  ("sin nombre", [("  ", "TEXTO")]),
                                  ("tipo inválido", [("Dirección", "RELACION")])):
            with self.subTest(mensaje=mensaje):
                self.write_manifest(self.given_caso(esperado_con_columnas(*columnas)))
                with self.assertRaisesRegex(ValueError, "columnas nuevas|columna nueva"):
                    evaluacion.cargar_set(self.raiz, True)

    def test_exact_detection_counts_hit_and_type(self):
        medicion = evaluacion.medir_columnas_nuevas(
            esperado_con_columnas(("Dirección", "TEXTO")), self.respuesta({"id": "x"}, [detectada("DIRECCION")]))

        self.assertEqual((medicion["aciertos"], medicion["tiposCorrectos"], medicion["falsosPositivos"],
                          medicion["omitidas"], medicion["exacto"]), (1, 1, 0, 0, True))

    def test_wrong_type_is_a_hit_but_not_a_correct_type(self):
        medicion = evaluacion.medir_columnas_nuevas(
            esperado_con_columnas(("Edad", "NUMERO")), self.respuesta({"id": "x"}, [detectada("Edad", "TEXTO")]))

        self.assertEqual((medicion["aciertos"], medicion["tiposCorrectos"]), (1, 0))

    def test_expected_column_without_type_accepts_any_type(self):
        medicion = evaluacion.medir_columnas_nuevas(
            esperado_con_columnas(("Edad", None)), self.respuesta({"id": "x"}, [detectada("Edad", "TEXTO")]))

        self.assertEqual(medicion["tiposCorrectos"], 1)

    def test_duplicating_an_existing_column_is_a_false_positive(self):
        medicion = evaluacion.medir_columnas_nuevas(
            esperado_con_columnas(), self.respuesta({"id": "x"}, [detectada("Celular")]))

        self.assertEqual((medicion["falsosPositivos"], medicion["exacto"]), (1, False))

    def test_missing_new_column_is_counted_as_omitted(self):
        medicion = evaluacion.medir_columnas_nuevas(
            esperado_con_columnas(("Dirección", "TEXTO")), self.respuesta({"id": "x"}, []))

        self.assertEqual((medicion["omitidas"], medicion["exacto"]), (1, False))

    def test_expecting_none_and_detecting_none_is_exact(self):
        medicion = evaluacion.medir_columnas_nuevas(esperado_con_columnas(), self.respuesta({"id": "x"}, []))

        self.assertTrue(medicion["exacto"])

    def test_failed_extraction_is_never_exact(self):
        respuesta = {"id": "x", "lectura": None, "error": "Falla", "camposNuevos": []}

        self.assertFalse(evaluacion.medir_columnas_nuevas(esperado_con_columnas(), respuesta)["exacto"])

    def test_report_summarizes_new_columns_with_precision_and_recall(self):
        caso = self.given_caso(esperado_con_columnas(("Dirección", "TEXTO"), ("Edad", "NUMERO")))

        reporte = self.given_informe(caso, [detectada("Dirección"), detectada("Marca")], cantidad=2)

        resumen = reporte["columnasNuevas"]
        self.assertEqual((resumen["casos"], resumen["esperadas"], resumen["detectadas"], resumen["aciertos"]),
                         (2, 4, 4, 2))
        self.assertEqual((resumen["precisionPct"], resumen["recallPct"], resumen["tipoCorrectoPct"]),
                         (50.0, 50.0, 100.0))
        self.assertEqual(resumen["casosExactosPct"], 0.0)
        self.assertEqual(reporte["casos"][0]["columnasNuevas"]["falsosPositivos"], 1)

    def test_report_without_destinations_has_no_new_column_summary(self):
        self.write_manifest(self.given_caso(esperado(), destino=None))
        manifest = json.loads((self.raiz / "manifest.json").read_text(encoding="utf-8"))
        _, casos = evaluacion.cargar_set(self.raiz, True)
        respuestas = [self.respuesta(casos[0], [])]

        reporte = evaluacion.informe(manifest, casos, respuestas, None)

        self.assertIsNone(reporte["columnasNuevas"])
        self.assertNotIn("columnasNuevas", reporte["casos"][0])

    def test_goal_also_requires_new_column_detection(self):
        caso = self.given_caso(esperado_con_columnas(("Dirección", "TEXTO")))

        mala = self.given_informe(caso, [detectada("Marca")], meta=100)
        buena = self.given_informe(caso, [detectada("Dirección")], meta=100)

        self.assertFalse(mala["cumpleMeta"])
        self.assertTrue(buena["cumpleMeta"])

    def test_goal_ignores_a_metric_without_base(self):
        caso = self.given_caso(esperado_con_columnas())

        reporte = self.given_informe(caso, [], meta=100)

        self.assertIsNone(reporte["columnasNuevas"]["recallPct"])
        self.assertTrue(reporte["cumpleMeta"])

    def test_simulation_with_destination_never_claims_real_evidence(self):
        caso = self.given_caso(esperado_con_columnas(("Dirección", "TEXTO")))
        manifest = self.write_manifest(caso)
        _, casos = evaluacion.cargar_set(self.raiz, True)
        respuestas = [self.respuesta(casos[0], [detectada("Dirección")], proveedor="simulado")]

        reporte = evaluacion.informe(manifest, casos, respuestas, 90)

        self.assertTrue(reporte["simulado"])
        self.assertFalse(reporte["evidenciaReal"])


if __name__ == "__main__":
    unittest.main()
