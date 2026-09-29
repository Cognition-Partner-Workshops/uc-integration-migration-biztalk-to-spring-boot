<?xml version="1.0" encoding="UTF-8"?>
<!--
  Carried over from Working-with-Maps/Grouping-Pattern-Selecting-distinct-nodes/
  SelectDistinctValues/Sample3/MapPerson.btm (BizTalk Mapper 2.0, OmitXmlDeclaration=Yes).

  The map is a single link from an inline-XSLT Scripting functoid to Person plus
  two XsltCallTemplate functoids (NationalityTemplate, EmailTemplate); the three
  scripts below are the functoid bodies verbatim.

  BizTalk parity: NationalityTemplate emits one <Nationality> per matching Person
  (so a grouped person can carry several), while EmailTemplate keeps only the
  Email of the last matching Person. This is what the recorded MapPerson_output.xml
  contains and is reproduced deliberately.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
                xmlns:var="http://schemas.microsoft.com/BizTalk/2003/var"
                exclude-result-prefixes="msxsl var s0"
                version="1.0"
                xmlns:s0="http://SelectDistinctValues.InputPersons"
                xmlns:ns0="http://SelectDistinctValues.OutputPersons">
  <xsl:output omit-xml-declaration="yes" method="xml" version="1.0" />

  <xsl:template match="/">
    <xsl:apply-templates select="/s0:Persons" />
  </xsl:template>

  <xsl:template match="/s0:Persons">
    <ns0:Persons>
      <xsl:for-each select="//Person[not(Name=preceding-sibling::Person/Name)]">
        <xsl:element name="Person">
          <xsl:element name="Name"><xsl:value-of select="Name" /></xsl:element>
          <xsl:call-template name="NationalityTemplate">
            <xsl:with-param name="param1" select="string(Name)" />
          </xsl:call-template>
          <xsl:call-template name="EmailTemplate">
            <xsl:with-param name="paramName" select="string(Name)" />
          </xsl:call-template>
        </xsl:element>
      </xsl:for-each>
    </ns0:Persons>
  </xsl:template>

  <xsl:template name="NationalityTemplate">
    <xsl:param name="param1" />
    <xsl:for-each select="//Person[Name=$param1]">
      <xsl:element name="Nationality"><xsl:value-of select="Nationality" /></xsl:element>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="EmailTemplate">
    <xsl:param name="paramName" />
    <xsl:for-each select="//Person[Name=$paramName]">
      <xsl:if test="position()=last()">
        <xsl:element name="Email"><xsl:value-of select="Email" /></xsl:element>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
