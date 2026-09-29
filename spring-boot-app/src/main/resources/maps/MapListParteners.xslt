<?xml version="1.0" encoding="UTF-8"?>
<!--
  Carried over from Working-with-Maps/Grouping-Pattern-Selecting-distinct-nodes/
  SelectDistinctValues/Sample1/MapListParteners.btm (BizTalk Mapper 2.0, 2009-09-14).

  The map is a single Scripting functoid (inline XSLT) linked to
  ListPartners/PartnerName; the surrounding skeleton is what the BizTalk
  compiler emits for an Input.xsd -> Output1.xsd map with
  OmitXmlDeclaration="Yes" and IgnoreNamespacesForLinks="Yes".
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
                xmlns:var="http://schemas.microsoft.com/BizTalk/2003/var"
                xmlns:s0="http://SelectDistinctValues.Input"
                xmlns:ns0="http://SelectDistinctValues.Output1"
                exclude-result-prefixes="msxsl var s0">
  <xsl:output omit-xml-declaration="yes" method="xml" version="1.0"/>

  <xsl:template match="/">
    <xsl:apply-templates select="/s0:ExternalEmployees"/>
  </xsl:template>

  <xsl:template match="/s0:ExternalEmployees">
    <ns0:ListPartners>
      <!-- Scripting functoid (FunctoidID=1, Language=Xslt), verbatim.
           XSLT 1.0 "distinct" idiom: keep a Company only if no preceding
           Employee sibling has the same Company; first-seen order. -->
      <xsl:variable name="unique-companies" select="//Employee[not(Company=preceding-sibling::Employee/Company)]/Company"/>
      <xsl:for-each select="$unique-companies">
        <PartnerName><xsl:value-of select="."/></PartnerName>
      </xsl:for-each>
    </ns0:ListPartners>
  </xsl:template>
</xsl:stylesheet>
