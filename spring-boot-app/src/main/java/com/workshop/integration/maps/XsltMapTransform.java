package com.workshop.integration.maps;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltExecutable;

/**
 * Base class for maps ported by carrying the BizTalk-generated XSLT over verbatim.
 * Subclasses supply the map name and the classpath location of the stylesheet.
 */
public abstract class XsltMapTransform implements MapTransform {

    private static final Processor PROCESSOR = new Processor(false);

    private final XsltExecutable executable;

    protected XsltMapTransform(String stylesheetResource) {
        try (var in = getClass().getResourceAsStream(stylesheetResource)) {
            if (in == null) {
                throw new IllegalStateException("stylesheet not found: " + stylesheetResource);
            }
            this.executable = PROCESSOR.newXsltCompiler().compile(new StreamSource(in));
        } catch (SaxonApiException | java.io.IOException e) {
            throw new IllegalStateException("cannot compile " + stylesheetResource, e);
        }
    }

    @Override
    public String transform(String inputXml) {
        try {
            var out = new StringWriter();
            var serializer = PROCESSOR.newSerializer(out);
            serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
            var transformer = executable.load30();
            transformer.transform(new StreamSource(new StringReader(inputXml)), serializer);
            return out.toString();
        } catch (SaxonApiException e) {
            throw new IllegalStateException(name() + " failed", e);
        }
    }
}
