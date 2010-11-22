package ideator.action;

import java.util.LinkedList;
import java.util.List;

import ideator.datamodel.IdeaFacade;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.model.content.ContentItem;
import kiwi.model.content.MediaContent;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

@Name("ideator.layoutAction")
@Scope(ScopeType.SESSION)
//@Transactional
public class IdeatorLayoutAction {

	@Logger
	Log log;
	
	@Out(required=false)
	private IdeaFacade ideaFacade;
	
	@In(value="#{facesContext}")
	private FacesContext facesContext;
	
	@In(required = false)
	private ContentItem currentContentItem;
	
	@In(value="#{facesContext.externalContext}")
	private ExternalContext extCtx;
	
	@In
	private KiWiEntityManager kiwiEntityManager;
	
	public String getSearchResultItemTemplate(ContentItem ci) {
		//TODO implement
		return "pages/templates/searchresults/idea.xhtml";
	}
	
	public String getSingleViewTemplate(ContentItem ci) {
		ideaFacade = kiwiEntityManager.createFacade(currentContentItem, IdeaFacade.class);		
		return "pages/templates/singleview/idea.xhtml";
	}
	
	public String download(MediaContent m) {
			HttpServletResponse response = (HttpServletResponse)extCtx.getResponse();
			
			String contentType = m.getMimeType();
			String fileName = m.getFileName();
			byte [] data = m.getData();
			
			response.setContentType(contentType);
	        response.addHeader("Content-disposition", "attachment; filename=\"" + fileName +"\"");
			try {
				ServletOutputStream os = response.getOutputStream();
				os.write(data);
				os.flush();
				os.close();
				facesContext.responseComplete();
			} catch(Exception e) {
				log.error("\nFailure : " + e.toString() + "\n");
			}
		
		return null;
	}
	
	public LinkedList<MediaContent> getAttachments(){
		LinkedList<MediaContent> lmMc = new LinkedList<MediaContent>();
		LinkedList<ContentItem> lm = ideaFacade.getMediaContents();
		
		for(ContentItem c: lm){
			lmMc.add(c.getMediaContent());
		}
		return lmMc;
	}
	
//	public  byte [] getData() {
//		log.info(ideaFacade.getMediaContents().size());
//		byte [] x =ideaFacade.getMediaContents().get(0).getMediaContent().getData();
//		log.info(x.length);
//		
//		return x;
//	}
//	
//	public String getFileName(){
//		String fn = ideaFacade.getMediaContents().get(0).getMediaContent().getFileName();
//		log.info(fn);
//		return fn;
//	}
//	
//	public String getContentType(){
//		String x = ideaFacade.getMediaContents().get(0).getMediaContent().getMimeType();
//		log.info(x);
//		return x;
//	}
}
