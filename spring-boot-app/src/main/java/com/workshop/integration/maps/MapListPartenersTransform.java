package com.workshop.integration.maps;

import org.springframework.stereotype.Component;

/**
 * BizTalk map {@code MapListParteners} (Grouping Pattern - Selecting distinct nodes, Sample 1):
 * {@code ExternalEmployees/Employee/Company} -> distinct {@code ListPartners/PartnerName},
 * first-seen order.
 *
 * <p>Strategy: XSLT carry-over. The map is one inline-XSLT Scripting functoid and a single
 * link; there is no C# and no external assembly, so the stylesheet is run as-is.
 *
 * <p>BizTalk parity: the XSLT 1.0 "preceding-sibling" distinct idiom is kept verbatim rather
 * than being replaced by {@code distinct-values()}; both yield the same first-seen order for
 * this map, but the recorded BizTalk output is the oracle, not the cleaner expression.
 */
@Component
public class MapListPartenersTransform extends XsltMapTransform {

    public MapListPartenersTransform() {
        super("/maps/MapListParteners.xslt");
    }

    @Override
    public String name() {
        return "MapListParteners";
    }
}
