package eu.interedition.collatex.subst;

import eu.interedition.collatex.simple.SimplePatternTokenizer;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ronalddekker on 01/05/16.
 */
public class WitnessTree {

    // this class creates a witness tree from a XML serialization of a witness
    // returns the root node of the tree
    public static WitnessNode createTree(String witnessXML) {
        // use a stax parser to go from XML data to XML tokens
        // NOTE: there is no implementation of Stream API for STAX
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader xmlStreamReader;
        WitnessNode currentNode = new WitnessNode("fakeroot");
        Function<String, Stream<String>> textTokenizer = SimplePatternTokenizer.BY_WS_OR_PUNCT;
        try {
            xmlStreamReader = xmlInputFactory.createXMLEventReader(new StringReader(witnessXML));
            while (xmlStreamReader.hasNext()) {
                XMLEvent next = xmlStreamReader.nextEvent();
                if (next.isStartElement()) {
                    StartElement el = next.asStartElement();
                    String localName = el.getName().getLocalPart();
                    WitnessNode child = new WitnessNode(localName);
                    currentNode.addChild(child);
                    currentNode = child;
                } else if (next.isCharacters()) {
                    Characters ch = next.asCharacters();
                    String text = ch.getData();
                    Stream<String> stringStream = textTokenizer.apply(text);
                    List<WitnessNode> newTokens = stringStream.map(content -> new WitnessNode(content)).collect(Collectors.toList());
                    newTokens.forEach(currentNode::addChild);
                } else if (next.isEndElement()) {
                    currentNode = currentNode.parent;
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return currentNode.children.get(0);
    }





    static class WitnessNode {
        private WitnessNode parent;
        private String data;
        private List<WitnessNode> children;

        public WitnessNode(String data) {
            this.data = data;
            this.children = new ArrayList<>();
        }
        public void addChild(WitnessNode child) {
            children.add(child);
            child.parent = this;
        }

        @Override
        public String toString() {
            return data;
        }

        public Stream<WitnessNode> depthFirstNodeStream() {
            Stream<WitnessNode> witnessNodeStream = Stream.concat(Stream.of(this), children.stream().map(c -> c.depthFirstNodeStream()).flatMap(Function.identity()));
            return witnessNodeStream;
        }

        public Stream<WitnessNodeEvent> depthFirstNodeEventStream() {
            // NOTE: this if can be removed with inheritance
            if (this.children.isEmpty()) {
                return Stream.of(new WitnessNodeTextEvent(this));
            }

            Stream a = Stream.of(new WitnessNodeStartElementEvent(this));
            Stream b = children.stream().map(c -> c.depthFirstNodeEventStream()).flatMap(Function.identity());
            Stream c = Stream.of(new WitnessNodeEndElementEvent(this));
            return Stream.concat(a, Stream.concat(b, c));
        }
    }

    static class WitnessNodeEvent {
        protected WitnessNode node;
        public WitnessNodeEvent(WitnessNode node) {
            this.node = node;
        }
    }

    static class WitnessNodeStartElementEvent extends WitnessNodeEvent {
        public WitnessNodeStartElementEvent(WitnessNode node) {
            super(node);
        }

        @Override
        public String toString() {
            return ">"+node.data;
        }
    }

    static class WitnessNodeEndElementEvent extends WitnessNodeEvent {
        public WitnessNodeEndElementEvent(WitnessNode node) {
            super(node);
        }

        @Override
        public String toString() {
            return "/"+node.data;
        }
    }

    static class WitnessNodeTextEvent extends WitnessNodeEvent {
        public WitnessNodeTextEvent(WitnessNode node) {
            super(node);
        }

        @Override
        public String toString() {
            return node.data;
        }
    }
}
