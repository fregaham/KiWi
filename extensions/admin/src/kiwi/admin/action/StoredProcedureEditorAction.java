/*
 * File : StoredProcedureEditorAction.java.java
 * Date : Sep 15, 2010
 * 
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 The KiWi Project. All rights reserved.
 * http://www.kiwi-project.eu
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  KiWi designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 */


package kiwi.admin.action;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import kiwi.admin.api.sunspace.StoredProcedureManagerException;
import kiwi.admin.api.sunspace.StoredProcedureManagerService;
import kiwi.model.ceq.StoredProcedure;
import kiwi.util.izpack.DatabaseSystem;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;


/**
 * Handle the user gestures with the stored procedure editor UI.
 * 
 * @author Mihai
 * @version 0.9
 * @since 0.9
 */
@Name("storedProcedureEditor")
public class StoredProcedureEditorAction {

    /**
     * The name for the actual stored procedure.
     */
    private String name;

    /**
     * The content(SQL or JAVA) for the actual stored procedure.
     */
    private String content;

    /**
     * The textual description for for the actual stored
     * procedure.
     */
    private String description;

    /**
     * The database type used by the actual stored procedure.
     */
    private String dabaseType;

    /**
     * All the KiWi action types for a stored procedure.
     */
    private List<String> actionTypes;

    /**
     * The KiWi action type for the current stored procedure.
     */
    private String actionType;

    /**
     * True if the current stored procedure is active or not.
     */
    private boolean active;

    /**
     * Contains all the already defined stored procedures.
     */
    private Set<StoredProcedure> storedProcedures;

    /**
     * The stored procedure selecred by the user.
     */
    private StoredProcedure selectedStoredProcedure;

    @In
    private EntityManager entityManager;

    /**
     * All the log massages goes trough this field.
     */
    @Logger
    private Log logger;

    /**
     * Sends messages to the UI.
     */
    @In
    private FacesMessages facesMessages;

    /**
     * Service that contains the logic for this view.
     */
    @In(create = true)
    private StoredProcedureManagerService storedProcedureManagerService;

    /**
     * Default constructor.
     */
    public StoredProcedureEditorAction() {
        // UNIMPLEMENTED
    }

    /**
     * Returns true if the edited stored procedure is active.
     * 
     * @return the active flag for the current procedure.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set the active flag for the edit stored procedure. There
     * can be only one stored active procedure for each type
     * (e.g. only one active stored procedure for edit )
     * otherwise the save method may thor an exception
     * 
     * @param active true for an active stored procedure.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Acts like an constructor for the seam environment.
     */
    @Create
    public void init() {
        actionTypes = new ArrayList<String>();
        actionTypes.add("AccessDeniedActivity");
        actionTypes.add("AnnotateActivity");
        actionTypes.add("AddFriendActivity");
        actionTypes.add("ChangeAuthorActivity");
        actionTypes.add("CommentActivity");
        actionTypes.add("CreateActivity");
        actionTypes.add("DeleteActivity");
        actionTypes.add("EditActivity");
        actionTypes.add("RateActivity");
        actionTypes.add("ShareActivity");
        actionTypes.add("TweetActivity");
        actionTypes.add("VisitActivity");
        actionTypes.add("RemoveFriendActivity");
        actionTypes.add("GroupCreatedActivity");
        actionTypes.add("LoginActivity");
        actionTypes.add("LogoutActivity");
        actionTypes.add("RegisterActivity");
        actionTypes.add("SearchActivity");
        actionTypes.add("UserCreatedActivity");
        actionTypes.add("UserRemvedActivity");

        Collections.sort(actionTypes);

        refreshStoredProceduresSet();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void save() {
        final StoredProcedure newProcedure = getStoredProcedure();
        try {
            storedProcedureManagerService.store(newProcedure);
            if (active) {
                storedProcedureManagerService.install(newProcedure);
            }

        } catch (StoredProcedureManagerException e) {
            final String message = e.getMessage();
            facesMessages.add(message);
            logger.error(message, e);
        }

        // Mihai :this line can produce an out of sysnc problem
// if the storedProcedureManagerService methods are called in
// other transaction.
        storedProcedures.add(newProcedure);
    }

    public void edit() {

        if (selectedStoredProcedure == null) {
            facesMessages.add("Plase Select an Stored procedure to edit.");
            return;
        }

        final String stToEditname = selectedStoredProcedure.getName();

        final Query query =
                entityManager
                        .createNamedQuery("select.storedProcedureForNameAndDatabase");
        query.setParameter("name", stToEditname);
        query.setParameter("databaseType", DatabaseSystem.POSTGRESS.getName());

        StoredProcedure storedProcedure;
        try {
            storedProcedure = (StoredProcedure) query.getSingleResult();
        } catch (NoResultException exception) {
            final IllegalStateException illegalStateException =
                    new IllegalStateException("No database with the name "
                            + stToEditname + " in the database.");
            logger.warn(illegalStateException.getMessage(),
                    illegalStateException);
            throw illegalStateException;
        } catch (NonUniqueResultException exception) {
            final IllegalStateException illegalStateException =
                    new IllegalStateException(
                            "The database contains a duplicate for the stored procedure with the name "
                                    + stToEditname
                                    + ", fails because of constrain.");
            logger.warn(illegalStateException.getMessage(),
                    illegalStateException);
            throw illegalStateException;
        }

        name = storedProcedure.getName();
        content = storedProcedure.getContent();
        description = storedProcedure.getDescription();
        active = storedProcedure.isActive();
        actionType = storedProcedure.getAction();
        final StoredProcedure storedProcedureToStore = getStoredProcedure();

        try {
            storedProcedureManagerService.store(storedProcedureToStore);
            logger.debug(" Stored procedure [#0] was persisted.");
            if (active) {
                storedProcedureManagerService.install(storedProcedureToStore);
                logger.debug(" Stored procedure [#0] was installed.");
            }

        } catch (StoredProcedureManagerException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultContent() {
        final String dbName = DatabaseSystem.POSTGRESS.getName();
        StoredProcedure storedProcedure;
        try {
            storedProcedure =
                    storedProcedureManagerService
                            .getDefaultStoredProcedure(dbName);
            setSelectedStoredProcedure(storedProcedure);

            name = storedProcedure.getName();
            content = storedProcedure.getContent();
            description = storedProcedure.getDescription();
            active = storedProcedure.isActive();
            actionType = storedProcedure.getAction();

        } catch (StoredProcedureManagerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private StoredProcedure getStoredProcedure() {
        final StoredProcedure result = new StoredProcedure();
        result.setContent(content);
        result.setName(name);
        result.setDescription(description);
        // FIXME : NOT nice use the database type
        result.setDatabaseType("postgresql");
        result.setAction(actionType);
        result.setActive(active);

        return result;
    }

    public void remove() {

        if (selectedStoredProcedure == null) {
            facesMessages.add("Plase Select an Stored procedure to edit.");
            return;
        }

        final String prToRemove = selectedStoredProcedure.getName();
        final Query query =
                entityManager
                        .createNamedQuery("remove.storedProcedureForNameAndDatabase");
        query.setParameter("name", prToRemove);
        query.setParameter("databaseType", "postgresql");

        query.executeUpdate();

        refreshStoredProceduresSet();
    }

    /**
     * Query the underlying persistence layer and returns all the
     * persisted stored procedures. If any exception occurs then
     * the message is logged using the logging framework and the
     * seam messages.
     */
    private void refreshStoredProceduresSet() {
        try {
            storedProcedures =
                    (Set<StoredProcedure>) storedProcedureManagerService
                            .getStoredAllProcedures("postgresql");
        } catch (StoredProcedureManagerException e) {
            final String message = e.getMessage();
            facesMessages.add(message);
            logger.error(message, e);
        }
    }

    public String getDabaseType() {
        return dabaseType;
    }

    public void setDabaseType(String dabaseType) {
        this.dabaseType = dabaseType;
    }

    /**
     * @return the actionType
     */
    public List<String> getActionTypes() {
        return actionTypes;
    }

    /**
     * Registers a new list of action types, the old one (if
     * exist) is overwritten by the new one.
     * 
     * @param actionTypes the new action type list.
     */
    public void setActionTypes(List<String> actionTypes) {
        this.actionTypes = actionTypes;
    }

    /**
     * @return the actionType
     */
    public String getActionType() {
        return actionType;
    }

    /**
     * @param actionType the actionType to set
     */
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    /**
     * @return the storedProcedures
     */
    public Set<StoredProcedure> getStoredProcedures() {
        return storedProcedures;
    }

    /**
     * @param storedProcedures the storedProcedures to set
     */
    public void setStoredProcedures(Set<StoredProcedure> storedProcedures) {
        this.storedProcedures = storedProcedures;
    }

    /**
     * @return the selectedStoredProcedure
     */
    public StoredProcedure getSelectedStoredProcedure() {
        return selectedStoredProcedure;
    }

    /**
     * @param selectedStoredProcedure the selectedStoredProcedure
     *            to set
     */
    public void setSelectedStoredProcedure(
            StoredProcedure selectedStoredProcedure) {
        this.selectedStoredProcedure = selectedStoredProcedure;
    }
}
