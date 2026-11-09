// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.readModels.ReadModel;
import java.util.HashMap;
import java.util.Map;
import kotlin.jvm.JvmClassMappingKt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dictionary entries, set once, caused by and children ordering, written from Java by property name.
 */
public final class JavaProjectionParityUsage {

    @Test
    public void dictionaryEntriesSetOnceAndCausedByAreCallableFromJava() {
        var builder = new ProjectionBuilderFor<>(JvmClassMappingKt.getKotlinClass(Ledger.class));
        new LedgerProjection().define(builder);
        var properties = builder.getFromEntries().get(0).getProperties();
        assertEquals("$add(amount)", properties.get("totals.$eventContent.currency"));
        assertEquals("$remove", properties.get("pending.$eventContent.currency"));
        assertEquals("$setOnce($causedBy(subject))", properties.get("openedBy"));
        assertEquals(new ChildrenOrdering(ChildrenPlacement.SORTED, "amount", SortDirection.DESCENDING),
            builder.getChildrenOrderings().get("postings"));
    }

    /** A posting in a currency. */
    @EventType
    public record AmountPosted(String currency, long amount) {
    }

    /** A single posting kept as a child. */
    public static class Posting {
        public long amount = 0;
    }

    /** What the projection builds. */
    @ReadModel
    public static class Ledger {
        public Map<String, Long> totals = new HashMap<>();
        public Map<String, Long> pending = new HashMap<>();
        public String openedBy = "";
        public java.util.List<Posting> postings = new java.util.ArrayList<>();
    }

    /** Uses the property-name overloads a Java caller reaches for. */
    public static class LedgerProjection implements IProjectionFor<Ledger> {
        @Override
        public void define(IProjectionBuilderFor<Ledger> builder) {
            builder
                .from(AmountPosted.class, from -> {
                    from.add("totals", DictionaryKey.fromEvent("currency")).with("amount");
                    from.removeKey("pending", DictionaryKey.fromEvent("currency"));
                    from.setOnce("openedBy").toCausedBy(CausedByProperty.SUBJECT);
                })
                .children("postings", Posting.class, children -> {
                    children.orderByDescending("amount");
                });
        }
    }
}
