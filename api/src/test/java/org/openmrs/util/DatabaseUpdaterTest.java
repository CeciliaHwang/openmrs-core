/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util;

import java.io.IOException;
import java.util.HashMap;
import liquibase.exception.LockException;
import org.junit.After;
import org.junit.Test;
import org.openmrs.test.BaseContextSensitiveTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests methods on the {@link DatabaseUpdater} class. This class expects /metadata/model to be on
 * the classpath so that the liquibase-update-to-latest.xml can be found.
 */
public class DatabaseUpdaterTest extends BaseContextSensitiveTest {

	private static final Logger log = LoggerFactory.getLogger(DatabaseUpdaterTest.class);

	/**
	 * @throws LockException
	 * @see DatabaseUpdater#updatesRequired()
	 */
	@Test
	public void updatesRequired_shouldAlwaysHaveAValidUpdateToLatestFile() throws Exception {
		// expects /metadata/model to be on the classpath so that
		// the liquibase-update-to-latest.xml can be found.
		try {
			DatabaseUpdater.updatesRequired();
		}
		catch ( RuntimeException | IOException rex) {
			log.error("Runtime Exception in test for Validation Errors");
		}
		// does not run DatabaseUpdater.update() because hsqldb doesn't like single quotes in strings
	}

	@Test ( expected =  IllegalArgumentException.class )
	public void shouldRejectNullAsChangelog() throws DatabaseUpdateException, InputRequiredException {
		DatabaseUpdater.executeChangelog( null, ( DatabaseUpdater.ChangeSetExecutorCallback ) null );
	}

	@Test
	public void shouldRejectNullAsChangelogFilenames() {
		try {
			DatabaseUpdater.getUnrunDatabaseChanges( (String[]) null );
			fail();
		} catch ( RuntimeException re ) {
			assertTrue( re.getCause() instanceof IllegalArgumentException );
		}
	}

	@Test
	public void shouldEmptyArrayAsChangelogFilenames() {
		try {
			DatabaseUpdater.getUnrunDatabaseChanges( new String[0] );
			fail();
		} catch ( RuntimeException re ) {
			assertTrue( re.getCause() instanceof IllegalArgumentException );
		}
	}
}
