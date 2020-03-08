/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.liquibase;

import java.util.Arrays;
import java.util.List;

/**
 * Defines which Liquibase snapshot and update change logs are available at all. 
 * 
 * The information provided by this class needs to be updated when new Liquibase snapshots and updates
 * are added to openmrs-api resources.
 * 
 * TODO TRUNK-4830 add reference to this class to openmrs-liquibase/README.md
 * 
 */
public class ChangeLogVersions {

	/*
	 * TODO TRUNK-4830 refactor folder and file structure for snapshot and update change log files
	 *  
	 * - move all files to the resources directory and give up the idea of having subfolders per version ?
	 * - make version number part of the filename (as opposed to having folders named after the version and using the 
	 *   same file names within these folders)
	 * - why use the same logical file path for all update files? That made sense for early ones but could be stopped.
	 * - are ids of change sets unique across change log files? extract all ids and check for uniqueness
	 * - would using the same logical file path for all versions would make life easier? No it would not, it is essential 
	 *   to prevent that an earlier versions of snapshot and updates are ever run on top of a later snapshot. Is there a 
	 *   Liquibase way of ensuring that? Consider preconditions at change log level
	 * 
	 */
	/**
	 * This definition of Liquibase snapshot versions needs to be kept in sync with the actual subfolders
	 * underneath openmrs-core/api/src/main/resources/liquibase-snapshots.
	 * 
	 * If the actual subfolders and the list get out of sync, org.openmrs.liquibase.ChangeLogVersionsTest fails. 
	 */
	private static final List<String> LIQUIBASE_SNAPSHOT_VERSIONS = Arrays.asList(
		"1.9.x",
		"2.1.x",
		"2.2.x",
		"2.3.x"
	);

	/**
	 * This definition of Liquibase snapshot versions needs to be kept in sync with the actual subfolders
	 * underneath openmrs-core/api/src/main/resources/liquibase-updates.
	 *
	 * If the actual subfolders and the list get out of sync, org.openmrs.liquibase.ChangeLogVersionsTest fails. 
	 */
	private static final List<String> LIQUIBASE_UPDATE_VERSIONS = Arrays.asList(
		"1.9.x",
		"2.0.x",
		"2.1.x",
		"2.2.x",
		"2.3.x",
		"2.4.x"
	);

	public List<String> getSnapshotVersions() {
		return LIQUIBASE_SNAPSHOT_VERSIONS;
	}

	public List<String> getUpdateVersions() {
		return LIQUIBASE_UPDATE_VERSIONS;
	}
}
