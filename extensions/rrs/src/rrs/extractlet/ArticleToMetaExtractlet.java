package rrs.extractlet;

import gate.Document;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;

import kiwi.api.triplestore.TripleStore;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.Context;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.service.informationextraction.AbstractExtractlet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.json.JSONObject;

@Name("rrs.extractlet.articleToMetaExtractlet")
@Scope(ScopeType.STATELESS)
public class ArticleToMetaExtractlet extends AbstractExtractlet {
	
	@In(create=true)
	TripleStore tripleStore;
	
	@Logger
	private Log log;
	
	private static final String NSPUB = "http://nlp.fit.vutbr.cz/2010/publications#"; 

	public ArticleToMetaExtractlet() {
		super("rrs.extractlet.articleToMetaExtractlet");
	}
	
	public Collection<Suggestion> extract(KiWiResource context, TextContent content, Document gateDoc, Locale language) {
		
		Collection<Suggestion> ret = new LinkedList<Suggestion> ();
		if (context == null || !context.hasType(tripleStore.createUriResource(NSPUB + "Publication"))) {
			log.info("Ignoring non-publication resource #0", context);
			return ret;
		}
		
		String plain = (String)gateDoc.getFeatures().get(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME);
		
		
		try {
			HttpClient client = new HttpClient();
			PutMethod put = new PutMethod("http://athena3.fit.vutbr.cz:31480/extractmeta");
			put.setContentChunked(false);
		
			put.setRequestEntity(new StringRequestEntity(plain, "text/plain", "UTF-8"));
		
			client.executeMethod(put);
				
			String jsonlines = put.getResponseBodyAsString();
			
			// log.info("jsonlines: \n#0", jsonlines);
			
			// JSONArray array = new JSONArray(json);
			
			//for (int i = 0; i < array.length(); ++i) {
			for (String line : jsonlines.split("\n")) {
				JSONObject annotation = new JSONObject(line);//array.getJSONObject(i);
				
				int begin = annotation.getInt("begin");
				int end = annotation.getInt("end");
				
				String type = annotation.getString("type");
				if ("abstract".equals(type)) {
					String text = plain.substring(begin, end);
					log.info("Abstract: #0", text);
					
					Suggestion suggestion = new Suggestion();
					InstanceEntity entity = new InstanceEntity();
					Context ctx = new Context();
					
					ctx.setContextBegin(begin);
					ctx.setContextEnd(end);
					ctx.setInBegin(begin);
					ctx.setInEnd(end);
					ctx.setInContext(text);
					ctx.setIsFragment(true);
					
					entity.setExtractletName(getName());
					entity.setContext(ctx);
					entity.setSourceResource(context);
					entity.setSourceTextContent(content);
					
					suggestion.setExtractletName(getName());
					suggestion.setInstance(entity);
					suggestion.setKind(Suggestion.DATATYPE);
					suggestion.getRoles().add(tripleStore.createUriResource(NSPUB + "abstract"));
					suggestion.setLabel("Abstract");
					suggestion.setScore(1.0f);
					suggestion.setFeedbackScore(1.0f);
					
					ret.add(suggestion);
				}
				else if ("title".equals(type)) {
					String text = plain.substring(begin, end);
					log.info("Title: #0", text);
					
					Suggestion suggestion = new Suggestion();
					InstanceEntity entity = new InstanceEntity();
					Context ctx = new Context();
					
					ctx.setContextBegin(begin);
					ctx.setContextEnd(end);
					ctx.setInBegin(begin);
					ctx.setInEnd(end);
					ctx.setInContext(text);
					ctx.setIsFragment(true);
					
					entity.setExtractletName(getName());
					entity.setContext(ctx);
					entity.setSourceResource(context);
					entity.setSourceTextContent(content);
					
					suggestion.setExtractletName(getName());
					suggestion.setInstance(entity);
					suggestion.setKind(Suggestion.DATATYPE);
					suggestion.getRoles().add(tripleStore.createUriResource(NSPUB + "title"));
					suggestion.setLabel("Title");
					suggestion.setScore(1.0f);
					suggestion.setFeedbackScore(1.0f);
					
					ret.add(suggestion);
				}
			}
		}
		catch(Exception x) {
			log.error("Failed calling article2meta web service.", x);
		}
		
		
		return ret;
	}
	
}
