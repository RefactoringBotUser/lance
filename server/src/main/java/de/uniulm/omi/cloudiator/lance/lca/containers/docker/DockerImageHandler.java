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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.RegistryAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;

final class DockerImageHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDockerContainerLogic.class);
    
    private final DockerOperatingSystemTranslator translator;
    private OperatingSystem os;
    private final DockerConnector client;
    private final AbstractComponent myComponent;
    private final DockerConfiguration dockerConfig;
    
    private volatile ImageCreationType initSource;
    
    DockerImageHandler(OperatingSystem osParam, DockerOperatingSystemTranslator translatorParam, 
                DockerConnector clientParam, AbstractComponent componentParam, DockerConfiguration dockerConfigParam) {
        if(osParam == null) 
            throw new NullPointerException("operating system has to be set.");
        
        dockerConfig = dockerConfigParam;
        os = osParam;
        translator = translatorParam;
        client = clientParam;
        myComponent = componentParam;
    }

    DockerImageHandler(DockerOperatingSystemTranslator translatorParam,
        DockerConnector clientParam, AbstractComponent componentParam, DockerConfiguration dockerConfigParam) {

        dockerConfig = dockerConfigParam;
        translator = translatorParam;
        client = clientParam;
        myComponent = componentParam;
    }

    static String createComponentInstallId(AbstractComponent myComponent) {
        return "dockering." + "component." + myComponent.getComponentId().toString(); 
    }
    
    private String createComponentInstallId() {
        return createComponentInstallId(myComponent);
    }
    
    private String buildImageTagName(ImageCreationType type, String componentInstallId) {
        final String key;
        switch(type){
        case COMPONENT: 
            key = imageFromComponent(componentInstallId);
            break;
        case OPERATING_SYSTEM:
            key = translator.translate(os);
            break;
        case COMPONENT_INSTANCE:
            throw new UnsupportedOperationException();
        default:
            throw new IllegalArgumentException();
        }
        return key;
    }

    private String buildImageTagName(ImageCreationType type, String componentInstallId, String imageName) {
        final String key;
        switch(type){
            case COMPONENT_INSTANCE:
                key = imageFromComponentInstance(imageName);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return key;
    }

    private String imageFromComponent(String componentInstallId){
        String tmpkey = componentInstallId; 
        String ostag = os.toString();
        ostag = ostag.replaceAll(":",  "_");
        String tmp = tmpkey.toLowerCase() + ":" + ostag.toLowerCase();
        if(!dockerConfig.registryCanBeUsed())
        	return tmp;
        return dockerConfig.prependRegistry(tmp);
    }

    //TODO: Maybe allow several writings for the image name (e.g. "cassandra", "cassandra 12.2.2", "cassandra:12.2.2", "cassandra latest", "cassandra:latest") and resolve via regex check
    // to a correct image name
    //TODO: Implement digest option
    private String imageFromComponentInstance(String imageName){
        String[] split = imageName.split(":");
        String imageNameUnTagged = split[0];
        String tagNameTmp = "";
        for (int i=1; i<split.length; ++i) {
            tagNameTmp += split[i];
        }
        String tagName = tagNameTmp.length() == 0 ? "latest" : tagNameTmp;
        String imageNameTagged = imageNameUnTagged + ":" + tagName;
        return imageNameTagged;
    }

    private String doGetSingleImage(String key) throws DockerException {
        if(client.findImage(key) != null) {
            return key;
        }
   
        try {      	
            client.pullImage(key); 
            return key; 
        } catch(DockerException de) {
            LOGGER.debug("could not pull image: " + key + " creating a new one.");
            return null; 
        }
    }
    
    /**
     * 
     * @param myId the instance id of the container
     * @return
     * @throws DockerException
     */
    String doPullImages(ComponentInstanceId myId) throws DockerException {
        // first step: try to find matching image for configured component
        String result = searchImageInLocalCache();
        if(result == null){
        	// second step: try to find matching image for prepared component
            // in case a custom docker registry is configured  
            result = getImageFromPrivateRepository();
            if(result == null) {
            	// third step: fall back to the operating system //
            	result = getImageFromDefaultLocation();
            }
        }
        if(result != null)
        	return result;

        throw new DockerException("cannot pull image: " + myId);
    }

    //TODO: Implement searching in generic repo and localCache
    String doPullImages(ComponentInstanceId myId, String imageName) throws DockerException {
        // first step: try to find matching image for configured component
        String result = searchImageInLocalCache(imageName);
        if(result == null){
            // second step: try to find matching image for prepared component
            // in case a custom docker registry is configured
            result = getImageFromPrivateRepository(imageName);
            if(result == null) {
                // third step: fall back to the operating system //
                result = getImageFromDefaultLocation(imageName);
            }
        }
        if(result != null)
            return result;

        throw new DockerException("cannot pull image: " + myId);
    }

    private String searchImageInLocalCache(String imageName) {
        // currently not implemented;
        return null;
    }

    private String searchImageInLocalCache() {
        // currently not implemented; 
    	return null;
    }

    private String getImageFromPrivateRepository() throws DockerException {
    	String componentInstallId = createComponentInstallId();
        String target = buildImageTagName(ImageCreationType.COMPONENT, componentInstallId);
        String result = doGetSingleImage(target);
        if(result != null) {
        	LOGGER.info("pulled prepared image: " + result);
            initSource = ImageCreationType.COMPONENT;
            return result;
        }
        return null;
    }

    private String getImageFromPrivateRepository(String imageName) {
        if (!dockerConfig.registryCanBeUsed()) {
            return null;
        }

        String result = null;
        String userName = dockerConfig.getUserName();
        String password = dockerConfig.getPassword();
        String hostName = DockerConfiguration.DockerConfigurationFields.getHostname();
        int port = DockerConfiguration.DockerConfigurationFields.getPort();

        try {
          if (userName.equals("") && password.equals("")) {
            result = doGetSingleImage(dockerConfig.prependRegistry(imageName));
          } else {
            RegistryAuth registryAuth =
                RegistryAuth.builder()
                    .username(userName)
                    .password(password)
                    .serverAddress(hostName)
                    .build();
            DockerClient dClient =
                DefaultDockerClient.builder()
                    .registryAuth(registryAuth)
                    /*.uri("http://" + xxx.xxx.xxx + ":" + 2375).build();*/
                    .uri("http://" + hostName + (!(port < 0) ? ":" + Integer.toString(port) : ""))
                    .build();

            dClient.pull(dockerConfig.prependRegistry(imageName));
            result = imageName;
          }
        } catch (InterruptedException e) {
            LOGGER.error("Problems with DockerClient. Cannot pull image " + imageName + " from private registry");
        } catch (com.spotify.docker.client.exceptions.DockerException e) {
            LOGGER.error("Problems with DockerClient. Cannot pull image " + imageName + " from private registry");
        } catch (DockerException e) {
            LOGGER.error("Cannot pull image " + imageName + " from private registry");
        }

        if(result != null) {
            LOGGER.info("pulled image: " + dockerConfig.prependRegistry(imageName) + " from private registry");
            initSource = ImageCreationType.COMPONENT_INSTANCE;
        }

        return result;
    }

    private String getImageFromDefaultLocation() throws DockerException {
        String target = buildImageTagName(ImageCreationType.OPERATING_SYSTEM, null);
        String result = doGetSingleImage(target);
        if(result != null) {
        	LOGGER.info("pulled default image: " + result);
        	initSource = ImageCreationType.OPERATING_SYSTEM;
            return result;
        }
        return null;
    }

    private String getImageFromDefaultLocation(String imageName) throws DockerException {
        String result = doGetSingleImage(imageName);
        if(result != null) {
            LOGGER.info("pulled default image: " + result);
            initSource = ImageCreationType.COMPONENT_INSTANCE;
            return result;
        }
        return null;
    }

    void runPostInstallAction(ComponentInstanceId myId) throws DockerException {
        if(initSource == ImageCreationType.OPERATING_SYSTEM) {
        	String componentInstallId = createComponentInstallId();
            String target = buildImageTagName(ImageCreationType.COMPONENT, componentInstallId);
            // we probably will not need this return value
            // let's keep it for debugging purposes, though
            // @SuppressWarnings("unused") String imageSnapshot = 
            client.createSnapshotImage(myId, target);
            client.pushImage(target);
        }
    }
    
    static enum ImageCreationType {
        COMPONENT,
        COMPONENT_INSTANCE,
        OPERATING_SYSTEM,
    }

    OperatingSystem getOperatingSystem() { 
    	return os; 
    }
}
