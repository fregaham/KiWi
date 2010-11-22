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
package kiwi.service.ontology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.SKOSServiceLocal;
import kiwi.api.ontology.SKOSServiceRemote;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.api.search.SolrService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.log.Log;

/**
 * @author Sebastian Schaffert
 *
 */
@Name("skosService")
@Scope(ScopeType.STATELESS)
@Stateless
@AutoCreate
public class SKOSServiceImpl implements SKOSServiceLocal, SKOSServiceRemote {


    @In
    private EntityManager entityManager;

    @In
    private KiWiEntityManager kiwiEntityManager;

    @In(create = true)
    private ContentItemService contentItemService;

    @In
    private SolrService solrService;

    @In
    private TripleStore tripleStore;

    @Logger
    private Log log;

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#getTopConcepts()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<SKOSConcept> getTopConcepts() {
        Query q = entityManager.createNamedQuery("tripleStore.objectByProperty");
        q.setParameter("prop_uri", Constants.NS_SKOS + "hasTopConcept");
        q.setHint("org.hibernate.cacheable", true);

        List<ContentItem> result = q.getResultList();

        if (result.size() != 0){
            Set<ContentItem> setItems = new LinkedHashSet<ContentItem>(result);
            result.clear();
            result.addAll(setItems);
            log.info("result.size: #0, data loaded", result.size());
            List<SKOSConcept> ls = kiwiEntityManager.createFacadeList(result, SKOSConcept.class, false);
            log.info("test1");


            Collections.sort(ls, new ConceptComparator());
            log.info("test2");

            return ls;
        }

        else {
            log.info("No data loaded");
            return Collections.EMPTY_LIST;
        }
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#getConcept(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public SKOSConcept getConcept(String conceptName) {
        String qString = "select ci from ContentItem ci, KiWiTriple con" +
        " where ci.resource.id = con.subject.id" +
        " and con.property.uri = :concept" +
        " and con.object.content = :title";
        String concept = Constants.NS_SKOS + "prefLabel";
        Query q = entityManager.createQuery(qString);
        q.setParameter("concept", concept);
        q.setParameter("title", conceptName);
        List<ContentItem> result = q.getResultList();
        if (result.size() != 0){
            return kiwiEntityManager.createFacade(result.get(0), SKOSConcept.class);
        } else {
            return null;
        }
    }


    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#getConceptByLabel(java.lang.String)
     */
    @Override
    public List<SKOSConcept> getConceptByLabel(String label){
        List<SKOSConcept> l = new ArrayList<SKOSConcept>();
        KiWiSearchCriteria c = new KiWiSearchCriteria();
        c.setSolrSearchString("type:\"uri::"+ Constants.NS_SKOS+"Concept\" "+label);
        c.setLimit(-1);
        for (SearchResult r : solrService.search(c).getResults()) {
            SKOSConcept concept = kiwiEntityManager.createFacade(r.getItem(), SKOSConcept.class);
            l.add(concept);
        }
        return l;
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#createSkosConcept(java.lang.String, java.util.Locale)
     */
    @Override
    public SKOSConcept createSkosConcept(String newConceptTitle, Locale loc){
        ContentItem c = contentItemService.createContentItem();
        contentItemService.updateTitle(c, newConceptTitle);
        SKOSConcept sk = kiwiEntityManager.createFacade(c, SKOSConcept.class);
        KiWiResource skr = sk.getResource();
        try {
            skr.setProperty("http://www.w3.org/2004/02/skos/core#prefLabel",newConceptTitle,loc);
        } catch (NamespaceResolvingException e) {
            e.printStackTrace();
        }
        return sk;
    }

    /**
     * Return given concept with subconcepts and related concepts
     *
     *
     * @return A List with all Sub-SKOSConcepts
     */
    @Override
    public List<SKOSConcept> getAllConcepts(SKOSConcept concept) {

        LinkedList<SKOSConcept> concepts = new LinkedList();
        concepts.addAll(getSkosConcept(concept));

        HashSet<SKOSConcept> conceptsSet = new HashSet();

        /*
         * Add all Sub trees
         *
         */
        for(int i = 0; i < concepts.size();i++) {
            conceptsSet.add(concepts.get(i));
        }

        /*
         * Find and add all related Concepts
         */
        for(int i = 0; i < concepts.size();i++) {

            Iterator<SKOSConcept> conceptsIter = concepts.get(i).getRelated().iterator();

            while(conceptsIter.hasNext()) {

                conceptsSet.add(conceptsIter.next());
            }
        }

        // Convert to a LinkedList
        LinkedList<SKOSConcept> skContentItems = new LinkedList<SKOSConcept>();
        Iterator<SKOSConcept> conceptsIter = conceptsSet.iterator();
        while(conceptsIter.hasNext()) {
            skContentItems.add(conceptsIter.next());
        }

        return skContentItems;
    }

    /**
     * Return all Concepts with a reverse Algo.
     *
     *
     * @return A List with all SKOSConcepts
     */
    @Override
    public List<SKOSConcept> getAllConcepts() {

        LinkedList<SKOSConcept> concepts = new LinkedList();
        List<SKOSConcept> topConcepts = this.getTopConcepts();

        for(int i = 0; i < topConcepts.size();i++) {
            concepts.addAll(getSkosConcept(topConcepts.get(i)));
        }

        return concepts;
    }

    private List<SKOSConcept> getSkosConcept(SKOSConcept parentConcept) {

        LinkedList<SKOSConcept> concepts = new LinkedList();
        concepts.add(parentConcept);

        Iterator<SKOSConcept> iter = parentConcept.getNarrower().iterator();

        while(iter.hasNext()) {

            concepts.addAll(getSkosConcept(iter.next()));
        }

        return concepts;
    }




    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#autocompleteConcept(java.lang.String)
     */
    @Override
    public List<KiWiResource> autocompleteConcept(String prefix) {

        if(prefix.length() >= 2) {
            StringBuilder qString = new StringBuilder();

            // add prefix to query string
            qString.append("title:"+prefix.toLowerCase()+"*");
            qString.append(" ");

            // add (type:skos:Concept)
            qString.append("(");
            qString.append("type:\"uri::"+Constants.NS_SKOS+"Concept\"");
            qString.append(")");

            KiWiSearchCriteria crit = new KiWiSearchCriteria();

            crit.setSolrSearchString(qString.toString());

            SolrQuery query = solrService.buildSolrQuery(crit);
            query.setStart(0);
            query.setRows(Integer.MAX_VALUE);
            query.setSortField("title_id", ORDER.asc);

            query.setFields("title","id");

            QueryResponse rsp = solrService.search(query);

            if(rsp == null){
                log.error("autocomplete: solr delivered null for the query #0",
                        qString.toString());
                return Collections.emptyList();
            }
            SolrDocumentList docs = rsp.getResults();

            // a resourceMapping for KiWiResourceConverter
            HashMap<String,KiWiResource> resourceMappings = new HashMap<String, KiWiResource>();
            List<KiWiResource> result = new LinkedList<KiWiResource>();
            for(SolrDocument doc : docs) {
                ContentItem item = entityManager.find(ContentItem.class, Long.parseLong((String)doc.getFieldValue("id")));
                if(item != null) {
                    result.add(item.getResource());

                    String label = item.getResource().getLabel() + " (" + item.getResource().getNamespacePrefix() + ")";
                    resourceMappings.put(label, item.getResource());
                }
            }

            Contexts.getPageContext().set("resourceMappings", resourceMappings);

            Collections.sort(result, KiWiResource.LabelComparator.getInstance());
            return result;

        } else {
            return Collections.emptyList();
        }
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#getPreferredLabel(kiwi.model.kbase.KiWiResource)
     */
    @Override
    public String getPreferredLabel(KiWiResource concept) {
        return concept.getProperty(Constants.NS_SKOS + "prefLabel");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#setPreferredLabel(kiwi.model.kbase.KiWiResource, java.lang.String)
     */
    @Override
    public void setPreferredLabel(KiWiResource concept, String label) {
        try {
            concept.setProperty(Constants.NS_SKOS+"prefLabel",label);
        } catch (NamespaceResolvingException e) {
            log.error("SKOS namespace does not exist");
        }
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#getAlternativeLabels(kiwi.model.kbase.KiWiResource)
     */
    @Override
    public List<String> listAlternativeLabels(KiWiResource concept) {
        List<String> result = new LinkedList<String>();

        try {
            for(String label : concept.getProperties(Constants.NS_SKOS+"altLabel")) {
                result.add(label);
            }

            Collections.sort(result);
        } catch (NamespaceResolvingException e) {
            log.error("SKOS namespace does not exist");
        }

        return result;
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#addAlternativeLabel(kiwi.model.kbase.KiWiResource, java.lang.String)
     */
    @Override
    public void addAlternativeLabel(KiWiResource concept, String label) {
        // check whether label already exists
        boolean exists = false;
        for(String exLabel : listAlternativeLabels(concept)) {
            if(label.equals(exLabel)) {
                exists = true;
                break;
            }
        }
        if(!exists) {
            KiWiUriResource p = tripleStore.createUriResource(Constants.NS_SKOS+"altLabel");
            KiWiLiteral l = tripleStore.createLiteral(label);
            tripleStore.createTriple(concept, p, l);
        }

    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#removeAlternativeLabel(kiwi.model.kbase.KiWiResource, java.lang.String)
     */
    @Override
    public void removeAlternativeLabel(KiWiResource concept, String label) {
        try {
            for(KiWiTriple t : concept.listOutgoing(Constants.NS_SKOS+"altLabel")) {
                if(t.getObject().isLiteral()) {
                    KiWiLiteral l = (KiWiLiteral)t.getObject();

                    if(l.getContent().equals(label)) {
                        tripleStore.removeTriple(t);
                    }
                }
            }
        } catch (NamespaceResolvingException e) {
            log.error("SKOS namespace does not exist");
        }

    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#listBroader(kiwi.model.kbase.KiWiResource, boolean)
     */
    @Override
    public List<KiWiResource> listBroader(KiWiResource concept, boolean inferred) {
        return listGenericSKOSRelation(concept, inferred, "broader", "narrower", "broaderTripleIds");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#addBroader(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
     */
    @Override
    public void addBroader(KiWiResource concept, KiWiResource broader) {
        addGenericSKOSRelation(concept, broader, "broader", "narrower");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#removeBroader(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
     */
    @Override
    public void removeBroader(KiWiResource concept, KiWiResource broader) {
        removeGenericSKOSRelation(concept, broader, "broader", "narrower");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#listNarrower(kiwi.model.kbase.KiWiResource, boolean)
     */
    @Override
    public List<KiWiResource> listNarrower(KiWiResource concept, boolean inferred) {
        return listGenericSKOSRelation(concept, inferred, "narrow", "broader", "narrowerTripleIds");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#addNarrower(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
     */
    @Override
    public void addNarrower(KiWiResource concept, KiWiResource narrower) {
        addGenericSKOSRelation(concept, narrower, "narrower", "broader");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#removeNarrower(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
     */
    @Override
    public void removeNarrower(KiWiResource concept, KiWiResource narrower) {
        removeGenericSKOSRelation(concept, narrower, "narrower", "broader");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#listRelated(kiwi.model.kbase.KiWiResource, boolean)
     */
    @Override
    public List<KiWiResource> listRelated(KiWiResource concept, boolean inferred) {
        return listGenericSKOSRelation(concept, inferred, "related", "related", "relatedTripleIds");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#addRelated(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
     */
    @Override
    public void addRelated(KiWiResource concept, KiWiResource related) {
        addGenericSKOSRelation(concept, related, "related", "related");
    }

    /* (non-Javadoc)
     * @see kiwi.api.ontology.SKOSService#removeRelated(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
     */
    @Override
    public void removeRelated(KiWiResource concept, KiWiResource related) {
        removeGenericSKOSRelation(concept, related, "related", "related");
    }



    private List<KiWiResource> listGenericSKOSRelation(KiWiResource r, boolean inferred, String property, String invProperty, String ctx_name) {
        Set<KiWiResource> result = new HashSet<KiWiResource>();

        HashMap<KiWiResource,Long> tripleIds = new HashMap<KiWiResource, Long>();
        try {
            for(KiWiTriple t : r.listOutgoing(Constants.NS_SKOS+property)) {
                if( t.isInferred() == inferred &&
                        (t.getObject().isUriResource() || t.getObject().isAnonymousResource())) {
                    result.add((KiWiResource)t.getObject());

                    if(t.isInferred()) {
                        tripleIds.put((KiWiResource)t.getObject(),t.getId());
                    }
                }
            }
            for(KiWiTriple t : r.listIncoming(Constants.NS_SKOS+invProperty)) {
                if( t.isInferred() == inferred ) {
                    result.add(t.getSubject());

                    if(t.isInferred()) {
                        tripleIds.put(t.getSubject(),t.getId());
                    }
                }
            }
        } catch (NamespaceResolvingException e) {
            log.error("error while resolving namespace:",e);
        }

        // outject triple ids in page scope for the UI to access (e.g. for giving explanations)
        Contexts.getPageContext().set(ctx_name, tripleIds);

        List<KiWiResource> result2 = new LinkedList<KiWiResource>();
        for(KiWiResource r2 : result) {
            result2.add(r2);
        }

        Collections.sort(result2, KiWiResource.LabelComparator.getInstance());

        return result2;

    }


    private void addGenericSKOSRelation(KiWiResource concept1, KiWiResource concept2, String property, String invProperty) {
        KiWiUriResource p1 = tripleStore.createUriResource(Constants.NS_SKOS + property);
        KiWiUriResource p2 = tripleStore.createUriResource(Constants.NS_SKOS + invProperty);

        tripleStore.createTriple(concept1, p1, concept2);
        tripleStore.createTriple(concept2, p2, concept1);
    }


    private void removeGenericSKOSRelation(KiWiResource concept1, KiWiResource concept2, String property, String invProperty) {
        KiWiUriResource p1 = tripleStore.createUriResource(Constants.NS_SKOS + property);
        KiWiUriResource p2 = tripleStore.createUriResource(Constants.NS_SKOS + invProperty);

        tripleStore.removeTriple(concept1, p1, concept2);
        tripleStore.removeTriple(concept2, p2, concept1);
    }

    /**
     * Returns all the narrower concepts for all the top level
     * concepts with a certain level of nesting. The nesting
     * level is applied to the all top level concepts.<br>
     * If the <code>level</code> argument is 0 then this method
     * has the same effect like the <code>getTopConcepts</code>
     * method.
     * 
     * @param level the level of nesting, this level will be
     *            applied to all the top concepts. It must be
     *            greater or equals with 0.
     * @return all the narrower concepts for all the top level
     *         concepts with a certain level of nesting.
     * @throws IllegalArgumentException if the <code>level</code>
     *             argument is smaller than 0.
     */
    @Override
    public Set<SKOSConcept> getAllSKOSConcepts(int level) {
        final List<SKOSConcept> topConcepts = getTopConcepts();

        if (level < 0) {
            final IllegalArgumentException exception =
                new IllegalArgumentException(
                "The level argumetn can not be null.");
            log.error(exception.getMessage(), exception);
            throw exception;
        }

        if (level == 0) {
            final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
            result.addAll(topConcepts);
            return result;
        }

        // I use sets to avoid duplicates :)
        Set<SKOSConcept> set = new HashSet<SKOSConcept>();
        set.addAll(topConcepts);

        Set<SKOSConcept> nextset = new HashSet<SKOSConcept>();
        final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
        // mihai : I know this loop in not efficent but I run out
        // of time.
        for (int actualLevel = 0; actualLevel < level; actualLevel++) {
            for (SKOSConcept concept : set) {
                final Set<SKOSConcept> narrower = concept.getNarrower();
                nextset.addAll(narrower);
            }
            result.addAll(nextset);

            set = nextset;
            nextset = new HashSet<SKOSConcept>();
        }

        return result;
    }

    /**
     * Returns all the narrower concepts for a certain (top
     * level) concept with a certain level of nesting.
     * 
     * @param topConcept the top level SKOS concept for the
     *            narrower query. It can not be null.
     * @param level the level of nesting, this level will be
     *            applied to all the top concepts. It must be
     *            greater or equals with 0.
     * @return all the narrower concepts for all the top level
     *         concepts with a certain level of nesting.
     * @throws NullPointerException if the
     *             <code>topConcept</code> argument is null.
     * @throws IllegalArgumentException if the <code>level</code>
     *             argument is smaller than 0.
     */
    @Override
    public Set<SKOSConcept> getAllSKOSConcepts(SKOSConcept topConcept, int level) {
        if (topConcept == null) {
            final NullPointerException nullException =
                new NullPointerException(
                "The topConcept argument ca not be null.");
            log.error(nullException.getMessage(), nullException);
            throw nullException;
        }

        if (level < 0) {
            final IllegalArgumentException exception =
                new IllegalArgumentException(
                "The level argumetn can not be null.");
            log.error(exception.getMessage(), exception);
            throw exception;
        }

        Set<SKOSConcept> set = topConcept.getNarrower();
        Set<SKOSConcept> nextset = new HashSet<SKOSConcept>();
        final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
        for (int actualLevel = 0; actualLevel < level; actualLevel++) {
            for (SKOSConcept concept : set) {
                final HashSet<SKOSConcept> narrower = concept.getNarrower();
                nextset.addAll(narrower);
            }
            result.addAll(nextset);

            set = nextset;
            nextset = new HashSet<SKOSConcept>();
        }

        return result;
    }
}
