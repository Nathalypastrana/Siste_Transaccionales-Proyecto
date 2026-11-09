#!/usr/bin/env python3
"""
Prueba de rendimiento del sistema (RNF 05). Solo usa la biblioteca estándar de Python 3.

Antes: ejecutar database/datos_rendimiento.sql (100 habitaciones P-001..P-100 y 10 000 reservas) y arrancar el backend.

Uso:
  export API_CORREO=...  API_PASSWORD=...        # un usuario del sistema
  python3 pruebas/rendimiento.py                 # carga mixta de 60 s (rápida)
  python3 pruebas/rendimiento.py --carga-segundos 300   # la duración que pide el documento
  python3 pruebas/rendimiento.py --desde 2027-02-01     # si tus tarifas vigentes no cubren los próximos 90 días

Escenarios y umbrales (tabla "Criterios cuantitativos de rendimiento" del documento):
  1. Consulta de disponibilidad, 30 repeticiones ........ p95 <= 2 s y máximo <= 5 s
  2. Crear / modificar / cancelar, 30 repeticiones c/u .. p95 <= 3 s
  3. Reservas simultáneas misma habitación y fechas ..... 2 sesiones x30 y 10 sesiones x10:
                                                           exactamente una reserva (201) en el 100 %, respuesta <= 5 s
  4. Carga mixta, 20 sesiones .......................... p95 <= 3 s, 0 errores de integridad (5xx)
La primera ejecución de cada escenario se descarta como calentamiento.
"""
import argparse
import json
import math
import os
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import date, timedelta

API = os.environ.get("API", "http://localhost:8080")


def llamar(metodo, ruta, token=None, cuerpo=None, timeout=30):
    """Devuelve (codigo_http, json_o_texto, segundos)."""
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            texto, codigo = r.read().decode(), r.status
    except urllib.error.HTTPError as e:
        texto, codigo = e.read().decode(), e.code
    except Exception as e:  # sin conexión, tiempo agotado...
        return 0, str(e), time.perf_counter() - t0
    dt = time.perf_counter() - t0
    try:
        return codigo, json.loads(texto), dt
    except ValueError:
        return codigo, texto, dt


def p95(valores):
    v = sorted(valores)
    return v[max(0, math.ceil(0.95 * len(v)) - 1)]


def linea(nombre, tiempos, umbral_p95=None, umbral_max=None):
    ok = True
    if umbral_p95 is not None and p95(tiempos) > umbral_p95:
        ok = False
    if umbral_max is not None and max(tiempos) > umbral_max:
        ok = False
    prom = sum(tiempos) / len(tiempos)
    print(f"  {'OK ' if ok else 'FALLA'} {nombre:<34} n={len(tiempos):<3} prom={prom:5.3f}s  p95={p95(tiempos):5.3f}s  max={max(tiempos):5.3f}s")
    return ok


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--carga-segundos", type=int, default=60)
    ap.add_argument("--desde", help="Fecha inicial AAAA-MM-DD de las reservas de prueba. Debe estar dentro de una "
                                    "tarifa vigente (por defecto, 30 días a partir de hoy).")
    args = ap.parse_args()

    correo, clave = os.environ.get("API_CORREO"), os.environ.get("API_PASSWORD")
    if not correo or not clave:
        sys.exit("Define API_CORREO y API_PASSWORD.")
    codigo, resp, _ = llamar("POST", "/api/auth/login", cuerpo={"correo": correo, "password": clave})
    if codigo != 200:
        sys.exit(f"No se pudo iniciar sesión ({codigo}): {resp}")
    token = resp["token"]

    _, habitaciones, _ = llamar("GET", "/api/habitaciones", token)
    rooms = [h["id"] for h in habitaciones if str(h["numero"]).startswith("P-")]
    if len(rooms) < 100:
        sys.exit("Faltan las habitaciones P-001..P-100: ejecuta database/datos_rendimiento.sql.")
    _, huespedes, _ = llamar("GET", "/api/huespedes", token)
    huesped = huespedes[0]["id"]

    # Las reservas necesitan una tarifa vigente: se usan fechas cercanas y distintas en cada ejecución
    base = date.fromisoformat(args.desde) if args.desde else \
        date.today() + timedelta(days=30 + (int(time.time()) // 60) % 60)

    def fechas(desplazamiento, noches=2):
        e = base + timedelta(days=desplazamiento)
        return e.isoformat(), (e + timedelta(days=noches)).isoformat()

    def reservar(sala, desp, noches=2, tk=token):
        e, s = fechas(desp, noches)
        return llamar("POST", "/api/reservas", tk,
                      {"huespedId": huesped, "habitacionId": sala, "fechaEntrada": e, "fechaSalida": s})

    resultados = []
    print(f"API: {API}   Habitaciones de prueba: {len(rooms)}   Fechas desde {base}\n")

    # 1. Disponibilidad
    print("1. Consulta de disponibilidad (1 sesión, 30 repeticiones)")
    t = []
    for i in range(31):
        e, s = fechas(20 + i)
        c, _, dt = llamar("GET", f"/api/habitaciones/disponibles?entrada={e}&salida={s}", token)
        if c != 200:
            print(f"  respuesta inesperada {c}")
        if i > 0:
            t.append(dt)
    resultados.append(linea("disponibilidad", t, 2, 5))

    # 2. Crear / modificar / cancelar (una sesión, sin competencia por la habitación)
    print("\n2. Crear, modificar y cancelar (1 sesión, 30 repeticiones de cada operación)")
    crear, modificar, cancelar = [], [], []
    for i in range(31):
        sala = rooms[i]
        c, r, dt = reservar(sala, 10)
        if c != 201:
            print(f"  crear: respuesta inesperada {c}: {r}")
            continue
        if i > 0:
            crear.append(dt)
        e, s = fechas(14)
        c, _, dt = llamar("PATCH", f"/api/reservas/{r['id']}", token, {"fechaEntrada": e, "fechaSalida": s})
        if c != 200:
            print(f"  modificar: respuesta inesperada {c}")
        elif i > 0:
            modificar.append(dt)
        c, _, dt = llamar("PATCH", f"/api/reservas/{r['id']}/cancelar", token)
        if c != 200:
            print(f"  cancelar: respuesta inesperada {c}")
        elif i > 0:
            cancelar.append(dt)
    for nombre, lista in (("crear reserva", crear), ("modificar reserva", modificar), ("cancelar reserva", cancelar)):
        resultados.append(linea(nombre, lista, 3) if lista else False)

    # 3. Reservas simultáneas sobre la misma habitación y fechas
    def simultaneas(sesiones, repeticiones, offset):
        exactas, tiempos = 0, []
        for i in range(repeticiones):
            sala = rooms[40 + (i % 50)]
            desp = offset + i * 3
            barrera = threading.Barrier(sesiones)

            def disparar(_):
                barrera.wait()
                return reservar(sala, desp)

            with ThreadPoolExecutor(sesiones) as ex:
                salidas = list(ex.map(disparar, range(sesiones)))
            creadas = sum(1 for c, _, _ in salidas if c == 201)
            otras_ok = all(c in (201, 409) for c, _, _ in salidas)
            if creadas == 1 and otras_ok:
                exactas += 1
            tiempos += [dt for _, _, dt in salidas]
        ok = exactas == repeticiones and max(tiempos) <= 5
        print(f"  {'OK ' if ok else 'FALLA'} {sesiones} sesiones x{repeticiones}: exactamente 1 reserva en "
              f"{exactas}/{repeticiones}   max={max(tiempos):.3f}s")
        return ok

    print("\n3. Reservas simultáneas sobre la misma habitación y fechas")
    resultados.append(simultaneas(2, 30, 100))
    resultados.append(simultaneas(10, 10, 300))

    # 4. Carga mixta
    print(f"\n4. Carga mixta (20 sesiones durante {args.carga_segundos} s: consultas y reservas en habitaciones distintas)")
    fin = time.time() + args.carga_segundos
    tiempos, errores5xx, otros = [], [0], [0]
    cerrojo = threading.Lock()

    def sesion(n):
        sala_base = n  # cada sesión usa sus propias habitaciones
        k = 0
        while time.time() < fin:
            if k % 3 == 0:
                sala = rooms[(sala_base + (k // 3) * 20) % 100]
                c, _, dt = reservar(sala, 20 + (k * 3) % 150)
            else:
                e, s = fechas(20 + k % 150)
                c, _, dt = llamar("GET", f"/api/habitaciones/disponibles?entrada={e}&salida={s}", token)
            with cerrojo:
                tiempos.append(dt)
                if c == 0 or c >= 500:
                    errores5xx[0] += 1
                elif c not in (200, 201, 409):
                    otros[0] += 1
            k += 1

    with ThreadPoolExecutor(20) as ex:
        list(ex.map(sesion, range(20)))
    ok = linea("mixta", tiempos, 3) and errores5xx[0] == 0
    print(f"  errores de servidor o sin respuesta: {errores5xx[0]}   respuestas no previstas: {otros[0]}")
    resultados.append(ok)

    print("\nVerifica además con database/verificar_flujo.sql que no haya reservas activas superpuestas (debe dar 0 filas).")
    todo = all(resultados)
    print("\nRESULTADO GLOBAL:", "CUMPLE los umbrales." if todo else "NO cumple algún umbral: documenta la causa y el ajuste.")
    sys.exit(0 if todo else 1)


if __name__ == "__main__":
    main()
