package se.redpill.alfresco.repo.bulkimportsite.facade;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;

public class AuthenticationUtilFacadeImpl implements AuthenticationUtilFacade {

  @Override
  public <R> R runAsSystem(RunAsWork<R> runAsWork) {
    return AuthenticationUtil.runAsSystem(runAsWork);
  }

}
