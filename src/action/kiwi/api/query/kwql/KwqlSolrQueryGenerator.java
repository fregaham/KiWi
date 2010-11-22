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
package kiwi.api.query.kwql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.solr.client.solrj.SolrQuery;

import kiwi.util.MD5;
import kiwi.model.Constants;


public class KwqlSolrQueryGenerator {

	/**
	 * Converts the parts of an Antl syntax tree which are expressible by Solr
	 * into a SolrQuery.
	 * 
	 * @param ast
	 *            the root of the tree
	 * @return a Solr query that can be evaluated by the Solr api
	 */
	public static SolrQuery buildSolrQuery(Tree body, Collection<String> interdependentVariables) {
		SolrQuery solrQuery = new SolrQuery();
		
		String solrQueryString = KwqlSolrQueryGenerator.convertQuery(body, interdependentVariables);

		solrQuery.setQuery(solrQueryString);
		
		solrQuery.setRows(Integer.MAX_VALUE);
		solrQuery.setFields("id","title", "kiwiid", "tag", "score");
		
		solrQuery.setHighlight(true);
		solrQuery.setHighlightFragsize(200);
		solrQuery.setHighlightSimplePre("<span class=\"highlight\">");
		solrQuery.setHighlightSimplePost("</span>");

		return solrQuery;
	}

	/**
	 * Traverses an Antl tree in depth first order and converts it into a Solr
	 * query String.
	 * 
	 * @param ast
	 *            the root of the tree
	 * @return a Solr query String
	 */
	private static String convertQuery(Tree body, Collection<String> interdependentVariables) {
		StringBuilder query = new StringBuilder();
		query.append("u_");
        query.append(MD5.md5sum(Constants.NS_RDF + "type"));
        query.append(":");
        
        query.append("uri\\:\\:");
        query.append(Constants.NS_KIWI_CORE.replace(":", "\\:"));
        query.append("ContentItem AND ");
		ArrayList<String> resources = new ArrayList<String>();
		String qualifierType = "#";
		convertQuery((CommonTree) body.getChild(0), query, resources,
				qualifierType, interdependentVariables);
		return query.toString();
	}

	@SuppressWarnings("unchecked")
	private static void convertQuery(CommonTree t, StringBuilder query,
			ArrayList<String> resources, String qualifierType, Collection<String> interdependentVariables) {

		if (t != null) {
			String resourceType;
			if (resources.size() > 0) {
				resourceType = resources.get(resources.size() - 1);
			} else {
				resourceType = "#";
			}
			String oldResourceType = resourceType;
			Boolean structural = false;
				
			String label = t.getText();
			if (label.equals("RESOURCE")) {
				int lastQueryIndex = query.length()-1;
				if (qualifierType.equals("target")
						|| qualifierType.equals("child")
						|| qualifierType.equals("descendant"))
				{
					structural =  true;	
					if (!hasAncestor(t, "NOT")){
					query.append("ci_uri:[* TO *] ");
					}
					
					else if (query.length()>0 && query.charAt(lastQueryIndex)=='-')
					{
						query.delete(lastQueryIndex-4, lastQueryIndex+1);
					}}
				if (t.getChild(0).getChildCount() > 0) {
					resourceType = t.getChild(0).getChild(0).getText()
							.toLowerCase();
					resources.add(t.getChild(0).getChild(0).getText()
							.toLowerCase());
				} else {
					resourceType = "#";
				}
				if(!structural){

				convertQuery((CommonTree) t.getChild(1), query, resources,
						qualifierType, interdependentVariables);}
			} else if (label.equals("QUALIFIER")) {
				if (t.getChild(0).getChildCount() > 0) {
					qualifierType = t.getChild(0).getChild(0).getText();

				} else {
					qualifierType = "#";
				}

				convertQuery((CommonTree) t.getChild(1), query, resources,
						qualifierType, interdependentVariables);

			}

			else if (label.equals("NOT")) {
				query.append("(*:* AND "); // workaround for Solr bug
				query.append("-");
				for (int i = 0; i < t.getChildCount(); i++) {
					ArrayList<String> resourcesBackup = (ArrayList<String>) resources.clone();

					convertQuery((CommonTree) t.getChild(i), query, resources,
							qualifierType, interdependentVariables);
					resources = resourcesBackup;
				}
				query.append(")");
			} else if (label.equals("AND")) {
				query.append("("); // workaround for Solr bug

				for (int i = 0; i < t.getChildCount(); i++) {
					ArrayList<String> resourcesBackup = (ArrayList<String>) resources.clone();

					convertQuery((CommonTree) t.getChild(i), query, resources,
							qualifierType, interdependentVariables);
					resources = resourcesBackup;
					if (i < t.getChildCount() - 1) {
						query.append(" AND "); // workaround for Solr bug
					}
				}
				query.append(")");
			} else if (label.equals("OR")) {
				query.append("("); // workaround for Solr bug

				for (int i = 0; i < t.getChildCount(); i++) {
					ArrayList<String> resourcesBackup = (ArrayList<String>) resources.clone();

					convertQuery((CommonTree) t.getChild(i), query, resources,
							qualifierType, interdependentVariables);
					resources = resourcesBackup;
					if (i < t.getChildCount() - 1) {
						query.append(" OR "); // workaround for Solr bug
					}
				}
				query.append(")");
			} else {
				
				if (!label.equals("STRING")) {
					if (resources.size()>1 && (resources.get(0).equals("ci")&&resources.get(0).equals("fragment"))){
						resources.remove(0);
					}
					
					for (String resource : resources) {
						query.append(resource + "_");
					}
					
					if (resources.size()>0){
					query.deleteCharAt(query.length()-1);
					}

					 if (resources.size()>0 && !qualifierType.equals("#")) {
						query.append("_"+qualifierType.toLowerCase());
					} else if (resourceType.equals("#")
							&& qualifierType.equals("#")) {
						query.append("all");
					}
					else if (resources.size()==0 && !qualifierType.equals("#")){
						query.append(qualifierType.toLowerCase());
						query.append("s");
					}
					else{
						query.append("s");
					}
					if (!label.equals("VAR")) {
						query.append(":" + label);
					} else if (!hasAncestor(t, "NOT") || !interdependentVariables.contains(t.getChild(0).toString())) {
						query.append(":[* TO *]");
					} else if (hasAncestor(t, "NOT") || interdependentVariables.contains(t.getChild(0).toString())) {
						int trunk = query.toString().lastIndexOf("-");
						query.delete(trunk, query.length());
						query.append("*:*");
					}
				}
					if (!label.equals("VAR") && !structural){
					for (int i = 0; i < t.getChildCount(); i++) {

						ArrayList<String> resourcesBackup = (ArrayList<String>) resources.clone();
						convertQuery((CommonTree) t.getChild(i), query,
								resources, qualifierType, interdependentVariables);
						resources = resourcesBackup;
					}
				}

			}
			resourceType = oldResourceType;
		}

	}
	
	private static Boolean hasAncestor(CommonTree t, String type) {
		return ancestorDistance(t, type) != -1 ? true : false;
	}

	@SuppressWarnings("unchecked")
	private static int ancestorDistance(CommonTree t, String type) {
		List<Tree> ancestors = t.getAncestors();
		for (int i = ancestors.size() - 1; i >= 0; i--) {
			if (ancestors.get(i).getText().equals(type)) {
				return ancestors.size() - i;
			}
		}
		return -1;
	}

}
