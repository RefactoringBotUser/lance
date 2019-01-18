package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import static java.util.Collections.singletonMap;

import de.uniulm.omi.cloudiator.lance.application.component.RemoteDockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandStack;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandUtils;
import java.util.Map;

public class RemoteDockerContainerLogic extends DockerContainerLogic {

  private final RemoteDockerComponent comp;

  //todo: Better construct a Builder that fits into the Class Hierarchy
  public RemoteDockerContainerLogic(DockerContainerLogic.Builder builder, RemoteDockerComponent comp) {
    super(builder);
    this.comp = comp;
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      DockerCommandStack dockerCommandStack = comp.getDockerCommandStack();
      DockerCommand createCmd = dockerCommandStack.getCreate();

      execHandler.doPullImages(comp.getFullImageNameRegStripped());
      //do Not copy lance environment in create command
      createCmd = envVarHandler.resolveDockerEnvVars(createCmd);
      dockerCommandStack.setCreate(createCmd);
      //todo: Create function to check, if these ports match the ports given in docker command
      //Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
      //comp.setPort(portsToSet);
      final String createCmdString = DockerCommandUtils.getFullDockerCommandString(dockerCommandStack,
          Type.CREATE, comp.getFullIdentifier(Type.CREATE));
      //todo: better log this in DockerConnectorClass
      LOGGER.debug(String
          .format("Creating container %s with docker cli command: %s.", myId, createCmdString));
      execHandler.executeSingleDockerCommand(createCmdString);
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    } catch (DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }
}
