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

package de.uniulm.omi.cloudiator.lance.lca.registry.etcd;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

final class EtcdRegistryImpl implements LcaRegistry, Serializable {

	private static final long serialVersionUID = 1L;
	
	private static String DESCRIPTION = "Description";
	private static String NAME = "Name";
	private static String MAIN_DESCRIPTION = "main directory for cloudiator's life cylce agent";
	private static String MAIN_NAME = "Life Cycle Agent";
	
	private static String APP_INSTANCE_DESCRIPTION = "Application Instance Directory with all components of this application instance";
	private static String COMPONENT_DESCRIPTION = "Component Directory with all component instances of this component in this application instance";
	private static String COMPONENT_INSTANCE_DESCRIPTION = "Component Instance Directory with all properties of this component instances";
	
	private final URI[] uris;
	private transient EtcdClient etcd;
	
	public EtcdRegistryImpl(URI[] _uris) throws RegistrationException {
		uris = _uris;
		etcd = new EtcdClient(uris);
		init();
	}
	
	private void init() throws RegistrationException {
		String dirName = "/lca";
		createDirectorIfItDoesNotExist(dirName);
		setPropertyInDirectory(dirName, DESCRIPTION, MAIN_DESCRIPTION);
		setPropertyInDirectory(dirName, NAME, MAIN_NAME);
	}
	
	@Override
	public void addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name) throws RegistrationException {
		String dirName = "/lca/" + instId.toString();
		createDirectorIfItDoesNotExist(dirName);
		setPropertyInDirectory(dirName, DESCRIPTION, APP_INSTANCE_DESCRIPTION);
		setPropertyInDirectory(dirName, NAME, name);
	}

	@Override
	public void addComponent(ApplicationInstanceId instId, ComponentId cid, String name) throws RegistrationException {
		String dirName = generateComponentDirectory(instId, cid);
		createDirectorIfItDoesNotExist(dirName);
		setPropertyInDirectory(dirName, DESCRIPTION, COMPONENT_DESCRIPTION);
		setPropertyInDirectory(dirName, NAME, name);
	}

	@Override
	public void addComponentInstance(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId)
			throws RegistrationException {
		String dirName = generateComponentInstanceDirectory(instId, cid, cinstId);
		createDirectorIfItDoesNotExist(dirName);
		setPropertyInDirectory(dirName, DESCRIPTION, COMPONENT_INSTANCE_DESCRIPTION);
	}

	@Override
	public void addComponentProperty(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId, String property, Object value) throws RegistrationException {
		String dirName = generateComponentInstanceDirectory(instId, cid, cinstId);
		setPropertyInDirectory(dirName, property, value.toString());
	}

	@Override
	public Map<ComponentInstanceId, Map<String, String>> dumpComponent(ApplicationInstanceId instId, ComponentId compId) throws RegistrationException {
		
		Map<ComponentInstanceId, Map<String, String>> retVal = null;
		String dirName = generateComponentDirectory(instId, compId);
		EtcdKeysResponse ccc = null;
		try {
			ccc = etcd.getDir(dirName).recursive().sorted().send().get();
			retVal = dumpFirstLevelKeys(ccc.node);
			for(ComponentInstanceId id : retVal.keySet()) {
				String secondLevelDir = generateComponentInstanceDirectory(instId, compId, id); 
				ccc = etcd.getDir(secondLevelDir).recursive().sorted().send().get();
				dumpSecondLevelKeys(ccc.node, retVal.get(id));
			}
		} catch(IOException ioe) {
			throw new RegistrationException(ioe);
		} catch (java.util.concurrent.TimeoutException e) {
			throw new RegistrationException(e);
		} catch (EtcdException e) {
			throw new RegistrationException(e);
		}
		return retVal;
	}
		
	private void fillMapWithValue(String key, String value, Map<String, String> map) {
		if(DESCRIPTION.equals(key)) return;
		if(NAME.equals(key)) return;
		map.put(key, value);
	}
	
	private Map<ComponentInstanceId, Map<String, String>> dumpFirstLevelKeys(EtcdNode root) {
		final String mainDir = root.key;
		Map<ComponentInstanceId, Map<String, String>> retVal = new HashMap<ComponentInstanceId, Map<String, String>>();
		final int length = mainDir.length() + 1;
		for(EtcdNode node : root.nodes) {
			if(! node.dir) continue;
			String key = node.key.substring(length);
			String[] split = key.split("/");
			if(split.length == 1) { // component instance element //
				createComponentInstanceIfNotExistantAndFillWithMap(key, retVal);
			} else {
				throw new IllegalStateException("invalid directory structure for key");
				// Map<String,String> map = createComponentInstanceIfNotExistantAndFillWithMap(split[0], retVal);
				// fillMapWithValue(split[1], map);
			}
		}
		return retVal;
	}
	
	private void dumpSecondLevelKeys(EtcdNode root, Map<String, String> map) {
		final String mainDir = root.key;
		final int length = mainDir.length() + 1;
		for(EtcdNode node : root.nodes) {
			if(node.dir) throw new IllegalStateException("unexpected to find directories in component instances");
			String key = node.key.substring(length);
			String[] split = key.split("/");
			if(split.length == 1) { // component instance element //
				fillMapWithValue(key, node.value, map);
			} else {
				throw new IllegalStateException("invalid directory structure for key");
				// Map<String,String> map = createComponentInstanceIfNotExistantAndFillWithMap(split[0], retVal);
				// fillMapWithValue(split[1], map);
			}
		}
	}

	private final Map<String, String> createComponentInstanceIfNotExistantAndFillWithMap(String key, Map<ComponentInstanceId, Map<String, String>> retVal) {
		if(DESCRIPTION.equals(key)) return Collections.emptyMap();
		if(NAME.equals(key)) return Collections.emptyMap();
		ComponentInstanceId inst = ComponentInstanceId.fromString(key);
		Map<String, String> map = retVal.get(inst);
		if(map == null) {
			map = new HashMap<String, String>();
			retVal.put(inst, map);
		} else {
			throw new IllegalStateException("unexpected event: map already exists.");
		}
		return map;
	}
	
	private final String generateComponentDirectory(ApplicationInstanceId instId, ComponentId cid) {
		return "/lca/" + instId.toString() + "/" + cid.toString();
	}
	
	private final String generateComponentInstanceDirectory(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId) {
		return "/lca/" + instId.toString() + "/" + cid.toString() + "/" + cinstId.toString();
	}
	
	@Override
	public String getComponentProperty(ApplicationInstanceId appInstId, ComponentId compId, ComponentInstanceId myId, String property) throws RegistrationException {
		String dirName = generateComponentInstanceDirectory(appInstId, compId, myId);
		return readPropertyFromDirectory(dirName, property);
	}
	
	private void createDirectorIfItDoesNotExist(String dirName) throws RegistrationException {
		if(directoryDoesExist(dirName)) return;
		try {
			EtcdKeysResponse ccc = etcd.putDir(dirName).send().get();
		} catch(IOException ioe) {
			throw new RegistrationException(ioe);
		} catch (java.util.concurrent.TimeoutException e) {
			throw new RegistrationException(e);
		} catch (EtcdException e) {
			throw new RegistrationException(e);
		}
	}
	
	private boolean directoryDoesExist(String dirName) throws RegistrationException {
		try { 
			EtcdKeysResponse aaa = etcd.getDir(dirName).send().get(); 
			if(aaa != null) return true;
			else return false;
		} catch(IOException ioe) {
			throw new RegistrationException(ioe);
		} catch (java.util.concurrent.TimeoutException e) {
			throw new RegistrationException(e);
		} catch (EtcdException e) {
			if(e.errorCode == 100) return false;
			throw new RegistrationException(e);
		}
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		etcd = new EtcdClient(uris);
	}
	
	private String readPropertyFromDirectory(String dirName, String prop) throws RegistrationException {
		try {
			EtcdKeysResponse ccc = etcd.get(dirName + "/" + prop).send().get();
			return ccc.node.value;
		} catch(IOException ioe) {
			throw new RegistrationException(ioe);
		} catch (java.util.concurrent.TimeoutException e) {
			throw new RegistrationException(e);
		} catch (EtcdException e) {
			throw new RegistrationException(e);
		}
	}
	
	private void setPropertyInDirectory(String dirName, String prop, String value) throws RegistrationException {
		try {
			EtcdKeysResponse ccc = etcd.put(dirName + "/" + prop, value.toString()).send().get();
			ccc.toString();
		} catch(IOException ioe) {
			throw new RegistrationException(ioe);
		} catch (java.util.concurrent.TimeoutException e) {
			throw new RegistrationException(e);
		} catch (EtcdException e) {
			throw new RegistrationException(e);
		}
	}
	
	/*
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		
	} 
	
	private void readObjectNoData() throws ObjectStreamException {
		
	}*/
}