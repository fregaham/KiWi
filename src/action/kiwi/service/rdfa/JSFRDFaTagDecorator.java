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

import java.util.ArrayList;
import java.util.Arrays;

import nu.xom.Attribute;
import nu.xom.Element;

import com.sun.facelets.tag.Tag;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagDecorator;

/**
 * JSFRDFaTagDecorator - a TagDecorator implementation taking care of the JSF+RDFa attributes.
 * <p>
 * Parses the tags in a JSF+RDFa file and replaces all RDFa annotations by appropriate JSF/EL calls to the
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
 * JSF Tag Decorators have no context and JSF supports no variable name shadowing, so we have a problem
 * with iteration constructs. In iteration constructs it is therefore currently necessary to explicitly
 * give a variable name using the "ctx" attribute ... if no such attribute is given, the default
 * context "ctx" is used, referring to the main context.
 * 
 * TODO: namespace substitution, support for arbitrary HTML elements, support for further attributes like
 * about, href, ...
 *
 * @author Sebastian Schaffert
 *
 */
public class JSFRDFaTagDecorator implements TagDecorator {

	private static long counter = 0;
	
    /**
     * If handled, return a new Tag instance, otherwise return null
     * 
     * @param tag
     *            tag to be decorated
     * @return a decorated tag, otherwise null
	 * @see com.sun.facelets.tag.TagDecorator#decorate(com.sun.facelets.tag.Tag)
	 */
	@Override
	public Tag decorate(Tag tag) {
		
		TagAttribute property = tag.getAttributes().get("property");
		TagAttribute rel      = tag.getAttributes().get("rel");
		TagAttribute rev 	  = tag.getAttributes().get("rev");
		
		// TODO: variable "ctx" is currently passed as ui:param in the JSF including the different
		// perspectives; it should rather be set depending on whether inside an iteration or not ...
		String contextEl, varEl;
		
		if(tag.getAttributes().get("ctx") != null) {
			contextEl = tag.getAttributes().get("ctx").getValue();
		} else {
			contextEl = "ctx";
		}
		if(tag.getAttributes().get("var") != null) {
			varEl = tag.getAttributes().get("var").getValue();
		} else {
			varEl = "ctx";
		}
		
		
		if(property != null) {
			// tag has an RDFa property attribute, we replace it by appropriate JSF value binding to JSFRDFaService
			// using EL
			
			
			ArrayList<TagAttribute> attrs = new ArrayList<TagAttribute>();

				
			
			if(isIterationConstruct(tag)) {
				// for iteration constructs, add a new value attribute listing all literals and add a new variable construct with a fresh variable
				TagAttribute value = new TagAttribute(tag.getLocation(),
													  "","value","value",
												      "#{"+contextEl+".listLiterals('"+property.getValue()+"')}");
				TagAttribute var   = new TagAttribute(tag.getLocation(), "", "var", "var", varEl);
				
				attrs.add(value);
				attrs.add(var);
				
			} else {
				// for non-iteration constructs, add a new value attribute 
				TagAttribute value = new TagAttribute(tag.getLocation(),
						  							  "","value","value",
						  							  "#{"+contextEl+".getLiteral('"+property.getValue()+"').value}");
				attrs.add(value);
			}
			for(TagAttribute attr : tag.getAttributes().getAll()) {
				if(!"value".equals(attr.getLocalName()) &&
				   !"ctx".equals(attr.getLocalName()) &&
				   !"var".equals(attr.getLocalName())) {
					attrs.add(attr);
				}
			}
			return new Tag(tag,new TagAttributes(attrs.toArray(new TagAttribute[0])));
		} else if(rel != null && !isHtmlLinkType(rel.getValue())) {
			// an object relation to another resource (non-property)
			
			ArrayList<TagAttribute> attrs = new ArrayList<TagAttribute>();
			
			if(isIterationConstruct(tag)) {
				// for iteration constructs, add a new value attribute listing all literals and add a new variable construct with a fresh variable
				TagAttribute value = new TagAttribute(tag.getLocation(),
													  "","value","value",
												      "#{"+contextEl+".listObjects('"+rel.getValue()+"')}");
				TagAttribute var   = new TagAttribute(tag.getLocation(), "", "var", "var", varEl);
				
				attrs.add(value);
				attrs.add(var);
				
			} else {
				// for non-iteration constructs, add a new value attribute 
				TagAttribute value = new TagAttribute(tag.getLocation(),
						  							  "","value","value",
						  							  "#{"+contextEl+".getObject('"+rel.getValue()+"').value}");
				TagAttribute resource = new TagAttribute(tag.getLocation(),
						  "","resource","resource",
						  "#{"+contextEl+".getObject('"+rel.getValue()+"').value}");
				attrs.add(value);
				attrs.add(resource);
			}
			for(TagAttribute attr : tag.getAttributes().getAll()) {
				if(!"value".equals(attr.getLocalName()) &&
				   !"ctx".equals(attr.getLocalName()) &&
				   !"resource".equals(attr.getLocalName()) &&
				   !"var".equals(attr.getLocalName())) {
					attrs.add(attr);
				}
			}
			return new Tag(tag,new TagAttributes(attrs.toArray(new TagAttribute[0])));
			
		}
		else if(rev != null && !isHtmlLinkType(rev.getValue())) {
			// an object relation to another resource (non-property)
			
			ArrayList<TagAttribute> attrs = new ArrayList<TagAttribute>();
			
			if(isIterationConstruct(tag)) {
				// for iteration constructs, add a new value attribute listing all literals and add a new variable construct with a fresh variable
				TagAttribute value = new TagAttribute(tag.getLocation(),
													  "","value","value",
												      "#{"+contextEl+".listSubjects('"+rev.getValue()+"')}");
				TagAttribute var   = new TagAttribute(tag.getLocation(), "", "var", "var", varEl);
				
				attrs.add(value);
				attrs.add(var);
				
			} else {
				// for non-iteration constructs, add a new value attribute 
				TagAttribute value = new TagAttribute(tag.getLocation(),
						  							  "","value","value",
						  							  "#{"+contextEl+".getSubject('"+rev.getValue()+"').value}");
				TagAttribute resource = new TagAttribute(tag.getLocation(),
						  "","resource","resource",
						  "#{"+contextEl+".getSubject('"+rev.getValue()+"').value}");
				attrs.add(value);
				attrs.add(resource);
			}
			for(TagAttribute attr : tag.getAttributes().getAll()) {
				if(!"value".equals(attr.getLocalName()) &&
				   !"ctx".equals(attr.getLocalName()) &&
				   !"resource".equals(attr.getLocalName()) &&
				   !"var".equals(attr.getLocalName())) {
					attrs.add(attr);
				}
			}
			return new Tag(tag,new TagAttributes(attrs.toArray(new TagAttribute[0])));
			
		}
		
		// TODO Auto-generated method stub
		return null;
	}

	private final static String getContextEL(TagAttributes attrs, String contextVar, String contextURI) {
		// find out how to address the current context in EL
		// precedence: new about > current context variable > old/outer about
		TagAttribute about = attrs.get("about");
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

	/**
	 * Check whether an element is an iteration construct or not.
	 * @param el
	 * @return
	 */
	private final static boolean isIterationConstruct(Tag el) {
		
		// ui:repeat
		if("repeat".equals(el.getLocalName()) && "http://java.sun.com/jsf/facelets".equals(el.getNamespace())) {
			return true;
		// rich:dataTable and other datatables
		} else if("dataTable".equals(el.getLocalName())) {
			return true;
		} else if("forEach".equals(el.getLocalName()) && "http://java.sun.com/jstl/core".equals(el.getNamespace())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	private final static boolean isHtmlLinkType(String rel) {
		String s = rel.toLowerCase();
		return "alternate".equals(s) ||
		       "stylesheet".equals(s) ||
		       "start".equals(s) ||
		       "next".equals(s) ||
		       "prev".equals(s) ||
		       "contents".equals(s) ||
		       "index".equals(s) ||
		       "glossary".equals(s) ||
		       "copyright".equals(s) ||
		       "chapter".equals(s) ||
		       "section".equals(s) ||
		       "subsection".equals(s) ||
		       "appendix".equals(s) ||
		       "help".equals(s) ||
		       "bookmark".equals(s) ||
		       "shortcut icon".equals(s);
		       
	}

}
