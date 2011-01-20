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
 */
package kiwi.test.action;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import kiwi.api.content.ContentItemService;
import kiwi.api.fragment.FragmentFacade;
import kiwi.api.fragment.FragmentService;
import kiwi.api.tagging.TaggingService;
import kiwi.context.CurrentContentItemFactory;
import kiwi.model.content.ContentItem;
import kiwi.model.tagging.Tag;
import kiwi.test.base.KiWiTest;
import kiwi.wiki.action.EditorAction;

import org.jboss.seam.Component;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.testng.annotations.Test;

import org.testng.Assert;


/**
 * Test behavior of the editorAction wrt. editing fragments.
 * 
 * @author Marek Schmidt
 *
 */
public class EditFragmentTest extends KiWiTest {
	Log log = Logging.getLog(this.getClass());
	String fragmentId;
	@Test
	public void testCreateFragment() throws Exception
	{	
		String id = new FacesRequest("/edit.xhtml") {
			@Override
			protected void updateModelValues() throws Exception
			{
				CurrentContentItemFactory ciFactory = (CurrentContentItemFactory) 
						Component.getInstance("currentContentItemFactory");
				ciFactory.setCurrentItemTitle("Lorem Ipsum With Fragments");
				ciFactory.loadContentItem();
				
				setValue("#{editorAction.content}", 
						"Lorem ipsum dolor sit amet, consetetur sadipscing " +
						"elitr, sed diam nonumy eirmod tempor invidunt ut " +
						"labore et dolore magna aliquyam erat, sed diam voluptua. " +
						"At vero eos et accusam et justo duo dolores et ea rebum. " +
						"Stet clita kasd gubergren, no sea takimata sanctus est " +
						"Lorem ipsum dolor sit amet.");
				setValue("#{editorAction.title}", "Lorem Ipsum With Fragments");
			}
			
			@Override
			protected void invokeApplication() throws Exception {
				invokeMethod("#{editorAction.storeContentItem}");

	             ContentItemService cis = (ContentItemService)
	             		Component.getInstance("contentItemService");
	             
	             ContentItem ci1 = cis.getContentItemByTitle("Lorem Ipsum With Fragments");
	             Assert.assertNotNull(ci1);
	             Assert.assertNotNull(ci1.getResource());
	             Assert.assertNotNull(ci1.getResource().getKiwiIdentifier());
	             String kiwiId = ci1.getResource().getKiwiIdentifier();
	             
	             Assert.assertTrue(kiwiId.length() > 0);
	             
	             ContentItem ci2 = cis.getContentItemByKiwiId(kiwiId);
	             Assert.assertNotNull(ci2);
	             Assert.assertEquals(ci1, ci2);
	             
	             Assert.assertNotNull(ci1.getTextContent());
	             
	             String content = ci1.getTextContent().getXmlString();
	             
	             Assert.assertTrue(content.length() > 0);
	             
	             Assert.assertEquals(content, "<div xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:kiwi=\"http://www.kiwi-project.eu/kiwi/html/\" kiwi:type=\"page\">Lorem ipsum dolor sit amet, consetetur sadipscing " +
						"elitr, sed diam nonumy eirmod tempor invidunt ut " +
						"labore et dolore magna aliquyam erat, sed diam voluptua. " +
						"At vero eos et accusam et justo duo dolores et ea rebum. " +
						"Stet clita kasd gubergren, no sea takimata sanctus est " +
						"Lorem ipsum dolor sit amet.</div>");
			}
		}.run();
		
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.fragmentJS}", " ");
			}
			
			@Override
			protected void invokeApplication() throws Exception {
				EditorAction editorAction = (EditorAction)Component.getInstance("editorAction", true);
				Assert.assertTrue(editorAction.getFragmentJS().length() > 0);
				
				fragmentId = editorAction.getFragmentJS();
			}
		}.run();
		
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.tagLabel}", "lorem ipsum");
			}
			
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.addTag}");
			}
		}.run();
		
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.fragmentCreate}");
			}
		}.run();
		
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.content}", 
						"[[BookmarkStart:" + fragmentId + "]]Lorem ipsum[[BookmarkEnd:" + fragmentId + "]] dolor sit amet, consetetur sadipscing " +
						"elitr, sed diam nonumy eirmod tempor invidunt ut " +
						"labore et dolore magna aliquyam erat, sed diam voluptua. " +
						"At vero eos et accusam et justo duo dolores et ea rebum. " +
						"Stet clita kasd gubergren, no sea takimata sanctus est " +
						"Lorem ipsum dolor sit amet.");
			}
			
			@Override
			protected void invokeApplication() throws Exception {
				invokeMethod("#{editorAction.storeContentItem}");
				
				ContentItemService cis = (ContentItemService)
         		Component.getInstance("contentItemService");
         
				ContentItem ci1 = cis.getContentItemByTitle("Lorem Ipsum With Fragments");
				Assert.assertNotNull(ci1);
				Assert.assertNotNull(ci1.getResource());
				Assert.assertNotNull(ci1.getResource().getKiwiIdentifier());
				
				Assert.assertNotNull(ci1.getTextContent());
         
				String content = ci1.getTextContent().getXmlString();
         
				Assert.assertTrue(content.length() > 0);
         
				Assert.assertEquals(content, "<div xmlns=\"http://www.w3.org/1999/xhtml\" " +
						"xmlns:kiwi=\"http://www.kiwi-project.eu/kiwi/html/\" kiwi:type=\"page\">" +
						"<kiwi:bookmarkstart id=\""+ fragmentId +"\"></kiwi:bookmarkstart>" +
						"Lorem ipsum" +
						"<kiwi:bookmarkend id=\"" + fragmentId + "\"></kiwi:bookmarkend>" +
						" dolor sit amet, consetetur sadipscing " +
						"elitr, sed diam nonumy eirmod tempor invidunt ut " +
						"labore et dolore magna aliquyam erat, sed diam voluptua. " +
						"At vero eos et accusam et justo duo dolores et ea rebum. " +
						"Stet clita kasd gubergren, no sea takimata sanctus est " +
					"Lorem ipsum dolor sit amet.</div>");
			}
		}.run();
		
		// new conversation
		id = new FacesRequest() {
			@Override
			protected void invokeApplication() throws Exception {
				ContentItemService cis = (ContentItemService)
         		Component.getInstance("contentItemService");
				
				TaggingService ts = (TaggingService)Component.getInstance("taggingService");
         
				ContentItem ci = cis.getContentItemByTitle("Lorem Ipsum With Fragments");
				
				ContentItem fragment = cis.getContentItemByKiwiId(fragmentId);
				Assert.assertNotNull(fragment);
					
				List<Tag> fragmentTaggings = ts.getTaggings(fragment);
				Assert.assertEquals(fragmentTaggings.size(), 1);
				Assert.assertEquals(fragmentTaggings.get(0).getLabel(), "lorem ipsum");
				
				//Assert.assertTrue(fragment.getTags().contains("lorem ipsum"));
				
				FragmentService fs = (FragmentService)Component.getInstance("fragmentService");
				Collection<FragmentFacade> fragments = fs.getContentItemFragments(ci, FragmentFacade.class);
				Assert.assertTrue(fragments.size() == 1);
				for (FragmentFacade ff : fragments) {
					ff.getDelegate().getKiwiIdentifier().equals(fragment.getKiwiIdentifier());
					//Assert.assertTrue(ff.getDelegate().getTagLabels().contains("lorem ipsum"));
					List<Tag> ffTaggings = ts.getTaggings(ff.getDelegate());
					Assert.assertEquals(ffTaggings.size(), 1);
					Assert.assertEquals(ffTaggings.get(0).getLabel(), "lorem ipsum");
				}
			}
		}.run();
		
		// we test adding a new tag to the fragment
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				
				CurrentContentItemFactory ciFactory = (CurrentContentItemFactory) 
				Component.getInstance("currentContentItemFactory");
				
				ciFactory.setCurrentItemTitle("Lorem Ipsum With Fragments");
				ciFactory.loadContentItem();
				
				setValue("#{editorAction.fragmentJS}", " " + fragmentId);
			}
			
			@Override
			protected void invokeApplication() throws Exception {
				EditorAction editorAction = (EditorAction)Component.getInstance("editorAction", true);
				Assert.assertEquals(editorAction.getFragmentJS(), fragmentId);
			}
		}.run();
		
		// fill tag label and click add
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.tagLabel}", "new");
			}
			
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.addTag}");
			}
		}.run();
		
		// click create/update
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.fragmentCreate}");
			}
		}.run();
		
		//save the editor, no content change
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.storeContentItem}");
			}
		}.run();
		
		// new conversation, check if both tags are there...
		id = new FacesRequest() {
			@Override
			protected void invokeApplication() throws Exception {
				
				ContentItemService cis = (ContentItemService)
         		Component.getInstance("contentItemService");
				
				TaggingService ts = (TaggingService)Component.getInstance("taggingService");
         
				ContentItem ci = cis.getContentItemByTitle("Lorem Ipsum With Fragments");
				
				ContentItem fragment = cis.getContentItemByKiwiId(fragmentId);
				Assert.assertNotNull(fragment);
					
				List<Tag> fragmentTaggings = ts.getTaggings(fragment);
				Assert.assertEquals(fragmentTaggings.size(), 2);
				
				Set<String> labels = new HashSet<String> ();
				for (Tag tagging : fragmentTaggings) {
					labels.add(tagging.getLabel());
				}
				
				Assert.assertTrue(labels.contains("lorem ipsum"));
				Assert.assertTrue(labels.contains("new"));
								
				FragmentService fs = (FragmentService)Component.getInstance("fragmentService");
				Collection<FragmentFacade> fragments = fs.getContentItemFragments(ci, FragmentFacade.class);
				Assert.assertTrue(fragments.size() == 1);
				for (FragmentFacade ff : fragments) {
					ff.getDelegate().getKiwiIdentifier().equals(fragment.getKiwiIdentifier());
					//Assert.assertTrue(ff.getDelegate().getTagLabels().contains("lorem ipsum"));
					List<Tag> ffTaggings = ts.getTaggings(ff.getDelegate());
					Assert.assertEquals(ffTaggings.size(), 2);
					
					Set<String> ffLabels = new HashSet<String> ();
					for (Tag tagging : ffTaggings) {
						ffLabels.add(tagging.getLabel());
					}
					
					Assert.assertTrue(ffLabels.contains("lorem ipsum"));
					Assert.assertTrue(ffLabels.contains("new"));
				}
			}
		}.run();
	}
	
	@Test
	public void testCreateTagAndDeleteFragment() throws Exception {
		String id = new FacesRequest("/edit.xhtml") {
			@Override
			protected void updateModelValues() throws Exception
			{
				CurrentContentItemFactory ciFactory = (CurrentContentItemFactory) 
					Component.getInstance("currentContentItemFactory");
				ciFactory.setCurrentItemTitle("Lorem Ipsum With Fragments2");
				ciFactory.loadContentItem();
				
				setValue("#{editorAction.content}", 
						"Lorem ipsum");
			}
		}.run();
		
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.fragmentJS}", " ");
			}
				
			@Override
			protected void invokeApplication() throws Exception {				
				fragmentId = (String)getValue("#{editorAction.fragmentJS}");
			}			
		}.run();
		
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.tagLabel}", "new");
			}
			
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.addTag}");
			}
		}.run();
		
		// click create/update
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void invokeApplication() throws Exception {	
				invokeMethod("#{editorAction.fragmentCreate}");
			}
		}.run();
		
		// select the fragment again
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void updateModelValues() throws Exception {
				setValue("#{editorAction.fragmentJS}", " " + fragmentId);
			}
		}.run();
		
		// delete the fragment
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void invokeApplication() throws Exception {
				invokeMethod("#{editorAction.fragmentDelete}");
			}
		}.run();
		
		// save the editor
		id = new FacesRequest("/edit.xhtml", id) {
			@Override
			protected void invokeApplication() throws Exception {
				invokeMethod("#{editorAction.storeContentItem}");
			}
		}.run();
	}
}
