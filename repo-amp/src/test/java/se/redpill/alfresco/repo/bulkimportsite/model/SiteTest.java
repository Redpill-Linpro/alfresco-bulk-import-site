package se.redpill.alfresco.repo.bulkimportsite.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

public class SiteTest {

  Mockery m;
  NamespaceService namespaceService;

  @Before
  public void setUp() {
    m = new JUnit4Mockery();
    namespaceService = m.mock(NamespaceService.class);
  }

  @Test
  public void testGetAlfrescoProperties() {
    m.checking(new Expectations() {
      {
        oneOf(namespaceService).getNamespaceURI("cm");
        will(returnValue("http://www.alfresco.org/model/content/1.0"));

      }
    });

    Properties p = new Properties();
    p.setProperty("type", "type");
    p.setProperty("cm:title", "testvalue");
    Site s = new Site();
    s.setProperties(p);
    Map<QName, Serializable> alfrescoProperties = s.getAlfrescoProperties(namespaceService, false);
    assertNotNull(alfrescoProperties);
    assertEquals(1, alfrescoProperties.size());
    Set<Entry<QName, Serializable>> entrySet = alfrescoProperties.entrySet();
    Iterator<Entry<QName, Serializable>> it = entrySet.iterator();
    Entry<QName, Serializable> next = it.next();
    QName qname = next.getKey();
    String value = (String) next.getValue();
    assertEquals(ContentModel.PROP_TITLE, qname);
    assertEquals("testvalue", value);
  }
}
