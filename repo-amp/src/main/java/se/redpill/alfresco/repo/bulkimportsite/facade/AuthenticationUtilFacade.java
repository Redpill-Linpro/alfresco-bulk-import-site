package se.redpill.alfresco.repo.bulkimportsite.facade;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;

/**
 * We use a facade to make it possible to test the code. In case of testing the
 * facade is mocked.
 * 
 * @see AuthenticationUtilFacadeMock
 * @author Marcus Svartmark - Redpill Linpro AB
 *
 */
public interface AuthenticationUtilFacade {
  /**
   * @see org.alfresco.repo.security.authentication#runAsSystem
   */
  public <R> R runAsSystem(RunAsWork<R> runAsWork);
}
