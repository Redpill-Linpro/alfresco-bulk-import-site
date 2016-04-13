package se.redpill.alfresco.repo.bulkimportsite.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import se.redpill.alfresco.repo.bulkimportsite.BulkImportSiteServiceImpl;
import se.redpill.alfresco.repo.bulkimportsite.connector.ShareConnector;
import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public class BulkImportSiteServiceImplIntegrationTest extends AbstractRepoIntegrationTest {
  private static final Logger LOG = Logger.getLogger(BulkImportSiteServiceImplIntegrationTest.class);
  @Autowired
  @Qualifier("redpill.bulkImportSiteService")
  BulkImportSiteServiceImpl bulkImportSiteService;

  @Autowired
  @Qualifier("redpill.bulkImportSiteRepositoryShareEmulatorConnector")
  ShareConnector shareConnector;

  @Autowired
  @Qualifier("AuthorityService")
  AuthorityService authorityService;

  @Override
  public void beforeClassSetup() {
    LOG.info("beforeClassSetup begin");
    super.beforeClassSetup();
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());

    LOG.info("beforeClassSetup end");
  }

  @Override
  public void afterClassSetup() {
    LOG.info("afterClassSetup begin");

    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());

    _authenticationComponent.clearCurrentSecurityContext();

    super.afterClassSetup();
    LOG.info("afterClassSetup end");
  }

  @Before
  public void setUp() {
    bulkImportSiteService.setShareConnector(shareConnector);
    String importPath = getClass().getResource("/dummyfile.donottouch").getFile();
    importPath = FilenameUtils.getFullPath(importPath) + "import";
    bulkImportSiteService.setImportPath(importPath);
    assertFalse("Site testSiteMetadata must not exist", _siteService.hasSite("testSiteMetadata"));
    assertFalse("Site testSiteNoMetadata must not exist", _siteService.hasSite("testSiteNoMetadata"));
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testImport() throws Exception {
    Site siteData = bulkImportSiteService.getSite("testSiteMetadata");
    bulkImportSiteService.importSite(siteData);
    assertTrue(_siteService.hasSite("testSiteMetadata"));
    SiteInfo siteInfo = _siteService.getSite("testSiteMetadata");
    NodeRef nodeRef = siteInfo.getNodeRef();
    _nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
    System.out.print("Waiting for import to finish");
    while (inProgress()) {
      Thread.sleep(1000);
      System.out.print(".");
    }
    System.out.println(" Done!");
    _siteService.deleteSite(siteInfo.getShortName());
    removeSiteRoles(siteInfo.getShortName());
    assertFalse(_siteService.hasSite(siteInfo.getShortName()));
  }

  @Test
  public void testImportInProgress() throws Exception {
    
    Site siteData = bulkImportSiteService.getSite("testSiteMetadata");
    Site siteData2 = bulkImportSiteService.getSite("testSiteNoMetadata");
    bulkImportSiteService.importSite(siteData);
    assertTrue(_siteService.hasSite("testSiteMetadata"));
    SiteInfo siteInfo = _siteService.getSite("testSiteMetadata");
    NodeRef nodeRef = siteInfo.getNodeRef();
    _nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
    try {
      bulkImportSiteService.importSite(siteData2);
      assertFalse(true);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Import already in progress"));
    }
    System.out.print("Waiting for import to finish");
    while (inProgress()) {
      Thread.sleep(1000);
      System.out.print(".");
    }
    System.out.println(" Done!");

    _siteService.deleteSite(siteInfo.getShortName());
    removeSiteRoles(siteInfo.getShortName());
    assertFalse(_siteService.hasSite(siteInfo.getShortName()));
  }
  
  @Test
  public void testIncrementalImport() throws Exception {
    Site siteData = bulkImportSiteService.getSite("testSiteMetadata");
    bulkImportSiteService.importSite(siteData);
    assertTrue(_siteService.hasSite("testSiteMetadata"));
    SiteInfo siteInfo = _siteService.getSite("testSiteMetadata");
    NodeRef nodeRef = siteInfo.getNodeRef();
    _nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
    System.out.print("Waiting for import to finish");
    while (inProgress()) {
      Thread.sleep(1000);
      System.out.print(".");
    }
    System.out.println(" Done!");

    bulkImportSiteService.setAllowIncremental(true);
    
    siteData = bulkImportSiteService.getSite("testSiteMetadata");
    bulkImportSiteService.importSite(siteData);
    assertTrue(_siteService.hasSite("testSiteMetadata"));
    siteInfo = _siteService.getSite("testSiteMetadata");
    nodeRef = siteInfo.getNodeRef();
    _nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
    System.out.print("Waiting for incremental import to finish");
    while (inProgress()) {
      Thread.sleep(1000);
      System.out.print(".");
    }
    System.out.println(" Done!");
    
    _siteService.deleteSite(siteInfo.getShortName());
    removeSiteRoles(siteInfo.getShortName());
    assertFalse(_siteService.hasSite(siteInfo.getShortName()));
  }

  public boolean inProgress() {
    return bulkImportSiteService.inProgress();
  }

  public void removeSiteRoles(String siteShortName) {
    String siteGroup = _siteService.getSiteGroup(siteShortName);
    authorityService.deleteAuthority(siteGroup, true);
  }
}
