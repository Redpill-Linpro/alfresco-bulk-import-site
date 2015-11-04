package se.redpill.alfresco.repo.bulkimportsite.connector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.URLDecoder;
import org.springframework.extensions.webscripts.connector.HttpMethod;
import org.springframework.extensions.webscripts.connector.RemoteClient;
import org.springframework.extensions.webscripts.connector.Response;
import org.springframework.util.Assert;

import se.redpill.alfresco.repo.bulkimportsite.model.Site;

public class ShareConnector implements InitializingBean {
  private static Logger logger = Logger.getLogger(ShareConnector.class);

  AuthenticationService authenticationService;
  NamespaceService namespaceService;
  SiteService siteService;
  RetryingTransactionHelper transactionHelper;
  
  private String alfrescoUsername;
  private String alfrescoPassword;
  private String shareUrl;
  private String shareProtocol;
 
  public Map<String, String> createSite(Site site) {
    Map<String, String> cookies;
    try {
      cookies = loginToShare();
    } catch (Exception e) {
      throw new AlfrescoRuntimeException("Error connecting to share", e);
    }
    touchCreateSiteWebscript(cookies);
    String sessionId = cookies.get("JSESSIONID");
    if (sessionId == null) {
      throw new AlfrescoRuntimeException("JSESSIONID can not be empty");
    }
    logger.info("creating site: " + site.getShortName());

    createSiteByShareWebscriptPost(site, cookies);
    return cookies;
  }

  public NodeRef createDocumentLibrary(Map<String, String> cookies, final String shortName) {
    String ticket = authenticationService.getCurrentTicket();
    RemoteClient remoteClient = new RemoteClient();

    remoteClient.setEndpoint(shareProtocol);
    remoteClient.setRequestMethod(HttpMethod.GET);
    remoteClient.setRequestContentType("application/json");

    remoteClient.setTicket(ticket);

    cookies.put("JSESSIONID", cookies.get("JSESSIONID"));
    cookies.put("alfLogin", cookies.get("alfLogin"));
    cookies.put("alfUsername3", cookies.get("alfUsername3"));

    remoteClient.setCookies(cookies);

    Map<String, String> headers = new HashMap<String, String>(8);
    headers.put("Connection", "keep-alive");
    remoteClient.setRequestProperties(headers);

    String endpoint = shareUrl + "/service/components/documentlibrary/data/doclist/all/site/" + shortName + "/documentLibrary/?" + remoteClient.getTicketName() + "=" + ticket;
    logger.trace("Calling "+endpoint+" to create a document library");
    Response response = remoteClient.call(endpoint);
    if (response.getStatus().getCode() != 200) {
      throw new RuntimeException(response.toString());
    }
    
    return transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
      @Override
      public NodeRef execute() throws Throwable {
        return siteService.getContainer(shortName, SiteService.DOCUMENT_LIBRARY);
      }
    }, true, true);

  }

  public void createSiteByShareWebscriptPost(Site place, Map<String, String> cookies) {
    String ticket = authenticationService.getCurrentTicket();
    RemoteClient remoteClient = new RemoteClient();

    remoteClient.setEndpoint(shareProtocol);
    remoteClient.setRequestMethod(HttpMethod.POST);
    remoteClient.setRequestContentType("application/json");

    remoteClient.setTicket(ticket);

    /*cookies.put("JSESSIONID", cookies.get("JSESSIONID"));
    cookies.put("alfLogin", cookies.get("alfLogin"));
    cookies.put("alfUsername3", cookies.get("alfUsername3"));
    if (cookies.containsKey("Alfresco-CSRFToken")) {
      cookies.put("Alfresco-CSRFToken", cookies.get("Alfresco-CSRFToken"));
    }*/
    remoteClient.setCookies(cookies);

    Map<String, String> headers = new HashMap<String, String>(8);
    headers.put("X-Requested-With", "application/json");
    headers.put("Connection", "keep-alive");
    if (cookies.containsKey("Alfresco-CSRFToken")) {
      headers.put("Alfresco-CSRFToken", cookies.get("Alfresco-CSRFToken"));
    }

    remoteClient.setRequestProperties(headers);

    Map<String, String> properties = new HashMap<String, String>();
    properties.put("ticket", ticket);
    properties.put("shortName", place.getShortName());
    properties.put("sitePreset", place.getPreset());
    properties.put("title", place.getTitle());
    properties.put("description", place.getDescription());
    properties.put("visibility", place.getVisibility());
    properties.put("type", place.getType(namespaceService));

    JSONObject json = new JSONObject(properties);
    logger.trace("Calling "+shareUrl + "/service/modules/create-site to create a site");
    Response response = remoteClient.call(shareUrl + "/service/modules/create-site", json.toString());

    if (response.getStatus().getCode() != 200) {
      throw new RuntimeException(response.toString());
    }
  }

  public void deleteShareSite(Site place, Map<String, String> cookies) {
    String ticket = authenticationService.getCurrentTicket();
    RemoteClient remote = new RemoteClient();

    remote.setEndpoint(shareProtocol);
    remote.setRequestMethod(HttpMethod.POST);
    remote.setRequestContentType("application/json");
    remote.setTicket(ticket);
    //Map<String, String> cookies = new HashMap<String, String>();
    //cookies.put("JSESSIONID", sessionId);
    remote.setCookies(cookies);
    
    Map<String, String> headers = new HashMap<String, String>(8);
    headers.put("X-Requested-With", "application/json");
    headers.put("Connection", "keep-alive");
    if (cookies.containsKey("Alfresco-CSRFToken")) {
      headers.put("Alfresco-CSRFToken", cookies.get("Alfresco-CSRFToken"));
    }

    remote.setRequestProperties(headers);

    Map<String, String> properties = new HashMap<String, String>();
    properties.put("ticket", ticket);
    properties.put("shortName", place.getShortName());
    properties.put("sitePreset", place.getPreset());
    properties.put("title", place.getTitle());
    properties.put("description", place.getDescription());
    properties.put("visibility", place.getVisibility());
    properties.put("type", place.getType(namespaceService));

    JSONObject json = new JSONObject(properties);
    logger.trace("Calling "+shareUrl + "/service/modules/delete-site to create a site");
    Response response = remote.call(shareUrl + "/service/modules/delete-site", json.toString());

    if (response.getStatus().getCode() != 200) {
      throw new RuntimeException(response.toString());
    }
  }

  public Map<String, String> loginToShare() throws UnsupportedEncodingException, IOException, ClientProtocolException {
    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpPost doLogin = new HttpPost(shareUrl + "/page/dologin");
    Map<String, String> cookies = new HashMap<String, String>();
    List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
    nvps.add(new BasicNameValuePair("username", alfrescoUsername));
    nvps.add(new BasicNameValuePair("password", alfrescoPassword));
    UrlEncodedFormEntity loginEntity = new UrlEncodedFormEntity(nvps, "UTF-8");
    doLogin.setEntity(loginEntity);

    ResponseHandler<String> responseHandler = new BasicResponseHandler();

    try {
      logger.trace("Logging in to share");
      httpclient.execute(doLogin, responseHandler);
    } catch (HttpResponseException e) {
      logger.info("catched an exception upon login", e);
    }

    for (Cookie cookie : httpclient.getCookieStore().getCookies()) {
      if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
        cookies.put("JSESSIONID", cookie.getValue());
      }
      if ("alfLogin".equalsIgnoreCase(cookie.getName())) {
        cookies.put("alfLogin", cookie.getValue());
      }

      if ("alfUsername3".equalsIgnoreCase(cookie.getName())) {
        cookies.put("alfUsername3", cookie.getValue());
      }

      if ("Alfresco-CSRFToken".equalsIgnoreCase(cookie.getName())) {
        String decodedToken = URLDecoder.decode(cookie.getValue());
        cookies.put("Alfresco-CSRFToken", decodedToken);
      }
    }

    return cookies;
  }

  public void touchCreateSiteWebscript(Map<String, String> cookies) {
    String ticket = authenticationService.getCurrentTicket();
    RemoteClient remoteClient = new RemoteClient();

    remoteClient.setEndpoint(shareProtocol);
    remoteClient.setRequestMethod(HttpMethod.GET);
    remoteClient.setRequestContentType("application/json");

    remoteClient.setTicket(ticket);

    cookies.put("JSESSIONID", cookies.get("JSESSIONID"));
    cookies.put("alfLogin", cookies.get("alfLogin"));
    cookies.put("alfUsername3", cookies.get("alfUsername3"));

    remoteClient.setCookies(cookies);

    Map<String, String> headers = new HashMap<String, String>(8);
    headers.put("Connection", "keep-alive");
    remoteClient.setRequestProperties(headers);

    try {
      logger.trace("Touching "+shareUrl + "/service/modules/create-site?htmlid=xxx");
      Response response = remoteClient.call(shareUrl + "/service/modules/create-site?htmlid=xxx");
      if (response.getStatus().getCode() != 200) {
        throw new RuntimeException(response.toString());
      }

    } catch (Exception e) {
      logger.info("expected behaviour");
    }

    // After touching we need to get the CSRF-token cookie as well
    Map<String, String> cookies2 = remoteClient.getCookies();
    if (cookies2.containsKey("Alfresco-CSRFToken")) {
      String decodedToken = URLDecoder.decode(cookies2.get("Alfresco-CSRFToken"));
      cookies.put("Alfresco-CSRFToken", decodedToken);
    }

  }

  public void setAlfrescoPassword(String alfrescoPassword) {
    this.alfrescoPassword = alfrescoPassword;
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

  public void setShareUrl(String shareUrl) {
    this.shareUrl = shareUrl;
  }

  public void setShareProtocol(String shareProtocol) {
    this.shareProtocol = shareProtocol;
  }
  
  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }
  
  public void setTransactionHelper(RetryingTransactionHelper transactionHelper) {
    this.transactionHelper = transactionHelper;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(shareUrl, "you must provide the share url in bulkimport.shareUrl in alfresco-global.properties");
    Assert.notNull(shareProtocol, "you must provide the share protocol in bulkimport.shareProtocol in alfresco-global.properties");
    Assert.notNull(alfrescoUsername, "you must provide a bulkimport.alfresco.username in alfresco-global.properties");
    Assert.notNull(alfrescoPassword, "you must provide a bulkimport.alfresco.password in alfresco-global.properties");
    Assert.notNull(siteService, "you must provide an instance of SiteService");
    Assert.notNull(authenticationService, "you must provide an instance of AuthenticationService");
    Assert.notNull(namespaceService, "you must provide an instance of NamespaceService");
    Assert.notNull(transactionHelper, "you must provide an instance of RetryingTransactionHelper");
  }
}
