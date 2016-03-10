package se.redpill.alfresco.repo.bulkimportsite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.bulkimport.BulkFilesystemImporter;
import org.alfresco.repo.bulkimport.BulkImportParameters;
import org.alfresco.repo.bulkimport.NodeImporter;
import org.alfresco.repo.bulkimport.impl.StreamingNodeImporterFactory;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.io.FilenameUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.redpill.alfresco.repo.bulkimportsite.connector.ShareConnector;
import se.redpill.alfresco.repo.bulkimportsite.facade.AuthenticationUtilFacade;
import se.redpill.alfresco.repo.bulkimportsite.mock.AuthenticationUtilFacadeMock;
import se.redpill.alfresco.repo.bulkimportsite.mock.RetryingTransactionHelperMock;
import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public class BulkImportSiteServiceImplTest {

  SiteService siteService;
  NodeService nodeService;
  PersonService personService;
  NamespaceService namespaceService;

  SearchService searchService;

  BulkFilesystemImporter bulkFilesystemImporter;

  StreamingNodeImporterFactory streamingNodeImporterFactory;

  RetryingTransactionHelper retryingTransactionHelper = new RetryingTransactionHelperMock();

  ShareConnector shareConnector;

  SiteInfo siteInfo;

  Mockery m;

  AuthenticationUtilFacade authenticationUtil = new AuthenticationUtilFacadeMock();

  BulkImportSiteServiceImpl bissi;

  final String SHORT_NAME = "testSiteNoMetadata";
  final String SHORT_NAME2 = "testSiteMetadata";

  @Before
  public void setUp() throws Exception {
    m = new JUnit4Mockery();
    m.setImposteriser(ClassImposteriser.INSTANCE);

    siteService = m.mock(SiteService.class);
    nodeService = m.mock(NodeService.class);
    searchService = m.mock(SearchService.class);
    bulkFilesystemImporter = m.mock(BulkFilesystemImporter.class);
    streamingNodeImporterFactory = m.mock(StreamingNodeImporterFactory.class);
    shareConnector = m.mock(ShareConnector.class);
    siteInfo = m.mock(SiteInfo.class, "site info 1");
    personService = m.mock(PersonService.class);
    namespaceService = m.mock(NamespaceService.class);
    bissi = new BulkImportSiteServiceImpl();

    String importPath = getClass().getResource("/dummyfile.donottouch").getFile();
    importPath = FilenameUtils.getFullPath(importPath) + "import";
    bissi.setImportPath(importPath);
    bissi.setAuthenticationUtil(authenticationUtil);
    bissi.setTransactionHelper(retryingTransactionHelper);
    bissi.setSiteService(siteService);
    bissi.setNodeService(nodeService);
    bissi.setSearchService(searchService);
    bissi.setBulkFilesystemImporter(bulkFilesystemImporter);
    bissi.setStreamingNodeImporterFactory(streamingNodeImporterFactory);
    bissi.setShareConnector(shareConnector);
    bissi.setPersonService(personService);
    bissi.setNamespaceService(namespaceService);
    bissi.afterPropertiesSet();
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testGetAllSites() throws Exception {
    m.checking(new Expectations() {
      {
        allowing(siteService).hasSite(SHORT_NAME);
        will(returnValue(false));
        allowing(siteService).hasSite(SHORT_NAME2);
        will(returnValue(false));
      }
    });
    List<Site> allSites = bissi.getAllSites();
    assertFalse(allSites.isEmpty());
    assertEquals(2, allSites.size());
  }

  @Test
  public void testGetSiteWithMetadata() throws Exception {
    m.checking(new Expectations() {
      {
        allowing(siteService).hasSite(SHORT_NAME);
        will(returnValue(false));
        allowing(siteService).hasSite(SHORT_NAME2);
        will(returnValue(false));
      }
    });
    List<Site> allSites = bissi.getAllSites();
    Site site = getSite(allSites, "testSiteMetadata");
    assertNotNull(site);
    assertNotNull(site.getProperties());
    assertNotNull(site.getDescription());
    assertNotNull(site.getType());
    assertNotNull(site.getPreset());
    assertNotNull(site.getDiskPath());
    assertNotNull(site.getTitle());
    assertNotNull(site.getVisibility());
    assertNotNull(site.getShortName());
    assertEquals(1, site.getSiteManagers().size());
    assertEquals(1, site.getSiteCollaborators().size());
    assertEquals(2, site.getSiteContributors().size());
    assertEquals(1, site.getSiteConsumers().size());
  }

  @Test
  public void testGetSiteWithoutMetadata() throws Exception {
    m.checking(new Expectations() {
      {
        allowing(siteService).hasSite(SHORT_NAME);
        will(returnValue(false));
        allowing(siteService).hasSite(SHORT_NAME2);
        will(returnValue(false));
      }
    });
    List<Site> allSites = bissi.getAllSites();
    Site site = getSite(allSites, "testSiteNoMetadata");
    assertNotNull(site);
    assertNull(site.getProperties());
    assertNotNull(site.getDescription());
    assertNotNull(site.getType());
    assertNotNull(site.getPreset());
    assertNotNull(site.getDiskPath());
    assertNotNull(site.getTitle());
    assertNotNull(site.getVisibility());
    assertNotNull(site.getShortName());
    assertEquals(0, site.getSiteManagers().size());
    assertEquals(0, site.getSiteCollaborators().size());
    assertEquals(0, site.getSiteContributors().size());
    assertEquals(0, site.getSiteConsumers().size());
  }

  @Test
  public void testImportSite() throws Exception {
    final NodeRef siteNodeRef = new NodeRef("workspace://SpacesStore/site");
    final NodeRef docLibNodeRef = new NodeRef("workspace://SpacesStore/docLib");
    // List<Site> allSites = bissi.getAllSites();
    m.checking(new Expectations() {
      {
        allowing(siteService).hasSite(SHORT_NAME);
        will(returnValue(false));
        allowing(siteService).hasSite(SHORT_NAME2);
        will(returnValue(false));
      }
    });
    final Site site = bissi.getSite(SHORT_NAME);
    
    assertNotNull(site);
    final NodeImporter ni = m.mock(NodeImporter.class);
    m.checking(new Expectations() {
      {
        oneOf(siteInfo).getNodeRef();
        will(returnValue(siteNodeRef));
        allowing(siteInfo).getShortName();
        will(returnValue(SHORT_NAME));
        oneOf(siteService).getSite(SHORT_NAME);
        will(returnValue(null));
        oneOf(siteService).getSite(SHORT_NAME2);
        will(returnValue(null));

        oneOf(shareConnector).createSite(site);
        will(returnValue(null));

        allowing(siteService).getSite(SHORT_NAME);
        will(returnValue(siteInfo));

        oneOf(shareConnector).createDocumentLibrary(null, SHORT_NAME);
        will(returnValue(docLibNodeRef));

        oneOf(streamingNodeImporterFactory).getNodeImporter(with(any(File.class)));
        will(returnValue(ni));

        oneOf(bulkFilesystemImporter).bulkImport(with(any(BulkImportParameters.class)), with(any(NodeImporter.class)));
        
        oneOf(nodeService).addProperties(with(siteNodeRef), with(any(Map.class)));
        oneOf(siteService).getContainer(SHORT_NAME, SiteService.DOCUMENT_LIBRARY);
        will(returnValue(docLibNodeRef));
      }
    });
    bissi.importSite(site);

  }

  private Site getSite(List<Site> sites, String shortName) {
    for (Site site : sites) {
      if (shortName.equalsIgnoreCase(site.getShortName())) {
        return site;
      }
    }
    return null;
  }
}
