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
package kiwi.action.ui.converter;

import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import kiwi.model.kbase.KiWiResource;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;

/**
 * KiWiResourceConverter. Converts resources of the form "title (namespace prefix)" into KiWi resources.
 * Used in the user interface e.g. in autocomplete fields.
 *
 * The mappings have to be stored in the page context variable resourceMappings before the converter
 * can be used. This is e.g. done by OntologyService.autocomplete.
 *
 * @author Sebastian Schaffert0
 *
 */
@Name("kiwiResourceConverter")
@BypassInterceptors
@org.jboss.seam.annotations.faces.Converter
public class KiWiResourceConverter implements Converter {

	private static Pattern p_title_namespace = Pattern.compile("(.+)\\s\\(([^\\)]*)\\)");
	
	/* (non-Javadoc)
	 * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
	 */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
	
		Map<String,KiWiResource> mapping = (Map<String,KiWiResource>)Component.getInstance("resourceMappings");

		if(mapping != null) {
			return mapping.get(arg2);
		} else {
			Log log              = Logging.getLog(KiWiEntityConverter.class);
			log.warn("'resourceMappings' not found in context; maybe the converter has been used improperly?",arg2);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
	 */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		if(arg2 instanceof KiWiResource) {
			KiWiResource r = (KiWiResource)arg2;
			return r.getLabel() + " (" + r.getNamespacePrefix() + ")";
		} else {
			Log log              = Logging.getLog(KiWiEntityConverter.class);
			log.warn("passed entity was not a KiWi resource: #0",arg2);
			
			return null;
		}
	}

}
