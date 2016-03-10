package se.redpill.alfresco.repo.bulkimportsite.mock;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;

import se.redpill.alfresco.repo.bulkimportsite.facade.AuthenticationUtilFacade;

/**
 * Mock class for testability of the Authentication UTil
 * @author Marcus Svartmark - Redpill Linpro AB
 *
 */
public class AuthenticationUtilFacadeMock implements AuthenticationUtilFacade {

  @Override
  public <R> R runAsSystem(RunAsWork<R> runAsWork) {
    R doWork = null;
    try {
      doWork = runAsWork.doWork();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return doWork;
  }

}
