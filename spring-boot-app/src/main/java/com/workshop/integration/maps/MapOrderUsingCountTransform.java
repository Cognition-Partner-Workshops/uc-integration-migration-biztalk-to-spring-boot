package com.workshop.integration.maps;

import org.springframework.stereotype.Component;

/**
 * Port of {@code MapOrderUsingCount.btm} (Working-with-Maps / Muenchian-Grouping-and-Sorting-without-losing-Map-functionalities,
 * Sample1): groups {@code InputOrder/Order} by {@code OrderId} with the Muenchian method (count() variant) and sorts
 * the groups ascending.
 *
 * <p>Strategy: XSLT carry-over. The map is a single link plus two scripting functoids that are both inline XSLT
 * (an {@code xsl:key} declared via "Inline XSLT Call Template" and a grouped {@code xsl:for-each} via "Inline XSLT"),
 * so the stylesheet under {@code maps/MapOrderUsingCount.xslt} carries BizTalk's semantics over verbatim.
 *
 * <p>// BizTalk parity: the recorded output emits each grouped item as {@code <Item>}, although the functoid's XSLT
 * and {@code OutputOrder.xsd} both name the element {@code ItemId}. The stylesheet reproduces {@code <Item>} because
 * the recorded BizTalk output is the oracle; see the note in the XSLT.
 */
@Component
public class MapOrderUsingCountTransform extends XsltMapTransform {

    public MapOrderUsingCountTransform() {
        super("/maps/MapOrderUsingCount.xslt");
    }

    @Override
    public String name() {
        return "MapOrderUsingCount";
    }
}
