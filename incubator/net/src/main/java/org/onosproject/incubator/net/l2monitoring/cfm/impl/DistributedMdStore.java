/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.incubator.net.l2monitoring.cfm.impl;

import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onlab.util.Identifier;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultComponent;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdStore;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdStoreDelegate;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Maintenance Domain Store implementation backed by consistent map.
 */
@Component(immediate = true)
@Service
public class DistributedMdStore extends AbstractStore<MdEvent, MdStoreDelegate>
    implements MdStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<MdId, MaintenanceDomain> maintenanceDomainConsistentMap;
    private Map<MdId, MaintenanceDomain> maintenanceDomainMap;

    private final InternalMdListener listener = new InternalMdListener();

    @Activate
    public void activate() {
        maintenanceDomainConsistentMap = storageService
                .<MdId, MaintenanceDomain>consistentMapBuilder()
                .withName("onos-cfm-ma-map")
                .withSerializer(Serializer.using(new KryoNamespace.Builder()
                        .register(KryoNamespaces.API)
                        .register(DefaultMaintenanceDomain.class)
                        .register(MdIdCharStr.class)
                        .register(MdIdDomainName.class)
                        .register(MdIdMacUint.class)
                        .register(MdIdNone.class)
                        .register(MaintenanceDomain.MdLevel.class)
                        .register(DefaultMaintenanceAssociation.class)
                        .register(MaIdCharStr.class)
                        .register(MaIdShort.class)
                        .register(MaId2Octet.class)
                        .register(MaIdIccY1731.class)
                        .register(MaIdPrimaryVid.class)
                        .register(MaIdRfc2685VpnId.class)
                        .register(MaintenanceAssociation.CcmInterval.class)
                        .register(DefaultComponent.class)
                        .register(MepId.class)
                        .register(Identifier.class)
                        .register(InternetDomainName.class)
                        .register(MacAddress.class)
                        .register(ImmutablePair.class)
                        .register(org.onosproject.incubator.net.l2monitoring
                                            .cfm.Component.MhfCreationType.class)
                        .register(org.onosproject.incubator.net.l2monitoring
                                            .cfm.Component.IdPermissionType.class)
                        .register(org.onosproject.incubator.net.l2monitoring
                                            .cfm.Component.TagType.class)
                        .build("md")))
                .build();

        maintenanceDomainMap = maintenanceDomainConsistentMap.asJavaMap();
    }

    @Override
    public Collection<MaintenanceDomain> getAllMaintenanceDomain() {
        return maintenanceDomainMap.values();
    }

    @Override
    public Optional<MaintenanceDomain> getMaintenanceDomain(MdId mdName) {
        return Optional.ofNullable(
                maintenanceDomainMap.get(mdName));
    }

    @Override
    public boolean deleteMaintenanceDomain(MdId mdName) {
        return maintenanceDomainMap.remove(mdName) == null ? false : true;
    }

    @Override
    public boolean createUpdateMaintenanceDomain(MaintenanceDomain md) {
        return maintenanceDomainMap.put(md.mdId(), md) == null ? false : true;
    }

    private class InternalMdListener implements MapEventListener<MdId, MaintenanceDomain> {
        @Override
        public void event(MapEvent<MdId, MaintenanceDomain> mapEvent) {
            final MdEvent.Type type;
            switch (mapEvent.type()) {
                case INSERT:
                    type = MdEvent.Type.MD_ADDED;
                    break;
                case UPDATE:
                    type = MdEvent.Type.MD_UPDATED;
                    break;
                case REMOVE:
                default:
                    type = MdEvent.Type.MD_REMOVED;
                    break;
            }
            notifyDelegate(new MdEvent(type, mapEvent.key()));
        }
    }
}
