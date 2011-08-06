/**
 * Layered Markup and Annotation Language for Java (lmnl4j):
 * implementation of LMNL, a markup language supporting layered and/or
 * overlapping annotations.
 *
 * Copyright (C) 2010 the respective authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.interedition.text.mem.SimpleQName;
import eu.interedition.text.rdbms.RelationalQNameRepository;
import eu.interedition.text.util.SimpleXMLParserConfiguration;
import eu.interedition.text.xml.XMLParser;
import eu.interedition.text.xml.XMLParserConfiguration;
import eu.interedition.text.xml.XMLParserModule;
import eu.interedition.text.xml.module.AnnotationStorageXMLParserModule;
import eu.interedition.text.xml.module.TextXMLParserModule;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import javax.xml.transform.stream.StreamSource;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Base class for tests working with documents generated from XML test resources.
 *
 * @author <a href="http://gregor.middell.net/" title="Homepage of Gregor Middell">Gregor Middell</a>
 */
@Transactional
public abstract class AbstractXMLTest extends AbstractTest {
  protected static final URI TEI_NS = URI.create("http://www.tei-c.org/ns/1.0");

  /**
   * Names of available XML test resources.
   */
  protected static final SortedSet<String> RESOURCES = Sets.newTreeSet(Lists.newArrayList(//
          "ignt-0101.xml", "archimedes-palimpsest-tei.xml", "george-algabal-tei.xml", "homer-iliad-tei.xml"));


  private Map<String, Text> sources = Maps.newHashMap();
  private Map<String, Text> documents = Maps.newHashMap();

  @Autowired
  private TextRepository textRepository;

  @Autowired
  private XMLParser xmlParser;

  @Autowired
  private RelationalQNameRepository nameRepository;

  @Autowired
  private AnnotationRepository annotationRepository;

  @After
  public void removeDocuments() {
    for (Iterator<Text> documentIt = documents.values().iterator(); documentIt.hasNext(); ) {
      textRepository.delete(documentIt.next());
      documentIt.remove();
    }
  }

  @AfterTransaction
  public void clearNameCache() {
    nameRepository.clearCache();
  }

  /**
   * Returns a test document generated from the resource with the given name.
   * <p/>
   * <p/>
   * <p/>
   * The generated test document is cached for later reuse.
   *
   * @param resource the name of the resource
   * @return the corresponding test document
   * @see #RESOURCES
   */
  protected synchronized Text document(String resource) {
    load(resource);
    return documents.get(resource);
  }

  protected synchronized Text source(String resource) {
    load(resource);
    return sources.get(resource);
  }

  protected synchronized void load(String resource) {
    final StopWatch stopWatch = new StopWatch();
    try {
      if (RESOURCES.contains(resource) && !documents.containsKey(resource)) {
        final URI uri = AbstractXMLTest.class.getResource("/" + resource).toURI();

        stopWatch.start("Load " + uri);
        final Text xml = textRepository.create(new StreamSource(uri.toASCIIString()));
        stopWatch.stop();

        sources.put(resource, xml);
        stopWatch.start("Parse " + uri);
        documents.put(resource, xmlParser.parse(xml, createXMLParserConfiguration()));
        stopWatch.stop();

        if (LOG.isDebugEnabled()) {
          LOG.debug("\n" + stopWatch.prettyPrint());
        }
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected XMLParserConfiguration createXMLParserConfiguration() {
    SimpleXMLParserConfiguration pc = new SimpleXMLParserConfiguration();

    pc.addLineElement(new SimpleQName(TEI_NS, "lg"));
    pc.addLineElement(new SimpleQName(TEI_NS, "l"));
    pc.addLineElement(new SimpleQName(TEI_NS, "speaker"));
    pc.addLineElement(new SimpleQName(TEI_NS, "stage"));
    pc.addLineElement(new SimpleQName(TEI_NS, "head"));
    pc.addLineElement(new SimpleQName(TEI_NS, "p"));

    pc.addContainerElement(new SimpleQName(TEI_NS, "text"));
    pc.addContainerElement(new SimpleQName(TEI_NS, "div"));
    pc.addContainerElement(new SimpleQName(TEI_NS, "lg"));
    pc.addContainerElement(new SimpleQName(TEI_NS, "subst"));
    pc.addContainerElement(new SimpleQName(TEI_NS, "choice"));

    pc.exclude(new SimpleQName(TEI_NS, "teiHeader"));
    pc.exclude(new SimpleQName(TEI_NS, "front"));
    pc.exclude(new SimpleQName(TEI_NS, "fw"));


    final List<XMLParserModule> parserModules = pc.getModules();
    parserModules.add(new TextXMLParserModule(textRepository));
    parserModules.add(new AnnotationStorageXMLParserModule(annotationRepository));
    parserModules.addAll(parserModules());

    return pc;
  }

  protected List<XMLParserModule> parserModules() {
    return Lists.<XMLParserModule>newArrayList(new AnnotationStorageXMLParserModule(annotationRepository));
  }

  /**
   * Returns a default test document.
   *
   * @return the document generated from the first available test resource
   */
  protected Text document() {
    return document(RESOURCES.first());
  }
}
