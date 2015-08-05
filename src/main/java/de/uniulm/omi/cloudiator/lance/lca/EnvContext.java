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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class EnvContext implements HostContext {

	public static String PUBLIC_IP_KEY = "host.ip.public";
	public static String PRIVATE_IP_KEY = "host.ip.private";
	public static String HOST_OS_KEY = "host.os";
	public static String TENANT_ID_KEY = "TENANT_ID";
	public static String VM_CLOUD_ID = "host.vm.cloud.id";
	public static String VM_ID_KEY = "VM_ID";
	
	private static String[] VALUES = new String[] {
		PUBLIC_IP_KEY, PRIVATE_IP_KEY, HOST_OS_KEY, TENANT_ID_KEY, VM_ID_KEY, VM_CLOUD_ID,
	};
	
	private final Map<String,String> hostContext;
	private final ScheduledExecutorService periodicExecutor = Executors.newScheduledThreadPool(4);
	private final ExecutorService executor = Executors.newScheduledThreadPool(4);
	
	EnvContext(Map<String,String> _ctx) {
		hostContext = _ctx;
	}
	
	static HostContext fromEnvironment() {
		Map<String,String> values = new HashMap<String,String>();
		for(String key : VALUES) {
			String s = System.getProperty(key);
			if(s == null || s.isEmpty()) s = "<unknown>";
			values.put(key, s);
		}
		return new EnvContext(values);
	}

	@Override
	public String getPublicIp() { return hostContext.get(PUBLIC_IP_KEY); }

	@Override
	public String getInternalIp() { return hostContext.get(PRIVATE_IP_KEY); }

	@Override
	public String toString() {
		return "HostContext: " + hostContext;
	}

	@Override
	public void scheduleAction(Runnable runner) {
		// TODO: reintroduce 
		// periodicExecutor.scheduleWithFixedDelay(runner, 0L, 10L, TimeUnit.SECONDS);
	}
	
	@Override
	public void run(Runnable runner) {
		executor.execute(runner);
	}

	@Override
	public void close() throws InterruptedException {
		executor.shutdownNow();
		while(true) {
			executor.awaitTermination(10, TimeUnit.SECONDS);
			if(executor.isTerminated()) return;
		}
	}

	@Override
	public String getCloudIdentifier() {
		return hostContext.get(VM_CLOUD_ID);
	}
}