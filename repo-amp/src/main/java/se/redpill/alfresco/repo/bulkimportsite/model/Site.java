package se.redpill.alfresco.repo.bulkimportsite.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

public class Site implements Serializable {

  private static final long serialVersionUID = 5279648153572952042L;

  private String shortName;
  private String diskPath;

  private Properties properties;
  private boolean imported = false;

  private Set<String> propertyBlackList = new HashSet<String>() {
    private static final long serialVersionUID = 2618813653270997348L;

    {
      add("type");
      add("title");
      add("visibility");
      add("site-preset");
      add("description");
      add("membership-site-manager");
      add("membership-site-collaborator");
      add("membership-site-contributer");
      add("membership-site-contributor");
      add("membership-site-consumer");

    }
  };

  private String cleanShortName(String shortName) {
    shortName = shortName.toLowerCase();

    shortName = shortName.replace('–', '-');
    shortName = shortName.replace('_', ' ');

    shortName = shortName.replace(',', ' ');
    shortName = shortName.replace('’', ' ');
    shortName = shortName.replace('(', ' ');
    shortName = shortName.replace(')', ' ');
    shortName = shortName.replace('/', ' ');
    shortName = shortName.replace('\\', ' ');
    shortName = shortName.replace(':', ' ');

    shortName = shortName.replace('é', 'e');
    shortName = shortName.replace('è', 'e');
    shortName = shortName.replace('ê', 'e');
    shortName = shortName.replace('á', 'a');
    shortName = shortName.replace('à', 'a');
    shortName = shortName.replace('â', 'a');

    shortName = shortName.replaceAll("ä", "ae");
    shortName = shortName.replaceAll("ö", "oe");
    shortName = shortName.replaceAll("ü", "ue");

    shortName = shortName.replace('å', 'a');

    shortName = shortName.replaceAll("'", "");
    shortName = shortName.replaceAll("&", "and");
    shortName = shortName.replace(' ', '-');
    shortName = shortName.replaceAll("--", "-");
    shortName = shortName.replaceAll("--", "-");

    int maxLength = 72;
    maxLength = 50;
    if (shortName.length() > maxLength) {
      shortName = shortName.substring(0, maxLength);
      shortName = shortName.substring(0, shortName.lastIndexOf("-"));
    }

    if (shortName.endsWith("-")) {
      shortName = shortName.substring(0, shortName.length() - 1);
    }

    return shortName;
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public void setDiskPath(String diskPath) {
    this.diskPath = diskPath;
  }

  public String getDiskPath() {
    return diskPath;
  }

  public void setImported(boolean imported) {
    this.imported = imported;
  }

  public boolean getImported() {
    return imported;
  }

  /**
   * Returns the visibility for a site. Defaults to public unless the
   * "visibility" metadata is set
   * 
   * @return
   */
  public String getVisibility() {
    if (properties == null || properties.getProperty("visibility") == null) {
      return SiteVisibility.PUBLIC.toString();
    } else {
      return (String) properties.getProperty("visibility");
    }
  }

  /**
   * Gets the site preset for a site. Defaults to "site-dashboard" unless the
   * "site-preset" metadata is set
   * 
   * @return
   */
  public String getPreset() {
    if (properties == null || properties.getProperty("site-preset") == null) {
      return "site-dashboard";
    } else {
      return (String) properties.getProperty("site-preset");
    }
  }

  /**
   * Gets the description for a site. Defaults to blank unless the "description"
   * metadata is set
   * 
   * @return
   */
  public String getDescription() {
    if (properties == null || properties.getProperty("description") == null) {
      return "";
    } else {
      return (String) properties.getProperty("description");
    }
  }

  /**
   * Gets the title for a site. Defaults to "shortName" unless the "title"
   * metadata is set
   * 
   * @return
   */
  public String getTitle() {
    if (properties == null || properties.getProperty("title") == null) {
      return shortName;
    } else {
      return (String) properties.getProperty("title");
    }
  }

  /**
   * Gets the type for a site. Defaults to "st:site" unless the "type" metadata
   * is set
   * 
   * @return
   */
  public String getType() {
    if (properties == null || properties.getProperty("type") == null) {
      return "st:site";
    } else {
      return (String) properties.getProperty("type");
    }
  }

  /**
   * Gets the full type name for a site. Defaults to "st:site" unless the "type"
   * metadata is set
   * 
   * @return
   */
  public String getType(NamespaceService namespaceService) {
    return getTypeQName(namespaceService).toString();
  }

  /**
   * Gets the full type name for a site. Defaults to "st:site" unless the "type"
   * metadata is set
   * 
   * @return
   */
  public QName getTypeQName(NamespaceService namespaceService) {
    QName qname = QName.resolveToQName(namespaceService, getType());
    return qname;
  }

  /**
   * Fetch all alfresco properties except those in the whitelist
   * 
   * @param namespaceService
   * @return
   */
  public Map<QName, Serializable> getAlfrescoProperties(NamespaceService namespaceService, boolean skipEmptyStrings) {
    Map<QName, Serializable> resultingProperties = new HashMap<QName, Serializable>();
    if (properties != null) {
      Set<String> stringPropertyNames = properties.stringPropertyNames();
      Iterator<String> it = stringPropertyNames.iterator();

      while (it.hasNext()) {
        String key = it.next();
        Serializable value = properties.getProperty(key);
        boolean isString = (value instanceof String);
        if (propertyBlackList.contains(key)) {
          continue;
        } else if (skipEmptyStrings && (value == null || (isString && ((String) value).length() == 0))) {
          continue;
        } else {
          QName qname = QName.resolveToQName(namespaceService, key);
          resultingProperties.put(qname, properties.getProperty(key));
        }
      }
    }
    return resultingProperties;
  }

  /**
   * Get a list of site managers
   * 
   * @return
   */
  public List<String> getSiteManagers() {
    return getUsers("membership-site-manager");
  }

  /**
   * Get a list of site collaborators
   * 
   * @return
   */
  public List<String> getSiteCollaborators() {
    return getUsers("membership-site-collaborator");
  }

  /**
   * Get a list of site contributors
   * 
   * @return
   */
  public List<String> getSiteContributors() {
    // Check for both contributors and contributers due to early spelling
    // mistake during a migration project
    List<String> users = getUsers("membership-site-contributer");
    users.addAll(getUsers("membership-site-contributor"));
    return users;
  }

  /**
   * Get a list of site consumers
   * 
   * @return
   */
  public List<String> getSiteConsumers() {
    return getUsers("membership-site-consumer");
  }

  protected List<String> getUsers(String role) {
    List<String> result = new ArrayList<String>();
    if (properties != null) {
      String property = properties.getProperty(role);
      if (property != null) {
        String[] splits = property.split(",");
        for (String split : splits) {
          String username = split.trim().toLowerCase();
          if (username.length() > 0) {
            result.add(username);
          }
        }
      }
    }
    return result;
  }
}
