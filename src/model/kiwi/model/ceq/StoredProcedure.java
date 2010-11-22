/*
 * File : SoredProcedure.java.java Date : Apr 13, 2010 DO NOT ALTER OR REMOVE
 * COPYRIGHT NOTICES OR THIS HEADER. Copyright 2008 The KiWi Project. All rights
 * reserved. http://www.kiwi-project.eu The contents of this file are subject to
 * the terms of either the GNU General Public License Version 2 only ("GPL") or
 * the Common Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the License.
 * You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html or nbbuild/licenses/CDDL-GPL-2-CP.
 * See the License for the specific language governing permissions and
 * limitations under the License. When distributing the software, include this
 * License Header Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP. KiWi designates this particular file as
 * subject to the "Classpath" exception as provided by Sun in the GPL Version 2
 * section of the License file that accompanied this code. If applicable, add
 * the following below the License Header, with the fields enclosed by brackets
 * [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]" If you wish your
 * version of this file to be governed by only the CDDL or only the GPL Version
 * 2, indicate your decision by adding "[Contributor] elects to include this
 * software in this distribution under the [CDDL or GPL Version 2] license." If
 * you do not indicate a single choice of license, a recipient has the option to
 * distribute your version of this file under either the CDDL, the GPL Version 2
 * or to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL Version
 * 2 license, then the option applies only if the new code is made subject to
 * such option by the copyright holder. Contributor(s):
 */


package kiwi.model.ceq;


import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;


/**
 * Used to store a stored procedure. <br>
 * This entity defines also the following named queries :
 * <ul>
 * <li>select.storedProcedureForDatabase - used for all stored
 * procedures for a given database (type).
 * <li>select.storedProcedureForNameAndDatabase - used for all
 * stored procedures with a given name and given database (type).
 * <li>select.storedProcedureForActionAndDatabase - used for all
 * stored procedures for a given kiwi action and given database
 * (type).
 * <li>select.storedActiveProcedures - used for all the active
 * (the active field is true) stored procedures for given
 * database (type).
 * <li>select.storedProcedureForContent - used for all the stored
 * procedures with a given content and a given database (type).
 * <li>remove.storedProcedureForNameAndDatabase - removes all the
 * stored procedures with a given name and given database (type).
 * </ul>
 * 
 * @author mradules
 * @version 0.9
 * @since 0.9
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "select.storedProcedureForDatabase", query = "SELECT sp FROM StoredProcedure AS sp WHERE "
                + "sp.databaseType=:databaseType"),

        @NamedQuery(name = "select.storedProcedureForNameAndDatabase", query = "SELECT sp FROM StoredProcedure AS sp WHERE "
                + "sp.name=:name  AND " + "sp.databaseType=:databaseType"),

        @NamedQuery(name = "select.storedProcedureForActionAndDatabase", query = "SELECT sp FROM StoredProcedure AS sp WHERE "
                + "sp.action=:action AND " + "sp.databaseType=:databaseType"),

        @NamedQuery(name = "select.storedActiveProcedures", query = "SELECT sp FROM StoredProcedure AS sp WHERE "
                + "sp.action=:action AND "
                + "sp.databaseType=:databaseType AND " + "sp.active=:active"),

        @NamedQuery(name = "select.storedProcedureForContent", query = "SELECT sp FROM StoredProcedure AS sp WHERE "
                + "sp.content=:content AND " + "sp.databaseType=:databaseType"),

        @NamedQuery(name = "remove.storedProcedureForNameAndDatabase", query = "DELETE FROM StoredProcedure AS sp WHERE "
                + "sp.name=:name AND " + "sp.databaseType=:databaseType")})
public class StoredProcedure implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1130143852702025197L;

    /**
     * The unique id for this entity.
     */
    private Long id;

    /**
     * The name for the stored procedure.
     */
    private String name;

    /**
     * The description for this stored procedure.
     */
    private String description;

    /**
     * The SQL/Java content for this stored procedure. In some
     * cases this content will be updated with a header and
     * footer according with the underlying database.
     */
    private String content;

    /**
     * The Database type for this stored procedure. <br>
     * FIXME : use the Enumm here (DatabaseSystem)
     */
    private String databaseType;

    /**
     * The KiWi action type for for this stored procedure.
     */
    private String action;

    /**
     * True if this stored procedure is active or not.
     */
    private boolean active;

    /**
     * Builds a <code>StoredProcedure</code> instance.
     */
    public StoredProcedure() {
        // UNIMPLEMENTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Lob
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns a string representation for this stored procedure
     * where most of the attributes are listed.
     * 
     * @return a string representation for this stored procedure.
     */
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("SoredProcedure [name=");
        result.append(name);
        result.append(", action type=");
        result.append(action);
        result.append(", action active=");
        result.append(active);
        result.append(", database type=");
        result.append(databaseType);
        result.append(", description=");
        result.append(description);
        result.append("content= ");
        result.append(content);
        result.append("]");

        return result.toString();
    }
}
