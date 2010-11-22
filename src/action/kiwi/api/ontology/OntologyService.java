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

package kiwi.api.ontology;

import java.util.List;

import kiwi.model.kbase.KiWiResource;
import kiwi.model.ontology.KiWiClass;
import kiwi.model.ontology.KiWiProperty;

/**
 * The OntologyService provides convenience operations for working with OWL and RDFS ontologies 
 * in the KiWi system, like listing classes, listing properties, etc.
 * 
 * @author Sebastian Schaffert
 *
 */
public interface OntologyService {

	/**
	 * List all classes that are defined in the KiWi system.
	 * 
	 * @return a list of KiWiClass objects representing the classes in the KiWi system
	 */
	public List<KiWiClass> listClasses();
	
	/**
	 * List all properties that are defined in the KiWi system.
	 * 
	 * @return a list of KiWiProperty objects representing the properties in the KiWi system
	 */
	public List<KiWiProperty> listProperties();
	
	/**
	 * List the properties that currently exist between a given subject and object. 
	 * @param subject the subject of the relation
	 * @param object the object of the relation
	 * @return the list of properties that are used in relations between subject and object
	 */
	public List<KiWiProperty> listExistingProperties(KiWiResource subject, KiWiResource object);
	
	
	/**
	 * List all datatype properties that are defined in the KiWi system.
	 * 
	 * @return a list of KiWiProperty objects representing the properties in the KiWi system
	 */
	public List<KiWiProperty> listDatatypeProperties();
	
	/**
	 * List the properties that are applicable between a given subject and object. Whether a
	 * property is applicable is decided based on the domain and range of the property: if one of 
	 * the types of the subject is in the range, and one of the types of the object is in the 
	 * domain, the property is applicable.
	 * 
	 * @param subject the subject to check
	 * @param object the object to check
	 * @return a list of applicable properties
	 */
	public List<KiWiProperty> listApplicableProperties(KiWiResource subject, KiWiResource object);

	/**
	 * List the datatype properties that are applicable for a given subject. Whether a property is
	 * applicable is decided based on the range of the property: if one of the types of the subject
	 * is in the range, the property is applicable. 
	 * <p>
	 * This method currently ignores the domain of the property. In the future, OntologyService
	 * could provide a method that takes into account the actual datatype.
	 * 
	 * @param subject the subject for which the properties are supposed to be listed
	 * @return a list of KiWiProperty facades that are applicable
	 */
	public List<KiWiProperty> listApplicableDataTypeProperties(KiWiResource subject);
	
	
	/**
	 * Provide autocompletion of content items that are of type "owl:Class" or "rdfs:Class" and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocomplete(String prefix);
	
	
	/**
	 * Provide autocompletion of content items that are of type "owl:ObjectProperty", "owl:DatatypeProperty
	 * or "rdf:Property" and whose title starts with "prefix". Autocompletion is delegated to the SOLR 
	 * search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocompleteProperties(String prefix);
	
	/**
	 * List all datatype properties that are defined in the KiWi system and match title 'name'.
	 * 
	 * @return a list of KiWiProperty objects representing the properties in the KiWi system
	 */
	public List<KiWiProperty> listDatatypePropertiesByName( String name );
	
	
	/**
	 * List all superclasses of the given resource. Implemented using KiWiResource.listOutgoing("rdfs:subClassOf").
	 * @param inferred list inferred classes or list base classes (never both)
	 */
	public List<KiWiResource> listSuperClasses(KiWiResource r, boolean inferred);
	
	/**
	 * Add a superclass to the given resource. If the class is already a superclass, nothing happens.
	 * @param cls
	 * @param supercls
	 */
	public void addSuperClass(KiWiResource cls, KiWiResource supercls);
	
	/**
	 * Remove a superclass from the given resource. If the class is not a superclass, nothing happens.
	 * @param cls
	 * @param supercls
	 */
	public void removeSuperClass(KiWiResource cls, KiWiResource supercls);
	
	/**
	 * List all (maximum: 100) subclasses of the given resource. Implemented using KiWiResource.listIncoming("rdfs:subClassOf").
	 * @param inferred list inferred classes or list base classes (never both)
	 */
	public List<KiWiResource> listSubClasses(KiWiResource r, boolean inferred);
	
	
	/**
	 * List all (maximum: 100) instances of the given resource. Implemented using KiWiResource.listIncoming("rdf:type").
	 * @param r
	 * @param inferred list inferred classes or list base classes (never both)
	 * @return
	 */
	public List<KiWiResource> listInstances(KiWiResource r, boolean inferred);
	
	
	/**
	 * Add an instance to a class. Uses KiWiResource.addType for implementation.
	 * @param cls
	 * @param instance
	 */
	public void addInstance(KiWiResource cls, KiWiResource instance);
	
	/**
	 * Remove an instance from a class. Uses KiWiResource.removeType for implementation.
	 * @param cls
	 * @param instance
	 */
	public void removeInstance(KiWiResource cls, KiWiResource instance);
	
	/**
	 * List all subproperties of the property resource given as parameter. Uses
	 * KiWiResource.listIncoming("rdfs:subPropertyOf").
	 * 
	 * @param p the property for which to list the subproperties
	 * @param inferred list only inferred classes or list only base classes
	 * @return
	 */
	public List<KiWiResource> listSubProperties(KiWiResource p, boolean inferred);
	
	/**
	 * Add the subproperty given as second argument to the property given as first argument.
	 * If the second argument is already a subproperty of the property, nothing happens.
	 * 
	 * Adds the &lt;subproperty> rdfs:subPropertyOf &lt;property> relation
	 * 
	 * @param property the property to which to add a subproperty
	 * @param subproperty the subproperty to add
	 */
	public void addSubProperty(KiWiResource property, KiWiResource subproperty);

	/**
	 * Remove the subproperty given as second argument from the property given as
	 * first argument. If the second argument is not a subproperty of property,
	 * nothing happens.
	 * 
	 * @param property property from which to remove the subproperty
	 * @param subproperty the superproperty to remove
	 */
	public void removeSubProperty(KiWiResource property, KiWiResource subproperty);
	
	/**
	 * List all superproperties of the property resource given as parameter. Uses
	 * KiWiResource.listOutgoing("rdfs:subPropertyOf").
	 * 
	 * @param p the property for which to list the superproperties
	 * @param inferred list only inferred classes or list only base classes
	 * @return
	 */
	public List<KiWiResource> listSuperProperties(KiWiResource p, boolean inferred);
	
	/**
	 * Add the superproperty given as second argument to the property given as first 
	 * argument. If the second argument is already a superproperty of the property,
	 * nothing happens.
	 * <p>
	 * Adds the &lt;property> rdfs:subPropertyOf &lt;subproperty> relation
	 * 
	 * @param property the property to which to add a superproperty
	 * @param superproperty
	 */
	public void addSuperProperty(KiWiResource property, KiWiResource superproperty);

	/**
	 * Remove the superproperty given as second argument from the property given as
	 * first argument. If the second argument is not a superproperty of property,
	 * nothing happens.
	 * 
	 * @param property
	 * @param superproperty
	 */
	public void removeSuperProperty(KiWiResource property, KiWiResource superproperty);

	
	/**
	 * List the range of the property resource given as parameter. Uses
	 * KiWiResource.listOutgoing("rdfs:range").
	 * 
	 * @param p the property for which to list the range
	 * @param inferred list only inferred classes or list only base classes
	 * @return
	 */
	public List<KiWiResource> listRange(KiWiResource p, boolean inferred);
	
	/**
	 * Add a new resource to the range of the property resource p. If the property
	 * already has this range, nothing happens.
	 * 
	 * @param p the property resource to add the range for
	 * @param range the resource representing the range to be added
	 */
	public void addRange(KiWiResource p, KiWiResource range);
	
	/**
	 * Remove a range from the property resource p. If the property does
	 * not have this range, nothing happens.
	 * 
	 * @param p the property resource to remove the range from
	 * @param range the range to be removed
	 */
	public void removeRange(KiWiResource p, KiWiResource range);
	
	/**
	 * List the domain of the property resource given as parameter. Uses
	 * KiWiResource.listOutgoing("rdfs:domain").
	 * 
	 * @param p the property for which to list the domain
	 * @param inferred list only inferred classes or list only base classes
	 * @return
	 */
	public List<KiWiResource> listDomain(KiWiResource p, boolean inferred);
	
	/**
	 * Add a new resource to the domain of the property resource p. If the property
	 * already has this domain, nothing happens.
	 * 
	 * @param p the property resource to add the domain for
	 * @param domain the domain to be added
	 */
	public void addDomain(KiWiResource p, KiWiResource domain);
	
	/**
	 * Remove a domain from the property resource p. If the property does not
	 * have this domain, nothing happens.
	 * 
	 * @param p the property resource to remove the domain from
	 * @param domain the domain to be removed
	 */
	public void removeDomain(KiWiResource p, KiWiResource domain);
	
	/**
	 * Return the inverse property of the property given as parameter. Uses the
	 * owl:inverseOf relation and tries both KiWiResource.listOutgoing and
	 * KiWiResource.listIncoming. Returns null when there is no inverse resource.
	 * 
	 * @param r
	 * @return
	 */
	public KiWiResource getInverse(KiWiResource r);
	
	/**
	 * Set the owl:inverseOf relation between p1 and p2. The method automatically
	 * takes care of the symmetry of owl:inverseOf and sets the relation explicitly
	 * in both ways. Since there can only be a single inverse, changing the inverse
	 * relation also removes any previous inverse relation.
	 * 
	 * @param p1
	 * @param p2
	 */
	public void setInverse(KiWiResource p1, KiWiResource p2);
	
	/**
	 * Remove the owl:inverseOf relation between p1 and p2. The method takes care of 
	 * removing the symmetric inverse as well.
	 * 
	 * @param p1
	 * @param p2
	 */
	public void removeInverse(KiWiResource p1, KiWiResource p2);
}
