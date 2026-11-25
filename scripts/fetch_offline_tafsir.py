#!/usr/bin/env python3
"""Fetch and verify the Arabic Al-Muyassar tafsir package for the Android build."""
from __future__ import annotations

import gzip
import hashlib
import json
import pathlib
import urllib.request

URL = "https://raw.githubusercontent.com/m-alhamry/Quran-text-assets/main/data/arabic_moyassar/1.0.0-3d110990721d30c0.json.gz"
EXPECTED_SHA256 = "3d110990721d30c07276c0e89494dcaece7828a16ee7d27a12caa67eb5e2a4b4"
EXPECTED_COMPRESSED_BYTES = 570486
EXPECTED_UNCOMPRESSED_BYTES = 2702135
DESTINATION = pathlib.Path("app/src/main/assets/arabic_moyassar.json.gz")


def main() -> None:
    request = urllib.request.Request(URL, headers={"User-Agent": "YaqeenAndroidBuild/1.0"})
    with urllib.request.urlopen(request, timeout=45) as response:
        compressed = response.read()
    if len(compressed) != EXPECTED_COMPRESSED_BYTES:
        raise SystemExit(f"Unexpected tafsir package size: {len(compressed)}")
    digest = hashlib.sha256(compressed).hexdigest()
    if digest != EXPECTED_SHA256:
        raise SystemExit(f"Unexpected tafsir SHA-256: {digest}")
    raw = gzip.decompress(compressed)
    if len(raw) != EXPECTED_UNCOMPRESSED_BYTES:
        raise SystemExit(f"Unexpected decompressed size: {len(raw)}")
    payload = json.loads(raw)
    verses = payload.get("verses")
    if not isinstance(verses, dict) or len(verses) != 114:
        raise SystemExit("Tafsir package does not contain all 114 surahs")
    total = 0
    for surah_number in range(1, 115):
        surah = verses.get(str(surah_number))
        if not isinstance(surah, dict) or not surah:
            raise SystemExit(f"Missing tafsir surah {surah_number}")
        for verse_number, entry in surah.items():
            if not isinstance(entry, dict) or not str(entry.get("text", "")).strip():
                raise SystemExit(f"Missing tafsir text at {surah_number}:{verse_number}")
            total += 1
    if total != 6236:
        raise SystemExit(f"Expected 6236 tafsir verses, found {total}")
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    DESTINATION.write_bytes(compressed)
    print(f"Verified Al-Muyassar: {total} verses, SHA-256 {digest}")


if __name__ == "__main__":
    main()
