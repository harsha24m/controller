/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.shardmanager.AbstractShardManagerCreator;
import org.opendaylight.controller.cluster.datastore.shardmanager.TestShardManager;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

public class TestDistributedDataStore extends DistributedDataStore {

    public TestDistributedDataStore(final ActorSystem actorSystem, final ClusterWrapper cluster,
                                    final Configuration configuration,
                                    final DatastoreContextFactory datastoreContextFactory,
                                    final DatastoreSnapshot restoreFromSnapshot) {
        super(actorSystem, cluster, configuration, datastoreContextFactory, restoreFromSnapshot);
    }

    TestDistributedDataStore(final ActorUtils actorUtils, final ClientIdentifier identifier) {
        super(actorUtils, identifier);
    }

    @Override
    protected AbstractShardManagerCreator<?> getShardManagerCreator() {
        return new TestShardManager.TestShardManagerCreator();
    }
}
