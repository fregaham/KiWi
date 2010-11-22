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

import static kiwi.service.rdfa.JSFRDFaNewProperty.TYPE.LITERAL;
import static kiwi.service.rdfa.JSFRDFaNewProperty.TYPE.URI;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import kiwi.api.ontology.OntologyService;
import kiwi.api.rdfa.JSFRDFaException;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.kbase.KiWiAnonResource;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiProperty;
import kiwi.service.entity.FacadeUtils;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Contexts;


/**
 * JSFRDFaNode - a wrapper around KiWiNode instances (literals and resources) suitable for easy use in JSF/EL
 * expressions. Offers the following EL expressions ("node" being the current JSFRDFaNode):
 * <ul>
 * <li>node.value - in case of literals: the literal value; in case of URI resources: the URI of the resource;
 *     in case of anonymous resources: the ID of the node.</li>
 * <li>node.label - return a human-readable String describing the node; corresponds to the rdfs:label or dc:title of the node
	   and is only applicable to resources</li>
 * <li>node.language - in case of literals: the language of the literal as ISO string; in case of resources: null</li>
 * <li>node.type - in case of literals: a suitable wrapper representing the type of the literal; in case of 
 *     resources: a randomly selected type associated with the resource or null if there is no type</li>
 * <li>node.listTypes() - like node.type, but returns an iterable list of all types</li>
 * <li>node.getLiteral(property_uri) - return a suitable wrapper around the (single/first) literal reachable from 
 *     the current node via the relation property_uri</li>
 * <li>node.listLiterals(property_uri) - return a list of suitable wrappers around all literals reachable from
 *     the current node via the relation property_uri</li>
 * <li>node.getObject(property_uri) - return a suitable wrapper around the (single/first) resource reachable from
 *     the current node via the relation property_uri</li>
 * <li>node.listObjects(property_uri) - return a list of suitable wrappers around all resources reachable from
 *     the current node via the relation property_uri</li>
 *    
 * </ul>
 *
 * JSFRDFaNode is read only, as it does not make sense to change values without a triple context. The subclass
 * JSFRDFaProperty provides updating capabilities by adding the triple context.
 *
 *
 * @author Sebastian Schaffert
 *
 */
public class JSFRDFaNode {

	protected KiWiNode kiwinode;
	
	private List<JSFRDFaNode> possibleLiteralProperties = null, possibleObjectProperties = null;
	
	
	protected JSFRDFaNode() {
		this.kiwinode = null;
	}
	
	protected JSFRDFaNode(KiWiNode node) {
		this.kiwinode = node;
	}
	
	
	/**
	 * Return the value of this KiWiNode.
	 * <ul>
	 * <li>for KiWiLiteral, it returns the literal content of the node</li>
	 * <li>for KiWiUriResource, it returns the URI of the node</li>
	 * <li>for KiWiAnonResource, it returns the anonymous ID of the node</li>
	 * </ul>
	 * @return
	 */
	public Object getValue() {
		if(kiwinode == null) {
			return null;
		} else if(kiwinode.isLiteral()) {
			KiWiLiteral l = (KiWiLiteral)kiwinode;
			
			if(l.getType().isUriResource()) {
				KiWiUriResource t = (KiWiUriResource)l.getType();
				
				if(t.getUri().equals(Constants.NS_XSD+"dateTime")) {
					return FacadeUtils.transformToBaseType(l.getContent(), Date.class);
				} else {
					return l.getContent();
				}
			} else {
			
				return l.getContent();
			}
		} else if(kiwinode.isUriResource()) {
			KiWiUriResource r = (KiWiUriResource)kiwinode;
			return r.getUri();
		} else if(kiwinode.isAnonymousResource()) {
			KiWiAnonResource a = (KiWiAnonResource)kiwinode;
			return a.getAnonId();
		} else {
			// should not happen, as there is no subclass that returns false on all three methods
			return null;
		}
	}
	
	/**
	 * Return a human-readable String describing the node; corresponds to the rdfs:label or dc:title of the node
	 * and is only applicable to resources.
	 * 
	 * @return
	 */
	public String getLabel() {
		if(kiwinode != null && kiwinode instanceof KiWiResource) {
			KiWiResource r = (KiWiResource)kiwinode;
			return r.getLabel();
		} else {
			return null;
		}
	}
	
	/**
	 * Return the namespace prefix of the node.
	 * @return
	 */
	public String getPrefix() {
		if(kiwinode != null && kiwinode instanceof KiWiResource) {
			KiWiResource r = (KiWiResource)kiwinode;
			return r.getNamespacePrefix();
		} else {
			return null;
		}		
	}
	
	/**
	 * Return the language value for literal nodes; returns null in all other cases.
	 * @return
	 */
	public String getLanguage() {
		if(kiwinode == null) {
			return null;
		} else if(kiwinode.isLiteral()) {
			KiWiLiteral l = (KiWiLiteral)kiwinode;
			if(l.getLanguage() != null) {
				return l.getLanguage().getLanguage();
			} else {
				return "any";
			}
		} else {
			return null;
		}
		
	}
	
	/**
	 * Return the JSFRDFaProperty wrapper around the literal that is reachable from the current node via the 
	 * relation identified by property_uri.
	 * 
	 * @param property_uri
	 * @return
	 */
	public JSFRDFaNode getLiteral(String property_uri) throws JSFRDFaException {
		if(kiwinode != null) {
			if(kiwinode.isUriResource() || kiwinode.isAnonymousResource()) {
				Iterator<KiWiTriple> literals;
				try {
					literals = ((KiWiResource)kiwinode).listOutgoing(property_uri, 1, false).iterator();
					if(literals.hasNext()) {
						KiWiTriple t = literals.next();
						if(! (t.getObject().isLiteral())) {
							throw new JSFRDFaException("property does not point to a literal");
						} else {				
							return new JSFRDFaProperty(t);
						}
					} else {
						return new JSFRDFaNewProperty(kiwinode,property_uri, LITERAL);
					}
				} catch (NamespaceResolvingException e) {
					throw new JSFRDFaException("could not resolve namespace",e);
				}
			} else {
				throw new JSFRDFaException("getLiteral is only applicable to resources");
			}
		} else {
			throw new JSFRDFaException("getLiteral must be called on existing resources");
		}
	}
	
	/**
	 * Return an iterable list of JSFRDFaProperty wrappers around the literals reachable from the current node
	 * via any kind of relation.
	 */
	public List<JSFRDFaProperty> listLiterals() throws JSFRDFaException {
		return listLiterals(null);
	}	
	
	/**
	 * Return an iterable list of JSFRDFaProperty wrappers around the literals reachable from the current node
	 * via the relation identified by property_uri.
	 */
	public List<JSFRDFaProperty> listLiterals(String property_uri) throws JSFRDFaException {
		if(kiwinode != null) {
			if(kiwinode.isUriResource() || kiwinode.isAnonymousResource()) {
				try {
					
					LinkedList<JSFRDFaProperty> result = new LinkedList<JSFRDFaProperty>();
					
					for(KiWiTriple t : ((KiWiResource)kiwinode).listOutgoing(property_uri)) {
						if(t.getObject().isLiteral()) {
							result.add(new JSFRDFaProperty(t));
						}
						
					}
					return result;
					
				} catch (NamespaceResolvingException e) {
					throw new JSFRDFaException("could not resolve namespace",e);
				}
			} else {
				throw new JSFRDFaException("listLiterals is only applicable to resources");
			}
		} else {
			throw new JSFRDFaException("listLiterals must be called on existing resources");			
		}
	}
	
	
	/**
	 * Return the JSFRDFaProperty wrapper around the resource that is reachable from the current node via the 
	 * relation identified by property_uri.
	 * 
	 * @param property_uri
	 * @return
	 */
	public JSFRDFaNode getObject(String property_uri) throws JSFRDFaException {
		if(kiwinode != null) {
			if(kiwinode.isUriResource() || kiwinode.isAnonymousResource()) {
				Iterator<KiWiTriple> objects;
				try {
					objects = ((KiWiResource)kiwinode).listOutgoing(property_uri, 1, false).iterator();
					if(objects.hasNext()) {
						KiWiTriple t = objects.next();
						if(! (t.getObject().isUriResource() || t.getObject().isAnonymousResource())) {
							throw new JSFRDFaException("property does not point to a resource");
						} else {				
							return new JSFRDFaProperty(t);
						}
					} else {
						return new JSFRDFaNewProperty(kiwinode,property_uri, URI);
					}
				} catch (NamespaceResolvingException e) {
					throw new JSFRDFaException("could not resolve namespace",e);
				}
			} else {
				throw new JSFRDFaException("getObject is only applicable to resources");
			}
		} else {
			throw new JSFRDFaException("getObject must be called on existing resources");
		}
		
	}
	
	/**
	 * Return a new relation from this subject node to some other node using some other property.
	 * The relation will be stored in the triple store upon setting of both its target object and property.
	 * 
	 * @return
	 * @throws JSFRDFaException
	 */
	public JSFRDFaNewProperty getNewRelation() throws JSFRDFaException {
		// retrieve current property from event context
		JSFRDFaNewProperty result = (JSFRDFaNewProperty)Contexts.getPageContext().get("newProperty");
		
		if(result == null) {
			if(kiwinode != null) {
				result = new JSFRDFaNewProperty(kiwinode,URI);
			} else {
				throw new JSFRDFaException("can only create a new relation for an existing subject node ...");
			}
			Contexts.getPageContext().set("newProperty",result);
		}
		return result;
		
	}
	
	
	/**
	 * Return a new relation from this subject node to some literal value using some property.
	 * The relation will be stored in the triple store upon setting of its target value and property.
	 * 
	 * TODO: get a new object on every call, or store it in page or event context?
	 * 
	 * @return
	 * @throws JSFRDFaException
	 */
	public JSFRDFaNewProperty getNewLiteral() throws JSFRDFaException {
		// retrieve current property from event context
		JSFRDFaNewProperty result = (JSFRDFaNewProperty)Contexts.getPageContext().get("newProperty");
		
		if(result == null) {
			if(kiwinode != null) {
				result = new JSFRDFaNewProperty(kiwinode,LITERAL);
			} else {
				throw new JSFRDFaException("can only create a new relation for an existing subject node ...");
			}
			Contexts.getPageContext().set("newProperty",result);
		}
		return result;
	}
	
	
	/**
	 * Return a list of UI wrappers containing all datatype or annotation property resources that are
	 * applicable for the current node as subject.
	 * 
	 * @return
	 * @throws JSFRDFaException
	 */
	public List<JSFRDFaNode> getPossibleLiteralProperties() throws JSFRDFaException {

		if(possibleLiteralProperties == null) {
		
			OntologyService ontologyService = (OntologyService) Component.getInstance("ontologyService");
			if(kiwinode==null){
				throw new JSFRDFaException("listing applicable properties is only possible for resources");
			} else if(!kiwinode.isUriResource() && ! kiwinode.isAnonymousResource()) {
				throw new JSFRDFaException("listing applicable properties is only possible for resources");
			} else {
				possibleLiteralProperties = new LinkedList<JSFRDFaNode>();
				for(KiWiProperty p : ontologyService.listApplicableDataTypeProperties((KiWiResource)kiwinode)) {
					possibleLiteralProperties.add(new JSFRDFaNode(p.getResource()));
				}
			}
		
		}
		return possibleLiteralProperties;

	}
	
	public List<JSFRDFaNode> getPossibleObjectProperties(JSFRDFaNode object) throws JSFRDFaException {
		if(possibleObjectProperties == null) {
			
			OntologyService ontologyService = (OntologyService) Component.getInstance("ontologyService");
			if(kiwinode==null || object == null || object.kiwinode == null){
				throw new JSFRDFaException("listing applicable properties is only possible for resources");
			} else if(!kiwinode.isUriResource() && ! kiwinode.isAnonymousResource() &&
					  !object.kiwinode.isUriResource() && !object.kiwinode.isAnonymousResource()) {
				throw new JSFRDFaException("listing applicable properties is only possible for resources");
			} else {
				possibleObjectProperties = new LinkedList<JSFRDFaNode>();
				for(KiWiProperty p : ontologyService.listApplicableProperties((KiWiResource)kiwinode, (KiWiResource)object.kiwinode)) {
					possibleObjectProperties.add(new JSFRDFaNode(p.getResource()));
				}
			}
		
		}
		return possibleObjectProperties;
		
	}
	
	/**
	 * Return an iterable list of JSFRDFaProperty wrappers around the literals reachable from the current node
	 * via any kind of relation.
	 */
	public List<JSFRDFaProperty> listObjects() throws JSFRDFaException {
		return listObjects(null);
	}
	
	/**
	 * Return an iterable list of JSFRDFaProperty wrappers around the literals reachable from the current node
	 * via the relation identified by property_uri.
	 */
	public List<JSFRDFaProperty> listObjects(String property_uri) throws JSFRDFaException {
		if(kiwinode != null) {
			if(kiwinode.isUriResource() || kiwinode.isAnonymousResource()) {
				try {
					
					LinkedList<JSFRDFaProperty> result = new LinkedList<JSFRDFaProperty>();
					
					for(KiWiTriple t : ((KiWiResource)kiwinode).listOutgoing(property_uri)) {
						if(t.getObject().isUriResource() || t.getObject().isAnonymousResource()) {
							result.add(new JSFRDFaProperty(t));
						}
						
					}
					return result;
					
				} catch (NamespaceResolvingException e) {
					throw new JSFRDFaException("could not resolve namespace",e);
				}
			} else {
				throw new JSFRDFaException("listObjects is only applicable to resources");
			}
		} else {
			throw new JSFRDFaException("listObjects must be called on existing resources");			
		}
	}

	/**
	 * Return the (first/single) type of the node wrapped by this object. Returns null if the node has no
	 * type. Returns an arbitrarily selected type in case there is more than one type.
	 * @return
	 * @throws JSFRDFaException
	 */
	public JSFRDFaNode getType() throws JSFRDFaException {
		if(kiwinode != null) {
			if(kiwinode.isAnonymousResource() || kiwinode.isUriResource()) {
				KiWiResource r = (KiWiResource)kiwinode;
				
				Collection<KiWiResource> types = r.getTypes();
				
				if(types.size() >= 1) {
					return new JSFRDFaNode(types.iterator().next());
				} 
			} else {
				// KiWiLiteral
				KiWiLiteral l = (KiWiLiteral)kiwinode;
				
				if(l.getType() != null) {
					return new JSFRDFaNode(l.getType());
				}
			}
		}
		return null;
	}
	
	/**
	 * Return an iterable list of the types of the node wrapped by this object.
	 * @return
	 * @throws JSFRDFaException
	 */
	public List<JSFRDFaNode> listTypes() throws JSFRDFaException {
		List<JSFRDFaNode> results = new LinkedList<JSFRDFaNode>();
		if(kiwinode != null) {
			if(kiwinode.isAnonymousResource() || kiwinode.isUriResource()) {
				KiWiResource r = (KiWiResource)kiwinode;
				
				Collection<KiWiResource> types = r.getTypes();
				
				
				for(KiWiResource t : types) {
					results.add(new JSFRDFaNode(types.iterator().next()));
				} 
			} else {
				// KiWiLiteral
				KiWiLiteral l = (KiWiLiteral)kiwinode;
				
				if(l.getType() != null) {
					results.add(new JSFRDFaNode(l.getType()));
				}
			}
		}
		return results;
	}
}
