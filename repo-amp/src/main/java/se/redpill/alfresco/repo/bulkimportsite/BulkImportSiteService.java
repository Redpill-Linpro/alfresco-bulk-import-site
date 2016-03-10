package se.redpill.alfresco.repo.bulkimportsite;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public interface BulkImportSiteService {
  /**
   * Fetch all sites
   * @return
   * @throws URISyntaxException
   * @throws InvalidPropertiesFormatException
   * @throws IOException
   */
  public List<Site> getAllSites() throws URISyntaxException, InvalidPropertiesFormatException, IOException;
  /**
   * Get a site
   * @param shortName The identifier of a site
   * @return
   * @throws URISyntaxException
   * @throws InvalidPropertiesFormatException
   * @throws IOException
   */
  public Site getSite(String shortName) throws URISyntaxException, InvalidPropertiesFormatException, IOException;
  /**
   * Import a site
   * @param site The site to import
   * @throws Exception
   */
  public void importSite(Site site) throws Exception;
}
