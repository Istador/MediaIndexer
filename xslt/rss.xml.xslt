<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:date="http://exslt.org/dates-and-times"
	extension-element-prefixes="date">
>



<xsl:output method="xml" encoding="UTF-8" />



<xsl:import href="EXSLT/date/functions/format-date/date.format-date.template.xsl" />



<xsl:strip-space elements="*"/>



<xsl:template match="/">
<rss version="2.0">
	<channel>
		<title>Video Links</title>
		<link>http://weitz.de/haw-videos/</link>
		<description>Ein Index aller Video Links</description>
		<language>de-de</language>
		<generator>XSLT</generator>
		<lastBuildDate>
			<xsl:call-template name="rssdate">
				<xsl:with-param name="date" select="/indexer[1]/@gendate"/>
			</xsl:call-template>
		</lastBuildDate>
		
		<xsl:for-each select="//vref">
			
			<xsl:variable name="id" select="@id" />
			<xsl:variable name="title" select="@title" />
		
			<xsl:for-each select="/indexer[1]/videos[1]/video[@id = $id]">
				<xsl:text>&#xa;</xsl:text>
				<item>
					<title><xsl:value-of select="$title"/></title>
					<link><xsl:value-of select="@url"/></link>
					<description>
						<a target="_blank">
							<xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
							<xsl:value-of select="$title"/>
						</a>
						
						<br/>Datum: <xsl:value-of select="@date"/>
						
						<xsl:if test="@duration">
							<br/>Dauer: <xsl:value-of select="@duration"/>
						</xsl:if>
						
						<br/>Dateien:<xsl:for-each select="file">
							<br/><a target="_blank">
								<xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
								<xsl:value-of select="@type"/>
							</a>
						</xsl:for-each>
					</description>
					<pubDate>
						<xsl:call-template name="rssdate">
							<xsl:with-param name="date" select="@pubdate"/>
						</xsl:call-template>
					</pubDate>
				</item>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:text>&#xa;</xsl:text>
	</channel>
</rss>
</xsl:template>



<!-- Funktion: YYYY-MM-DD zu RSS-Datumsformat -->
<xsl:template name="rssdate">
	<!-- Funktionsparameter -->
	<xsl:param name="date" />
		
	<!-- Funktionskörper -->
	<xsl:call-template name="date:format-date">
		<xsl:with-param name="date-time" select="$date" />
		<xsl:with-param name="pattern" select="'EEE, dd MMM yyyy HH:mm:ss'" />
	</xsl:call-template>
	<xsl:text> +0100</xsl:text>
</xsl:template>



</xsl:stylesheet>