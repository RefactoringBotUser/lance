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

package de.uniulm.omi.cloudiator.lance.lifecycle.handlers;

import de.uniulm.omi.cloudiator.lance.deployment.Deployment;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;

/**
 * invoked when the Lifecycle controller starts; 
 * may be used for validating system environment;
 * 
 * @author Joerg Domaschka
 */
public interface InitHandler extends LifecycleHandler {
	
	public static final InitHandler DEFAULT = new InitHandler() {

		@Override public void execute(ExecutionContext ec) {
			// throw new UnsupportedOperationException();
			System.err.println("DEFAULT InitHandler doing nothing");
		}
	}; 
	
	public static final LifecycleHandlerFactory<InitHandler> FACTORY = new LifecycleHandlerFactory<InitHandler>() {
		@Override public final InitHandler getDefault() { return DEFAULT; }

		@Override
		public InitHandler getDeploymentHandler(Deployment d) {
			return new InitDeploymentHandler(d);
		} 
	};
}

final class InitDeploymentHandler implements InitHandler {
	
	private final Deployment d;
	
	InitDeploymentHandler(Deployment _d) {
		d = _d;
	}

	@Override
	public void execute(ExecutionContext ec) {
		d.execute(LifecycleHandlerType.INIT, ec);
	}
}