<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">



<xsl:output method="html"
	encoding="UTF-8"
	doctype-system="about:legacy-compat"
	omit-xml-declaration="yes"
	standalone="yes"
	indent="no"
/>



<xsl:strip-space elements="*"/>



<xsl:template match="/">
<html lang="de">
<head>
	<title>Videoindex - Mediathek - DMI - HAW Hamburg</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<script type="text/javascript" src="jquery-1.10.2.min.js"></script>      
	<script type="text/javascript" src="script.js"></script>
	<link rel="stylesheet" type="text/css" href="style.css"/>
	<link rel="alternate" type="application/rss+xml" title="Alle Videos (RSS)" href="rss.xml" />
</head>
<body>
	<h3>Videoindex - Mediathek - DMI - HAW Hamburg (<a title="Alle Videos (RSS)" href="rss.xml">Feed</a>)</h3>
	<xsl:for-each select="/indexer[1]/index[1]/layer">
		<xsl:call-template name="layer" />
	</xsl:for-each>
	<p>
	<input type="button" id="check" value="alle auswählen"/>
	<input type="button" id="uncheck" value="auswahl aufheben"/>
	<input type="button" id="reset" value="alles zurücksetzen"/>
	</p>
	<p>Diese Seite wurde erstellt von <a href="https://blackpinguin.de/" target="_blank">Robin C. Ladiges</a>.</p>
	<!-- mit Scala, Java, XML, XSLT, HTML, JavaScript und CSS. -->
	<!-- unter Verwendung von:
		AsyncHttpClient
		HtmlCleaner
		Xalan-Java
		(slf4j)
	-->
	<!-- Version 3 (2014-03-09) -->
</body>
</html>
</xsl:template>



<!-- Rekursive Funktion: Ausgabe einer Ebene -->
<xsl:template name="layer">
	<!-- Funktionsparameter mit Default-Wert -->
	<xsl:param name="path" select="@name" />

	<!-- Funktionskörper -->
	<details>
		<xsl:attribute name="id"><xsl:value-of select="concat('d_', $path)"/></xsl:attribute>
		<summary>
			<xsl:if test="@checkbox and @checkbox = 'true'">
				<input type="checkbox">
					<xsl:attribute name="name"><xsl:value-of select="concat('cb_', $path)"/></xsl:attribute>
				</input>
			</xsl:if>
			<xsl:value-of select="@name"/>
		</summary>
		
		<div>
			<xsl:if test="@videos or @duration">
				<div>
					<!-- Videoanzahl -->
					<xsl:if test="@videos">Anzahl Videos: <xsl:value-of select="@videos"/></xsl:if>
					<xsl:if test="@videos and @duration">, </xsl:if>
					<xsl:if test="@duration">Dauer: <xsl:value-of select="@duration"/></xsl:if>
				</div>
			</xsl:if>		
			<!-- für alle layer Unterelemente -->
			<xsl:for-each select="layer">
				<!-- Rekursionsaufruf mit Parameter -->
				<xsl:call-template name="layer">
					<xsl:with-param name="path" select="concat($path, '_', @name)"/>
				</xsl:call-template>
			</xsl:for-each>
		
			<!-- für alle vref Unterelemente -->
			<xsl:for-each select="vref">
				<!-- Funktionsaufruf mit Parameter -->
				<xsl:call-template name="vref">
					<xsl:with-param name="path" select="$path"/>
				</xsl:call-template>
			</xsl:for-each>
		</div>
		
	</details>
</xsl:template>



<!-- Funktion: Finden und Ausgeben eines Videos anhand der ID -->
<xsl:template name="vref">
	<!-- Funktionsparameter -->
	<xsl:param name="path" />
	
	<xsl:variable name="id" select="@id" />
	<xsl:variable name="title" select="@title" />
	
	<xsl:for-each select="/indexer[1]/videos[1]/video[@id=$id]">
		<xsl:call-template name="video">
			<xsl:with-param name="path" select="concat($path, '_', $title)"/>
			<xsl:with-param name="title" select="$title"/>
		</xsl:call-template>
	</xsl:for-each>
	
</xsl:template>



<!-- Funktion: Ausgabe eines Videos -->
<xsl:template name="video">
	<!-- Funktionsparameter -->
	<xsl:param name="path" />
	<xsl:param name="title" />

	<!-- Funktionskörper -->
	<div>
		<!-- Checkbox -->
		<input type="checkbox">
			<xsl:attribute name="name"><xsl:value-of select="concat('cb_', $path)"/></xsl:attribute>
		</input>
		
		<!-- Link und Titel -->
		<a target="_blank">
			<xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
			<xsl:value-of select="$title"/>
		</a>
		
		<!-- Videodauer, falls vorhanden -->
		<xsl:if test="@duration">
			<xsl:text> </xsl:text>
			<span>(<xsl:value-of select="@duration"/>)</span>
		</xsl:if>
		
		<!-- Videodateien -->
		<xsl:for-each select="file">
			<xsl:text> </xsl:text>
			<a target="_blank">
				<xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
				<xsl:value-of select="concat('[',@type,']')"/>
			</a>
		</xsl:for-each>
		
	</div>
</xsl:template>



</xsl:stylesheet>