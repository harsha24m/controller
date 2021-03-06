/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated(forRemoval = true)
abstract class AbstractDOMRpcRoutingTableEntry {
    private final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> implementations;
    private final SchemaPath schemaPath;

    AbstractDOMRpcRoutingTableEntry(final SchemaPath schemaPath,
        final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> implementations) {
        this.schemaPath = requireNonNull(schemaPath);
        this.implementations = requireNonNull(implementations);
    }

    final SchemaPath getSchemaPath() {
        return schemaPath;
    }

    final List<DOMRpcImplementation> getImplementations(final YangInstanceIdentifier context) {
        return implementations.get(context);
    }

    final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> getImplementations() {
        return implementations;
    }

    final boolean containsContext(final YangInstanceIdentifier contextReference) {
        return implementations.containsKey(contextReference);
    }

    final Set<YangInstanceIdentifier> registeredIdentifiers(final DOMRpcAvailabilityListener listener) {
        return Maps.filterValues(implementations, list -> list.stream().anyMatch(listener::acceptsImplementation))
                .keySet();
    }

    /**
     * Adds an entry to the DOM RPC routing table.
     *
     * @param implementation RPC implementation
     * @param newRpcs List of new RPCs, must be mutable
     */
    final AbstractDOMRpcRoutingTableEntry add(final DOMRpcImplementation implementation,
            final List<YangInstanceIdentifier> newRpcs) {
        final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
        for (final Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : implementations.entrySet()) {
            if (newRpcs.remove(ve.getKey())) {
                final List<DOMRpcImplementation> i = new ArrayList<>(ve.getValue().size() + 1);
                i.addAll(ve.getValue());
                i.add(implementation);

                // New implementation is at the end, this will move it to be the last among implementations
                // with equal cost -- relying on sort() being stable.
                i.sort(Comparator.comparingLong(DOMRpcImplementation::invocationCost));
                vb.put(ve.getKey(), i);
            } else {
                vb.put(ve);
            }
        }
        for (final YangInstanceIdentifier ii : newRpcs) {
            final List<DOMRpcImplementation> impl = new ArrayList<>(1);
            impl.add(implementation);
            vb.put(ii, impl);
        }

        return newInstance(vb.build());
    }

    final AbstractDOMRpcRoutingTableEntry remove(final DOMRpcImplementation implementation,
            final List<YangInstanceIdentifier> removed) {
        final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
        for (final Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : implementations.entrySet()) {
            if (removed.remove(ve.getKey())) {
                final List<DOMRpcImplementation> i = new ArrayList<>(ve.getValue());
                i.remove(implementation);
                // We could trimToSize(), but that may perform another copy just to get rid
                // of a single element. That is probably not worth the trouble.
                if (!i.isEmpty()) {
                    vb.put(ve.getKey(), i);
                }
            } else {
                vb.put(ve);
            }
        }

        final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> v = vb.build();
        return v.isEmpty() ? null : newInstance(v);
    }

    protected abstract CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(NormalizedNode<?, ?> input);

    protected abstract AbstractDOMRpcRoutingTableEntry newInstance(
            Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls);
}
