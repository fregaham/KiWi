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


package kiwi.test.service.sun;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.KiWiSearchResults;
import kiwi.api.search.KiWiSearchResults.SearchResult;
import kiwi.api.search.SolrService;
import kiwi.api.sun.SunTagMapperService;
import kiwi.api.sun.SunTagService;
import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;
import kiwi.test.base.KiWiTest;

import org.jboss.seam.Component;
import org.jboss.seam.security.Identity;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * SolrServiceTest
 * 
 * @author Sebastian Schaffert
 */
@Test
public class SunTagServiceTest extends KiWiTest {

    /**
     * This test adds a skos concept and
     * 
     * @throws Exception
     */
    @Test
    public void testGetPrefixes() throws Exception {

        new FacesRequest() {

            @Override
            protected void invokeApplication() {

                Identity.setSecurityEnabled(false);

                final ContentItemService contentItemService =
                        (ContentItemService) Component
                                .getInstance("contentItemService");
                final KiWiEntityManager kiwiEntityManager =
                        (KiWiEntityManager) Component
                                .getInstance("kiwiEntityManager");

                final ContentItem item = contentItemService.createContentItem();
                final SKOSConcept concept =
                        kiwiEntityManager.createFacade(item, SKOSConcept.class);
                concept.setTitle("my_test_title");
                kiwiEntityManager.persist(concept);

            }
        }.run();

        new FacesRequest() {

            @Override
            protected void invokeApplication() {

                Identity.setSecurityEnabled(false);

                final SunTagMapperService sunTagMapperService =
                        (SunTagMapperService) Component
                                .getInstance("sunTagMapperService");

                final SunTagService sunTagService =
                        (SunTagService) Component.getInstance("sunTagService");
                sunTagMapperService.defaultMapping();

                final Set<String> prefixes = sunTagService.getPrefixes();
                System.out.println(">>" + prefixes);
            }
        }.run();

        new FacesRequest() {

            @Override
            protected void invokeApplication() {

                Identity.setSecurityEnabled(false);

                final SunTagService sunTagService =
                        (SunTagService) Component.getInstance("sunTagService");

                final Set<String> prefixes = sunTagService.getPrefixes();
                Assert.assertFalse(prefixes.isEmpty());
            }
        }.run();

    }

    /**
     * Searches Uses the follow string :
     * 
     * <pre>
     * type:kiwi:Tag title:"Res*"
     * </pre>
     * 
     * This test raises a SLOR parse exception and this is wrong.
     * 
     * @throws Exception a SOLR Exception.
     */
    @Test
    public void testSearchTagForPrefix() throws Exception {

        new FacesRequest() {

            @Override
            protected void invokeApplication() {
                Identity.setSecurityEnabled(false);
                final String prefix = "Res";
                final SolrService solrService =
                        (SolrService) Component.getInstance("solrService");

                buildCriteria(solrService, prefix);
            }

        }.run();

    }

    @Test
    public void testSearchTagForNullPrefix() throws Exception {

        new FacesRequest() {

            @Override
            protected void invokeApplication() {
                Identity.setSecurityEnabled(false);
                final String prefix = null;
                final SolrService solrService =
                        (SolrService) Component.getInstance("solrService");

                final KiWiSearchCriteria criteria =
                        buildCriteria(solrService, prefix);
                final List<ContentItem> searchResult =
                        search(solrService, criteria);
            }

        }.run();

    }

    /**
     * Builds a <code>KiWiSearchCriteria</code> for a given
     * String, the search criteria will match all the (KiWi) tags
     * with the title attribute starting with the given prefix.<br>
     * The Strings look like this :
     * 
     * <pre>
     * type:kiwi:Tag title:"prefix*"
     * </pre>
     * 
     * where the prefix is the specify prefix. If the prefix is
     * null then the search String lokes like this :
     * 
     * <pre>
     * type:kiwi:Tag
     * </pre>
     * 
     * @param solrService the SOLR service - used to build the
     *            <code>KiWiSearchCriteria</code>.
     * @param prefix the prefix for the kiwi tag title.
     * @return a <code>KiWiSearchCriteria</code> for a given
     *         String.
     */
    private KiWiSearchCriteria buildCriteria(SolrService solrService,
            String prefix) {
        final String searchWithPrefix =
                "type:kiwi:Tag title:\"" + prefix + "*\"";
        final String searchWithoutPrefix = "type:kiwi:Tag";

        final KiWiSearchCriteria criteria =
                solrService.parseSearchString(prefix == null
                        ? searchWithoutPrefix
                        : searchWithPrefix);

        return criteria;
    }

    /**
     * Search using a given criteria and the given search
     * service.
     * 
     * @param solrService the SOLR service - used to search with
     *            the given <code>KiWiSearchCriteria</code>.
     * @param criteria the criteria to search.
     * @return the search result for the given criteria.
     */
    private List<ContentItem> search(SolrService solrService,
            KiWiSearchCriteria criteria) {
        final KiWiSearchResults search = solrService.search(criteria);
        final List<ContentItem> result = new ArrayList<ContentItem>();
        final List<SearchResult> results = search.getResults();
        for (SearchResult items : results) {
            final ContentItem item = items.getItem();
            result.add(item);
        }

        return result;
    }

}
