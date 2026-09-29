package com.workshop.integration.maps;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Java rewrite of {@code NameValueSolution3.btm} (Name-Value Transformation Pattern,
 * hierarchical schema to name/value pair, solution 3: inline XSLT).
 *
 * <p>The BizTalk map is two direct links ({@code Header/Nprocesso -> Id},
 * {@code Body/ServiceName -> ServiceName}) plus an inline-XSLT scripting functoid that
 * builds {@code Properties} and calls a C# scripting functoid ({@code EmptyOrNull}) to skip
 * empty values. Because of the C# dependency the map is rewritten in Java; the two
 * {@code xsl:for-each} loops of the functoid are mirrored one-to-one below.
 */
@Component
public class NameValueSolution3Transform implements MapTransform {

    static final String SOURCE_NS = "http://SandroPereira.MappingToNameValueRecord.Request";
    static final String TARGET_NS = "http://SandroPereira.MappingToNameValueRecord.Provisioning";

    @Override
    public String name() {
        return "NameValueSolution3";
    }

    @Override
    public String transform(String inputXml) {
        Document source = parse(inputXml);
        Element request = source.getDocumentElement();
        if (!"Request".equals(request.getLocalName()) || !SOURCE_NS.equals(request.getNamespaceURI())) {
            throw new IllegalArgumentException(name() + " expects a " + SOURCE_NS + " Request document");
        }
        Element header = child(request, "Header");
        Element body = child(request, "Body");

        Document out = newDocument();
        Element provisioning = out.createElementNS(TARGET_NS, "ns0:Provisioning");
        out.appendChild(provisioning);

        // BizTalk parity: the two linked fields are emitted unqualified (schema has no
        // elementFormDefault="qualified"), while the Properties subtree produced by the inline
        // XSLT ends up in the Provisioning namespace. The recorded output mixes both.
        provisioning.appendChild(unqualified(out, "Id", firstText(child(header, "Nprocesso"))));

        Element properties = out.createElementNS(TARGET_NS, "ns0:Properties");
        provisioning.appendChild(properties);
        if (body != null) {
            // for-each /s0:Request/Body/*  (skip ServiceName and LAN, skip empty)
            for (Element field : children(body)) {
                String local = field.getLocalName();
                if (local.equals("ServiceName") || local.equals("LAN")) {
                    continue;
                }
                appendProperty(out, properties, field);
            }
            // for-each /s0:Request/Body/LAN/*  (skip empty)
            // BizTalk parity: LAN routes are appended after every scalar Body field, not at
            // LAN's position in the source document.
            for (Element lan : children(body)) {
                if (!lan.getLocalName().equals("LAN")) {
                    continue;
                }
                for (Element route : children(lan)) {
                    appendProperty(out, properties, route);
                }
            }
        }

        provisioning.appendChild(unqualified(out, "ServiceName", firstText(child(body, "ServiceName"))));
        return serialize(out);
    }

    /** userCSharp:EmptyOrNull(string(.)) = 'true' — string.IsNullOrEmpty, so whitespace counts as a value. */
    private static void appendProperty(Document out, Element properties, Element field) {
        String value = field.getTextContent();
        if (value.isEmpty()) {
            return;
        }
        Element property = out.createElementNS(TARGET_NS, "ns0:Property");
        property.appendChild(qualified(out, "ns0:Name", field.getLocalName()));
        property.appendChild(qualified(out, "ns0:Value", value));
        properties.appendChild(property);
    }

    private static Element unqualified(Document out, String name, String text) {
        Element el = out.createElementNS(null, name);
        el.setTextContent(text);
        return el;
    }

    private static Element qualified(Document out, String name, String text) {
        Element el = out.createElementNS(TARGET_NS, name);
        el.setTextContent(text);
        return el;
    }

    private static Element child(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Element el : children(parent)) {
            if (localName.equals(el.getLocalName())) {
                return el;
            }
        }
        return null;
    }

    private static java.util.List<Element> children(Element parent) {
        var result = new java.util.ArrayList<Element>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) n);
            }
        }
        return result;
    }

    /** xsl:value-of select="X/text()" — the first text node, or empty when the element is absent. */
    private static String firstText(Element el) {
        if (el == null) {
            return "";
        }
        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                return n.getNodeValue();
            }
        }
        return "";
    }

    private static Document parse(String xml) {
        try {
            return documentBuilderFactory().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | java.io.IOException e) {
            throw new IllegalArgumentException("cannot parse input", e);
        }
    }

    private static Document newDocument() {
        try {
            return documentBuilderFactory().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String serialize(Document doc) {
        try {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            var out = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("cannot serialize output", e);
        }
    }
}
