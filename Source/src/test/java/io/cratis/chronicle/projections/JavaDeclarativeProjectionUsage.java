// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections;

import io.cratis.chronicle.events.EventType;

import java.util.List;

/**
 * A complete declarative projection, written the way Java writes one - property names instead of
 * {@link kotlin.reflect.KProperty1}, {@code Class} instead of {@link kotlin.reflect.KClass}.
 *
 * Before the string-name overloads on {@link IProjectionBuilderFor} and its sub-builders, none of
 * this compiled from Java: {@code join}, {@code on}, {@code nested}, {@code children} and explicit
 * {@code set} all demanded a {@link kotlin.reflect.KProperty1}, which Java cannot produce. This
 * fixture exercises every one of them plus {@code fromEvery}, a counter, and {@code noAutoMap} in a
 * single projection.
 *
 * Nothing here runs - compiling it is the assertion.
 */
public final class JavaDeclarativeProjectionUsage {

    private JavaDeclarativeProjectionUsage() {
    }

    // --- Events ---------------------------------------------------------------------------------

    /** An order was placed by a customer. */
    @EventType
    public record JavaDeclarativeOrderPlaced(String orderId, String customerId) {
    }

    /** A line was added to an order. */
    @EventType
    public record JavaDeclarativeOrderLineAdded(String orderId, String product, int quantity) {
    }

    /** An order was cancelled. */
    @EventType
    public record JavaDeclarativeOrderCancelled(String orderId) {
    }

    /** A customer changed their display name - joined against by customer id. */
    @EventType
    public record JavaDeclarativeCustomerRenamed(String customerId, String name) {
    }

    // --- Read model, nested object and child collection element -------------------------------

    /** The child collection element - one per order line. JavaBean-shaped, so AutoMap and the string-name overloads can see its properties. */
    public static class JavaDeclarativeOrderLine {
        private String product = "";
        private int quantity;

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    /** The nested single-object member, cleared when the order is cancelled. */
    public static class JavaDeclarativeOrderSummary {
        private String note = "";

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    /** The read model the projection below builds. */
    public static class JavaDeclarativeOrder {
        private String id = "";
        private String customerName = "";
        private int version;
        private List<JavaDeclarativeOrderLine> lines = List.of();
        private JavaDeclarativeOrderSummary summary;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public List<JavaDeclarativeOrderLine> getLines() {
            return lines;
        }

        public void setLines(List<JavaDeclarativeOrderLine> lines) {
            this.lines = lines;
        }

        public JavaDeclarativeOrderSummary getSummary() {
            return summary;
        }

        public void setSummary(JavaDeclarativeOrderSummary summary) {
            this.summary = summary;
        }
    }

    // --- The projection itself -------------------------------------------------------------------

    /** Builds {@link JavaDeclarativeOrder} using every corner of the declarative builder, from Java. */
    public static class JavaDeclarativeOrderProjection implements IProjectionFor<JavaDeclarativeOrder> {
        @Override
        public void define(IProjectionBuilderFor<JavaDeclarativeOrder> builder) {
            builder
                .from(JavaDeclarativeOrderPlaced.class, fb -> {
                    fb.set("id").toProperty("orderId");
                    fb.set("customerName").toProperty("customerId");
                })
                .from(JavaDeclarativeOrderLineAdded.class, fb -> {
                    // A counter: bumped by one every time this event fires for the instance.
                    fb.increment("version");
                })
                .join(JavaDeclarativeCustomerRenamed.class, jb -> {
                    jb.on("customerName");
                    jb.set("customerName").toProperty("name");
                })
                .nested("summary", JavaDeclarativeOrderSummary.class, nb -> {
                    nb.from(JavaDeclarativeOrderPlaced.class, fb -> {
                        fb.set("note").toProperty("orderId");
                    });
                    nb.clearWith(JavaDeclarativeOrderCancelled.class);
                })
                .children("lines", JavaDeclarativeOrderLine.class, cb -> {
                    cb.identifiedBy("product");
                    cb.from(JavaDeclarativeOrderLineAdded.class, cfb -> {
                        cfb.usingKey("product");
                        cfb.usingParentKey("orderId");
                        cfb.set("quantity").toProperty("quantity");
                    });
                })
                .fromEvery(feb -> {
                    feb.set("id").toEventSourceId();
                })
                .noAutoMap();
        }
    }
}
