/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2008-2009, The KiWi Project (http://www.kiwi-project.eu)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the KiWi Project nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * 
 * 
 */
package kiwi.service.render.savelet;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.ejb.Stateless;

import kiwi.api.content.ContentItemService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.kbase.KiWiNamespace;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.util.KiWiXomUtils;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

/**
 * This savelet processes all navigational links in the text content of a content item. It performs
 * two operations:
 * 
 * <ol>
 * <li>
 *   Scan all kiwi:target attributes in the content and look up the title that is contained in them,
 *   replacing the (unstable and possibly ambiguous) title by the unique and stable kiwi identifier
 *   (see KiWiResource.getKiwiIdentifier() ).
 * </li>
 * <li>
 *   Find all &lt;a href="..." &gt; links in the text content of a content item and create appropriate
 *   triples for them typed with kiwi:internalLink and kiwi:externalLink. Removes all previous 
 *   internal and external links before adding them anew.
 * </li>
 * </ol>
 * 
 * @author Sebastian Schaffert
 *
 */
@Stateless
@AutoCreate
@Name("kiwi.service.render.savelet.NavigationalLinksSavelet")
public class NavigationalLinksSavelet implements TextContentSavelet {


	@In
	private ContentItemService contentItemService;

	@In
	private TripleStore tripleStore;
	
	@In("storingService.contentItemCache")
	private Map<String, ContentItem> contentItemCache;
	
	@Logger
	private static Log log;

	
	/* (non-Javadoc)
	 * @see kiwi.service.render.savelet.Savelet#apply(kiwi.model.kbase.KiWiResource, java.lang.Object)
	 */
	@Override
	public TextContent apply(KiWiResource context, TextContent content) {
		
		XPathContext namespaces = new XPathContext();
		namespaces.addNamespace("kiwi", Constants.NS_KIWI_HTML);
		namespaces.addNamespace("html", Constants.NS_XHTML);
		
		KiWiUriResource propInt = null;
		KiWiUriResource propExt = null;

		// remove old triples, they will be replaced below with new ones
		try {
			for(KiWiTriple t : context.listOutgoing(Constants.NS_KIWI_CORE + "internalLink")) {
				tripleStore.removeTriple(t);
			}
		} catch (NamespaceResolvingException e) {
			e.printStackTrace();
		}
		try {
			for(KiWiTriple t : context.listOutgoing(Constants.NS_KIWI_CORE + "externalLink")) {
				tripleStore.removeTriple(t);
			}
		} catch (NamespaceResolvingException e) {
			e.printStackTrace();
		}
		
		// Turn RDFa "links" (spans with "resource") attribute into ordinary navigational links.
		Nodes spans = content.getXmlDocument().query("//html:span[@resource]", namespaces);
		log.debug("found #0 resource spans attribute", spans.size());
		for (int i=0; i<spans.size(); i++) {
			// If the resource is an URL, it is an external resource,
			Element span = (Element)spans.get(i);
			
			String resource = span.getAttributeValue("resource");
			
			// Change the span element to the link element.
			Element link = new Element("a", Constants.NS_XHTML);
			
			String kind;
			if (resource.startsWith("uri::") || resource.startsWith("bnode::")) {
				// This is an ordinary internal link
				kind = "intlink";
			}
			else {
				try {
					URL url = new URL(resource);
					// we turn non-semantic links with valid URL into external links. (such links used URI in the place of the title, 
					// so we assume the user wanted to link to that URL...
					if (span.getAttribute("rel") == null && url.getProtocol() != null && !"".equals(url.getProtocol())) {
						kind = "extlink";
						link.addAttribute(new Attribute("href", resource));
					}
					else {
						kind = "intlink";
					}
				} catch (MalformedURLException e) {
					kind = "intlink";
				}
			}
			
			link.addAttribute(new Attribute("kiwi:kind", Constants.NS_KIWI_HTML, kind));
			link.addAttribute(new Attribute("kiwi:target", Constants.NS_KIWI_HTML, resource));
			
			if (span.getAttribute("rel") != null) {
				link.addAttribute(new Attribute("rel", span.getAttributeValue("rel")));
			}
			
			KiWiXomUtils.replaceElement(span, link);
		}
		
		
		Nodes intLinks = content.getXmlDocument().query("//html:a[@kiwi:kind='intlink']", namespaces);

		log.debug("found #0 internal links",intLinks.size());
		
		if(intLinks.size() > 0) {
			propInt = tripleStore.createUriResource(Constants.NS_KIWI_CORE + "internalLink");
		}
		
		for(int i=0; i<intLinks.size(); i++) {
			Element link = (Element)intLinks.get(i);

			// find target title from kiwi:target attribute
			String ci_target = link.getAttributeValue("target", Constants.NS_KIWI_HTML);
			
			ContentItem ci = null;
			
			// Check if the title is a resource kiwiid
			if (ci_target != null && ci_target.startsWith("uri::") || ci_target.startsWith("bnode::")) {
				ci = contentItemService.getContentItemByKiwiId(ci_target);
			}
			
			// Try to find an existing CI with the specified title.
			if(ci == null && ci_target != null) {
				ci = contentItemService.getContentItemByTitle(ci_target);
			}
			
			// If this also fails, try to find a CI with a matching CURIE (to allow links, such as [[ rdf:type :: foaf:Person ]]
			if (ci == null && ci_target != null) {
				if (ci_target.contains(":")) {
					String[] split = ci_target.split(":", 2);
					KiWiNamespace ns = tripleStore.getNamespace(split[0]);
					if (ns != null) {
						ci = contentItemService.getContentItemByUri(ns.getUri() + split[1]);
					}
				}
			}
			
			// if no content item found, create a new empty content item with random uri
			if(ci == null) {				
				// create an empty content item to link to
				ci = contentItemService.createContentItem();
				contentItemService.updateTitle(ci, ci_target);
				contentItemService.saveContentItem(ci);
				contentItemCache.put(ci.getKiwiIdentifier(), ci);
				log.info("created empty content item '#0' with ID #1",ci.getTitle(),ci.getKiwiIdentifier());
			}
			
			// add attribute href to <a..> element so that it always points to the right element
			link.getAttribute("target", Constants.NS_KIWI_HTML).setValue(ci.getResource().getKiwiIdentifier());
				
			// add a triple in the triplestore connecting the two contentitems with kiwi:internalLink
			tripleStore.createTriple(context, propInt, ci.getResource());
			
			log.debug("added internal link relation from #0 to #1", context.getKiwiIdentifier(), ci.getResource().getKiwiIdentifier());
			
		}

		// for all external links, add a kiwi:externalLink relation to the triple store
		Nodes extLinks = content.getXmlDocument().query("//html:a[@kiwi:kind='extlink']", namespaces);

		log.debug("found #0 external links",extLinks.size());

		if(extLinks.size() > 0) {
			propExt = tripleStore.createUriResource(Constants.NS_KIWI_CORE + "externalLink");
		}
		
		for(int i=0; i<extLinks.size(); i++) {
			Element link = (Element)extLinks.get(i);
			
			// find target href from kiwi:target attribute
			String ci_href = link.getAttributeValue("target", Constants.NS_KIWI_HTML);
			
			// check whether ci_href is a valid URI
			try {
				URI uri = new URI(ci_href);
				
				if(uri.isAbsolute()) {
				
					KiWiUriResource target = tripleStore.createUriResource(ci_href);
					
					tripleStore.createTriple(context, propExt, target);
					log.debug("added external link relation from #0 to #1", context.getKiwiIdentifier(), target.getKiwiIdentifier());
				} else {
					log.error("resource #0, external link does not point to a absolute URI: '#1'",context,ci_href);					
				}
			} catch(URISyntaxException ex) {
				log.error("resource #0, external link does not point to a URI: '#1'",context,ci_href);
			}
		}
		
		Nodes intImages = content.getXmlDocument().query("//html:img[@kiwi:kind='intlink']", namespaces);

		log.debug("found #0 internal images",intImages.size());
		
		KiWiUriResource propImg = null;
		if(intImages.size() > 0) {
			propImg = tripleStore.createUriResource(Constants.NS_KIWI_CORE + "includesImage");
		}
		
		for(int i=0; i<intImages.size(); i++) {
			Element link = (Element)intImages.get(i);

			// find target title from kiwi:target attribute
			String ci_kiwiid = link.getAttributeValue("target", Constants.NS_KIWI_HTML);
			
			// if title found, look for content item with that title and overwrite the kiwi:target
			// attribute with the kiwi identifier.
			// if no content item found, create a new empty content item with random uri
			ContentItem ci = null;
			if(ci_kiwiid != null) {
				ci = contentItemService.getContentItemByKiwiId(ci_kiwiid);
			}
				
			if(ci != null) {				
				// add a triple in the triplestore connecting the two contentitems with kiwi:internalLink
				tripleStore.createTriple(context, propImg, ci.getResource());
				
				log.debug("added internal image relation from #0 to #1", context.getKiwiIdentifier(), ci.getResource().getKiwiIdentifier());
			}
						
			
		}

		
		return content;
	}

}
