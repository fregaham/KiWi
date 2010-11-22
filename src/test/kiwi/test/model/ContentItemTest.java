/*
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
 * 
 * 
 */

package kiwi.test.model;

import static org.junit.Assert.fail;
import junit.framework.Assert;
import kiwi.model.content.ContentItem;

import org.junit.Test;

/**
 * @author Sebastian Schaffert
 *
 */
public class ContentItemTest {

	/**
	 * Test method for {@link kiwi.model.content.ContentItem#hashCode()}.
	 */
	@Test
	public void testHashCode() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link kiwi.model.content.ContentItem#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		ContentItem ci1 = new ContentItem();
		ContentItem ci2 = new ContentItem();
		
		Assert.assertTrue(ci1.equals(ci1));
		Assert.assertTrue(ci1.equals(ci2));
		
		ci1.setTitle("Title 1");
		ci2.setTitle("Title 2");
		
		Assert.assertFalse(ci1.equals(ci2));
	}

}
