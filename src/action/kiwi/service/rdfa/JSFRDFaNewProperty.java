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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import kiwi.api.rdfa.JSFRDFaException;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.kbase.KiWiAnonResource;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;

import org.jboss.seam.Component;

/**
 * JSFRDFaNewProperty
 *
 * @author Sebastian Schaffert
 *
 */
public class JSFRDFaNewProperty extends JSFRDFaNode {

	public enum TYPE {
		LITERAL, URI, ANON
	}
	
	private KiWiNode subject, object;
	private KiWiUriResource property;
	private String property_uri;
	private Object content;
	private TYPE type;
	private KiWiTriple triple;
	
	private String language;
	
	/**
	 * This constructor takes care of a special case where the JSFRDFaProperty is not yet in existance. The constructor
	 * does not yet create a triple, this is performed when setValue is called.
	 * 
	 * @param node
	 * @param property_uri
	 */
	protected JSFRDFaNewProperty(KiWiNode node, String property_uri, TYPE t) throws JSFRDFaException {
		super();
		
		this.subject = node;
		this.property_uri = property_uri;
		this.type = t;
		checkSave();
	}

	protected JSFRDFaNewProperty(KiWiNode node, TYPE t) {
		super();
		
		this.subject = node;
		this.type = t;
		if(t == TYPE.LITERAL) {
			// new literals are always created as empty
			this.content = "";
		}
	}
	
	
	/**
	 * Set the value for the property to be created. Will create a new object
	 * @param content
	 */
	public void setValue(Object content) throws JSFRDFaException {
		this.content = content;
		checkSave();
	}
	
	public void setLanguage(String language) {
		this.language = language;
		if(this.object != null && this.object.isLiteral()) {
			((KiWiLiteral)this.object).setLanguage(new Locale(language));
		}
	}
	
	private void checkValidURI(String uri) throws JSFRDFaException {
		try {
			URI u = new URI(uri);
		} catch (URISyntaxException e) {
			throw new JSFRDFaException(uri+" is not a valid value for a resource URI");
		}
	}

	/**
	 * Check whether the new relation is ready for saving, and if yes, store it in the triple store.
	 */
	private void checkSave() throws JSFRDFaException {
		TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
		
		// indicates whether object and/or property have really been updated
		boolean updated = false;
		
		// create new property resource using the property_uri given
		if(property_uri != null && !(property != null && property_uri.equals(property.getUri()))) {
			this.property = ts.createUriResource(property_uri);
			updated = true;
		}
		
		
		// create new object depending on type and content
		if( content != null ) {
			switch(type) {
			case LITERAL:
				if(object == null || !content.equals(((KiWiLiteral)object).getContent())) {
					if(language != null) {
						this.object = ts.createLiteral(content,new Locale(language), ts.getXSDType(content.getClass()));
					} else {
						this.object = ts.createLiteral(content);
					}
					updated = true;
				}
				break;
			case URI:
				if(object == null || !content.equals(((KiWiUriResource)object).getUri())) {
					checkValidURI((String)content);
					this.object = ts.createUriResource((String)content);
					updated = true;
				}
				break;
			case ANON:
				if(object == null || !content.equals(((KiWiAnonResource)object).getAnonId())) {
					this.object = ts.createAnonResource((String)content);
					updated = true;
				}
				break;
			}
			
		}
		
		// remove any existing relation in case this wrapper has already been used
		if(this.triple != null && updated) {
			ts.removeTriple(this.triple);	
		}
		
		// create new relation using the data in property_uri and content
		if(property != null && object != null && updated) {
			this.triple = ts.createTriple((KiWiResource)subject, this.property, this.object);			
		}
		
	}
	
	/**
	 * @return the property_uri
	 */
	public JSFRDFaNode getProperty() {
		if(this.property != null)
			return new JSFRDFaNode(this.property);
		else
			return null;
	}

	/**
	 * Update the property of the relation represented by this wrapper. Will remove any previously
	 * created relation.
	 * 
	 * @param property_uri the property_uri to set
	 */
	public void setProperty(JSFRDFaNode n) throws JSFRDFaException {
		if(n != null) {			
			checkValidURI(n.getValue().toString());
			this.property_uri = n.getValue().toString();
			checkSave();
		}
	}

	/**
	 * @return the object
	 */
	public JSFRDFaNode getObject() {
		if(this.object != null)
			return new JSFRDFaNode(object);
		else
			return null;
	}

	/**
	 * @param object the object to set
	 */
	public void setObject(JSFRDFaNode object) throws JSFRDFaException {
		setValue(object.getValue().toString());
		checkSave();
	}

	
}
