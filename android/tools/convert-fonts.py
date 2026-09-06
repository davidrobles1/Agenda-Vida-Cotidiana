#!/usr/bin/env python3
"""
Convierte a TTF las fuentes WOFF2 que la Web ya trae, para Android.

POR QUÉ EXISTE
--------------
Las nueve agendas (ADR-023) usan las mismas familias en Web y Android. La Web
las sirve desde `@fontsource` en formato WOFF2, que Android no sabe leer:
`res/font` admite TTF y OTF. Este script hace esa traducción una sola vez.

NO forma parte del build. Gradle no lo invoca, el APK no lo contiene y nadie
necesita ejecutarlo para compilar el proyecto: los 31 `.ttf` que produce ya
están versionados en `android/app/src/main/res/font/`. Solo hace falta
volver a correrlo si se añade una familia o un grosor nuevo.

REQUISITOS
----------
    pip install fonttools brotli

`brotli` no es opcional: es el algoritmo con el que WOFF2 comprime, y sin él
fontTools abre el archivo pero no puede descomprimirlo.

USO
---
    python3 android/tools/convert-fonts.py \\
        web/node_modules/@fontsource \\
        android/app/src/main/res/font

Los nombres de destino se normalizan al convenio de recursos de Android
(minúsculas, guion bajo, sin dígitos iniciales), que es más estricto que el de
npm: `Archivo Black-400.woff2` no es un nombre de recurso válido y
`archivo_black.ttf` sí.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    from fontTools.ttLib import TTFont
except ImportError:
    sys.exit("Falta fontTools. Instala:  pip install fonttools brotli")


def android_resource_name(source: Path) -> str:
    """`Fraunces-500-italic.woff2` → `fraunces_500_italic.ttf`."""
    stem = source.stem.lower()
    stem = re.sub(r"[^a-z0-9]+", "_", stem).strip("_")
    # Un recurso de Android no puede empezar por dígito.
    if stem and stem[0].isdigit():
        stem = f"f_{stem}"
    return f"{stem}.ttf"


def convert(source: Path, destination: Path) -> None:
    font = TTFont(source)
    # `flavor = None` escribe TTF plano en vez de reempaquetar en WOFF2.
    font.flavor = None
    font.save(destination)


def main() -> int:
    if len(sys.argv) != 3:
        sys.exit(f"Uso: {sys.argv[0]} <directorio-woff2> <directorio-destino>")

    source_root = Path(sys.argv[1])
    destination_root = Path(sys.argv[2])
    if not source_root.is_dir():
        sys.exit(f"No existe: {source_root}")
    destination_root.mkdir(parents=True, exist_ok=True)

    converted = 0
    for woff2 in sorted(source_root.rglob("*.woff2")):
        target = destination_root / android_resource_name(woff2)
        if target.exists():
            continue
        try:
            convert(woff2, target)
        except Exception as error:  # noqa: BLE001 — se reporta y se sigue
            print(f"  ✗ {woff2.name}: {error}", file=sys.stderr)
            continue
        converted += 1
        print(f"  ✓ {woff2.name} → {target.name}")

    print(f"\n{converted} fuentes convertidas en {destination_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
