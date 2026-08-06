// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.concepts;

/**
 * Concepts declared and used from Java, exercised from Kotlin specs.
 *
 * A concept is an ordinary type implementing {@link ConceptAs}, so Java declares one as a record and
 * uses it like any other value. This fixture fails to compile if that stops being true - which is
 * exactly what a {@code @JvmInline value class} would have cost, and why the interface is what the
 * client asks for.
 */
public final class JavaConceptUsage {

    private JavaConceptUsage() {
    }

    /** An identifier declared in Java. */
    public record JavaBookId(String value) implements ConceptAs<String> {
        @Override
        public String getValue() {
            return value;
        }
    }

    /** A different identifier, which the compiler will not let you confuse with the first. */
    public record JavaMemberId(String value) implements ConceptAs<String> {
        @Override
        public String getValue() {
            return value;
        }
    }

    /** An event whose properties are typed rather than being three interchangeable strings. */
    public record JavaBookBorrowed(JavaBookId book, JavaMemberId member) {
    }

    /** Constructing one, which is all a Java caller ever has to do. */
    public static JavaBookBorrowed borrow(String bookId, String memberId) {
        return new JavaBookBorrowed(new JavaBookId(bookId), new JavaMemberId(memberId));
    }

    /** Reading the underlying value back out. */
    public static String valueOf(ConceptAs<String> concept) {
        return concept.getValue();
    }
}
