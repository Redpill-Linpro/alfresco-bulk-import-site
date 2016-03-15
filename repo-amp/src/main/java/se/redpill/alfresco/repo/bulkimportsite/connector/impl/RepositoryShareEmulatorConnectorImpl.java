package se.redpill.alfresco.repo.bulkimportsite.connector.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import se.redpill.alfresco.repo.bulkimportsite.connector.ShareConnector;
import se.redpill.alfresco.repo.bulkimportsite.model.Site;

/**
 * This is currently used for testing. Will not work with a live import since it
 * does not generate dashboard.xmls and stuff
 *
 * @author Marcus Svartmark <marcus.svartmark@redpill-linpro.com>
 *
 */
public class RepositoryShareEmulatorConnectorImpl implements ShareConnector, InitializingBean {

  private static Logger logger = Logger.getLogger(RepositoryShareEmulatorConnectorImpl.class);

  protected AuthenticationService authenticationService;
  protected NamespaceService namespaceService;
  protected SiteService siteService;
  protected RetryingTransactionHelper transactionHelper;
  protected NodeService nodeService;
  protected String alfrescoUsername;

  @Override
  public void createSite(Map<String, String> cookies, Site siteData) {
    final String sitePreset = siteData.getPreset();
    final String description = siteData.getDescription();
    final String shortName = siteData.getShortName();
    final String visibility = siteData.getVisibility();
    final String title = siteData.getTitle();
    final QName siteType = siteData.getTypeQName(namespaceService);

    transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<SiteInfo>() {
      @Override
      public SiteInfo execute() throws Throwable {
        return siteService.createSite(sitePreset, shortName, title, description, SiteVisibility.valueOf(visibility), siteType);
      }
    }, false, true);

  }

  @Override
  public NodeRef createDocumentLibrary(Map<String, String> cookies, final String shortName) {
    NodeRef docLibNodeRef = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
      @Override
      public NodeRef execute() throws Throwable {
        NodeRef container = siteService.getContainer(shortName, SiteService.DOCUMENT_LIBRARY);
        if (container == null || nodeService.exists(container)) {
          container = siteService.createContainer(shortName, SiteService.DOCUMENT_LIBRARY, ContentModel.TYPE_FOLDER, null);
        } else {
          container = null;
        }
        return container;
      }
    }, false, true);
    return docLibNodeRef;
  }

  @Override
  public void deleteShareSite(final Site place, Map<String, String> cookies) {
    transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
      @Override
      public Void execute() throws Throwable {
        siteService.deleteSite(place.getShortName());
        return null;
      }
    }, false, true);
  }

  public void setAlfrescoUsername(String alfrescoUsername) {
    this.alfrescoUsername = alfrescoUsername;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void setAuthenticationService(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public void setTransactionHelper(RetryingTransactionHelper transactionHelper) {
    this.transactionHelper = transactionHelper;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(alfrescoUsername, "you must provide a bulkimport.alfresco.username in alfresco-global.properties");
    Assert.notNull(siteService, "you must provide an instance of SiteService");
    Assert.notNull(authenticationService, "you must provide an instance of AuthenticationService");
    Assert.notNull(namespaceService, "you must provide an instance of NamespaceService");
    Assert.notNull(transactionHelper, "you must provide an instance of RetryingTransactionHelper");
    Assert.notNull(nodeService, "you must provide an instance of NodeService");
  }

  @Override
  public Map<String, String> loginToShare() throws UnsupportedEncodingException, IOException, ClientProtocolException {
    return new HashMap<String, String>();
  }

}
