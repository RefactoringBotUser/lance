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

package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.util.Registrator;
import de.uniulm.omi.cloudiator.lance.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LifecycleAgentBooter {

  private final static Logger LOGGER = LoggerFactory.getLogger(LifecycleAgentBooter.class);
  private final static Registrator<LifecycleAgent> reg = Registrator.create(LifecycleAgent.class);
  private static volatile LifecycleAgentImpl lca = createAgentImplementation();
  private static volatile LifecycleAgent stub = reg.export(lca, LcaConstants.AGENT_RMI_PORT);

  public static void main(String[] args) {
    LOGGER
        .info("LifecycleAgentBooter: starting. running version: " + Version.getVersionString());

    // TODO: it might be worth exploiting ways to get rid of this
    // dependency to a registry. note that there does not seem to
    // be an easy way to do it (i.e. relaying on standard interfaces)
    if (stub != null && reg.addToRegistry(stub, LcaConstants.AGENT_REGISTRY_KEY)) {
      // from here on RMI takes over //
      LOGGER.info("LifecycleAgentBooter: agent exported. waiting for requests.");
      Thread idle = new Thread(new IdleRunnable());
      idle.setDaemon(true);
      idle.start();
      Runtime.getRuntime().addShutdownHook(new Thread(
          () -> {
            LOGGER.info("LifecycleAgentBooter is shutting down.");
            unregister(lca);
            idle.interrupt();
          }));
    } else {
      LOGGER.error("cannot start lifecycle agent; exiting.");
      Runtime.getRuntime().exit(-128);
    }
  }

  private static LifecycleAgentImpl createAgentImplementation() {
    HostContext ctx = EnvContext.fromEnvironment();
    LOGGER.info("LifecycleAgentBooter: created host context: " + ctx);
    LifecycleAgentImpl impl = new LifecycleAgentImpl(ctx);
    impl.init();
    return impl;
  }

  public static void unregister(LifecycleAgentImpl lifecycleAgentImpl) {
    reg.unregister(lifecycleAgentImpl);
  }

  private static class IdleRunnable implements Runnable {

    @Override
    public void run() {
      while (!Thread.interrupted()) {
        LOGGER.info("Idling forever.");
        Object obj = new Object();
        synchronized (obj) {
          try {
            obj.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
      LOGGER.info("Received interrupt. Ending.");
    }
  }


}
