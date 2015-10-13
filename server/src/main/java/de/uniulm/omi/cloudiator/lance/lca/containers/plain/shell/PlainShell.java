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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell;

import de.uniulm.omi.cloudiator.lance.lca.container.environment.ShellLikeInterface;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

import java.io.File;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public interface PlainShell extends ShellLikeInterface {

    public ExecutionResult executeCommand(String command);

    public ExecutionResult executeBlockingCommand(String command);

    public void close();

    public void setDirectory(String directory);

    public void setEnvVar(String key, String value);
}
