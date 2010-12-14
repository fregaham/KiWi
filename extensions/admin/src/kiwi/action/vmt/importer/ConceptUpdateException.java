/*
 * File : ConceptUpdateException.java.java
 * Date : Dec 10, 2010
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


package kiwi.action.vmt.importer;


import org.openrdf.model.Statement;


/**
 * /** Signals a error occur during a thesaurus update process
 * for a singular concept. In most of the cases it chain the real
 * cause for the exception, this can be obtained by calling the
 * <code>getCause</code> method. More this exception can
 * transport the rdf statement (concept) that generate the
 * exception, this can be obtained with the
 * <code>getStatement</code> method .
 * 
 * @author mihai
 * @version 1.01
 * @since 1.01
 */
public class ConceptUpdateException extends ThesaurusUpdateException {

    private Statement statement;

    ConceptUpdateException() {
    }

    ConceptUpdateException(Throwable cause, Statement statement) {
        super(cause);
        this.statement = statement;
    }

    ConceptUpdateException(Throwable cause) {
        super(cause);
    }

    ConceptUpdateException(Statement statement) {
        this.statement = statement;
    }

    Statement getStatement() {
        return statement;
    }

    void setStatement(Statement statement) {
        this.statement = statement;
    }
}