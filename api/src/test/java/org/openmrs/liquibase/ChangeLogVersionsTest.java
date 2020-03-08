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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.Assert.assertEquals;

public class ChangeLogVersionsTest {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger( ChangeLogVersionFinderTest.class);

	private static final String LIQUIBASE_SNAPSHOTS_PATTERN = "classpath:liquibase-snapshots/*";
	private static final String LIQUIBASE_UPDATES_PATTERN = "classpath:liquibase-updates/*";

	/**
	 * This test compares the static list of Liquibase snapshot versions that is defined by
	 * org.openmrs.liquibase.LiquibaseVersionFinder#LIQUIBASE_SNAPSHOT_VERSIONS with the names of sub-folders 
	 * underneath openmrs-core/api/src/main/resources/liquibase-snapshots.
	 *
	 * If this test fails, org.openmrs.liquibase.LiquibaseVersionFinder#LIQUIBASE_SNAPSHOT_VERSIONS needs to be updated.
	 *
	 * @throws IOException
	 */
	@Test
	public void shouldGetLiquibaseSnapshotVersions() throws IOException {
		ChangeLogVersions changeLogVersions = new ChangeLogVersions();
		List<String> actual = changeLogVersions.getSnapshotVersions();
		List<String> expected = lookupLiquibaseVersions( LIQUIBASE_SNAPSHOTS_PATTERN );
		assertEquals( expected, actual );
	}

	/**
	 * This test compares the static list of Liquibase snapshot versions that is defined by
	 * org.openmrs.liquibase.LiquibaseVersionFinder#LIQUIBASE_UPDATE_VERSIONS with the names of sub-folders 
	 * underneath openmrs-core/api/src/main/resources/liquibase-updates.
	 *
	 * If this test fails, org.openmrs.liquibase.LiquibaseVersionFinder#LIQUIBASE_UPDATE_VERSIONS needs to be updated.
	 *
	 * @throws IOException
	 */
	@Test
	public void shouldGetLiquibaseUpdateVersions() throws IOException {
		ChangeLogVersions changeLogVersions = new ChangeLogVersions();
		List<String> actual = changeLogVersions.getUpdateVersions();
		List<String> expected = lookupLiquibaseVersions( LIQUIBASE_UPDATES_PATTERN );
		assertEquals( expected, actual );
	}

	private List<String> lookupLiquibaseVersions( String resourcePattern ) throws IOException {
		PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resourcePatternResolver.getResources( resourcePattern );

		log.debug( String.format( "Liquibase resources found for pattern '%s' are: %s", resourcePattern, Arrays.toString( resources ) ) );

		return Arrays.stream( resources )
			.map( resource -> resource.getFilename() )
			.sorted()
			.collect( Collectors.toList());
	}
}
