#!/usr/bin/env python3
# Copyright (c) Cratis. All rights reserved.
# Licensed under the MIT license. See LICENSE file in the project root for full license information.

import re
import shutil
import subprocess
import sys
import textwrap
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
SNIPPET_ROOT = REPO_ROOT / "Documentation" / "client-snippets"
JAVA_SNIPPET_ROOT = REPO_ROOT / "Documentation" / "client-snippets-java"
GENERATED_SOURCE = REPO_ROOT / "Source" / "src" / "test" / "kotlin" / "DocumentationClientSnippetCompilation.kt"
GENERATED_JAVA_SOURCE_ROOT = REPO_ROOT / "Source" / "src" / "test" / "java" / "io" / "cratis" / "chronicle" / "documentation"
FENCE_RE = re.compile(r"```([^\s`]+)[^\n]*\n(.*?)\n```", re.DOTALL)
UNSUPPORTED_SNIPPET_MARKER = "does not support this workflow yet"

BODY_SNIPPETS = {
    "get-started/client-flow": "",
    "events/appending/schema-validation": """
        val store: IEventStore = error("compile-only snippet dependency")
        val eventSourceId = "order-123"
        val customerId = "customer-42"
        val total = 42.0
    """,
    "read-models/getting-single-instance/basic": """
        val store: IEventStore = error("compile-only snippet dependency")
        val accountId = "account-42"
    """,
}


def snippet_files(root: Path) -> list[Path]:
    if not root.exists():
        return []

    files = sorted([*root.rglob("*.md"), *root.rglob("*.mdx")])
    snippets = {}
    for path in files:
        key = snippet_key(path, root)
        if key in snippets:
            raise ValueError(f"Duplicate client snippet {key}: {snippets[key]} and {path.relative_to(REPO_ROOT)}")
        snippets[key] = path.relative_to(REPO_ROOT)
    return files


def snippet_key(path: Path, root: Path) -> str:
    return path.relative_to(root).with_suffix("").as_posix()


def extract_snippet(path: Path, expected_language: str) -> str | None:
    raw = path.read_text(encoding="utf-8")
    matches = FENCE_RE.findall(raw)
    if len(matches) != 1:
        raise ValueError(f"{path.relative_to(REPO_ROOT)} must contain exactly one fenced {expected_language} snippet")

    language, code = matches[0]
    if language == "text" and UNSUPPORTED_SNIPPET_MARKER in code:
        return None
    if language != expected_language:
        raise ValueError(f"{path.relative_to(REPO_ROOT)} must use a {expected_language} code fence, got {language!r}")

    return code.strip()


def split_imports(code: str) -> tuple[list[str], str]:
    imports: list[str] = []
    body: list[str] = []
    for line in code.splitlines():
        if line.startswith("import "):
            imports.append(line)
        else:
            body.append(line)
    return imports, "\n".join(body).strip()


def function_name(relative_path: str) -> str:
    return "snippet_" + re.sub(r"[^A-Za-z0-9_]", "_", relative_path)


def generate_source() -> str:
    files = snippet_files(SNIPPET_ROOT)
    if not files:
        raise ValueError(f"No client snippets found in {SNIPPET_ROOT}")

    imports = {
        "import io.cratis.chronicle.ChronicleClient",
        "import io.cratis.chronicle.ChronicleOptions",
        "import io.cratis.chronicle.IEventStore",
    }
    declarations: list[str] = []
    functions: list[str] = []
    declarations.append('data class AccountInfo(val name: String = "", val balance: Double = 0.0)')

    for path in files:
        relative_path = snippet_key(path, SNIPPET_ROOT)
        snippet = extract_snippet(path, "kotlin")
        if snippet is None:
            continue

        snippet_imports, body = split_imports(snippet)
        imports.update(snippet_imports)

        if relative_path in BODY_SNIPPETS:
            prelude = textwrap.dedent(BODY_SNIPPETS[relative_path]).strip()
            lines = [line for line in [prelude, body] if line]
            function_body = textwrap.indent("\n\n".join(lines), "    ")
            functions.append(f"suspend fun {function_name(relative_path)}() {{\n{function_body}\n}}")
        else:
            declarations.append(body)

    return "\n".join([
        '@file:Suppress("RedundantSuspendModifier", "UNUSED_VARIABLE", "UNUSED_PARAMETER")',
        "",
        *sorted(imports),
        "",
        *declarations,
        "",
        *functions,
        "",
    ])


def java_snippet_file_name(path: Path) -> str:
    return "Snippet_" + re.sub(r"[^A-Za-z0-9_]", "_", snippet_key(path, JAVA_SNIPPET_ROOT)) + ".java"


def write_java_sources() -> list[Path]:
    files = snippet_files(JAVA_SNIPPET_ROOT)
    if not files:
        return []

    GENERATED_JAVA_SOURCE_ROOT.mkdir(parents=True, exist_ok=True)
    generated_files: list[Path] = []

    for path in files:
        code = extract_snippet(path, "java")
        if code is None:
            continue

        source = "\n".join([
            "package io.cratis.chronicle.documentation;",
            "",
            code,
            "",
        ])
        generated_path = GENERATED_JAVA_SOURCE_ROOT / java_snippet_file_name(path)
        generated_path.write_text(source, encoding="utf-8")
        generated_files.append(generated_path)

    return generated_files


def gradle_command() -> list[str]:
    gradlew = REPO_ROOT / "gradlew"
    executable = str(gradlew) if gradlew.exists() else "gradle"
    return [executable, ":Source:compileTestKotlin", ":Source:compileTestJava", "--no-configuration-cache"]


def main() -> int:
    GENERATED_SOURCE.parent.mkdir(parents=True, exist_ok=True)
    GENERATED_SOURCE.write_text(generate_source(), encoding="utf-8")
    generated_java_sources = write_java_sources()

    try:
        subprocess.run(gradle_command(), cwd=REPO_ROOT, check=True)
    finally:
        GENERATED_SOURCE.unlink(missing_ok=True)
        for generated_java_source in generated_java_sources:
            generated_java_source.unlink(missing_ok=True)

        gradle_dirs = [
            GENERATED_JAVA_SOURCE_ROOT,
            REPO_ROOT / "Source" / "src" / "test" / "java" / "io" / "cratis" / "chronicle",
            REPO_ROOT / "Source" / "src" / "test" / "java" / "io" / "cratis",
            REPO_ROOT / "Source" / "src" / "test" / "java" / "io",
            REPO_ROOT / "Source" / "src" / "test" / "java",
            REPO_ROOT / "Source" / "src" / "test" / "kotlin",
            REPO_ROOT / "Source" / "src" / "test",
            REPO_ROOT / "Source" / "src",
        ]
        for directory in gradle_dirs:
            try:
                directory.rmdir()
            except OSError:
                break

    print("Kotlin Chronicle client snippets compiled successfully.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"Client snippet validation failed: {error}", file=sys.stderr)
        raise SystemExit(1)
