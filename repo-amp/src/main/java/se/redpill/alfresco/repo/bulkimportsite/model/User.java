package se.redpill.alfresco.repo.bulkimportsite.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

public class User implements Serializable {

  private static final long serialVersionUID = 6378197356992044909L;

  private String userId;
  private String dn;
  private String email;
  private String quickrRole;
  
  private static final Map<String, String> roleMap = new HashMap<String, String>();
  
  static {
    roleMap.put("Reader", "SiteConsumer");
    roleMap.put("Author", "SiteContributor");
    roleMap.put("Contributor", "SiteContributor");
    roleMap.put("Editor", "SiteCollaborator");
    roleMap.put("Manager", "SiteManager");
    roleMap.put("Owner", "SiteManager");
  }
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDn() {
    return dn;
  }

  public void setDn(String dn) {
    this.dn = dn;
  }
  
  public void setEmail(String email) {
    this.email = email;
  } 
  
  public String getEmail() {
    return email;
  }
  
  public void setQuickrRole(String quickrRole) {
    this.quickrRole = quickrRole;
  }
  
  public String getQuickrRole() {
    return quickrRole;
  }
  
  public String getAlfrescoRole(){
    return roleMap.get(quickrRole);
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
