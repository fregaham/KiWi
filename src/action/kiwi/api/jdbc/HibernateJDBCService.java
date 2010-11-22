/*
 * File : HibernateJDBCService.java.java
 * Date : Sep 29, 2010
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


package kiwi.api.jdbc;


import kiwi.util.izpack.DatabaseSystem;

import org.hibernate.jdbc.Work;


/**
 * Define a hibernate specific way to use a JDBC connection. <br>
 * The interface define two methods, one (the execute method) is
 * used to execute JDBC statements on the underlying database and
 * the other one is used to obtain information about the
 * underlying database system, this because some JDBC related
 * action may depend on the database. <br>
 * <b>Note : </b> This service must be used when the JPA standard
 * features are not enough (e.g. BLOB manipulation, native SQL,
 * stored procedures).
 * 
 * @author mihai
 * @version 0.9
 * @since 0.9
 */
public interface HibernateJDBCService {

    /**
     * Runs an <code>Work</code> instance over the underlying
     * persistence layer. The Work is used to encapsulate an unit
     * of work that can be executed on a given JDBC connection.<br>
     * The next describes shows the usage for a simple SQL query
     * :
     * 
     * <pre>
     * ....
     * HibernateJDBCService jdbcService;
     * ...
     * final Work work = new Work() {
     *     public void execute(Connection conn) throws SQLException {
     *          CallableStatement stmt = conn.prepareCall("SELECT * FROM *");
     *          stmt.execute();
     *     }
     * };
     * ....
     * jdbcService.execute(work);
     * ...
     * </pre>
     * 
     * @param work the task(unit of work) to execute.
     */
    void execute(Work work);

    /**
     * Returns the underlying database system.
     * 
     * @return the underlying database system.
     */
    DatabaseSystem getDatasbaseSystem();
}
