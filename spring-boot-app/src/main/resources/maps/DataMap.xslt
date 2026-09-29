<?xml version="1.0" encoding="UTF-8"?>
<!--
  Carried over from Working-with-Maps/Grouping-Pattern-Selecting-distinct-nodes/
  SelectDistinctValues/Sample2/DataMap.btm (BizTalk Mapper 2.0). The map is a single
  link from an inline-XSLT Scripting functoid to /Data plus an XsltCallTemplate
  functoid (NameValueTemplate); the mapsource frame (match="/" -> match="/In",
  OmitXmlDeclaration="Yes") is what the BizTalk compiler wraps around them.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
                xmlns:var="http://schemas.microsoft.com/BizTalk/2003/var"
                exclude-result-prefixes="msxsl var"
                version="1.0">
  <xsl:output omit-xml-declaration="yes" method="xml" version="1.0"/>

  <xsl:template match="/">
    <xsl:apply-templates select="/In"/>
  </xsl:template>

  <xsl:template match="/In">
    <!-- Scripting functoid 1 (Inline XSLT), linked to the Data root -->
    <xsl:element name="Data">
      <!-- BizTalk parity: distinct Header via preceding-sibling test, first-seen order;
           Header/date/Record are emitted as siblings per group with date taken from the
           first Data of that Header only. -->
      <xsl:for-each select="Data[not(@Header=preceding-sibling::Data/@Header)]">
        <xsl:element name="Header"><xsl:value-of select="@Header"/></xsl:element>
        <xsl:element name="date"><xsl:value-of select="@date"/></xsl:element>
        <xsl:element name="Record">
          <xsl:call-template name="NameValueTemplate">
            <xsl:with-param name="param1" select="string(@Header)"/>
          </xsl:call-template>
        </xsl:element>
      </xsl:for-each>
    </xsl:element>
  </xsl:template>

  <!-- Scripting functoid 2 (Inline XSLT Call Template) -->
  <!-- BizTalk parity: Name/Value pairs are flattened as direct siblings under Record
       (no per-item wrapper), selected document-wide with //Data[@Header=$param1]. -->
  <xsl:template name="NameValueTemplate">
    <xsl:param name="param1"/>
    <xsl:for-each select="//Data[@Header=$param1]">
      <xsl:element name="Name"><xsl:value-of select="@Name"/></xsl:element>
      <xsl:element name="Value"><xsl:value-of select="@Value"/></xsl:element>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
