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
package kiwi.admin.action;

import java.util.LinkedList;
import java.util.List;

import kiwi.api.triplestore.TripleStore;
import kiwi.model.kbase.KiWiNamespace;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

/**
 * NamespaceConfigurationAction - update namespace associations in KiWi
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.admin.namespaceConfigurationAction")
@Scope(ScopeType.PAGE)
public class NamespaceConfigurationAction {

	
	@Logger
	private Log log;

	@In
	private FacesMessages facesMessages;
	
	@In
	private TripleStore tripleStore;
	
	
	private List<KiWiNamespace> namespaces;


	private String nsPrefix, nsUri;
	
	
	/**
	 * @return the namespaces
	 */
	public List<KiWiNamespace> getNamespaces() {
		
		if(namespaces == null) {
			namespaces = new LinkedList<KiWiNamespace>();
			for(KiWiNamespace ns : tripleStore.listNamespaces()) {
				namespaces.add(ns);
			}
		}
		return namespaces;
	}


	/**
	 * @param namespaces the namespaces to set
	 */
	public void setNamespaces(List<KiWiNamespace> namespaces) {
		this.namespaces = namespaces;
	}
	
	
	
	
	/**
	 * @return the nsPrefix
	 */
	public String getNsPrefix() {
		return nsPrefix;
	}


	/**
	 * @param nsPrefix the nsPrefix to set
	 */
	public void setNsPrefix(String nsPrefix) {
		this.nsPrefix = nsPrefix;
	}


	/**
	 * @return the nsUri
	 */
	public String getNsUri() {
		return nsUri;
	}


	/**
	 * @param nsUri the nsUri to set
	 */
	public void setNsUri(String nsUri) {
		this.nsUri = nsUri;
	}

	/**
	 * Update the namespace passed as parameter; called by user interface.
	 * @param ns
	 */
	public void updateNamespace(KiWiNamespace ns) {
		tripleStore.setNamespace(ns);
	}
	
	/**
	 * Called by submission of the add namespace form, uses nsPrefix and nsUri values.
	 */
	public void addNamespace() {
		// first check whether namespace prefix has already been used
		if(tripleStore.getNamespace(nsPrefix) != null) {
			facesMessages.add("Namespace prefix #0 is already in use - please use a different prefix",nsPrefix);
			nsPrefix = "";
		} else if(tripleStore.getNamespacePrefix(nsUri) != null) {
			facesMessages.add("URI #0 is already associated with namespace prefix #1",nsUri, tripleStore.getNamespacePrefix(nsUri));
			nsPrefix = "";
			nsUri = "";
		} else {
			tripleStore.setNamespace(nsPrefix,nsUri);
			nsPrefix = "";
			nsUri = "";
		}
		
	}
	
	public void deleteNamespace(KiWiNamespace ns) {
		tripleStore.removeNamespace(ns);
	}
}
