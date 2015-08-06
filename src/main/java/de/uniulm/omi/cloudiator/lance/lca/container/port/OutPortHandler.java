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

package de.uniulm.omi.cloudiator.lance.lca.container.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

final class OutPortHandler {

    private static final Logger logger = LoggerFactory.getLogger(OutPort.class);
    
    private static final Map<PortHierarchyLevel, List<DownstreamAddress>> EMPTY_VISIT_MAP;
    
    static {
        Map<PortHierarchyLevel, List<DownstreamAddress>> map = new HashMap<>();
        map.put(PortRegistryTranslator.PORT_HIERARCHY_0, Collections.<DownstreamAddress>emptyList());
        map.put(PortRegistryTranslator.PORT_HIERARCHY_1, Collections.<DownstreamAddress>emptyList());
        EMPTY_VISIT_MAP = Collections.unmodifiableMap(map);
    }
    
    private final List<OutPortState> portStates = new ArrayList<>();
    private final DeployableComponent myComponent;
    
    public OutPortHandler(DeployableComponent myComponentParam) {
        myComponent = myComponentParam;
    }
    
    void initPortStates(PortRegistryTranslator accessor, PortHierarchy portHierarchy) throws RegistrationException {
        List<OutPort> outPorts = myComponent.getDownstreamPorts();
        if(outPorts.isEmpty()) return;
        
        for(OutPort out : outPorts) {
            Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> instances = accessor.findDownstreamInstances(out, portHierarchy);
            OutPortState state = new OutPortState(out, instances);
            portStates.add(state);
        }
    }
    
    List<PortDiff<DownstreamAddress>> updateDownstreamPorts(PortRegistryTranslator accessor, PortHierarchy portHierarchy) throws RegistrationException {
        List<PortDiff<DownstreamAddress>> changedPorts = new LinkedList<>();
        for(OutPortState out_port : portStates) {
            Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> instances = accessor.findDownstreamInstances(out_port.getPort(), portHierarchy);
            instances = filterInstancesWithUnsetPorts(instances);
            PortDiff<DownstreamAddress> diff = out_port.updateWithDiff(instances);
            if(diff.hasDiffs()) changedPorts.add(diff);
        }
        return changedPorts;
    }

    public boolean requiredDownstreamPortsSet() {
        for(OutPortState state : portStates) {
            if(!state.requiredAndSet()) return false;
        }
        return true;
    }

    @SuppressWarnings("static-method")
	public void startPortUpdaters() {
        logger.error("Port updaters are currently not run.");
    }
    
    private static Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> filterInstancesWithUnsetPorts(Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> instances) {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> retVal = new HashMap<>();
        for(Entry<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> entry : instances.entrySet()) {
            boolean doSet = true;
            ComponentInstanceId id = entry.getKey();
            HierarchyLevelState<DownstreamAddress> state = entry.getValue();
            for(PortHierarchyLevel level : state) {
                DownstreamAddress i = state.valueAtLevel(level);
                if(! i.hasValidPort()) {doSet = false; break; } 
            }
            if(doSet) retVal.put(id, state);
        }
        return retVal;
    }

    void accept(NetworkVisitor visitor) {
        for(OutPortState out : portStates) {
            Map<PortHierarchyLevel, List<DownstreamAddress>> elements = out.sinksByHierarchyLevel();
            Map<PortHierarchyLevel, List<DownstreamAddress>> toVisit = elements.isEmpty() ? 
                    doCollect(out, EMPTY_VISIT_MAP) : doCollect(out, elements);        
            doVisit(visitor, out, toVisit);
        }
    }
    
    private static Map<PortHierarchyLevel, List<DownstreamAddress>> doCollect(OutPortState out, Map<PortHierarchyLevel, List<DownstreamAddress>> elements) {
        Map<PortHierarchyLevel, List<DownstreamAddress>> toVisit = new HashMap<>();        
        for(Entry<PortHierarchyLevel, List<DownstreamAddress>> entry : elements.entrySet()) {
            PortHierarchyLevel level = entry.getKey();
            List<DownstreamAddress> sinks = entry.getValue();
            sinks = out.adaptSinkListByBoundaries(sinks);
            // FIXME: filter out element that are not reachable on a certain hierarchy level
            // because they are on a different cluster (e.g. different cloud provider)
            toVisit.put(level, sinks);
            // sinks are required, but no sinks known for this port at this 
            // hierarchy level it is probably best to drop the entire sink 
            // from exporting in order to avoid inconsistencies between 
            // multiple levels
            if(sinks == null) return Collections.emptyMap();
        }
        return toVisit;
    }
    
    private static void doVisit(NetworkVisitor visitor, OutPortState out, Map<PortHierarchyLevel, List<DownstreamAddress>> toVisit) {
        for(Entry<PortHierarchyLevel, List<DownstreamAddress>> entry : toVisit.entrySet()) {
            PortHierarchyLevel level = entry.getKey();
            List<DownstreamAddress> sinks = entry.getValue();
            visitor.visitOutPort(out.getPortName(), level, sinks);
        }
    }
    
    /*
        private OutPortState findMatchingOutPortState(OutPort the_port) {
        for(OutPortState state : portStates) {
            if(state.matchesPort(the_port)) return state;
        }
        throw new IllegalStateException("PortState for port " + the_port + " not found in component: " + myComponent + "@" + deploymentContext);
    }
        public void startPortUpdaters() {
        for(OutPortState state : portStates) {
            state.enactScheduling(this);
            //FIXME: the scheduling should not be the task of the OutPortState
        }
    }
    
    private void addConnectionListAsEnvironmentVariable(DockerShell dshell, String portName, List<String> addresses) {
        String s = "";
        for(String a : addresses) {
            if(! s.isEmpty()) s = s + ",";
            s = s + a;
        }
        addEnvironmentVariable(dshell, portName, s);
    }
    
    private void flushOutgoingPorts(DockerShell dshell) {
        List<OutPortState> currentVersion = null;
        List<OutPortState> statesWithoutInitialValue = new ArrayList<OutPortState>(portStates);
        while(true) {
            currentVersion = statesWithoutInitialValue;
            statesWithoutInitialValue = new ArrayList<OutPortState>(currentVersion);
            
            for(OutPortState out : currentVersion) {
                try {
                    out.init();
                    if(flushSinglePort(dshell, out)) {
                        statesWithoutInitialValue.remove(out);
                    }
                } catch(RegistrationException re) {
                    re.printStackTrace();
                }    
            }
            if(statesWithoutInitialValue.size() == 0) break;
            System.err.println("did not initial values for all out ports; sleeping for some time... ");
            try {
                Thread.sleep(3000);
            } catch(InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
        
    private final boolean flushSinglePort(DockerShell dshell, OutPort an_out_port, final Map<ComponentInstanceId, Map<String, String>> the_sinks) {
        String this_cloud_id = registryAccessor.getLocalCloudProdivder();
        OutPortState port_state = findMatchingOutPortState(an_out_port);
        
        List<String> list = port_state.getBoundaryAwareSinkList(this_cloud_id, the_sinks);
        if(list != null) {
            addConnectionListAsEnvironmentVariable(dshell, an_out_port.getName(), list);
        } else {
            // do nothing, but retry later on? // 
        }
        return list != null;
    }
    
    private final boolean flushSinglePort(DockerShell dshell, OutPortState out) {
        List<String> list = out.getBoundaryAwareSinkList();
        if(list != null) {
            addConnectionListAsEnvironmentVariable(dshell, out.getPortName(), list);
        } else { // means there are too few sinks available for this out port //
            // do nothing, but retry later on? // 
        }
        return list != null;
    }
    */
}
