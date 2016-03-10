package se.redpill.alfresco.repo.bulkimportsite.connector;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public interface ShareConnector {

  Map<String, String> createSite(Site place);

  NodeRef createDocumentLibrary(Map<String, String> cookies, String shortName);

  void deleteShareSite(Site place, Map<String, String> cookies);

}
