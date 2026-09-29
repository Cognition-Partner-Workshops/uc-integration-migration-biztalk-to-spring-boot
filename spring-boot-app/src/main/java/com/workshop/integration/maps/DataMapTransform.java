package com.workshop.integration.maps;

import org.springframework.stereotype.Component;

/**
 * Grouping Pattern - Selecting distinct nodes, Sample 2: {@code DataMap.btm}.
 *
 * <p>Strategy: XSLT carry-over. The map has no C# and no functoid links other than one
 * inline-XSLT Scripting functoid plus an XsltCallTemplate functoid, so the stylesheet is
 * carried verbatim into {@code maps/DataMap.xslt}.
 *
 * <p>// BizTalk parity: groups {@code In/Data} by distinct {@code @Header} in first-seen
 * order and emits {@code Header}, {@code date} (from the group's first {@code Data} only)
 * and a {@code Record} whose {@code Name}/{@code Value} pairs are flat siblings, exactly as
 * the recorded {@code DataMap_output.xml} shows.
 */
@Component
public class DataMapTransform extends XsltMapTransform {

    public DataMapTransform() {
        super("/maps/DataMap.xslt");
    }

    @Override
    public String name() {
        return "DataMap";
    }
}
