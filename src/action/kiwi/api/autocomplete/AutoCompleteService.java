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
package kiwi.api.autocomplete;

import java.util.List;
import java.util.Set;

import kiwi.model.kbase.KiWiResource;

/**
 * AutoCompleteService - a service offering autocompletion facilities over the resources stored 
 * in the KiWi System. 
 * 
 * <p>
 * Autocompletion works over titles as well as metadata properties (e.g. type,  tags, certain fields) 
 * and is implemented using the KiWi search facilities. In the most complex case, autocompletion is
 * specified using an AutocompleteCriteria object that further restricts the returned results.
 * <p>
 * 
 * 
 *
 * @author Sebastian Schaffert
 *
 */
public interface AutoCompleteService {

	/**
	 * Standard autocompletion over the title of resources. 
	 * 
	 * @param prefix the string prefix of the title to match
	 * @return a list of content items matching the prefix, or empty list in case prefix is less than or equal 2 characters.
	 */
	public List<KiWiResource> autocomplete(String prefix);
	
	
	/**
	 * Autocompletion over the title of resources restricted to one of the types whose URI is given in the set
	 * passed as second argument.
	 * @param prefix the string prefix of the title to match
	 * @param typeURIs a set containing the URIs of types whose instances are admitted in the autocompletion result
	 * @return a list of content items matching the prefix, or empty list in case prefix is less than or equal 2 characters.
	 */
	public List<KiWiResource> autocomplete(String prefix, Set<String> typeURIs);
	
	
	/**
	 * Autocompletion over the title of resources. Result will be a list of tuples restricted to the search fields given
	 * as argument; useful for efficient autocompletion with minimal data.
	 * 
	 * @param prefix
	 * @param fields
	 * @return
	 */
	public List<String[]> autocompleteFields(String prefix, String[] fields);

}
