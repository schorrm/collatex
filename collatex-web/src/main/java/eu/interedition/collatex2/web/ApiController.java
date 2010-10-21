/**
 * CollateX - a Java library for collating textual sources,
 * for example, to produce an apparatus.
 *
 * Copyright (C) 2010 ESF COST Action "Interedition".
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

package eu.interedition.collatex2.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jackson.JsonParseException;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.interedition.collatex2.implementation.CollateXEngine;
import eu.interedition.collatex2.implementation.input.Phrase;
import eu.interedition.collatex2.implementation.input.tokenization.DefaultTokenNormalizer;
import eu.interedition.collatex2.implementation.input.tokenization.WhitespaceTokenizer;
import eu.interedition.collatex2.implementation.output.apparatus.ApparatusEntry;
import eu.interedition.collatex2.implementation.output.apparatus.ParallelSegmentationApparatus;
import eu.interedition.collatex2.implementation.output.apparatus.TeiParallelSegmentationApparatusBuilder;
import eu.interedition.collatex2.implementation.output.jgraph.JVariantGraphCreator;
import eu.interedition.collatex2.interfaces.IAlignmentTable;
import eu.interedition.collatex2.interfaces.IJVariantGraph;
import eu.interedition.collatex2.interfaces.IJVariantGraphEdge;
import eu.interedition.collatex2.interfaces.IJVariantGraphVertex;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.ITokenNormalizer;
import eu.interedition.collatex2.interfaces.ITokenizer;
import eu.interedition.collatex2.interfaces.IVariantGraph;
import eu.interedition.collatex2.interfaces.IVariantGraphEdge;
import eu.interedition.collatex2.interfaces.IVariantGraphVertex;
import eu.interedition.collatex2.interfaces.IWitness;
import eu.interedition.collatex2.web.io.ApiObjectMapper;

@Controller
@RequestMapping("/api/**")
public class ApiController implements InitializingBean {
  private static final String SVG_SERVER = "http://localhost:1080/svg";
  protected static final String COLLATEX_NS = "http://interedition.eu/collatex/ns/1.0";
  protected static final String TEI_NS = "http://www.tei-c.org/ns/1.0";

  private final ITokenizer defaultTokenizer = new WhitespaceTokenizer();
  private final ITokenNormalizer defaultNormalizer = new DefaultTokenNormalizer();

  @Autowired
  private ApiObjectMapper objectMapper;

  @Override
  public void afterPropertiesSet() throws Exception {
    jsonView = new MappingJacksonJsonView();
    jsonView.setObjectMapper(objectMapper);
  }

  @RequestMapping(value = "collate", headers = { "Content-Type=application/json", "Accept=application/json" }, method = RequestMethod.POST)
  public ModelAndView collateToJson(@RequestBody final ApiInput input) throws Exception {
    return new ModelAndView(jsonView, "alignment", collate(input));
  }

  @RequestMapping(value = "collate", headers = { "Content-Type=application/json", "Accept=application/xml" }, method = RequestMethod.POST)
  public ModelAndView collateToTei(@RequestBody final ApiInput input) throws Exception {
    return new ModelAndView(teiView, "alignment", collate(input));
  }

  @RequestMapping(value = "collate", headers = { "Content-Type=application/json", "Accept=image/svg+xml" }, method = RequestMethod.POST)
  public ModelAndView collateToSvg(@RequestBody final ApiInput input) throws Exception {
    String svg = convert2svg(jcollate2dot(input));
    ModelAndView modelAndView = new ModelAndView(svgView, "svg", svg);
    return modelAndView;
  }

  private String convert2svg(String dot) throws IOException, HttpException {
    HttpClient client = new HttpClient();
    PostMethod postMethod = new PostMethod(SVG_SERVER);
    postMethod.setParameter("dot", dot);
    client.executeMethod(postMethod);
    String svg = postMethod.getResponseBodyAsString();
    return svg;
  }

  //  @RequestMapping(value = "collate", headers = { "Content-Type=application/json" }, method = RequestMethod.POST)
  //  public ModelAndView collateToHtml(@RequestBody final ApiInput input) throws Exception {
  //    return new ModelAndView("api/alignment", "alignment", collate(input));
  //  }

  @RequestMapping(value = "collate", headers = { "Content-Type=application/json" }, method = RequestMethod.POST)
  public ModelAndView collateToHtmlP(@RequestBody final ApiInput input) throws Exception {
    List<Map<String, Object>> rows = parallelSegmentationRows(input);
    return new ModelAndView("api/apparatus", "rows", rows);
  }

  static final Phrase EMPTY_PHRASE = new Phrase(Lists.<INormalizedToken> newArrayList());

  private List<Map<String, Object>> parallelSegmentationRows(final ApiInput input) throws ApiException {
    IAlignmentTable alignmentTable = collate(input);
    ParallelSegmentationApparatus apparatus = new CollateXEngine().createApparatus(alignmentTable);

    List<ApparatusEntry> entries = apparatus.getEntries();
    List<Map<String, Object>> rows = Lists.newArrayList();
    for (String sigil : alignmentTable.getSigla()) {
      List<String> phrases = Lists.newArrayList();
      for (ApparatusEntry apparatusEntry : entries) {
        String phrase = apparatusEntry.containsWitness(sigil) ? apparatusEntry.getPhrase(sigil).getContent() : "";
        phrases.add(phrase);
      }
      Map<String, Object> row = rowMap(sigil, phrases);
      rows.add(row);
    }
    return rows;
  }

  private Map<String, Object> rowMap(String sigil, Collection<String> phrases) {
    Map<String, Object> row = Maps.newHashMap();
    row.put("sigil", sigil);
    row.put("cells", phrases);
    return row;
  }

  @RequestMapping(value = "collate")
  public void documentation() {}

  private IAlignmentTable collate(ApiInput input) throws ApiException {
    final List<ApiWitness> witnesses = checkInputAndExtractWitnesses(input);
    return new CollateXEngine().align(witnesses.toArray(new ApiWitness[witnesses.size()]));
  }

  private String collate2dot(ApiInput input) throws ApiException {
    final List<ApiWitness> witnesses = checkInputAndExtractWitnesses(input);
    ApiWitness[] array = witnesses.toArray(new ApiWitness[witnesses.size()]);
    IVariantGraph graph = new CollateXEngine().graph(array);
    VertexNameProvider<IVariantGraphVertex> vertexIDProvider = new IntegerNameProvider<IVariantGraphVertex>();
    VertexNameProvider<IVariantGraphVertex> vertexLabelProvider = new VertexNameProvider<IVariantGraphVertex>() {
      @Override
      public String getVertexName(IVariantGraphVertex v) {
        return v.getNormalized();
      }
    };
    EdgeNameProvider<IVariantGraphEdge> edgeLabelProvider = new EdgeNameProvider<IVariantGraphEdge>() {
      @Override
      public String getEdgeName(IVariantGraphEdge e) {
        List<String> sigils = Lists.newArrayList();
        for (IWitness witness : e.getWitnesses()) {
          sigils.add(witness.getSigil());
        }
        Collections.sort(sigils);
        return Joiner.on(",").join(sigils);
      }
    };
    DOTExporter<IVariantGraphVertex, IVariantGraphEdge> exporter = new DOTExporter<IVariantGraphVertex, IVariantGraphEdge>(vertexIDProvider, vertexLabelProvider, edgeLabelProvider);
    Writer writer = new StringWriter();
    exporter.export(writer, graph);
    return writer.toString();
  }

  static final VertexNameProvider<IJVariantGraphVertex> VERTEX_ID_PROVIDER = new IntegerNameProvider<IJVariantGraphVertex>();
  static final VertexNameProvider<IJVariantGraphVertex> VERTEX_LABEL_PROVIDER = new VertexNameProvider<IJVariantGraphVertex>() {
    @Override
    public String getVertexName(IJVariantGraphVertex v) {
      return v.getNormalized();
    }
  };
  static final EdgeNameProvider<IJVariantGraphEdge> EDGE_LABEL_PROVIDER = new EdgeNameProvider<IJVariantGraphEdge>() {
    @Override
    public String getEdgeName(IJVariantGraphEdge e) {
      List<String> sigils = Lists.newArrayList();
      for (IWitness witness : e.getWitnesses()) {
        sigils.add(witness.getSigil());
      }
      Collections.sort(sigils);
      return Joiner.on(",").join(sigils);
    }
  };
  static final DOTExporter<IJVariantGraphVertex, IJVariantGraphEdge> DOT_EXPORTER = new DOTExporter<IJVariantGraphVertex, IJVariantGraphEdge>(//
      VERTEX_ID_PROVIDER, VERTEX_LABEL_PROVIDER, EDGE_LABEL_PROVIDER //
  );

  private String jcollate2dot(ApiInput input) throws ApiException {
    final List<ApiWitness> witnesses = checkInputAndExtractWitnesses(input);
    ApiWitness[] array = witnesses.toArray(new ApiWitness[witnesses.size()]);
    IVariantGraph graph = new CollateXEngine().graph(array);

    IJVariantGraph jgraph = JVariantGraphCreator.parallelSegmentate(graph);
    Writer writer = new StringWriter();
    DOT_EXPORTER.export(writer, jgraph);
    return writer.toString();
  }

  private List<ApiWitness> checkInputAndExtractWitnesses(ApiInput input) throws ApiException {
    Set<String> sigle = new HashSet<String>();
    for (ApiWitness witness : input.getWitnesses()) {
      String sigil = witness.getSigil();
      if (sigil == null) {
        throw new ApiException("Witness without id/sigil given");
      }
      if (sigle.contains(sigil)) {
        throw new ApiException("Duplicate id/sigil: " + sigil);
      }
      sigle.add(sigil);

      if ((witness.getTokens() == null) && (witness.getContent() != null)) {
        Iterable<INormalizedToken> tokens = Iterables.transform(defaultTokenizer.tokenize(sigil, witness.getContent()), defaultNormalizer);
        witness.setTokens(Lists.newArrayList(Iterables.transform(tokens, TO_API_TOKEN)));
      }

      int tokenPosition = 0;
      for (ApiToken token : witness.getApiTokens()) {
        token.setSigil(sigil);
        token.setPosition(++tokenPosition);
        if (token.getNormalized() == null || token.getNormalized().trim().length() == 0) {
          token.setNormalized(defaultNormalizer.apply(token).getNormalized());
        }
      }
    }
    final List<ApiWitness> witnesses = input.getWitnesses();
    return witnesses;
  }

  @ExceptionHandler( { ApiException.class, JsonParseException.class })
  public ModelAndView apiError(HttpServletResponse response, Exception exception) {
    return new ModelAndView(new MappingJacksonJsonView(), new ModelMap("error", exception.getMessage()));
  }

  private static final Function<INormalizedToken, ? extends INormalizedToken> TO_API_TOKEN = new Function<INormalizedToken, ApiToken>() {

    @Override
    public ApiToken apply(INormalizedToken from) {
      return new ApiToken(from);
    }
  };

  private MappingJacksonJsonView jsonView;

  private final AbstractView teiView = new AbstractView() {

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
      IAlignmentTable alignmentTable = (IAlignmentTable) model.get("alignment");
      Assert.notNull(alignmentTable);

      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = xml.createElementNS(COLLATEX_NS, "collatex:apparatus");
      xml.appendChild(root);
      root.setAttribute("xmlns", TEI_NS);

      TeiParallelSegmentationApparatusBuilder.build(new CollateXEngine().createApparatus(alignmentTable), root);

      response.setContentType("application/xml");
      response.setCharacterEncoding("UTF-8");
      PrintWriter out = response.getWriter();

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      TransformerUtils.enableIndenting(transformer, 4);
      transformer.transform(new DOMSource(xml), new StreamResult(out));
      out.flush();
    }
  };
  private final AbstractView svgView = new AbstractView() {

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
      String svg = (String) model.get("svg");
      Assert.notNull(svg);
      response.setCharacterEncoding("UTF-8");
      PrintWriter out = response.getWriter();
      out.write(svg);
      out.flush();
    }
  };
}
