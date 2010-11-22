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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.persistence.EntityManager;

import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiTriple;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * JSFRDFaEntityConverter - converts string representations of JSFRDFaNodes to objects by looking them up
 * in the triple store based on the KiWi-ID.
 *
 * @author Sebastian Schaffert
 *
 */
@Name("jsfRdfaEntityConverter")
@BypassInterceptors
@org.jboss.seam.annotations.faces.Converter
public class JSFRDFaEntityConverter implements Converter {

	/* (non-Javadoc)
	 * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
	 */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		if(arg2.startsWith("p::")) {
			String tripleId = arg2.substring(4);
			
			EntityManager     em = (EntityManager)     Component.getInstance("entityManager");
			em.joinTransaction();

			return new JSFRDFaProperty(em.find(KiWiTriple.class, Long.parseLong(tripleId)));
		} else if(arg2.startsWith("n::")) {
			String nodeId = arg2.substring(4);
			
			EntityManager     em = (EntityManager)     Component.getInstance("entityManager");
			em.joinTransaction();

			return new JSFRDFaNode(em.find(KiWiNode.class, Long.parseLong(nodeId)));
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
	 */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		if(arg2 instanceof JSFRDFaProperty) {
			JSFRDFaProperty p = (JSFRDFaProperty)arg2;
			
			return "p::"+p.getTriple().getId();
		} else if(arg2 instanceof JSFRDFaNode) {
			JSFRDFaNode     n = (JSFRDFaNode)arg2;
			
			return "n::"+n.kiwinode.getId();
		}
		return null;
	}

}
