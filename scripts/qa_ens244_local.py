"""QA reproducible de ENS-244 contra frontend y backend locales reales."""
import json
import time
import uuid
from urllib.parse import urlparse
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE = "http://127.0.0.1:5173"
OUT = Path(__file__).resolve().parents[1] / "docs/qa/ens244-2026-10-26"
OUT.mkdir(parents=True, exist_ok=True)

def uid():
    return "e" + str(uuid.uuid4())

def campo(key, tipo="text"):
    return {"key": key, "label": key, "type": tipo, "required": False}

def bloque(tipo, nombre, campos, filas):
    return {"id": uid(), "type": tipo, "name": nombre, "description": nombre,
            "color": "petrol", "width": "half", "fields": campos, "rows": filas, "content": ""}

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(base_url=BASE, viewport={"width": 1440, "height": 1100})
    page = context.new_page()
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    requests = []
    listener = lambda request: requests.append(request.url.split("/api/")[-1]) if urlparse(request.url).path.startswith("/api/") else None
    page.goto("/")
    page.get_by_role("link", name="Ensambla, ir al inicio", exact=True).wait_for()
    page.set_viewport_size({"width": 360, "height": 800})
    assert page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "Overflow landing"
    page.get_by_role("button", name="Abrir menú", exact=True).click()
    page.get_by_role("link", name="Planes", exact=True).click()
    page.set_viewport_size({"width": 1440, "height": 1100})
    expect(page.get_by_role("article", name="Plan Premium", exact=True)).to_be_visible()
    page.on("request", listener)
    page.reload()
    expect(page.get_by_role("article", name="Plan Premium", exact=True)).to_be_visible()
    page.wait_for_load_state("networkidle")
    assert set(requests) <= {"auth/me", "planes/catalogo"}, requests
    page.remove_listener("request", listener)
    page.keyboard.press("Tab")
    expect(page.get_by_role("link", name="Ir al contenido", exact=True)).to_be_focused()
    page.keyboard.press("Enter")
    expect(page.locator("#main")).to_be_focused()
    page.keyboard.press("Tab")
    page.evaluate("document.activeElement.blur()")
    page.evaluate("window.scrollTo(0,0)")
    page.screenshot(path=str(OUT / "planes-publicos-desktop.png"), full_page=True)
    page.set_viewport_size({"width": 360, "height": 800})
    page.screenshot(path=str(OUT / "planes-publicos-mobile.png"), full_page=True)
    assert page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "Overflow planes"
    assert context.request.get("/api/planes").status == 401
    assert context.request.get("/api/planes/renovacion").status == 401
    email = f"ens244-{int(time.time())}@example.test"
    password = "Ens244Local2026!"
    csrf = context.request.get("/api/auth/csrf").json()
    headers = {csrf["headerName"]: csrf["token"]}
    response = context.request.post("/api/auth/register", headers=headers, data={
        "nombre": "Prueba ENS-244", "nombreNegocio": "Almacén ENS-244", "email": email, "contrasenia": password})
    assert response.status == 201, response.text()
    response = context.request.post("/api/auth/login", headers=headers, data={"email": email, "contrasenia": password})
    assert response.ok, response.text()
    usuario = response.json()
    csrf = context.request.get("/api/auth/csrf").json()
    headers = {csrf["headerName"]: csrf["token"]}
    response = context.request.put("/api/preferencias", headers=headers, data={"bienvenidaVista": True, "grupos": {}, "descartadas": []})
    assert response.ok, response.text()
    ventas = bloque("sales", "Ventas", [campo("quantity", "number"), campo("price", "currency")], [
        {"id": uid(), "quantity": 4, "price": 2500},
        {"id": uid(), "quantity": 100, "price": 10000, "__example": True}])
    inventario = bloque("inventory", "Inventario", [campo("name"), campo("quantity", "number"), campo("minimum", "number")], [
        {"id": uid(), "name": "Yerba", "quantity": 2, "minimum": 5},
        {"id": uid(), "name": "Arroz", "quantity": 10, "minimum": 3}])
    pedidos = bloque("orders", "Pedidos", [campo("name"), campo("total", "currency"), campo("status")], [
        {"id": uid(), "name": "Pedido uno", "total": 100000, "status": "Pendiente"},
        {"id": uid(), "name": "Pedido dos", "total": 20000, "status": "Entregado"}])
    clientes = bloque("clients", "Clientes", [campo("name")], [{"id": uid(), "name": "Cliente de prueba"}])
    now = "2026-10-26T21:00:00.000Z"
    sistema = {"id": uid(), "name": "Almacén ENS-244", "createdAt": now, "updatedAt": now,
               "sections": [{"id": uid(), "name": "Mi negocio", "blocks": [ventas, pedidos, inventario, clientes]}]}
    response = context.request.put(f"/api/sistemas/{sistema['id']}", headers=headers, data=sistema)
    assert response.ok, response.text()
    page.set_viewport_size({"width": 1440, "height": 1100})
    page.goto("/inicio")
    page.wait_for_load_state("networkidle")
    expect(page.get_by_role("heading", name="Cómo viene tu negocio", exact=True)).to_be_visible()
    venta = page.get_by_role("article", name="Ventas registradas", exact=True)
    expect(venta.locator(".inicio-metrica-valor")).to_have_text("$ 10.000,00")
    expect(page.get_by_role("article", name="Pedidos pendientes", exact=True).locator(".inicio-metrica-valor")).to_have_text("1")
    expect(page.get_by_role("article", name="Productos con stock bajo", exact=True).locator(".inicio-metrica-valor")).to_have_text("1")
    venta.get_by_role("button", name="Definir meta", exact=True).click()
    venta.get_by_role("spinbutton").fill("15000")
    venta.get_by_role("button", name="Guardar meta", exact=True).click()
    expect(venta.get_by_role("progressbar")).to_have_attribute("value", "10000")
    expect(venta.get_by_role("button", name="Cambiar meta")).to_be_focused()
    page.reload(); page.wait_for_load_state("networkidle")
    expect(venta.get_by_text("Meta en curso", exact=False)).to_be_visible()
    page.screenshot(path=str(OUT / "home-desktop.png"), full_page=True)
    page.screenshot(path=str(OUT / "home-principal.png"))
    page.set_viewport_size({"width": 360, "height": 800})
    page.screenshot(path=str(OUT / "home-mobile.png"), full_page=True)
    assert page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "Overflow home"
    bajo = page.get_by_role("article", name="Productos con stock bajo", exact=True)
    bajo.get_by_role("button", name="Definir meta", exact=True).click()
    bajo.get_by_role("spinbutton").fill("1")
    bajo.get_by_role("button", name="Guardar meta", exact=True).click()
    expect(bajo.get_by_text("✓ Meta alcanzada", exact=False)).to_be_visible()
    page.goto("/planes"); page.wait_for_load_state("networkidle")
    expect(page.get_by_role("heading", name="Gratuito, tu espacio actual", exact=True)).to_be_visible()
    expect(page.get_by_role("button", name="Probar Premium por 7 días", exact=True)).to_be_enabled()
    page.get_by_role("button", name="Probar Premium por 7 días", exact=True).click()
    expect(page.get_by_role("dialog")).to_be_visible()
    page.keyboard.press("Escape")
    expect(page.get_by_role("dialog")).to_have_count(0)
    page.evaluate("document.activeElement.blur(); window.scrollTo(0,0)")
    page.screenshot(path=str(OUT / "planes-cuenta-mobile.png"), full_page=True)
    assert page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "Overflow planes cuenta"
    assert not errors, errors
    resultado = {"resultado": "OK", "entorno": "backend y PostgreSQL locales reales", "usuarioPrueba": usuario["id"],
                 "sistemaPrueba": sistema["id"], "solicitudesPublicas": sorted(set(requests)),
                 "ventas": 10000, "pedidosPendientes": 1, "stockBajo": 1,
                 "metas": "persistencia, progreso, foco y máximo alcanzado", "erroresJS": errors}
    (OUT / "resultado.json").write_text(json.dumps(resultado, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(resultado, ensure_ascii=False))
    browser.close()
