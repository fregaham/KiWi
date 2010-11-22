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
package kiwi.test.service.informationextraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


import kiwi.api.content.ContentItemService;
import kiwi.api.informationextraction.InformationExtractionService;
import kiwi.api.informationextraction.KiWiGATEService;
import kiwi.api.search.SolrService;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.Suggestion;
import kiwi.test.base.KiWiTest;
import kiwi.util.KiWiXomUtils;

import nu.xom.Builder;
import nu.xom.Document;

import org.jboss.seam.Component;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.jboss.seam.security.Identity;
import org.testng.annotations.Test;

import org.testng.Assert;


/**
 * 
 * @author Marek Schmidt
 *
 */
@Test
public class InformationExtractionTest extends KiWiTest {
	Log log = Logging.getLog(this.getClass());
	
	@Test
	public void testExtractSuggestions() throws Exception {
		
		new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);

            	final ContentItemService cs = (ContentItemService) Component.getInstance("contentItemService");

            	final ContentItem c1 = cs.createTextContentItem("<p>Foxes are agile animals, capable of jumping over other animals.</p>");
            	cs.updateTitle(c1, "Fox");
            	cs.saveContentItem(c1);
            }
        }.run();
         
    	
    	new FacesRequest() {
    		  @Override
              protected void invokeApplication() {
    			  Identity.setSecurityEnabled(false);
    			  
    			  final SolrService solrService = (SolrService) Component.getInstance("solrService");
    			  solrService.rebuildIndex();
    			  
    			  try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Assert.fail("Interrupted sleep");
				}
    		  }
    	}.run();
		
		/*
		 * 
		 */
    	new FacesRequest() {
            @Override
            protected void invokeApplication() {
            	KiWiGATEService kiwiGATEService = (KiWiGATEService)Component.getInstance("kiwi.informationextraction.gateService");
            	InformationExtractionService informationExtractionService = (InformationExtractionService)Component.getInstance("kiwi.informationextraction.informationExtractionService");
            	ContentItemService contentItemService = (ContentItemService)Component.getInstance("contentItemService");
            	
            	kiwiGATEService.init();
            	
            	TextContent tc = new TextContent();
            	tc.setXmlString("<div><p>A quick brown fox jumped over a lazy dog.</p></div>");
            	
            	ContentItem item = contentItemService.createContentItem();
            	
            	Collection<Suggestion> suggestions = informationExtractionService.extractSuggestions(item.getResource(), tc, Locale.ENGLISH);
            	
            	Assert.assertTrue(suggestions.size() > 0, "Suggestions should not be empty.");
            	
            	for (Suggestion suggestion : suggestions) {
            		log.info("suggestion kind: " + suggestion.getKind() + " label: " + suggestion.getLabel());
            	}
            }
    	}.run();
	}
	
	
	@Test
	public void testXomToPlaintext() throws Exception {
		
		Map<String, String> testset = new HashMap<String, String>();
		
		testset.put("<div>Hello, <b>world!</b></div>", "\nHello, world!");
		testset.put("<div><p>Lorem ipsum</p><table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table></div>", "\n\nLorem ipsum\n\n\n\ta\t\tb\t\n\n\tc\t\td");
		
		Builder b = new Builder();
		
		for (Map.Entry<String, String> entry : testset.entrySet()) {
			Document xom = b.build(entry.getKey(), null);
			String plain = KiWiXomUtils.xom2plain(xom);
			
			log.info("xml   : #0", entry.getKey());
			log.info("expect: #0", entry.getValue());
			log.info("result: #0", plain);
			
			Assert.assertEquals(plain, entry.getValue());
		}
	}
}
