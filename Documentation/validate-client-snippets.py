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

# --- Documentation page snippets -------------------------------------------
# The client-snippets folders above feed the shared Chronicle docs. The pages
# under Documentation/ (get-started, guides, reference) are separate, and used
# to be unchecked — which is how they drifted to referencing APIs that no
# longer existed. Every kotlin/java fence on those pages now carries a
# `<!-- validate: ... -->` directive and is compiled here too.
PAGE_ROOT = REPO_ROOT / "Documentation"
PAGE_EXCLUDED_DIRS = {"client-snippets", "client-snippets-java"}
GENERATED_PAGE_SOURCE = REPO_ROOT / "Source" / "src" / "test" / "kotlin" / "DocumentationPageSnippetCompilation.kt"
GENERATED_PAGE_JAVA_ROOT = REPO_ROOT / "Source" / "src" / "test" / "java" / "io" / "cratis" / "chronicle" / "documentation" / "pages"
PAGE_JAVA_PACKAGE = "io.cratis.chronicle.documentation.pages"
# Kotlin and Java page snippets declare the same type names (EmployeeHired,
# EmployeeProfile, ...) as two language variants of one example, so they need
# separate packages to coexist in the same compilation.
PAGE_KOTLIN_PACKAGE = "io.cratis.chronicle.documentation.kotlinpages"
PAGE_DIRECTIVE_RE = re.compile(r"<!--\s*validate:\s*([^>]*?)\s*-->")
PAGE_FENCE_RE = re.compile(r"^```(kotlin|java)\b", re.M)

# Values a `needs=` list may request, and how each is declared per language.
PAGE_PRELUDES = {
    "kotlin": {
        "client": 'val client: io.cratis.chronicle.ChronicleClient = error("compile-only")',
        "store": 'val store: io.cratis.chronicle.EventStore = error("compile-only")',
        "employeeId": 'val employeeId: String = "emp-001"',
    },
    "java": {
        "client": "io.cratis.chronicle.ChronicleClient client = null;",
        "store": "io.cratis.chronicle.EventStore store = null;",
        "employeeId": 'String employeeId = "emp-001";',
    },
}

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


def page_files() -> list[Path]:
    return sorted(
        path
        for path in PAGE_ROOT.rglob("*.md")
        if not PAGE_EXCLUDED_DIRS & set(path.relative_to(PAGE_ROOT).parts)
    )


def page_snippets() -> list[dict]:
    """Every kotlin/java fence on a documentation page, with its directive."""
    snippets = []
    for path in page_files():
        lines = path.read_text(encoding="utf-8").splitlines()
        for index, line in enumerate(lines):
            match = PAGE_FENCE_RE.match(line)
            if not match:
                continue

            language = match.group(1)
            preceding = [text for text in lines[max(0, index - 3):index] if text.strip()]
            directives = [
                PAGE_DIRECTIVE_RE.search(text)
                for text in preceding
                if PAGE_DIRECTIVE_RE.search(text)
            ]
            location = f"{path.relative_to(REPO_ROOT)}:{index + 1}"
            if not directives:
                raise ValueError(
                    f"{location}: ```{language} fence has no '<!-- validate: ... -->' "
                    f"directive. Use skip, declarations, or body [needs=a,b]."
                )

            directive = directives[-1].group(1).split()
            mode = directive[0]
            if mode not in {"skip", "declarations", "body"}:
                raise ValueError(f"{location}: unknown validate mode {mode!r}")

            needs: list[str] = []
            for extra in directive[1:]:
                if not extra.startswith("needs="):
                    raise ValueError(f"{location}: unknown validate option {extra!r}")
                needs = [name for name in extra[len("needs="):].split(",") if name]
                for name in needs:
                    if name not in PAGE_PRELUDES[language]:
                        raise ValueError(f"{location}: unknown needs value {name!r}")

            closing = next(
                (offset for offset in range(index + 1, len(lines)) if lines[offset].startswith("```")),
                None,
            )
            if closing is None:
                raise ValueError(f"{location}: unterminated code fence")

            snippets.append({
                "location": location,
                "language": language,
                "mode": mode,
                "needs": needs,
                "code": "\n".join(lines[index + 1:closing]).strip(),
            })
    return snippets


def generate_page_kotlin(snippets: list[dict]) -> str:
    imports: set[str] = set()
    declarations: list[str] = []
    functions: list[str] = []

    for number, snippet in enumerate(snippets):
        if snippet["language"] != "kotlin" or snippet["mode"] == "skip":
            continue

        snippet_imports, body = split_imports(snippet["code"])
        imports.update(snippet_imports)
        if not body:
            continue

        if snippet["mode"] == "declarations":
            declarations.append(body)
        else:
            prelude = [PAGE_PRELUDES["kotlin"][name] for name in snippet["needs"]]
            block = textwrap.indent("\n".join([*prelude, body]), "    ")
            functions.append(f"suspend fun pageSnippet_{number}() {{\n{block}\n}}")

    return "\n".join([
        '@file:Suppress("RedundantSuspendModifier", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "unused")',
        "",
        # Own package so page snippets never collide with the client-snippet
        # declarations, which live in the root package.
        f"package {PAGE_KOTLIN_PACKAGE}",
        "",
        *sorted(imports),
        "",
        *declarations,
        "",
        *functions,
        "",
    ])


PUBLIC_TYPE_RE = re.compile(r"^public\s+(?:final\s+)?(?:class|record|interface|enum)\s+(\w+)", re.M)


def write_page_java(snippets: list[dict]) -> list[Path]:
    GENERATED_PAGE_JAVA_ROOT.mkdir(parents=True, exist_ok=True)
    written: list[Path] = []

    # A tutorial page shows its imports once and the later fences build on
    # them, so pool every java import on the pages and give it to each
    # generated file — the same way the kotlin generator pools into one file.
    pooled_imports: set[str] = set()
    for snippet in snippets:
        if snippet["language"] == "java" and snippet["mode"] != "skip":
            pooled_imports.update(split_imports(snippet["code"])[0])

    for number, snippet in enumerate(snippets):
        if snippet["language"] != "java" or snippet["mode"] == "skip":
            continue

        body = split_imports(snippet["code"])[1]
        header = [f"package {PAGE_JAVA_PACKAGE};", "", *sorted(pooled_imports), ""]

        if snippet["mode"] == "declarations":
            public_types = PUBLIC_TYPE_RE.findall(body)
            if len(public_types) > 1:
                raise ValueError(
                    f"{snippet['location']}: a java snippet may declare at most one "
                    f"public top-level type, found {public_types}"
                )
            name = public_types[0] if public_types else f"PageSnippet_{number}"
            source = "\n".join([*header, body, ""])
        else:
            prelude = [PAGE_PRELUDES["java"][need] for need in snippet["needs"]]
            name = f"PageSnippet_{number}"
            block = textwrap.indent("\n".join([*prelude, body]), "        ")
            source = "\n".join([
                *header,
                f"class {name} {{",
                "    @SuppressWarnings(\"unused\")",
                "    void run() throws Exception {",
                block,
                "    }",
                "}",
                "",
            ])

        generated = GENERATED_PAGE_JAVA_ROOT / f"{name}.java"
        generated.write_text(source, encoding="utf-8")
        written.append(generated)

    return written


def gradle_command() -> list[str]:
    gradlew = REPO_ROOT / "gradlew"
    executable = str(gradlew) if gradlew.exists() else "gradle"
    return [executable, ":Source:compileTestKotlin", ":Source:compileTestJava", "--no-configuration-cache"]


def main() -> int:
    # Parse everything before writing anything, so a malformed directive fails
    # without leaving generated sources behind in the tree.
    snippets = page_snippets()
    checked = sum(1 for snippet in snippets if snippet["mode"] != "skip")
    print(f"Compiling {checked} documentation page snippets ({len(snippets) - checked} skipped).")

    generated_java_sources: list[Path] = []
    generated_page_java_sources: list[Path] = []
    GENERATED_SOURCE.parent.mkdir(parents=True, exist_ok=True)

    try:
        GENERATED_SOURCE.write_text(generate_source(), encoding="utf-8")
        generated_java_sources = write_java_sources()
        GENERATED_PAGE_SOURCE.write_text(generate_page_kotlin(snippets), encoding="utf-8")
        generated_page_java_sources = write_page_java(snippets)

        subprocess.run(gradle_command(), cwd=REPO_ROOT, check=True)
    finally:
        GENERATED_SOURCE.unlink(missing_ok=True)
        GENERATED_PAGE_SOURCE.unlink(missing_ok=True)
        for generated_java_source in generated_java_sources:
            generated_java_source.unlink(missing_ok=True)
        for generated_java_source in generated_page_java_sources:
            generated_java_source.unlink(missing_ok=True)

        gradle_dirs = [
            GENERATED_PAGE_JAVA_ROOT,
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
    print("Documentation page snippets compiled successfully.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"Client snippet validation failed: {error}", file=sys.stderr)
        raise SystemExit(1)
