package se.redpill.alfresco.repo.bulkimportsite.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.extension.bulkfilesystemimport.BulkImportStatus;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

import se.redpill.alfresco.repo.bulkimportsite.BulkImportSiteService;

public class BulkImportSiteStatusGet extends DeclarativeWebScript implements InitializingBean {
  Logger logger = Logger.getLogger(BulkImportSiteStatusGet.class);
  BulkImportSiteService bulkImportSiteService;

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(bulkImportSiteService, "You must provide an instance of BulkImportSiteService");
  }

  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("inProgress", true); // Default to in progress to avoid running
                                    // on failure
    
    BulkImportStatus status2 = bulkImportSiteService.getStatus();
    boolean inProgress = status2.inProgress();
    result.put("inProgress", inProgress);
    result.put("status", status2);
    return result;
  }

  public void setBulkImportSiteService(BulkImportSiteService bulkImportSiteService) {
    this.bulkImportSiteService = bulkImportSiteService;
  }

}
