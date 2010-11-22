/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kiwi.wiki.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import kiwi.api.pagelink.PageLinkService;
import kiwi.model.pagelinkentry.PagelinkEntry;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 *
 * @author Stefan
 */
@Name("wikiPageLinkAction")
public class WikiPageLinkAction implements Serializable {


    @In(create=true)
    private PageLinkService pagelinkService;

    private List<PagelinkEntry> entries = new ArrayList();

    public String getURI(PagelinkEntry entry) {

        return entry.getContentItem().getKiwiIdentifier().substring(5);
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
