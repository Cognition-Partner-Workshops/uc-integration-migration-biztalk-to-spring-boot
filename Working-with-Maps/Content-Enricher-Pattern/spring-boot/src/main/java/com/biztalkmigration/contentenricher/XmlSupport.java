package com.biztalkmigration.contentenricher;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Namespace-aware, XXE-safe DOM helpers shared by the mapper and the web layer. */
public final class XmlSupport {

    private XmlSupport() {
    }

    public static Document parse(InputStream in) {
        try {
            return newDocumentBuilderFactory().newDocumentBuilder().parse(in);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Invalid XML message", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Document newDocument() {
        try {
            return newDocumentBuilderFactory().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Serialises without an XML declaration, mirroring the map's {@code OmitXmlDeclaration="Yes"}. */
    public static String serialize(Document document) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Element requireRoot(Document document, String namespace, String localName) {
        Element root = document.getDocumentElement();
        if (root == null || !namespace.equals(root.getNamespaceURI()) || !localName.equals(root.getLocalName())) {
            throw new IllegalArgumentException("Expected root element {" + namespace + "}" + localName);
        }
        return root;
    }

    public static List<Element> childElements(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                result.add(element);
            }
        }
        return result;
    }

    public static Element requireChild(Element parent, String localName) {
        List<Element> matches = childElements(parent, localName);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Missing element <" + localName + "> under <" + parent.getLocalName() + ">");
        }
        return matches.get(0);
    }

    /** Text of the first child with that name, or "" when absent (BizTalk's {@code value-of} yields empty). */
    public static String childText(Element parent, String localName) {
        List<Element> matches = childElements(parent, localName);
        return matches.isEmpty() ? "" : matches.get(0).getTextContent();
    }

    public static Element appendElement(Element parent, String localName, String text) {
        Element element = parent.getOwnerDocument().createElement(localName);
        element.setTextContent(text);
        parent.appendChild(element);
        return element;
    }

    private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        return factory;
    }
}
