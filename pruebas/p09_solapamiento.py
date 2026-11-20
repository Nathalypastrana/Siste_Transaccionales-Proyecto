#!/usr/bin/env python3
"""P09 - Solapamiento de reservas.

Crea una reserva base en una habitación libre y luego intenta reservar la misma habitación en fechas
que se cruzan de distintas formas. Todas deben responder 409 sin crear nada; la reserva que empieza
justo el día de salida de la base sí debe crearse (la noche de salida no está ocupada).
Al final cancela las reservas que creó.

Uso (desde la raíz del proyecto, con el backend corriendo):
  API_CORREO=... API_PASSWORD=... python3 pruebas/p09_solapamiento.py
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import date, datetime

API = os.environ.get("API", "http://localhost:8080")


def llamar(metodo, ruta, token=None, cuerpo=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            texto, codigo = r.read().decode(), r.status
    except urllib.error.HTTPError as e:
        texto, codigo = e.read().decode(), e.code
    try:
        return codigo, json.loads(texto)
    except ValueError:
        return codigo, texto


def detalle(cuerpo):
    if isinstance(cuerpo, dict):
        return cuerpo.get("detail") or cuerpo.get("mensaje") or ""
    return str(cuerpo)[:100]


def main():
    correo, clave = os.environ.get("API_CORREO"), os.environ.get("API_PASSWORD")
    if not correo or not clave:
        sys.exit("Define API_CORREO y API_PASSWORD (recepcionista o administrador).")
    c, r = llamar("POST", "/api/auth/login", cuerpo={"correo": correo, "password": clave})
    if c != 200:
        sys.exit(f"No se pudo iniciar sesión: HTTP {c} {detalle(r)}")
    token = r["token"]

    # Base: 10 al 13 de junio de 2027. Se elige una habitación libre en toda la ventana 08-16.
    base_e, base_s = date(2027, 6, 10), date(2027, 6, 13)
    c, libres = llamar("GET", "/api/habitaciones/disponibles?entrada=2027-06-08&salida=2027-06-16", token)
    if c != 200 or not libres:
        sys.exit(f"No hay habitaciones libres del 8 al 16 de junio de 2027 (HTTP {c}).")
    hab = libres[0]
    c, huespedes = llamar("GET", "/api/huespedes", token)
    # Se prefiere un huésped de demostración (correo @example.com) para no enviar avisos a personas reales.
    demo = [h for h in huespedes if (h.get("correo") or "").endswith("@example.com")]
    huesped = (demo or huespedes)[0]

    commit = subprocess.run(["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True).stdout.strip()
    print(f"P09 - Solapamiento · {datetime.now():%Y-%m-%d %H:%M} · commit {commit}")
    print(f"Habitación {hab.get('numero')} (id {hab['id']}) · huésped id {huesped['id']}\n")

    creadas = []

    def reservar(e, s):
        return llamar("POST", "/api/reservas", token, {
            "habitacionId": hab["id"], "huespedId": huesped["id"],
            "fechaEntrada": e.isoformat(), "fechaSalida": s.isoformat()})

    c, r = reservar(base_e, base_s)
    if c != 201:
        sys.exit(f"No se pudo crear la reserva base: HTTP {c} {detalle(r)}")
    creadas.append(r["id"])
    print(f"Reserva base #{r['id']}: {base_e} a {base_s} (3 noches) -> HTTP 201\n")

    casos = [
        ("Mismas fechas", date(2027, 6, 10), date(2027, 6, 13), 409),
        ("Cruce al inicio (entra antes, sale dentro)", date(2027, 6, 8), date(2027, 6, 11), 409),
        ("Cruce al final (entra dentro, sale después)", date(2027, 6, 12), date(2027, 6, 15), 409),
        ("Dentro de la base (11 a 12)", date(2027, 6, 11), date(2027, 6, 12), 409),
        ("Contiene a la base (9 a 14)", date(2027, 6, 9), date(2027, 6, 14), 409),
        ("Límite: entra el día de salida de la base (13 a 15)", date(2027, 6, 13), date(2027, 6, 15), 201),
        ("Límite: sale el día de entrada de la base (8 a 10)", date(2027, 6, 8), date(2027, 6, 10), 201),
    ]
    todo = True
    for nombre, e, s, esperado in casos:
        c, r = reservar(e, s)
        if c == 201:
            creadas.append(r["id"])
        ok = c == esperado
        todo &= ok
        print(f"  {'OK ' if ok else 'FALLA'} {nombre:<52} {e} a {s}  HTTP {c} (esperado {esperado})  {detalle(r) if c != 201 else '#' + str(r['id'])}")

    # En la BD no debe quedar ningún cruce activo para esa habitación
    c, todas = llamar("GET", f"/api/reservas?habitacionId={hab['id']}", token)
    activas = [x for x in todas if x["habitacionId"] == hab["id"] and x["id"] in creadas]
    cruces = [(a["id"], b["id"]) for i, a in enumerate(activas) for b in activas[i + 1:]
              if a["fechaEntrada"] < b["fechaSalida"] and b["fechaEntrada"] < a["fechaSalida"]]
    print(f"\nReservas creadas en la prueba: {len(creadas)} (esperado 3: base + 2 de límite) · cruces entre ellas: {len(cruces)}")
    todo &= len(creadas) == 3 and not cruces

    for rid in creadas:
        llamar("PATCH", f"/api/reservas/{rid}/cancelar", token)
    print(f"Limpieza: canceladas {', '.join('#' + str(x) for x in creadas)}")
    print("\nRESULTADO:", "CUMPLE" if todo else "NO CUMPLE")


if __name__ == "__main__":
    main()
