package org.basex.test;

//import static javax.xml.stream.XMLStreamConstants.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;
import javax.xml.xquery.XQSequence;
import javax.xml.xquery.XQStaticContext;
import junit.framework.TestCase;
import org.basex.io.CachedOutput;
import org.basex.test.xqj.TestContentHandler;
import org.basex.test.xqj.TestXMLFilter;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * This class tests the XQJ features.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-07, ISC License
 * @author Christian Gruen
 */
public class XQJTest extends TestCase {
  /** Test file. */
  private final String input = "doc('/home/db/xml/input.xml')";
  /** Driver reference. */
  protected String drv;

  @Before
  @Override
  protected void setUp() {
    drv = "org.basex.api.xqj.BXQDataSource";
  }

  /**
   * Creates and returns a connection to the specified driver.
   * @param drv driver
   * @return connection
   * @throws Exception exception
   */
  private static XQConnection conn(final String drv) throws Exception {
    return ((XQDataSource) Class.forName(drv).newInstance()).getConnection();
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test1() throws Exception {
    final XQConnection conn = conn(drv);
    final XQPreparedExpression expr = conn.prepareExpression(input + "//li");

    // query execution
    final XQResultSequence result = expr.executeQuery();

    // output of result sequence
    final StringBuilder sb = new StringBuilder();
    while(result.next()) sb.append(result.getItemAsString(null));
    assertEquals("", "<li>Exercise 1</li><li>Exercise 2</li>", sb.toString());
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test2() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();
    final XQSequence seq = expr.executeQuery("'Hello World!'");
    final CachedOutput co = new CachedOutput();
    seq.writeSequence(co, new Properties());
    final String str = co.toString().replaceAll("<\\?.*?>", "");
    assertEquals("", "Hello World!", str);
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test3() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();
    expr.bindInt(new QName("x3"), 21, null);

    final XQSequence result = expr.executeQuery(
       "declare variable $x3 as xs:integer external; for $i in $x3 return $i");

    final StringBuilder sb = new StringBuilder();
    while(result.next()) sb.append(result.getItemAsString(null));
    assertEquals("", "21", sb.toString());
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test4() throws Exception {
    final XQConnection conn = conn(drv);

    final XQPreparedExpression expr = conn.prepareExpression(
        "declare variable $i as xs:integer external; $i");
    expr.bindInt(new QName("i"), 21, null);
    final XQSequence result = expr.executeQuery();

    final StringBuilder sb = new StringBuilder();
    while(result.next()) sb.append(result.getItemAsString(null));
    assertEquals("", "21", sb.toString());
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test5() throws Exception {
    final XQConnection conn = conn(drv);

    conn.createItemFromString("Hello",
        conn.createAtomicType(XQItemType.XQBASETYPE_NCNAME));
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test6() throws Exception {
    final XQConnection conn = conn(drv);

    try {
      conn.createItemFromInt(1000,
          conn.createAtomicType(XQItemType.XQBASETYPE_BYTE));
      fail("1000 cannot be converted to byte.");
    } catch(final Exception ex) {
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test7() throws Exception {
    final XQConnection conn = conn(drv);

    try {
      conn.createItemFromByte((byte) 123,
          conn.createAtomicType(XQItemType.XQBASETYPE_INTEGER));
    } catch(final Exception ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test8() throws Exception {
    final XQConnection conn = conn(drv);
    final XMLReader r = new TestXMLFilter("<e>ha</e>");

    try {
      conn.createItemFromDocument(r, null);
    } catch(final Exception ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test9() throws Exception {
    final XQConnection conn = conn(drv);
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder parser = factory.newDocumentBuilder();
    final Document doc = parser.parse(new InputSource(new StringReader("<a>b</a>")));

    try {
      conn.createItemFromNode(doc, null);
    } catch(final Exception ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test10() throws Exception {
    final XQConnection conn = conn(drv);
    final XQStaticContext sc = conn.getStaticContext();
    sc.setScrollability(XQConstants.SCROLLTYPE_SCROLLABLE);

    final XQExpression ex = conn.createExpression();
    final XQResultSequence seq = ex.executeQuery("1,2,3,4");
    seq.absolute(2);
    assertEquals(2, seq.getPosition());
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test11() throws Exception {
    final XQConnection conn = conn(drv);
    final XQStaticContext sc = conn.getStaticContext();
    sc.setScrollability(XQConstants.SCROLLTYPE_SCROLLABLE);

    final XQExpression ex = conn.createExpression();
    final String query = "doc('/home/db/projects/basex/input.xml')//title";
    //String query = "1,2";
    final XQResultSequence seq = ex.executeQuery(query);
    final XMLStreamReader xsr = seq.getSequenceAsStream();
    while(xsr.hasNext()) xsr.next();
  }

  /**
   * Test.
   * @throws Exception exception
   */
  @Test
  public void test12() throws Exception {
    final XQConnection conn = conn(drv);
    final XQStaticContext sc = conn.getStaticContext();
    sc.setScrollability(XQConstants.SCROLLTYPE_SCROLLABLE);

    final XQExpression ex = conn.createExpression();
    final String query = "1,'haha',2.0e3,2";
    //String query = "1";
    final XQResultSequence seq = ex.executeQuery(query);
    final XMLStreamReader xsr = seq.getSequenceAsStream();

    final XMLInputFactory xif = XMLInputFactory.newInstance();
    final XMLEventReader xer = xif.createXMLEventReader(xsr);

    while(xer.hasNext()) {
      final XMLEvent ev = xer.nextEvent();
      if(ev.isStartElement()) {
        final StartElement se = ev.asStartElement();
        se.getName();
      } else if(ev.isCharacters()) {
        //Characters ch = ev.asCharacters();
        //ch.getData());
      } else {
        //ev.getEventType());
      }
      //System.out.println(xer.getElementText());
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test13() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();

    final XQResultSequence seq = expr.executeQuery("<H><K/><K/></H>");
    seq.next();
    seq.getSequenceAsString(null);
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test14() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();

    final XQResultSequence seq = expr.executeQuery("1,'test'");
    final StringWriter sw = new StringWriter();
    seq.next();
    seq.writeItemToResult(new StreamResult(sw));
    seq.next();
    seq.writeItemToResult(new StreamResult(sw));

    assertEquals("1test", sw.toString());
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test15() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();

    final String query = "<H><K>B</K></H>";
    final XQResultSequence seq = expr.executeQuery(query);
    seq.next();
    final TestContentHandler result = new TestContentHandler();
    seq.writeItemToSAX(result);

    assertEquals(query, result.buffer.toString());
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test16() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();

    final String query = "<ee>Hello world!</ee>";
    final XQItem item = conn.createItemFromDocument(
        expr.executeQuery(query).getSequenceAsStream(), null);

    assertEquals(query, item.getItemAsString(null));
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test17() throws Exception {
    final XQConnection conn = conn(drv);
    final String query = "declare variable $x as xs:integer external; $x";
    final XQPreparedExpression expr = conn.prepareExpression(query);

    int nr = 0;
    final QName var = new QName("x");
    expr.bindInt(var, 45, null);
    XQResultSequence result = expr.executeQuery();
    while(result.next()) nr += result.getInt();
    result.close();

    expr.bindInt(var, 54, null);
    result = expr.executeQuery();
    while (result.next()) nr += result.getInt();
    result.close();

    assertEquals(99, nr);
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test18() throws Exception {
    final XQConnection conn = conn(drv);
    
    try {
      conn.prepareExpression("declare variable $var1 := $var1; true()");
      fail("Exception expected for invalid XQuery expression.");
    } catch (XQException e) {
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test19() throws Exception {
    final XQConnection conn = conn(drv);
    
    try {
      final XQPreparedExpression expr = conn.prepareExpression(
        "declare variable $v external; $v");

      expr.bindInt(new QName("v"), 123, null);
      XQResultSequence result = expr.executeQuery();
      result.next();
      assertEquals(123, result.getInt());
    } catch (XQException e) {
      fail(e.getMessage());
    }
  }

  /**
   * Test.
   * @throws Exception exception
   */
  public void test20() throws Exception {
    final XQConnection conn = conn(drv);
    final XQExpression expr = conn.createExpression();

    final XQResultSequence result = expr.executeQuery("'Hello world!'");
    result.isScrollable();
    expr.close();

    final XQSequence seq = conn.createSequence(result);
    seq.beforeFirst();
  }
}
