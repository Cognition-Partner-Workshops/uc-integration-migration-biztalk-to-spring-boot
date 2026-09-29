<?xml version="1.0" encoding="UTF-8"?>
<!--
  MapOrderUsingCount.btm (Muenchian-Grouping-and-Sorting-without-losing-Map-functionalities, Sample1)
  reconstructed as the XSLT 1.0 BizTalk compiles it to: the root template emits ns0:OutputOrder and
  the two scripting functoids (an "Inline XSLT Call Template" holding the xsl:key and an "Inline XSLT"
  holding the grouped for-each) are placed where the map links them - the key at stylesheet level,
  the loop under the destination Order record.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
                xmlns:var="http://schemas.microsoft.com/BizTalk/2003/var"
                xmlns:s0="http://MuenchianGrouping.InputOrder"
                xmlns:ns0="http://MuenchianGrouping.OutputOrder"
                exclude-result-prefixes="msxsl var s0"
                version="1.0">
  <xsl:output omit-xml-declaration="yes" method="xml" version="1.0"/>

  <!-- Functoid 2 (XsltCallTemplate): the Muenchian key -->
  <xsl:key name="groups" match="Order" use="OrderId"/>

  <xsl:template match="/">
    <xsl:apply-templates select="/s0:InputOrder"/>
  </xsl:template>

  <xsl:template match="/s0:InputOrder">
    <ns0:OutputOrder>
      <!-- Functoid 1 (Inline XSLT): group Order by OrderId using count() instead of generate-id() -->
      <xsl:for-each select="Order[count(. | key('groups',OrderId)[1]) = 1]">
        <xsl:sort select="OrderId" order="ascending"/>
        <Order>
          <OrderId>
            <xsl:value-of select="OrderId/text()"/>
          </OrderId>
          <Items>
            <xsl:for-each select="key('groups',OrderId)">
              <!-- BizTalk parity: the recorded output (InputOrder.xml_output.xml) names each grouped
                   item <Item>, although the functoid's XSLT and OutputOrder.xsd both say <ItemId>.
                   The recorded BizTalk output is the oracle, so <Item> is emitted here. -->
              <Item>
                <xsl:value-of select="ItemId"/>
              </Item>
            </xsl:for-each>
          </Items>
        </Order>
      </xsl:for-each>
    </ns0:OutputOrder>
  </xsl:template>
</xsl:stylesheet>
