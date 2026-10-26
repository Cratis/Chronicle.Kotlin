// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const root = fileURLToPath(new URL('../', import.meta.url));
const loadValidator = String.raw`
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location("validator", "Documentation/validate-client-snippets.py")
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)
`;

function runPython(source, timeout) {
    const result = spawnSync('python3', ['-'], {
        cwd: root,
        input: loadValidator + source,
        encoding: 'utf8',
        env: { ...process.env, PYTHONUNBUFFERED: '1' },
        timeout,
        maxBuffer: 32 * 1024 * 1024,
    });
    // Keep the real compiler diagnostics and task outcomes visible in CI and baseline runs.
    if (result.stdout) process.stdout.write(result.stdout);
    if (result.stderr) process.stderr.write(result.stderr);
    assert.ifError(result.error);
    assert.equal(result.status, 0, `Python regression failed (signal: ${result.signal})`);
}

test('Java task-scoped rerun, subprocess contract and owned-source cleanup', () => {
    runPython(String.raw`
import subprocess
import tempfile
import unittest
from contextlib import ExitStack
from unittest.mock import patch

class ValidatorTests(unittest.TestCase):
    def expected_command(self, executable):
        return [executable, ":Testing:compileTestKotlin", ":Testing:compileTestJava",
                "--rerun", "--no-configuration-cache"]

    def test_wrapper_and_fallback(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(v, "REPO_ROOT", Path(directory)):
            self.assertEqual(v.gradle_command(), self.expected_command("gradle"))
            wrapper = v.REPO_ROOT / "gradlew"
            wrapper.touch()
            self.assertEqual(v.gradle_command(), self.expected_command(str(wrapper)))

    def test_main_success_and_failure_cleanup(self):
        for fails in (False, True):
            with self.subTest(fails=fails), tempfile.TemporaryDirectory() as directory, ExitStack() as stack:
                root = Path(directory)
                for name in ("GENERATED_SOURCE", "GENERATED_PAGE_SOURCE", "GENERATED_JAVA_SOURCE_ROOT",
                             "GENERATED_PAGE_JAVA_ROOT", "GENERATED_SITE_CONCEPTS_DIR",
                             "GENERATED_SITE_CONCEPTS_JAVA_ROOT"):
                    stack.enter_context(patch.object(v, name, root / getattr(v, name).relative_to(v.REPO_ROOT)))
                stack.enter_context(patch.object(v, "REPO_ROOT", root))
                owned_java = v.GENERATED_JAVA_SOURCE_ROOT / "Owned.java"
                owned_page = v.GENERATED_PAGE_JAVA_ROOT / "OwnedPage.java"
                owned_site = v.GENERATED_SITE_CONCEPTS_DIR / "DocumentationSiteConcepts_owned.kt"
                owned_site_java = v.GENERATED_SITE_CONCEPTS_JAVA_ROOT / "Owned.java"
                sentinel = v.GENERATED_JAVA_SOURCE_ROOT / "Unrelated.java"
                sentinel.parent.mkdir(parents=True)
                sentinel.write_bytes(b"preserve me")

                def write(paths):
                    for path in paths:
                        path.parent.mkdir(parents=True, exist_ok=True)
                        path.write_text("generated")
                    return paths

                for name, value in (("page_snippets", []), ("validate_schema_validation_examples", None),
                                    ("generate_source", "kotlin"), ("generate_page_kotlin", "page kotlin")):
                    stack.enter_context(patch.object(v, name, return_value=value))
                stack.enter_context(patch.object(v, "write_java_sources", side_effect=lambda: write([owned_java])))
                stack.enter_context(patch.object(v, "write_page_java", side_effect=lambda _: write([owned_page])))
                stack.enter_context(patch.object(v, "write_site_concepts_sources",
                                                side_effect=lambda: write([owned_site, owned_site_java])))
                error = subprocess.CalledProcessError(1, v.gradle_command()) if fails else None
                run = stack.enter_context(patch.object(v.subprocess, "run", side_effect=error))
                if fails:
                    with self.assertRaises(subprocess.CalledProcessError):
                        v.main()
                else:
                    self.assertEqual(v.main(), 0)
                run.assert_called_once_with(self.expected_command("gradle"), cwd=root, check=True)
                for path in (v.GENERATED_SOURCE, v.GENERATED_PAGE_SOURCE, owned_java, owned_page,
                             owned_site, owned_site_java):
                    self.assertFalse(path.exists(), str(path))
                self.assertEqual(sentinel.read_bytes(), b"preserve me")

unittest.main()
`, 110_000);
});

// Opt in locally; CI always enables this test. Never clean outputs between these runs.
if (process.env.VALIDATE_CLIENT_SNIPPETS_REAL === '1') {
    test('real validator: warm valid → invalid → restored valid', () => {
        runPython(String.raw`
import re
import subprocess
import sys

snippet = v.JAVA_SNIPPET_ROOT / "events/appending/example.md"
original = snippet.read_bytes()
identifier = b"MissingSnippetCompilationType131"
anchor = b"        var result = store.getEventLog().append(orderId, new OrderPlaced(customerId, total));"
assert original.count(anchor) == 1, "Snippet mutation anchor changed"
assert identifier not in original, "Negative-control identifier already exists"

# Refuse to overwrite generated/user-owned files. Empty directories are harmless.
def generated_files():
    return ([path for path in (v.GENERATED_SOURCE, v.GENERATED_PAGE_SOURCE) if path.exists()]
            + list(v.GENERATED_SITE_CONCEPTS_DIR.glob("DocumentationSiteConcepts_*.kt"))
            + [path for path in v.GENERATED_JAVA_SOURCE_ROOT.rglob("*") if path.is_file() or path.is_symlink()])

assert not generated_files(), "Generated-source ownership collision: " + str(generated_files())
sentinel = v.GENERATED_JAVA_SOURCE_ROOT.parent / "SnippetCompilationSentinel131.txt"
sentinel_bytes = b"preserve sibling during snippet validation\n"
sentinel.parent.mkdir(parents=True, exist_ok=True)
with sentinel.open("xb") as stream:
    stream.write(sentinel_bytes)


def run(label):
    result = subprocess.run([sys.executable, "Documentation/validate-client-snippets.py"],
                            cwd=v.REPO_ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, timeout=180)
    print(f"=== {label}: exit {result.returncode} ===", flush=True)
    print(result.stdout, flush=True)
    assert not generated_files(), "Generated sources remain after " + label
    assert sentinel.read_bytes() == sentinel_bytes, "Sibling sentinel changed after " + label
    return result

try:
    valid = run("warm valid")
    assert valid.returncode == 0, "Baseline valid snippets failed; stop"
    try:
        snippet.write_bytes(original.replace(anchor, b"        " + identifier + b".forceCompile();\n" + anchor))
        invalid = run("warm invalid")
    finally:
        snippet.write_bytes(original)
    restored = run("warm restored valid")
    # Collect all three outcomes before assertions so the same test can capture pre-fix evidence.
    print(f"Warm statuses: {valid.returncode} -> {invalid.returncode} -> {restored.returncode}", flush=True)
    assert [valid.returncode, invalid.returncode, restored.returncode] == [0, 1, 0]
    assert re.search(r"error: cannot find symbol[\s\S]*\bMissingSnippetCompilationType131\b", invalid.stdout), \
        "Missing javac diagnostic for the deliberate undefined identifier"
    assert re.search(r"^> Task :Testing:compileTestJava FAILED$", invalid.stdout, re.M)
    assert "Client snippet validation failed:" in invalid.stdout
    assert "snippets compiled successfully." not in invalid.stdout
    for result in (valid, restored):
        outcomes = re.findall(r"^> Task :Testing:compileTestJava([^\r\n]*)", result.stdout, re.M)
        assert outcomes == [""], "Java must execute (not UP-TO-DATE/FROM-CACHE): " + str(outcomes)
        assert "Kotlin Chronicle client snippets compiled successfully." in result.stdout
        assert "Documentation page snippets compiled successfully." in result.stdout
finally:
    snippet.write_bytes(original)
    try:
        assert snippet.read_bytes() == original, "Snippet bytes were not restored"
        assert sentinel.read_bytes() == sentinel_bytes, "Sibling sentinel was not preserved"
    finally:
        sentinel.unlink(missing_ok=True)
`, 570_000);
    });
}
