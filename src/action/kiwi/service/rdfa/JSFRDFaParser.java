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
 * sschaffe
 * 
 */
package kiwi.service.rdfa;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;

/**
 * JSFRDFaUtil
 *
 * @author Sebastian Schaffert
 *
 */
public class JSFRDFaParser {

    private static final LogProvider log = Logging.getLogProvider(JSFRDFaParser.class);
    
    private static long serial = 0;
        
	/**
	 * Parse a JSF+RDFa file and replace all RDFa annotations by appropriate JSF/EL calls to the
	 * JSFRDFaService.
	 * <p>
	 * Performs the following substitutions:
	 * <ul>
	 * <li>property="URI" to either value="#{rdf.getCurrentResource().listLiterals(URI)}" for iteration constructs or
	 *     value="#{rdf.getCurrentResource().getLiteral(URI).value}" for ordinary constructs</li>
	 * <li>rel="URI" to either value="#{rdf.getCurrentResource().listObjects(URI)" for iteration constructs or
	 *     value="#{rdf.getCurrentResource().getObject(URI).value" for ordinary constructs</li>
	 * </ul>
	 * 
	 * TODO: namespace substitution, support for arbitrary HTML elements, support for further attributes like
	 * about, href, ...
	 * 
	 * @param data
	 * @return
	 */
	public static String parseJSFRDFa(String data) {
		Builder builder = new Builder();
		try {
			Document doc = builder.build(new StringReader(data));
			
			parseJSFRDFa(doc.getRootElement(),null,null);
			
			return doc.toXML();
		} catch (Exception e) {
			log.info("invalid JSF+RDFa data passed to parser");
			return data;
		}
	}

	public static byte[] parseJSFRDFa(byte[] bytes) {
		Builder builder = new Builder();
		try {
			Document doc = builder.build(new ByteArrayInputStream(bytes));
			
			parseJSFRDFa(doc.getRootElement(),null,null);
			
			return doc.toXML().getBytes();
		} catch (Exception e) {
			log.info("invalid JSF+RDFa data passed to parser");
			return bytes;
		}
	}

	
	private static void parseJSFRDFa(Element el, String contextVar, String contextURI) {
		Attribute property = el.getAttribute("property");
		if(property != null) {
			el.removeAttribute(property);
			
			String contextEl = getContextEL(el,contextVar, contextURI);
			
			Attribute oldvalue = el.getAttribute("value");
			if(oldvalue != null) {
				el.removeAttribute(oldvalue);
			}
			
			
			if(isIterationConstruct(el)) {
				// for iteration constructs, add a new value attribute listing all literals and add a new variable construct with a fresh variable
				Attribute newvalue = new Attribute("value","#{"+contextEl+".listLiterals('"+property.getValue()+"')}");
				el.addAttribute(newvalue);
			
				// iteration variable overrides setting of contextUrl
				contextURI = null;
				contextVar = "v"+(serial++);
				
				Attribute var = new Attribute("var",contextVar);
				el.addAttribute(var);
				
			} else {
				// for non-iteration constructs, add a new value attribute 
				Attribute newvalue = new Attribute("value","#{"+contextEl+".getLiteral('"+property.getValue()+"').value}");
				el.addAttribute(newvalue);
				
				log.info("adding new value "+newvalue.getValue());
			}
			
		}
		
		// iterate over child elements recursively
		Elements childs = el.getChildElements();
		
		for(int i=0; i<childs.size(); i++) {
			parseJSFRDFa(childs.get(i), contextVar, contextURI);
		}
	}
	
	/**
	 * Check whether an element is an iteration construct or not.
	 * @param el
	 * @return
	 */
	private static boolean isIterationConstruct(Element el) {
		
		// ui:repeat
		if(el.getLocalName().equals("repeat") && el.getNamespaceURI().equals("http://java.sun.com/jsf/facelets")) {
			return true;
		// rich:dataTable and other datatables
		} else if(el.getLocalName().equals("dataTable")) {
			return true;
		} else if(el.getLocalName().equals("forEach") && el.getNamespaceURI().equals("http://java.sun.com/jstl/core")) {
			return true;
		} else {
			return false;
		}
		
	}
	
	
	private static String getContextEL(Element e, String contextVar, String contextURI) {
		// find out how to address the current context in EL
		// precedence: new about > current context variable > old/outer about
		Attribute about = e.getAttribute("about");
		if(about != null) {
			contextURI = about.getValue(); 
			contextVar = null;
		}
		String contextEl = null;
		if(contextURI != null) {
			contextEl = "rdf.getResource('"+contextURI+"')";
		} else if(contextVar != null) {
			contextEl = contextVar;
		} else {
			// if both contexts are null, the current resource is the context
			contextEl = "rdf.getCurrentResource()";
		}
		return contextEl;
	}
}
