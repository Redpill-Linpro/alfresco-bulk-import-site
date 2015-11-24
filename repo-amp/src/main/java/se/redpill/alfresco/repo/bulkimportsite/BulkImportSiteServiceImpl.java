package se.redpill.alfresco.repo.bulkimportsite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.bulkimport.BulkFilesystemImporter;
import org.alfresco.repo.bulkimport.BulkImportParameters;
import org.alfresco.repo.bulkimport.NodeImporter;
import org.alfresco.repo.bulkimport.impl.StreamingNodeImporterFactory;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import se.redpill.alfresco.repo.bulkimportsite.connector.ShareConnector;
import se.redpill.alfresco.repo.bulkimportsite.facade.AuthenticationUtilFacade;
import se.redpill.alfresco.repo.bulkimportsite.facade.AuthenticationUtilFacadeImpl;
import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public class BulkImportSiteServiceImpl implements InitializingBean, BulkImportSiteService {

  private static Logger logger = Logger.getLogger(BulkImportSiteServiceImpl.class);

  SiteService siteService;
  SearchService searchService;
  NodeService nodeService;
  BulkFilesystemImporter bulkFilesystemImporter;
  StreamingNodeImporterFactory streamingNodeImporterFactory;
  RetryingTransactionHelper transactionHelper;
  Map<String, String> userCache = new HashMap<String, String>();
  ShareConnector shareConnector;
  AuthenticationUtilFacade authenticationUtil = new AuthenticationUtilFacadeImpl();
  PersonService personService;
  NamespaceService namespaceService;
  AuthorityService authorityService;

  private String importPath;
  private boolean skipEmptyStrings = false;
  // for logging
  static int noOfFilesWritten = 0;

  @Override
  public List<Site> getAllSites() throws URISyntaxException, InvalidPropertiesFormatException, IOException {
    List<Site> result = new ArrayList<Site>();
    File file = new File(importPath);
    String[] directories = file.list(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        return new File(current, name).isDirectory();
      }
    });
    if (directories != null && directories.length > 0) {
      for (String dir : directories) {
        Site site = new Site();
        site.setShortName(dir);
        site.setDiskPath(importPath + "/" + dir);
        File metadataFile = new File(site.getDiskPath() + ".metadata.properties.xml");
        Properties siteProperties = null;
        if (metadataFile.exists()) {
          siteProperties = getSiteProperties(metadataFile);
        } else {
          logger.warn("No metadata file found for site " + site.getShortName());
        }
        site.setProperties(siteProperties);

        if (siteService.hasSite(dir)) {
          site.setImported(true);
        }
        result.add(site);
      }
    }
    return result;
  }

  /**
   * Read site properties from file
   * 
   * @param metadataFile
   *          Path to the property file to read
   * @return
   * @throws InvalidPropertiesFormatException
   * @throws IOException
   */
  protected Properties getSiteProperties(File metadataFile) throws InvalidPropertiesFormatException, IOException {

    FileInputStream fileInput = new FileInputStream(metadataFile);
    Properties properties = new Properties();
    try {
      properties.loadFromXML(fileInput);
    } finally {
      fileInput.close();
    }
    return properties;
  }

  @Override
  public Site getSite(String placeShortName) throws URISyntaxException, InvalidPropertiesFormatException, IOException {
    // I have found no convenient way (that acutally works) to get
    // one place in the domino atom REST Api
    // lets use up some time...
    if (placeShortName != null) {
      List<Site> allPlaces = getAllSites();
      for (Site place : allPlaces) {
        if (placeShortName.equalsIgnoreCase(place.getShortName())) {
          return place;
        }
      }
    }

    return null;
  }

  @Override
  public void importSite(final Site place) throws Exception {

    authenticationUtil.runAsSystem(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {
        SiteInfo siteInfo = siteService.getSite(place.getShortName());

        if (siteInfo == null) {
          // shareConnector.createSite(place);
        } else {
          throw new AlfrescoRuntimeException("Site already exist " + siteInfo.getShortName());
        }

        if (siteInfo == null) {
          final Map<String, String> cookies = shareConnector.createSite(place);
          siteInfo = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<SiteInfo>() {
            @Override
            public SiteInfo execute() throws Throwable {
              return siteService.getSite(place.getShortName());
            }
          }, true, true);

          final SiteInfo finalSiteInfo = siteInfo;
          if (siteInfo == null) {
            throw new AlfrescoRuntimeException("Could not create site with short name " + place.getShortName());
          } else {
            // Create doclib
            shareConnector.createDocumentLibrary(cookies, siteInfo.getShortName());
            final NodeRef documentLibrary = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
              @Override
              public NodeRef execute() throws Throwable {
                addMembers(place, finalSiteInfo);
                nodeService.addProperties(finalSiteInfo.getNodeRef(), place.getAlfrescoProperties(namespaceService, skipEmptyStrings));
                return siteService.getContainer(finalSiteInfo.getShortName(), SiteService.DOCUMENT_LIBRARY);
              }
            }, false, true);

            final String documentsPath = importPath + "/" + place.getShortName() + "/documentLibrary";

            logger.info("Documents to import for site " + place.getShortName() + " are in directory: " + documentsPath);

            try {
              if (documentLibrary == null) {
                throw new AlfrescoRuntimeException("Document library does not exist for site " + place.getShortName());
              }
              transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {

                  bulkImport(documentsPath, documentLibrary);
                  return null;
                }
              }, true, true);

            } catch (Exception e) {
              logger.error("Exception occured when importing documents into site. Deleting site");
              // DO NOT ADD ASPECT TEMPORARY TO SITES, IT WILL LEAVE STUFF
              // BEHIND SUCH AS SITE GROUPS

              shareConnector.deleteShareSite(place, cookies);

              throw new AlfrescoRuntimeException("Failed to bulk import documents into site " + siteInfo.getShortName(), e);
            }
          }
        }
        return null;
      }

    });

  }

  /**
   * Go on with bulk importing data
   * 
   * @param documentsPath
   *          Path on disk where the documents are located
   * @param documentLibrary
   *          A node ref to the target where the documents should be imported
   * @throws NotSupportedException
   * @throws SystemException
   * @throws SecurityException
   * @throws IllegalStateException
   * @throws RollbackException
   * @throws HeuristicMixedException
   * @throws HeuristicRollbackException
   */
  protected void bulkImport(String documentsPath, NodeRef documentLibrary) throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException,
      HeuristicMixedException, HeuristicRollbackException {
    logger.info("Starting import for " + documentsPath + " into " + documentLibrary);
    NodeImporter nodeImporter = streamingNodeImporterFactory.getNodeImporter(new File(documentsPath));
    BulkImportParameters bulkImportParameters = new BulkImportParameters();
    bulkImportParameters.setTarget(documentLibrary);
    bulkImportParameters.setReplaceExisting(true);
    bulkImportParameters.setBatchSize(40);
    bulkImportParameters.setNumThreads(4);
    bulkFilesystemImporter.bulkImport(bulkImportParameters, nodeImporter);

  }

  /**
   * Add members to site
   * 
   * @param place
   *          The site
   * @param siteInfo
   *          The site info
   * @throws URISyntaxException
   */
  protected void addMembers(Site place, SiteInfo siteInfo) throws URISyntaxException {
    List<String> users = place.getSiteManagers();
    addMembersWithRole(siteInfo.getShortName(), "SiteManager", users);
    users = place.getSiteCollaborators();
    addMembersWithRole(siteInfo.getShortName(), "SiteCollaborator", users);
    users = place.getSiteContributers();
    addMembersWithRole(siteInfo.getShortName(), "SiteContributor", users);
    users = place.getSiteConsumers();
    addMembersWithRole(siteInfo.getShortName(), "SiteConsumer", users);
  }

  /**
   * Add members with a specific role
   * 
   * @param siteShortName
   *          The site short name
   * @param role
   *          The site role
   * @param users
   *          A list of users
   */
  protected void addMembersWithRole(String siteShortName, String role, List<String> users) {
    for (String user : users) {
      if (user.length() > 0) {
        NodeRef personOrNull = personService.getPersonOrNull(user);
        if (personOrNull == null) {
          logger.warn("User " + user + " could not not be added as role " + role + " on site " + siteShortName + ". The user does not exist.");
        } else {
          logger.info("Adding user " + user + " as role " + role + " on site " + siteShortName);
          siteService.setMembership(siteShortName, user, role);
        }
      }
    }
  }

  /**
   * Cleans the file name according to the Alfresco cm:name requirement. This
   * regex can be found in Alfresco contentModel.xml in the cm:filename
   * constraint.
   * 
   * @param name
   * @return
   */

  protected String cleanNodeName(String str) {
    char[] replace = { '\\', '/', ':', '*', '?', '"', '<', '>', '|' };
    for (char c : replace) {
      str = str.replace(c, ' ');
    }
    return str.trim();
  }

  public void setImportPath(String importPath) {
    this.importPath = importPath;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setBulkFilesystemImporter(BulkFilesystemImporter bulkFilesystemImporter) {
    this.bulkFilesystemImporter = bulkFilesystemImporter;
  }

  public void setTransactionHelper(RetryingTransactionHelper transactionHelper) {
    this.transactionHelper = transactionHelper;
  }

  public void setStreamingNodeImporterFactory(StreamingNodeImporterFactory streamingNodeImporterFactory) {
    this.streamingNodeImporterFactory = streamingNodeImporterFactory;
  }

  public void setShareConnector(ShareConnector shareConnector) {
    this.shareConnector = shareConnector;
  }

  public void setAuthenticationUtil(AuthenticationUtilFacade authenticationUtil) {
    this.authenticationUtil = authenticationUtil;
  }

  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public void setSkipEmptyStrings(boolean skipEmptyStrings) {
    this.skipEmptyStrings = skipEmptyStrings;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

    Assert.notNull(importPath, "you must provide the import path bulkimport.bulkimportpath in alfresco-global.properties");
    logger.info("Using import path: " + importPath);
    logger.info("Skipping empty strings: " + skipEmptyStrings);
    Assert.notNull(siteService, "you must provide an instance of SiteService");
    Assert.notNull(nodeService, "you must provide an instance of NodeService");
    Assert.notNull(searchService, "you must provide an instance of SearchService");
    Assert.notNull(bulkFilesystemImporter, "you must provide an instance of BulkFilesystemImporter");
    Assert.notNull(streamingNodeImporterFactory, "you must provide an instance of StreamingNodeImporterFactory");
    Assert.notNull(transactionHelper, "you must provide an instance of RetryingTransactionHelper");
    Assert.notNull(shareConnector, "you must provide an instance of ShareConnector");
    Assert.notNull(personService, "you must provide an instance of PersonService");
    Assert.notNull(namespaceService, "you must provide an instance of NamespaceService");
  }
}
