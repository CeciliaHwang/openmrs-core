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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.Assert.assertEquals;

public class ChangeLogVersionsTest {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger( ChangeLogVersionFinderTest.class);

	// TODO TRUNK-4830 base names are redundant with definition in ChangeLogVersionFinder
	//
	private static final String CORE_DATA_BASE_NAME = "liquibase-core-data-";
	private static final String CORE_DATA_PATTERN = "classpath:org/openmrs/liquibase/snapshots/core-data/*";

	private static final String SCHEMA_ONLY_BASE_NAME = "liquibase-schema-only-";
	private static final String SCHEMA_ONLY_PATTERN = "classpath:org/openmrs/liquibase/snapshots/schema-only/*";
	
	private static final String UPDATE_TO_LATEST_BASE_NAME = "liquibase-update-to-latest-";
	private static final String UPDATE_TO_LATEST_PATTERN = "classpath:org/openmrs/liquibase/updates/*";

	private ChangeLogVersions changeLogVersions;
	
	@Before
	public void setup() {
		changeLogVersions = new ChangeLogVersions();	
	}

	/**
	 * This test compares the static list of Liquibase snapshot versions defined by
	 * org.openmrs.liquibase.ChangeLogVersions#getSnapshotVersions() with the list of actual change log files 
	 * in the two folders
	 * <li> openmrs-core/api/src/main/resources/liquibase/snapshots/core-data
	 * <li> openmrs-core/api/src/main/resources/liquibase/snapshots/schema-only
	 *
	 * If this test fails, org.openmrs.liquibase.ChangeLogVersions#SNAPSHOT_VERSIONS needs to be updated.
	 *
	 * @throws IOException
	 */
	@Test
	public void shouldGetSnapshotVersions() throws IOException {
		compareActualAndExpectedChangeLogs(
			changeLogVersions.getSnapshotVersions(),
			CORE_DATA_BASE_NAME,
			CORE_DATA_PATTERN
		);
		compareActualAndExpectedChangeLogs(
			changeLogVersions.getSnapshotVersions(),
			SCHEMA_ONLY_BASE_NAME,
			SCHEMA_ONLY_PATTERN
		);
	}
	
	/**
	 * This test compares the static list of Liquibase update versions defined by
	 * org.openmrs.liquibase.ChangeLogVersions#getUpdateVersions() with the list of actual change log files 
	 * in the folder openmrs-core/api/src/main/resources/liquibase/updates.
	 *
	 * If this test fails, org.openmrs.liquibase.ChangeLogVersions#UPDATE_VERSIONS needs to be updated.
	 *
	 * @throws IOException
	 */
	@Test
	public void shouldGetUpdateVersions() throws IOException {
		compareActualAndExpectedChangeLogs( 
			changeLogVersions.getUpdateVersions(),
			UPDATE_TO_LATEST_BASE_NAME,
			UPDATE_TO_LATEST_PATTERN
		);
	}

	/**
	 * Tests a helper method implemented in this test class.
	 */
	@Test
	public void shouldGetChangeLogNameFromVersions() {
		List<String> actual = this.getChangelogNamesFromVersions( 
			Arrays.asList( "alpha", "bravo", "charlie" ),
			"basename-"
		);
		List<String> expected = Arrays.asList(
			"basename-alpha.xml",
			"basename-bravo.xml",
			"basename-charlie.xml"
			);
		assertEquals( expected, actual );
	}
	
	private void compareActualAndExpectedChangeLogs( 
		List<String> versions, 
		String basename, 
		String pattern
	) throws IOException {
		List<String> expectedChangeLogFiles = getChangelogNamesFromVersions(
			versions,
			basename
		);
		List<String> actualChangeLogFiles = lookupLiquibaseChangeLogs( pattern );
		assertEquals( expectedChangeLogFiles, actualChangeLogFiles );
	}
	
	private List<String> getChangelogNamesFromVersions(
		List<String> versions, 
		String baseName 
	) {
		List<String> changeLogNames = new ArrayList<>(  );
		for ( String version : versions ) {
			changeLogNames.add( String.format( "%s%s.xml", baseName, version ) );
		}
		return changeLogNames;
	}

	private List<String> lookupLiquibaseChangeLogs( String resourcePattern ) throws IOException {
		PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resourcePatternResolver.getResources( resourcePattern );

		log.debug( String.format( "Liquibase resources found for pattern '%s' are: %s", resourcePattern, Arrays.toString( resources ) ) );

		return Arrays.stream( resources )
			.map( resource -> resource.getFilename() )
			.sorted()
			.collect( Collectors.toList());
	}
}
