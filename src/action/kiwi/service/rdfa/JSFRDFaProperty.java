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

import kiwi.api.event.KiWiEvents;
import kiwi.api.rdfa.JSFRDFaException;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiTriple;

import org.jboss.seam.Component;
import org.jboss.seam.core.Events;

/**
 * JSFRDFaProperty - an implementation of JSFRDFaNode with EL update capability (i.e., implements the setValue
 * method in addition to the getValue method)...
 *
 * @author Sebastian Schaffert
 *
 */
public class JSFRDFaProperty extends JSFRDFaNode {

	private KiWiTriple triple;
	
	/**
	 * This constructor is the default case where the JSFRDFaProperty is initialised from an existing RDF relation.
	 * 
	 * @param triple
	 */
	protected JSFRDFaProperty(KiWiTriple triple) {
		super(triple.getObject());
		
		this.triple = triple;
	}

	
	public JSFRDFaNode getProperty() {
		return new JSFRDFaNode(triple.getProperty());
	}
	
	
	/**
	 * Update the value of the wrapped property. Depending on the type of the object, this is done by creating a
	 * new version of the object with updated value and then replacing the old triple with a new triple pointing
	 * to the new version of the object.
	 * 
	 * In case of literals, the values for language and type are retained.
	 * 
	 * @param content the updated content (literal content, URI, or anonymous ID)
	 * @throws JSFRDFaException if the object is a URI resource but the given content is not a valid URI
	 */
	public void setValue(Object content) throws JSFRDFaException {
		TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
		
		KiWiNode newObject; 
		if(triple.getObject().isLiteral()) {
			KiWiLiteral l = (KiWiLiteral) triple.getObject();
			newObject = ts.createLiteral(content, l.getLanguage(), l.getType());
		} else if(triple.getObject().isAnonymousResource()) {
			newObject = ts.createAnonResource((String)content);
		} else if(triple.getObject().isUriResource()) {
			checkValidURI((String)content);
			newObject = ts.createUriResource((String)content);
		} else {
			throw new JSFRDFaException("object is neither a literal nor a resource; a strange bug has occurred!");
		}
		
		ts.removeTriple(this.triple);
		this.triple = ts.createTriple(triple.getSubject(), triple.getProperty(), newObject);
		this.kiwinode = newObject;
		
		Events.instance().raiseEvent(KiWiEvents.METADATA_UPDATED, triple.getSubject().getContentItem());
	}
	
	/**
	 * In case of literals, update the language of the content. The original triple is removed and replaced by a new 
	 * triple.
	 * 
	 * @param content
	 * @throws JSFRDFaException
	 */
	public void setLanguage(String content) throws JSFRDFaException {
		if(triple.getObject().isLiteral()) {
			TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
			KiWiLiteral l = (KiWiLiteral) triple.getObject();
			KiWiNode newObject;
			if(content.equals("any")) {
				newObject = ts.createLiteral(content, null, l.getType());
			} else {
				newObject = ts.createLiteral(content, new Locale(content), l.getType());
			}
			ts.removeTriple(this.triple);
			this.triple = ts.createTriple(triple.getSubject(), triple.getProperty(), newObject);
			this.kiwinode = newObject;
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
	 * @return the triple
	 */
	public KiWiTriple getTriple() {
		return triple;
	}
	
	
	
}
