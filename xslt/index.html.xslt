<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:str="http://exslt.org/strings"
	extension-element-prefixes="str"
>


<xsl:import href="EXSLT/str/functions/replace/str.replace.template.xsl" />


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
	<script type="text/javascript" src="jquery-1.10.2.min.js"></script>      
	<script type="text/javascript" src="script.js"></script>
	<link rel="stylesheet" type="text/css" href="style.css"/>
	<link rel="alternate" type="application/rss+xml" title="Alle Videos (RSS)" href="feed.rss" />
</head>
<body>
	<header>
		<p>
			Videoindex - Mediathek - DMI - HAW Hamburg
			<a class="rss" title="Alle Videos (RSS)" href="feed.rss">
				<img class="rss" src="rss.png" alt="RSS Icon"/>
			</a>
		</p>
	</header>
	
	<div id="background">
		<div id="videocont" class="round"></div>
	</div>
	
	<xsl:for-each select="/indexer[1]/index[1]/layer">
		<xsl:call-template name="layer" />
	</xsl:for-each>
	
	<footer>
		<p>
		<input type="button" id="check" value="alle auswählen"/>
		<input type="button" id="uncheck" value="auswahl aufheben"/>
		<input type="button" id="reset" value="alles zurücksetzen"/>
		</p>
		<p>Diese Seite wurde zuletzt generiert <xsl:call-template name="str:replace">
			<xsl:with-param name="string" select="/indexer[1]/@gendate" />
			<xsl:with-param name="search" select="' '" />
			<xsl:with-param name="replace" select="' um '" />
		</xsl:call-template> Uhr, mit einem Program von <a href="https://blackpinguin.de/" target="_blank">Robin C. Ladiges</a>.</p>
	</footer>
	<!-- mit Scala, Java, XML, XSLT, HTML, JavaScript und CSS. -->
	<!-- unter Verwendung von:
		AsyncHttpClient
		HtmlCleaner
		Xalan-Java
		Apache Commons Lang
		(slf4j)
	-->
	<!-- Version 3 (2014-03-09) -->
</body>
</html>
</xsl:template>



<!-- Rekursive Funktion: Ausgabe einer Ebene -->
<xsl:template name="layer">
	<!-- Funktionsparameter mit Default-Wert -->
	<xsl:param name="pathparam" select="@name" />

	<xsl:variable name="path">
		<xsl:call-template name="str:replace">
			<xsl:with-param name="string" select="$pathparam" />
			<xsl:with-param name="search" select="' '" />
			<xsl:with-param name="replace" select="'_'" />
		</xsl:call-template>
	</xsl:variable>
	
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
			
			<xsl:if test="not(@checkbox and @checkbox = 'true')">
				<xsl:text> </xsl:text>
				<a class="extern">
					<xsl:attribute name="title"><xsl:value-of select="@name"/></xsl:attribute>
					<xsl:attribute name="href"><xsl:value-of select="$path"/>/</xsl:attribute>
					<img class="extern" src="extern.png" alt="Extern Icon"/>
				</a>
				<xsl:text> </xsl:text>
				<a class="rss">
					<xsl:attribute name="title"><xsl:value-of select="@name"/> als Feed abonnieren (RSS)</xsl:attribute>
					<xsl:attribute name="href"><xsl:value-of select="$path"/>/feed.rss</xsl:attribute>
					<img class="rss" src="rss.png" alt="RSS Icon"/>
				</a>
			</xsl:if>
			
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
					<xsl:with-param name="pathparam" select="concat($path, '/', @name)"/>
				</xsl:call-template>
			</xsl:for-each>
		
			<!-- für alle vref Unterelemente -->
			<xsl:for-each select="vref">
				<!-- Funktionsaufruf mit Parameter -->
				<xsl:call-template name="vref">
					<xsl:with-param name="pathparam" select="$path"/>
				</xsl:call-template>
			</xsl:for-each>
		</div>
		
	</details>
</xsl:template>



<!-- Funktion: Finden und Ausgeben eines Videos anhand der ID -->
<xsl:template name="vref">
	<!-- Funktionsparameter -->
	<xsl:param name="pathparam" />
	
	<xsl:variable name="id" select="@id" />
	<xsl:variable name="title" select="@title" />
	
	<xsl:for-each select="/indexer[1]/videos[1]/video[@id=$id]">
		<xsl:call-template name="video">
			<xsl:with-param name="pathparam" select="concat($pathparam, '/', $title)"/>
			<xsl:with-param name="title" select="$title"/>
		</xsl:call-template>
	</xsl:for-each>
	
</xsl:template>



<!-- Funktion: Ausgabe eines Videos -->
<xsl:template name="video">
	<!-- Funktionsparameter -->
	<xsl:param name="pathparam" />
	<xsl:param name="title" />
	
	<xsl:variable name="path">
		<xsl:call-template name="str:replace">
			<xsl:with-param name="string" select="$pathparam" />
			<xsl:with-param name="search" select="' '" />
			<xsl:with-param name="replace" select="'_'" />
		</xsl:call-template>
	</xsl:variable>
	
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
		
		<!-- Button um den eigebetteten Player zu öffnen -->
		<xsl:text> </xsl:text>
		<input type="button" class="videobtn" value="anschauen"/>
		
	</div>
</xsl:template>



</xsl:stylesheet>