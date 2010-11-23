/*
 * File : SKOSPrefixMapperServiceImpl.java
 * Date : Nov 3, 2010
 * 
 *
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


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.SKOSPrefixMapperServiceLocal;
import kiwi.api.ontology.SKOSPrefixMapperServiceRemote;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.ontology.SKOSToPrefixMapper;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;


/**
 * @author Mihai
 * @version 1.00
 * @since 1.00
 */
@Name("skosPrefixMapperService")
@Scope(ScopeType.STATELESS)
@Stateless
@AutoCreate
public class SKOSPrefixMapperServiceImpl implements
        SKOSPrefixMapperServiceLocal, SKOSPrefixMapperServiceRemote {

    @Logger
    private Log log;

    @In
    private EntityManager entityManager;

    @In
    private KiWiEntityManager kiwiEntityManager;

    @In
    private ContentItemService contentItemService;

    @Override
    public void assingSKOSToPrefix(SKOSConcept concept, String prefix, int level) {
        final ContentItem delegate = concept.getDelegate();
        final String uri = ((KiWiUriResource) delegate.getResource()).getUri();

        final SKOSConcept topConcept = getTopConcept(concept);
        final ContentItem delegateTopConcept = topConcept.getDelegate();
        final String topConceptURI =
                ((KiWiUriResource) delegateTopConcept.getResource()).getUri();

        final boolean mapExists = getPrefix(uri) != null;
        final String title = concept.getTitle();
        if (!mapExists) {
            entityManager.persist(new SKOSToPrefixMapper(uri, topConceptURI,
                    prefix, title, level));
        } else {
            entityManager.merge(new SKOSToPrefixMapper(uri, topConceptURI,
                    prefix, title, level));
        }
    }

    public void assingAllSKOSToPrefix(String conceptURI, String prefix, int level) {
        final Query query = entityManager.createNamedQuery("update.skosConceptsForParentAndLevel");
        
        query.setParameter("parentURI", conceptURI);
        query.setParameter("prefix", prefix);
        query.setParameter("level", level);
        
        final int executeUpdate = query.executeUpdate();
        System.out.println("executeUpdate : " + executeUpdate);
        
    }
    
    public void assingAllSKOSToPrefix(SKOSConcept concept, String prefix, int level) {
    }


    private SKOSConcept getTopConcept(SKOSConcept nextConcept) {
        SKOSConcept top = nextConcept;
        while (top.getBroader() != null) {
            top = top.getBroader();
        }

        return top;
    }

    private String getPrefix(String uri) {
        final Query query =
                entityManager.createNamedQuery("select.skosMapForURI");
        query.setParameter("uri", uri);
        final SKOSToPrefixMapper result;
        try {
            result = (SKOSToPrefixMapper) query.getSingleResult();
        } catch (NoResultException noResultException) {
            return null;
        }
        return result.getPrefix();
    }

    @Override
    public String getTopConceptPrefix(SKOSConcept concept) {
        final ContentItem delegate = concept.getDelegate();
        final String prefix = getTopConceptPrefix(delegate);
        return prefix;
    }

    @Override
    public String getTopConceptPrefix(ContentItem item) {
        final String uri = ((KiWiUriResource) item.getResource()).getUri();
        final String prefix = getPrefix(uri);
        return prefix;
    }

    @Override
    public Set<String> getAllTopConceptPrefixes() {
        final Query query =
                entityManager.createNamedQuery("select.skosMapForLevel");
        query.setParameter("level", 0);

        final List resultList = query.getResultList();
        final Set<String> result = new HashSet<String>(resultList);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * kiwi.api.ontology.SKOSPrefixMapperService#getConcept(java
     * .lang.String)
     */
    @Override
    public Set<SKOSConcept> getConcepts(String prefix) {
        final Query query =
                entityManager.createNamedQuery("select.skosMapForPrefix");
        query.setParameter("prefix", prefix);

        final List<SKOSToPrefixMapper> prefixMappers = query.getResultList();
        final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
        for (SKOSToPrefixMapper mapper : prefixMappers) {
            final String uri = mapper.getUri();
            final ContentItem item =
                    contentItemService.getContentItemByUri(uri);
            final SKOSConcept facade =
                    kiwiEntityManager.createFacade(item, SKOSConcept.class);
            result.add(facade);
        }

        return result;

    }

    @Override
    public Set<SKOSConcept> getConcepts(String prefix, String pattern) {
        final Query query =
                entityManager
                        .createNamedQuery("select.skosMapForPrefixAndLabel");
        query.setParameter("prefix", prefix);
        query.setParameter("pattern", pattern);

        final List<SKOSToPrefixMapper> prefixMappers = query.getResultList();
        final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
        for (SKOSToPrefixMapper mapper : prefixMappers) {
            final String uri = mapper.getUri();
            final ContentItem item =
                    contentItemService.getContentItemByUri(uri);
            final SKOSConcept facade =
                    kiwiEntityManager.createFacade(item, SKOSConcept.class);
            result.add(facade);
        }

        return result;
    }

    @Override
    public List<SKOSToPrefixMapper> getAllMappings() {
        final Query query =
                entityManager.createNamedQuery("select.allSkosMaps");
        final List<SKOSToPrefixMapper> resultList = query.getResultList();

        return resultList;
    }

    @Override
    public List<SKOSToPrefixMapper> getAllMappings(int nestLevel) {
        final Query query =
            entityManager.createNamedQuery("select.skosMapForLevel");
        query.setParameter("level", nestLevel);
        final List<SKOSToPrefixMapper> resultList = query.getResultList();

        return resultList;
    }

    @Override
    public List<SKOSToPrefixMapper> getAllMappings(String topConceptUri, String prefix, int nestLevel) {
        final Query query =
            entityManager.createNamedQuery("select.skosMapForTopConceptPrefixAndLevel");
        query.setParameter("parentURI", topConceptUri);
        query.setParameter("level", nestLevel);
        query.setParameter("prefix", prefix);
        final List<SKOSToPrefixMapper> resultList = query.getResultList();

        return resultList;
    }
    
    @Override
    public Set<SKOSConcept> getAllConcepts(String topConceptUri, String prefix, int nestLevel) {
        final List<SKOSToPrefixMapper> prefixMappers =
                getAllMappings(topConceptUri, prefix, nestLevel);
        
        final Set<SKOSConcept> result = new HashSet<SKOSConcept>();
        for (SKOSToPrefixMapper mapper : prefixMappers) {
            final String uri = mapper.getUri();
            final ContentItem item =
                    contentItemService.getContentItemByUri(uri);
            final SKOSConcept facade =
                    kiwiEntityManager.createFacade(item, SKOSConcept.class);
            result.add(facade);
        }

        return result;
    }


    @Override
    public List<SKOSToPrefixMapper> getAllMappings(SKOSConcept topConcept, String prefix, int nestLevel) {
        final ContentItem delegate = topConcept.getDelegate();
        final String uri = ((KiWiUriResource) delegate.getResource()).getUri();

        return getAllMappings(uri, prefix,nestLevel);
    }

    @Override
    public void removeAllMapping() {
        final Query query =
                entityManager.createNamedQuery("delete.allSkosMaps");
        query.executeUpdate();
    }

    @Override
    public int getSKOSNestigLevel(SKOSConcept concept) {
        final String uri = ((KiWiUriResource) concept.getResource()).getUri();

        try {
            final Query query =
                entityManager.createNamedQuery("select.skosTopConceptLevel");
            query.setParameter("parentURI", uri);
            final Integer singleResult = (Integer) query.getSingleResult();
            // FIXME : document this, I mean the -1.
            return singleResult == null ? -1 : singleResult.intValue();
        } catch (NoResultException e) {
            // FIXME : document this, I mean the -1. 
            return -1;
        }
    }
}
