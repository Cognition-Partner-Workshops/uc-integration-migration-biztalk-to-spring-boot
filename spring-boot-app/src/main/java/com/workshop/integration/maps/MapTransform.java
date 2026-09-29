package com.workshop.integration.maps;

/**
 * A migrated BizTalk map: one XML document in, one XML document out.
 *
 * Implementations are Spring beans discovered by {@link MapRegistry}; {@link #name()}
 * is the BizTalk map name (the {@code .btm} file stem, e.g. {@code MapPerson}).
 */
public interface MapTransform {

    String name();

    String transform(String inputXml);
}
