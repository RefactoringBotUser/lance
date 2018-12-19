/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.cloudiator.lance.lca.container;

import static de.uniulm.omi.cloudiator.lance.lca.container.ContainerType.DOCKER_REMOTE;
import static de.uniulm.omi.cloudiator.lance.lca.container.ContainerType.values;

import de.uniulm.omi.cloudiator.lance.application.component.RemoteDockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerContainerManagerFactory;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.PlainContainerManagerFactory;
import java.util.EnumMap;

public final class ContainerManagerFactory {

  private static final EnumMap<ContainerType, SpecificContainerManagerFactory> mapper;

  static {
    // FIXME: add initialisation code
    mapper = new EnumMap<>(ContainerType.class);
    for (ContainerType t : values()) {
      mapper.put(t, getContainerFactoryFromContainerType(t));
    }
  }

  private ContainerManagerFactory() {
    // empty constructor //
  }

  public static ContainerManager createContainerManager(HostContext myId, ContainerType type) {
    SpecificContainerManagerFactory sf = mapper.get(type);
    if (sf == null) throw new IllegalArgumentException("Type: " + type + " not supported");
    return sf.createContainerManager(myId);
  }

  public static ContainerManager createRemoteContainerManager(
      HostContext myId, RemoteDockerComponent.DockerRegistry dReg) {
    if (mapper.get(DOCKER_REMOTE) == null)
      throw new IllegalArgumentException("Type: " + DOCKER_REMOTE + " not supported");
    return new DockerContainerManager(myId, dReg);
  }

  private static SpecificContainerManagerFactory getContainerFactoryFromContainerType(
      ContainerType containerType) {

    switch (containerType) {
      case PLAIN:
        return PlainContainerManagerFactory.INSTANCE;

      case DOCKER:
        return DockerContainerManagerFactory.INSTANCE;

      case DOCKER_REMOTE:
        return DockerContainerManagerFactory.REMOTE;

      default:
        throw new IllegalStateException("Unsupported Container type: " + containerType.toString());
    }
  }
}
