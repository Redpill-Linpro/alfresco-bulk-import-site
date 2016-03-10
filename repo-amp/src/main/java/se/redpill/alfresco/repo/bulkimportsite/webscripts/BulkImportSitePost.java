package se.redpill.alfresco.repo.bulkimportsite.webscripts;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

import se.redpill.alfresco.repo.bulkimportsite.BulkImportSiteService;
import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public class BulkImportSitePost extends DeclarativeWebScript implements InitializingBean {

  Logger logger = Logger.getLogger(BulkImportSitePost.class);
  BulkImportSiteService bulkImportSiteService;

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(bulkImportSiteService, "You must provide an instance of BulkImportSiteService");
  }

  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    Map<String, Object> result = new HashMap<String, Object>();
    Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
    String placeShortName = templateArgs.get("placeShortName");

    Site placeToMigrate = null;
    try {
      placeToMigrate = bulkImportSiteService.getSite(placeShortName);
    } catch (URISyntaxException e) {
      throw new WebScriptException(Status.STATUS_NOT_FOUND, "Failed to find site with shortName " + placeShortName, e);
    } catch (InvalidPropertiesFormatException e) {
      throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Unable to parse properties for site " + placeShortName, e);
    } catch (IOException e) {
      throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Unable to read properties file for site " + placeShortName, e);
    }

    try {
      bulkImportSiteService.importSite(placeToMigrate);
    } catch (Exception e) {
      throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Failed to import place namned " + placeShortName + " into Alfresco", e);
    }
    return result;
  }

  public void setBulkImportSiteService(BulkImportSiteService bulkImportSiteService) {
    this.bulkImportSiteService = bulkImportSiteService;
  }

}
