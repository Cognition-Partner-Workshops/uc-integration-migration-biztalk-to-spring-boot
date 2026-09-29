package com.workshop.integration.maps;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Java rewrite of {@code X1EDISample/Maps/Enrollment_to_5010_834.btm} (Looping Pattern sample):
 * ArrayHealth {@code EnrollmentSet} to X12 5010 834 ({@code X12_00501_834}).
 *
 * <p>Rewritten in Java rather than carried over as XSLT because the map's BGN page is driven by
 * C# scripting functoids ({@code DateTime.UtcNow}), and because the recorded BizTalk output does
 * not correspond to the XSLT the current {@code .btm} would compile to (see the parity notes
 * inline). Each parity note reproduces what BizTalk recorded, not what a reading of the map
 * would suggest.
 */
@Component
public class EnrollmentTo5010834Transform implements MapTransform {

    static final String SOURCE_NS = "http://schemas.arrayhealth.com/Enrollment/v1.0";
    static final String X12_NS = "http://schemas.microsoft.com/BizTalk/EDI/X12/2006";
    private static final String NS0 = "ns0:";

    private static final DateTimeFormatter EDI_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter EDI_TIME = DateTimeFormatter.ofPattern("HHmmssSS");

    private final Clock clock;

    // BizTalk parity: the scripting functoids keep EDIDATE/EDITIME in C# static fields that are
    // assigned once and never reset, so every message mapped by the same host process carries the
    // first message's creation date and time. The singleton bean plays the role of the static.
    private volatile String ediDate;
    private volatile String ediTime;

    public EnrollmentTo5010834Transform(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return "Enrollment_to_5010_834";
    }

    @Override
    public String transform(String inputXml) {
        Document source = parse(inputXml);
        Document target = newDocument();

        Element root = target.createElementNS(X12_NS, NS0 + "X12_00501_834");
        target.appendChild(root);

        Element bgn = child(root, X12_NS, "BGN_BeginningSegment");
        field(bgn, "BGN01_TransactionSetPurposeCode", "00");
        field(bgn, "BGN03_TransactionSetCreationDate", ediDate());
        field(bgn, "BGN04_TransactionSetCreationTime", ediTime());
        field(bgn, "BGN05_TimeZoneCode", "UT");

        List<Element> subscribers = new ArrayList<>();
        List<Element> dependents = new ArrayList<>();
        for (Element enrollment : enrollments(source)) {
            for (Element subscriber : children(enrollment, "Subscriber")) {
                if (!children(subscriber, "MemberId").isEmpty()) {
                    subscribers.add(subscriber);
                }
            }
            for (Element dependentsGroup : children(enrollment, "Dependents")) {
                for (Element dependent : children(dependentsGroup, "Dependent")) {
                    if (!children(dependent, "MemberId").isEmpty()) {
                        dependents.add(dependent);
                    }
                }
            }
        }

        // BizTalk parity: the recorded output lists every subscriber loop first and every
        // dependent loop after, not subscriber/dependent interleaved per Enrollment. That is the
        // order two independent BizTalk looping links (Subscriber -> TS834_2000_Loop, then
        // Dependent -> TS834_2000_Loop) produce.
        for (Element ignored : subscribers) {
            Element loop = child(root, X12_NS, "TS834_2000_Loop");
            // BizTalk parity: the recorded output carries the Y/N flag in INS01_MemberIndicator,
            // although the current .btm links the table extractor to INS02_IndividualRelationshipCode.
            field(child(loop, X12_NS, "INS_MemberLevelDetail"), "INS01_MemberIndicator", "Y");
            // BizTalk parity: REF02_SubscriberIdentifier is the literal "SSN_0" for every subscriber
            // in the recorded output; it is not the subscriber's SSN and the current .btm has no
            // link into REF_SubLoop at all.
            Element ref = child(child(loop, X12_NS, "REF_SubLoop"), X12_NS, "REF_SubscriberIdentifier");
            field(ref, "REF02_SubscriberIdentifier", "SSN_0");
        }
        for (Element ignored : dependents) {
            Element loop = child(root, X12_NS, "TS834_2000_Loop");
            field(child(loop, X12_NS, "INS_MemberLevelDetail"), "INS01_MemberIndicator", "N");
            // BizTalk parity: dependents get an empty REF_SubscriberIdentifier record (no REF02).
            child(child(loop, X12_NS, "REF_SubLoop"), X12_NS, "REF_SubscriberIdentifier");
        }

        return serialize(target);
    }

    private String ediDate() {
        if (ediDate == null) {
            ediDate = now().format(EDI_DATE);
        }
        return ediDate;
    }

    private String ediTime() {
        if (ediTime == null) {
            ediTime = now().format(EDI_TIME);
        }
        return ediTime;
    }

    private ZonedDateTime now() {
        return ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static List<Element> enrollments(Document source) {
        List<Element> result = new ArrayList<>();
        Element root = source.getDocumentElement();
        for (Element bySubscriber : children(root, "EnrollmentsBySubscriber")) {
            result.addAll(children(bySubscriber, "Enrollment"));
        }
        return result;
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE
                && localName.equals(node.getLocalName())
                && SOURCE_NS.equals(node.getNamespaceURI())) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static Element child(Element parent, String namespace, String localName) {
        Element element = parent.getOwnerDocument().createElementNS(namespace, NS0 + localName);
        parent.appendChild(element);
        return element;
    }

    /** X12 schema fields are unqualified ({@code elementFormDefault} is not set). */
    private static void field(Element parent, String name, String value) {
        Element element = parent.getOwnerDocument().createElementNS(null, name);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | java.io.IOException e) {
            throw new IllegalStateException("Enrollment_to_5010_834: cannot parse input", e);
        }
    }

    private static Document newDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String serialize(Document document) {
        try {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            var out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Enrollment_to_5010_834: cannot serialize output", e);
        }
    }
}
