package se.redpill.alfresco.repo.bulkimportsite.connector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.client.ClientProtocolException;

import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public interface ShareConnector {

  /**
   * Establishes a share session
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws ClientProtocolException
   */
  Map<String, String> loginToShare() throws UnsupportedEncodingException, IOException, ClientProtocolException;

  /**
   * Creates a site
   * @param cookies
   * @param place
   * @return
   */
  void createSite(Map<String, String> cookies, Site place);

  /**
   * Creates the document library
   * @param cookies
   * @param shortName
   * @return
   */
  NodeRef createDocumentLibrary(Map<String, String> cookies, String shortName);

  /**
   * Deletes a share site
   * @param place
   * @param cookies
   */
  void deleteShareSite(Site place, Map<String, String> cookies);

}
