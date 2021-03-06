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

import de.uniulm.omi.cloudiator.lance.lca.container.environment.StaticEnvVars;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

public interface HostContext extends StaticEnvVars {

  String getPublicIp();

  String getCloudIp();

  String getContainerIp();

  String getPrivateIp();

  String getTenantId();

  String getInternalIp();

  String getCloudIdentifier();

  String getVMIdentifier();

  void close() throws InterruptedException;

  Future<?> run(Runnable runner);

  <T> Future<T> run(Callable<T> callable);

  ScheduledFuture<?> scheduleAction(Runnable runner);

}
