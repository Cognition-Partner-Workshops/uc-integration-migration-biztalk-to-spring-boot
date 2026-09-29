package com.workshop.integration.maps;

import org.springframework.stereotype.Component;

/**
 * BizTalk map {@code MapPerson} (Grouping Pattern - Selecting Distinct Nodes, Sample 3):
 * groups {@code Person} records by {@code Name} from
 * {@code http://SelectDistinctValues.InputPersons} into
 * {@code http://SelectDistinctValues.OutputPersons}.
 *
 * Strategy: XSLT carry-over. The map is one inline-XSLT scripting functoid plus two
 * XSLT call-template functoids, so the stylesheet is the map's own logic.
 *
 * BizTalk parity: a grouped person keeps every Nationality of every record with that
 * Name but only the Email of the last such record (see {@code maps/MapPerson.xslt}).
 */
@Component
public class MapPersonTransform extends XsltMapTransform {

    public MapPersonTransform() {
        super("/maps/MapPerson.xslt");
    }

    @Override
    public String name() {
        return "MapPerson";
    }
}
