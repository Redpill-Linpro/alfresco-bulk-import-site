package se.redpill.alfresco.repo.bulkimportsite.webscripts;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

import se.redpill.alfresco.repo.bulkimportsite.BulkImportSiteService;
import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public class BulkImportSiteGet extends DeclarativeWebScript implements InitializingBean {
  Logger logger = Logger.getLogger(BulkImportSiteGet.class);
  BulkImportSiteService bulkImportSiteService;

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(bulkImportSiteService, "You must provide an instance of BulkImportSiteService");
  }

  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    Map<String, Object> result = new HashMap<String, Object>();

    try {
      List<Site> places = bulkImportSiteService.getAllSites();
      result.put("places", places);
    } catch (URISyntaxException | IOException e) {
      logger.error("Failed to get all sites to import", e);
    }
    return result;
  }

  public void setBulkImportSiteService(BulkImportSiteService bulkImportSiteService) {
    this.bulkImportSiteService = bulkImportSiteService;
  }

}
