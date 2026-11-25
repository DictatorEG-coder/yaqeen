#!/usr/bin/env python3
"""Build a compact Arabic-only SQLite hadith library from Open Hadith Data."""
from __future__ import annotations

import io
import json
import pathlib
import re
import sqlite3
import urllib.request
import zipfile

URL = "https://github.com/Jaguar16/open-hadith-data/releases/download/v1.1.0/collections-json.zip"
EXPECTED_COLLECTIONS = {
    "bukhari", "muslim", "nasai", "abudawud", "tirmidhi", "ibnmajah",
    "malik", "ahmad", "darimi", "adab", "riyadussalihin", "bulugh",
    "shamail", "mishkat", "nawawi40", "qudsi40", "shahwaliullah40",
}
DESTINATION = pathlib.Path("app/src/main/assets/hadith_ar.db")


def normalize_arabic(value: str) -> str:
    value = re.sub(r"[\u064B-\u065F\u0670\u0640\u200B-\u200F\uFEFF]", "", value or "")
    return (value.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
                 .replace("ٱ", "ا").replace("ى", "ي").replace("ؤ", "و").replace("ئ", "ي"))


def clean(value):
    if value is None:
        return None
    value = str(value).strip()
    return value if value and value.lower() != "none" else None


def main() -> None:
    request = urllib.request.Request(URL, headers={"User-Agent": "YaqeenAndroidBuild/1.0"})
    with urllib.request.urlopen(request, timeout=90) as response:
        archive_bytes = response.read()

    db_path = DESTINATION
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()
    db = sqlite3.connect(db_path)
    db.executescript("""
        PRAGMA journal_mode=OFF;
        PRAGMA synchronous=OFF;
        PRAGMA temp_store=MEMORY;
        CREATE TABLE collections (
            id TEXT PRIMARY KEY, name_ar TEXT NOT NULL, total_hadiths INTEGER NOT NULL,
            collection_type TEXT NOT NULL
        );
        CREATE TABLE books (
            collection_id TEXT NOT NULL, book_number TEXT NOT NULL,
            name_ar TEXT NOT NULL, total_hadiths INTEGER NOT NULL,
            PRIMARY KEY(collection_id, book_number)
        );
        CREATE TABLE hadiths (
            id INTEGER PRIMARY KEY,
            collection_id TEXT NOT NULL,
            book_number TEXT NOT NULL DEFAULT '',
            chapter_number TEXT,
            hadith_number TEXT NOT NULL,
            reference TEXT,
            chapter_name_ar TEXT,
            text_ar TEXT NOT NULL,
            narrator TEXT,
            grade_ar TEXT,
            source_reference TEXT,
            url_source TEXT,
            search_text TEXT NOT NULL
        );
        CREATE INDEX idx_hadith_collection_book ON hadiths(collection_id, book_number, id);
    """)

    seen = set()
    total = 0
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as archive:
        json_files = [name for name in archive.namelist() if name.lower().endswith(".json")]
        # Prefer the combined collection files over per-book JSON duplicates.
        json_files.sort(key=lambda name: ("collections" not in name.lower(), name))
        for filename in json_files:
            if not filename.lower().endswith(".json"):
                continue
            try:
                payload = json.loads(archive.read(filename).decode("utf-8-sig"))
            except (UnicodeDecodeError, json.JSONDecodeError):
                continue
            if not isinstance(payload, dict):
                continue
            metadata = payload.get("collection") or {}
            collection_id = clean(metadata.get("id"))
            if collection_id not in EXPECTED_COLLECTIONS or collection_id in seen:
                continue
            name_ar = clean(metadata.get("name_ar")) or collection_id
            collection_type = clean(metadata.get("type")) or ("compilation" if collection_id in {
                "riyadussalihin", "bulugh", "shamail", "mishkat"
            } else "primary")
            chapter_names = {}
            collection_count = 0
            books_to_insert = []
            hadith_rows = []

            def add_hadith(item, book_number="", book_name="", local_chapters=None):
                nonlocal collection_count
                text_ar = clean(item.get("text_ar")) or clean(item.get("matn_ar"))
                if not text_ar:
                    return
                number = clean(item.get("hadith_number")) or str(collection_count + 1)
                chapter_number = clean(item.get("chapter_number"))
                chapter_name = (local_chapters or {}).get(chapter_number) if chapter_number else None
                matn_ar = clean(item.get("matn_ar"))
                narrator = clean(item.get("narrator"))
                grade_ar = clean(item.get("grade_ar"))
                reference = clean(item.get("reference"))
                source_reference = clean(item.get("source_reference"))
                url_source = clean(item.get("url_source"))
                searchable = normalize_arabic(" ".join(filter(None, [
                    matn_ar or text_ar, number, chapter_name, book_name, narrator, reference
                ])))
                hadith_rows.append((
                    collection_id, book_number, chapter_number, number, reference,
                    chapter_name, text_ar, narrator, grade_ar,
                    source_reference, url_source, searchable
                ))
                collection_count += 1

            books = payload.get("books") or []
            if isinstance(books, list):
                for book in books:
                    if not isinstance(book, dict):
                        continue
                    book_number = clean(book.get("book_number")) or str(len(books_to_insert) + 1)
                    book_name = clean(book.get("name_ar")) or f"كتاب {book_number}"
                    local_chapters = {}
                    for chapter in book.get("chapters") or []:
                        if isinstance(chapter, dict):
                            key = clean(chapter.get("chapter_number"))
                            title = clean(chapter.get("name_ar"))
                            if key and title:
                                local_chapters[key] = title
                    book_hadiths = book.get("hadiths") or []
                    before = collection_count
                    if isinstance(book_hadiths, list):
                        for item in book_hadiths:
                            if isinstance(item, dict):
                                add_hadith(item, book_number, book_name, local_chapters)
                    books_to_insert.append((collection_id, book_number, book_name, collection_count - before))

            flat_hadiths = payload.get("hadiths") or []
            if isinstance(flat_hadiths, list):
                for item in flat_hadiths:
                    if isinstance(item, dict):
                        add_hadith(item)

            if collection_count:
                db.execute("INSERT INTO collections VALUES (?, ?, ?, ?)",
                           (collection_id, name_ar, collection_count, collection_type))
                db.executemany("INSERT OR REPLACE INTO books VALUES (?, ?, ?, ?)", books_to_insert)
                db.executemany("""
                    INSERT INTO hadiths (
                        collection_id, book_number, chapter_number, hadith_number, reference,
                        chapter_name_ar, text_ar, narrator, grade_ar,
                        source_reference, url_source, search_text
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, hadith_rows)
                seen.add(collection_id)
                total += collection_count

    db.commit()
    db.execute("PRAGMA user_version=1")
    db.execute("VACUUM")
    db.close()

    missing = EXPECTED_COLLECTIONS - seen
    if missing:
        raise SystemExit("Missing Arabic hadith collections: " + ", ".join(sorted(missing)))
    if total < 45000:
        raise SystemExit(f"Hadith dataset unexpectedly incomplete: {total} records")
    print(f"Built Arabic-only hadith database: {total} hadiths in {len(seen)} collections")
    print(f"Database size: {db_path.stat().st_size} bytes; English translation columns were not included.")


if __name__ == "__main__":
    main()
