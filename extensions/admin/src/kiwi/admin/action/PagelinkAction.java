/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kiwi.admin.action;

import java.util.ArrayList;
import java.util.List;
import org.jboss.seam.annotations.Logger;
import kiwi.api.content.ContentItemService;
import kiwi.api.pagelink.PageLinkFailedException;
import kiwi.api.pagelink.PageLinkService;
import kiwi.model.pagelinkentry.PagelinkEntry;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

/**
 *
 * @author Stefan
 */
@Name("kiwi.admin.pagelinkAction")
@Scope(ScopeType.PAGE)
public class PagelinkAction {

    @Logger
    private Log log;
    @In
    private ContentItemService contentItemService;
    @In(create=true)
    private PageLinkService pagelinkService;
    @In
    private FacesMessages facesMessages;
    
    private String linkname = "";

    private String contentItemTitle = "";

    private List<PagelinkEntry> entries = new ArrayList();

    public void addLink() {

        if(contentItemService.getContentItemByTitle(contentItemTitle) == null) {
            log.error("can not create link, page do not exist");
            facesMessages.add("can not create link, page do not exist");
            return;
        }

        PagelinkEntry entry = new PagelinkEntry();

        entry.setLinkname(linkname);
        entry.setContentItem(contentItemService.getContentItemByTitle(contentItemTitle));

        try {
            pagelinkService.savePageLink(entry);
        } catch (PageLinkFailedException e) {
            log.error("can not create link #0", e.getMessage());
            facesMessages.add("Error: #0", e.getMessage());
        }
    }

    public List<String> complete(String query) {

        return pagelinkService.getAutocompletResult(query);
    }

    public void removeLink(PagelinkEntry entry) {
        try {
            pagelinkService.removePageLink(entry);
        } catch (PageLinkFailedException e) {
            log.error("can not remove link #0", e.getMessage());
            facesMessages.add("Error: #0", e.getMessage());
        }
    }

    /**
     * @return the linkname
     */
    public String getLinkname() {
        return linkname;
    }

    /**
     * @param linkname the linkname to set
     */
    public void setLinkname(String linkname) {
        this.linkname = linkname;
    }

    /**
     * @return the contentItemTitle
     */
    public String getContentItemTitle() {
        return contentItemTitle;
    }

    /**
     * @param contentItemTitle the contentItemTitle to set
     */
    public void setContentItemTitle(String contentItemTitle) {
        this.contentItemTitle = contentItemTitle;
    }

    /**
     * @return the entries
     */
    public List<PagelinkEntry> getEntries() {
        entries = pagelinkService.getPageLinks();
        return entries;
    }

    /**
     * @param entries the entries to set
     */
    public void setEntries(List<PagelinkEntry> entries) {
        this.entries = entries;
    }
}
