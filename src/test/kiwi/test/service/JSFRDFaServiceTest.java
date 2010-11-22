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
package kiwi.test.service;

import java.util.List;

import kiwi.api.rdfa.JSFRDFaService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.service.rdfa.JSFRDFaNewProperty;
import kiwi.service.rdfa.JSFRDFaNode;
import kiwi.service.rdfa.JSFRDFaProperty;
import kiwi.test.base.KiWiTest;

import org.jboss.seam.Component;
import org.jboss.seam.security.Identity;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * JSFRDFaServiceTest
 *
 * @author Sebastian Schaffert
 *
 */
@Test
public class JSFRDFaServiceTest extends KiWiTest {

	@Test
	public void testCurrentResource() throws Exception {
		/*
		 * Check whether we can get a wrapper around current content item
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {
            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	final ContentItem ci = (ContentItem) Component.getInstance("currentContentItem");
  
            	JSFRDFaNode ctx = rdf.getCurrentResource();
            	
            	Assert.assertNotNull(ctx);
            	Assert.assertNotNull(ctx.getValue());
            	Assert.assertEquals(ctx.getValue(),((KiWiUriResource)ci.getResource()).getUri());
            }
    	}.run();
		
	}
	
	
	@Test
	public void testRead() throws Exception {
		// first setup a new resource with a few properties by loading a FOAF file
		String[] ontologies = { "imports/foaf.owl", "test/henry_foaf.rdf" };
		setupDatabase(ontologies);
		System.out.println("Setup completed");
		
		/*
		 * try to get a wrapper around Henry's foaf representation
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode henry = rdf.getResource("http://bblfish.net/people/henry/card#me");
            	
            	Assert.assertNotNull(henry);
            	
            	// try to get a literal (foaf:family_name) and check its value
            	JSFRDFaNode fn = henry.getLiteral("http://xmlns.com/foaf/0.1/family_name");
            	Assert.assertEquals(fn.getValue(), "Story");
            	Assert.assertTrue(fn instanceof JSFRDFaProperty);
            	
            	// try to iterate over all foaf:knows relations
            	List<JSFRDFaProperty> knows = henry.listObjects("http://xmlns.com/foaf/0.1/knows");
            	Assert.assertNotNull(knows);
            	Assert.assertTrue(knows.size() > 0);
            	boolean passed = false;
            	for(JSFRDFaProperty p : knows) {
            		if("Axel Rauschmayer".equals(p.getLiteral("http://xmlns.com/foaf/0.1/name").getValue())) {
            			passed = true;
            		}
             	}
            	Assert.assertTrue(passed);
            	
            }
    	}.run();

    	shutdownDatabase();
		
	}
	
	@Test
	public void testUpdateLiteral() throws Exception {
		
    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode n = rdf.getResource("http://www.example.com/n");
            	JSFRDFaNode p = rdf.getResource("http://www.example.com/p1");
            	
            	// try setting the new property http://www.example.com/p1
            	JSFRDFaNewProperty p1 = n.getNewLiteral();
            	p1.setProperty(p);
            	p1.setValue("test");
            	rdf.addRelation();
              }
    	}.run();

    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode n = rdf.getResource("http://www.example.com/n");
            	JSFRDFaNode p1 = n.getLiteral("http://www.example.com/p1");
            	
            	// check whether updated in JSF+RDFa
            	Assert.assertNotNull(p1);
            	Assert.assertEquals(p1.getValue(),"test");
            	
            	// check whether updated in triple store
            	TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
            	KiWiUriResource subj = ts.createUriResource("http://www.example.com/n");
            	KiWiUriResource prop = ts.createUriResource("http://www.example.com/p1");
            	
            	List<KiWiTriple> l = ts.getTriplesBySP(subj, prop);
            	
            	Assert.assertTrue(l.size() > 0);
            	Assert.assertTrue(l.get(0).getObject().isLiteral());
            	Assert.assertEquals(((KiWiLiteral)l.get(0).getObject()).getContent(), "test");
            	
            	
            	// check whether we can update the relation 
            	JSFRDFaProperty p1p = (JSFRDFaProperty)p1;
            	p1p.setValue("test2");
            	
            }
    	}.run();

    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode n = rdf.getResource("http://www.example.com/n");
            	JSFRDFaNode p1 = n.getLiteral("http://www.example.com/p1");
            	
            	// check whether updated in JSF+RDFa
            	Assert.assertNotNull(p1);
            	Assert.assertEquals(p1.getValue(),"test2");
            	
            	// check whether updated in triple store
            	TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
            	KiWiUriResource subj = ts.createUriResource("http://www.example.com/n");
            	KiWiUriResource prop = ts.createUriResource("http://www.example.com/p1");
            	
            	List<KiWiTriple> l = ts.getTriplesBySP(subj, prop);
            	
            	Assert.assertTrue(l.size() > 0);
            	Assert.assertTrue(l.get(0).getObject().isLiteral());
            	Assert.assertEquals(((KiWiLiteral)l.get(0).getObject()).getContent(), "test2");
            
            	// check whether we can remove the relation
            	rdf.removeRelation((JSFRDFaProperty)p1);
            }
    	}.run();
    	
    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
             	
            	// check whether updated in triple store
            	TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
            	KiWiUriResource subj = ts.createUriResource("http://www.example.com/n");
            	KiWiUriResource prop = ts.createUriResource("http://www.example.com/p1");
            	
            	List<KiWiTriple> l = ts.getTriplesBySP(subj, prop);
            	
            	Assert.assertTrue(l.size() == 0);
            }
    	}.run();
	}

	@Test
	public void testUpdateObject() throws Exception {
		
    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode n = rdf.getResource("http://www.example.com/n2");
            	JSFRDFaNode p = rdf.getResource("http://www.example.com/p2");
            	JSFRDFaNode o = rdf.getResource("http://www.example.com/o");
            	
            	// try setting the new property http://www.example.com/p1
            	JSFRDFaNewProperty p1 = n.getNewRelation();
            	p1.setProperty(p);
            	p1.setObject(o);
              }
    	}.run();

    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode n = rdf.getResource("http://www.example.com/n2");
            	JSFRDFaNode p2 = n.getObject("http://www.example.com/p2");
            	
            	// check whether updated in JSF+RDFa
            	Assert.assertNotNull(p2);
            	Assert.assertEquals(p2.getValue(),"http://www.example.com/o");
            	
            	// check whether updated in triple store
            	TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
            	KiWiUriResource subj = ts.createUriResource("http://www.example.com/n2");
            	KiWiUriResource prop = ts.createUriResource("http://www.example.com/p2");
            	
            	List<KiWiTriple> l = ts.getTriplesBySP(subj, prop);
            	
            	Assert.assertTrue(l.size() > 0);
            	Assert.assertTrue(l.get(0).getObject().isUriResource());
            	Assert.assertEquals(((KiWiUriResource)l.get(0).getObject()).getUri(), "http://www.example.com/o");
            	
            	// test updating the relation
            	JSFRDFaProperty p2p = (JSFRDFaProperty)p2;
            	p2p.setValue("http://www.example.com/o2");
            }
    	}.run();

    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
            	final JSFRDFaService rdf = (JSFRDFaService) Component.getInstance("rdf");
            	JSFRDFaNode n = rdf.getResource("http://www.example.com/n2");
            	JSFRDFaNode p2 = n.getObject("http://www.example.com/p2");
            	
            	// check whether updated in JSF+RDFa
            	Assert.assertNotNull(p2);
            	Assert.assertEquals(p2.getValue(),"http://www.example.com/o2");
            	
            	// check whether updated in triple store
            	TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
            	KiWiUriResource subj = ts.createUriResource("http://www.example.com/n2");
            	KiWiUriResource prop = ts.createUriResource("http://www.example.com/p2");
            	
            	List<KiWiTriple> l = ts.getTriplesBySP(subj, prop);
            	
            	Assert.assertTrue(l.size() > 0);
            	Assert.assertTrue(l.get(0).getObject().isUriResource());
            	Assert.assertEquals(((KiWiUriResource)l.get(0).getObject()).getUri(), "http://www.example.com/o2");
            	
            	// test removing the relation
            	JSFRDFaProperty p2p = (JSFRDFaProperty)p2;
            	rdf.removeRelation(p2p);
            }
    	}.run();
    	
    	new FacesRequest() {

            @Override
            protected void invokeApplication() throws Exception {

            	Identity.setSecurityEnabled(false);
            	
             	
            	// check whether updated in triple store
            	TripleStore ts = (TripleStore) Component.getInstance("tripleStore");
            	KiWiUriResource subj = ts.createUriResource("http://www.example.com/n2");
            	KiWiUriResource prop = ts.createUriResource("http://www.example.com/p2");
            	
            	List<KiWiTriple> l = ts.getTriplesBySP(subj, prop);
            	
            	Assert.assertTrue(l.size() == 0);
            }
    	}.run();
	}
	
	
}
