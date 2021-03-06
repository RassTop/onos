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
package org.onosproject.cfm.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

/**
 * Deletes a Maintenance Domain from the existing list.
 */
@Command(scope = "onos", name = "cfm-md-delete",
        description = "Delete a CFM Maintenance Domain and its children.")
public class CfmMdDeleteCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Maintenance Domain name and type (in brackets)",
            required = true, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        CfmMdService service = get(CfmMdService.class);

        String[] nameParts = name.split("[()]");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Invalid name format. " +
                    "Must be in the format of <identifier(name-type)>");
        }

        MdId mdId = null;
        MdId.MdNameType nameTypeEnum = MdId.MdNameType.valueOf(nameParts[1]);
        switch (nameTypeEnum) {
            case DOMAINNAME:
                mdId = MdIdDomainName.asMdId(nameParts[0]);
                break;
            case MACANDUINT:
                mdId = MdIdMacUint.asMdId(nameParts[0]);
                break;
            case NONE:
                mdId = MdIdNone.asMdId();
                break;
            case CHARACTERSTRING:
            default:
                mdId = MdIdCharStr.asMdId(nameParts[0]);
        }

        try {
            boolean deleted = service.deleteMaintenanceDomain(mdId);
            print("Maintenance Domain %s is %ssuccessfully deleted.",
                    mdId, deleted ? "" : "NOT ");
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
