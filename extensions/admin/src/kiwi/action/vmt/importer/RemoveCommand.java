package kiwi.action.vmt.importer;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.model.content.ContentItem;

import org.jboss.seam.log.Log;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;

/**
 * This class is used to remove to the underlaying persistence layer
 * a SKOS concept for a given RDF statement.
 *
 * @author mihai
 */
final class RemoveCommand implements Runnable {

    private final Statement statement;

    private final ContentItemService contentItemService;

    private final Log log;

    private final KiWiEntityManager kiwiEntityManager;

    RemoveCommand(Statement statement,
            ContentItemService contentItemService,
            KiWiEntityManager kiwiEntityManager, Log log) {
        this.statement = statement;
        this.kiwiEntityManager = kiwiEntityManager;
        this.contentItemService = contentItemService;
        this.log = log;
    }

    @Override
    public void run() {
        final Resource subject = statement.getSubject();

        final ContentItem ci =
                contentItemService.getContentItemByUri(subject.toString());
        log.debug("Try to delete concept for : #0", subject.toString());
        if (ci != null) {
            kiwiEntityManager.remove(ci);
            log.debug("Concept for [#0] as succesful deleted.",
                    subject.toString());
        }
    }

    @Override
    public String toString() {
        final StringBuffer result =
                new StringBuffer("Delete Concept for [");
        result.append("Subject : ");
        result.append(statement.getSubject());
        result.append(", ");

        result.append("Predicat : ");
        result.append(statement.getSubject());
        result.append(", ");

        result.append("Object : ");
        result.append(statement.getObject());
        result.append("].");

        return result.toString();
    }
}
