/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kiwi.service.pagelink;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.pagelink.PageLinkFailedException;
import kiwi.api.pagelink.PageLinkServiceLocal;
import kiwi.api.pagelink.PageLinkServiceRemote;
import kiwi.model.pagelinkentry.PagelinkEntry;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import javax.persistence.Query;

import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;

/**
 *
 * @author Stefan
 */
@Name("pagelinkService")
public class PageLinkServiceImpl implements PageLinkServiceLocal, PageLinkServiceRemote {

    @Logger
    private Log log;
    @In
    private EntityManager entityManager;

    public void savePageLink(PagelinkEntry entry) throws PageLinkFailedException {

        if (entry == null || entry.getLinkname() == null || entry.getLinkname().equals("")) {
            throw new PageLinkFailedException("Not valid entry or entry linkname");
        }

        if (entry.getLinkname() == null) {
            log.info("Persisting new PageLink #0 ", entry.getLinkname());
            entityManager.persist(entry);
        } else if (!entityManager.contains(entry)) {
            log.info("Merging ContentItem #0 ", entry.getLinkname());
            entityManager.merge(entry);
        } else {
            log.debug("Do nothing with ContentItem #0 ", entry.getLinkname());
        }

    }

    public void removePageLink(PagelinkEntry entry) throws PageLinkFailedException {

        if (entry == null || entry.getLinkname() == null || entry.getLinkname().equals("")) {
            throw new PageLinkFailedException("Not valid entry or entry linkname");
        } else if (!this.containsPageLink(entry.getLinkname())) {
            throw new PageLinkFailedException("PagelinkEntry do not exist: " + entry.getLinkname());
        }

        PagelinkEntry remove = entityManager.find(PagelinkEntry.class, entry.getLinkname());
        entityManager.remove(remove);
    }

    public List<PagelinkEntry> getPageLinks() {
        Query q = entityManager.createNamedQuery("pagelinkService.allPagelinks");
        List<PagelinkEntry> result = q.getResultList();
        return result;
    }

    public PagelinkEntry getPageLink(String page) throws PageLinkFailedException {
        return entityManager.find(PagelinkEntry.class, page);
    }

    public List<String> getAutocompletResult(String contentItemTitle) {

        if (contentItemTitle == null || contentItemTitle.equals("")) {
            return new ArrayList();
        }

        Query q = entityManager.createNamedQuery("pagelinkService.autocomplete");
        q.setParameter("part", contentItemTitle.toLowerCase() + "%");
        q.setMaxResults(10);
        List<String> result = q.getResultList();
        return result;
    }

    public boolean containsPageLink(String page) {

        try {
            return getPageLink(page) != null;
        } catch (PageLinkFailedException e) {
            return false;
        }
    }
}
