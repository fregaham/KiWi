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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import kiwi.api.query.KiWiQueryResult;
import kiwi.api.query.QueryService;
import kiwi.api.rdfa.JSFRDFaException;
import kiwi.api.rdfa.JSFRDFaServiceLocal;
import kiwi.api.rdfa.JSFRDFaServiceRemote;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiUriResource;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;

/**
 * The JSFRDFaService provides access to the RDF context from JSF templates. The RDF context offers the following 
 * EL expressions:
 * <ul>
 *   <li>rdf.getResource(URI) returns a suitable representation of the resource identified by the given URI</li>
 * </ul>
 *
 * @author Sebastian Schaffert
 *
 */
@Name("rdf")
@Stateless
@Scope(ScopeType.STATELESS)
@AutoCreate
@Transactional
public class JSFRDFaServiceImpl implements JSFRDFaServiceLocal, JSFRDFaServiceRemote {

	@Logger
	private Log log;
	
	@In
	private TripleStore tripleStore;
	
	/**
	 * Return a JSF+RDFa wrapper around the resource identified by the given URI.
	 * 
	 * @param uri
	 * @return
	 */
	@Override
	public JSFRDFaNode getResource(String uri) {
		KiWiUriResource r = tripleStore.createUriResource(uri);
		
		return new JSFRDFaNode(r);
	}

	/**
	 * Return a JSF+RDFa wrapper around the resource of the currently active content item (currentContentItem).
	 * 
	 * @return
	 */
	@Override
	public JSFRDFaNode getCurrentResource() {
		ContentItem currentContentItem = (ContentItem)Component.getInstance("currentContentItem");
		return new JSFRDFaNode(currentContentItem.getResource());
	}

	/**
	 * Return a list of wrappers around RDF resources and literals determined by a query in one of the languages 
	 * supported by KiWi (e.g. SPARQL, KWQL, KIWI). The items in the list themselves are read only.
	 * 
	 * @param query
	 * @param language
	 * @return
	 */
	@Override
	public List<JSFRDFaNode> query(String query, String language) throws JSFRDFaException {
		QueryService qs = (QueryService)Component.getInstance("queryService");
		
		KiWiQueryResult kqr = qs.query(query, language);
		
		if(kqr.getColumnTitles().size() != 1) {
			throw new JSFRDFaException("Ambiguous query results in query \""+query+"\". Please specify exactly one variable for projection in the query.");
		} else {
			
			String title = kqr.getColumnTitles().get(0);
			
			List<JSFRDFaNode> result = new LinkedList<JSFRDFaNode>();
			for(Map<String,Object> row : kqr) {
				if(row.get(title) instanceof KiWiNode) {
					result.add(new JSFRDFaNode((KiWiNode)row.get(title)));
				} else if(row.get(title) instanceof ContentItem) {
					result.add(new JSFRDFaNode(((ContentItem)row.get(title)).getResource()));					
				} else {
					log.warn("result item for query \"#0\" is not a KiWiNode or ContentItem - please check query");
				}
			}
			return result;
		}
		
		
	}

	/* (non-Javadoc)
	 * @see kiwi.api.rdfa.JSFRDFaService#removeRelation(kiwi.service.rdfa.JSFRDFaProperty)
	 */
	@Override
	public void removeRelation(JSFRDFaProperty prop) {
		log.info("removing triple #0", prop.getTriple().toString());
		
		tripleStore.removeTriple(prop.getTriple());
		
		// commit transaction
		try {
			Transaction.instance().commit();
		} catch (SecurityException e) {
			log.error("security exception while committing transaction", e);
		} catch (IllegalStateException e) {
			log.error("illegal state exception while committing transaction", e);
		} catch (RollbackException e) {
			log.error("rollback exception while committing transaction", e);
		} catch (HeuristicMixedException e) {
			log.error("heuristic mixed exception while committing transaction", e);
		} catch (HeuristicRollbackException e) {
			log.error("heuristic rollback exception while committing transaction", e);
		} catch (SystemException e) {
			log.error("system exception while committing transaction", e);
		}
	}

	/* (non-Javadoc)
	 * @see kiwi.api.rdfa.JSFRDFaService#addRelation()
	 */
	@Override
	public void addRelation()  {
		// flush newProperty environment variable
		Contexts.getPageContext().remove("newProperty");
		
		// commit transaction
		try {
			Transaction.instance().commit();
		} catch (SecurityException e) {
			log.error("security exception while committing transaction", e);
		} catch (IllegalStateException e) {
			log.error("illegal state exception while committing transaction", e);
		} catch (RollbackException e) {
			log.error("rollback exception while committing transaction", e);
		} catch (HeuristicMixedException e) {
			log.error("heuristic mixed exception while committing transaction", e);
		} catch (HeuristicRollbackException e) {
			log.error("heuristic rollback exception while committing transaction", e);
		} catch (SystemException e) {
			log.error("system exception while committing transaction", e);
		}
	}

	
}
