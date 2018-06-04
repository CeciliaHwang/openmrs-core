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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiquibaseVersionFinderTest {
	private static final String VERSION_1_9_X = "1.9.x";
	private static final String VERSION_2_0_X = "2.0.x";
	private static final String VERSION_2_1_X = "2.1.x";
	private static final String VERSION_2_2_X = "2.2.x";

	private static final String LIQUIBASE_SNAPSHOTS_1_9_X_LIQUIBASE_SCHEMA_ONLY_XML = "liquibase-snapshots/1.9.x/liquibase-schema-only.xml";
	private static final String LIQUIBASE_SNAPSHOTS_1_9_X_LIQUIBASE_CORE_DATA_XML = "liquibase-snapshots/1.9.x/liquibase-core-data.xml";

	private static final String LIQUIBASE_SNAPSHOTS_2_1_X_LIQUIBASE_SCHEMA_ONLY_XML = "liquibase-snapshots/2.1.x/liquibase-schema-only.xml";
	private static final String LIQUIBASE_SNAPSHOTS_2_1_X_LIQUIBASE_CORE_DATA_XML = "liquibase-snapshots/2.1.x/liquibase-core-data.xml";

	private static final String LIQUIBASE_UPDATES_2_1_X_LIQUIBASE_UPDATE_TO_LATEST_XML = "liquibase-updates/2.1.x/liquibase-update-to-latest.xml";
	private static final String LIQUIBASE_UPDATES_2_2_X_LIQUIBASE_UPDATE_TO_LATEST_XML = "liquibase-updates/2.2.x/liquibase-update-to-latest.xml";
	private static final String LIQUIBASE_UPDATES_2_0_X_LIQUIBASE_UPDATE_TO_LATEST_XML = "liquibase-updates/2.0.x/liquibase-update-to-latest.xml";

	private File snapshotFolderMock;
	private File updateFolderMock;

	private LiquibaseVersionFinder versionFinder;
	
	@Before
	public void setup() {
		initialiseMockFolders();
		
		versionFinder = new LiquibaseVersionFinder();
		versionFinder.setSnapshotFolder( snapshotFolderMock );
		versionFinder.setUpdateFolder( updateFolderMock );
	}
	
	private void initialiseMockFolders() {
		File folder_1_9 = mock( File.class );
		when( folder_1_9.getName() ).thenReturn( VERSION_1_9_X );
		when( folder_1_9.isDirectory() ).thenReturn( true );

		File folder_2_0 = mock( File.class );
		when( folder_2_0.getName() ).thenReturn( VERSION_2_0_X );
		when( folder_2_0.isDirectory() ).thenReturn( true );

		File folder_2_1 = mock( File.class );
		when( folder_2_1.getName() ).thenReturn( VERSION_2_1_X );
		when( folder_2_1.isDirectory() ).thenReturn( true );

		File folder_2_2 = mock( File.class );
		when( folder_2_2.getName() ).thenReturn( VERSION_2_2_X );
		when( folder_2_2.isDirectory() ).thenReturn( true );

		snapshotFolderMock = mock( File.class );
		when( snapshotFolderMock.listFiles() ).thenReturn( new File[]{ folder_1_9, folder_2_1 } );

		updateFolderMock = mock( File.class );
		when( updateFolderMock.listFiles() ).thenReturn( new File[]{ folder_2_0, folder_2_1, folder_2_2 } );
	}

	@Test
	public void shouldGetAllLiquibaseSnapshotVersions() {
		Set<String> actual = versionFinder.getLiquibaseSnapshotVersions();
		Set<String> expected = new HashSet<>( Arrays.asList( VERSION_1_9_X, VERSION_2_1_X ) );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldGetAllLiquibaseUpdateVersions() {
		Set<String> actual = versionFinder.getLiquibaseUpdateVersions();
		Set<String> expected = new HashSet<>( Arrays.asList( VERSION_2_0_X, VERSION_2_1_X, VERSION_2_2_X ) );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldGetLatestLiquibaseSnapshotVersion() {
		Optional<String> actual = versionFinder.getLatestLiquibaseSnapshotVersion();
		Optional<String> expected = Optional.of( VERSION_2_1_X );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldRecogniseThatAllLiquibaseUpdatesAreNeeded() {
		List<String> actual = versionFinder.getApplicableLiquibaseUpdateVersions( VERSION_1_9_X );
		List<String> expected = Arrays.asList( VERSION_2_0_X, VERSION_2_1_X, VERSION_2_2_X );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldRecogniseThatSomeLiquibaseUpdatesAreNeeded() {
		List<String> actual = versionFinder.getApplicableLiquibaseUpdateVersions( VERSION_2_0_X );
		List<String> expected = Arrays.asList( VERSION_2_1_X, VERSION_2_2_X );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldRecogniseThatNoLiquibaseUpdateIsNeeded() {
		List<String> actual = versionFinder.getApplicableLiquibaseUpdateVersions( VERSION_2_2_X );
		assertTrue( actual.isEmpty() );
	}

	@Test( expected = IllegalArgumentException.class )
	public void shouldHandleEmptyVersion() {
		versionFinder.getApplicableLiquibaseUpdateVersions( "" );
	}

	@Test
	public void shouldGetLatestLiquibaseSnapshotFilenameForSchema() {
		Optional<String> actual = versionFinder.getLatestLiquibaseSchemaSnapshotFilename();
		Optional<String> expected = Optional.of( LIQUIBASE_SNAPSHOTS_2_1_X_LIQUIBASE_SCHEMA_ONLY_XML );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldGetLatestLiquibaseSnapshotFilenameForCoreData() {
		Optional<String> actual = versionFinder.getLatestLiquibaseCoreDataSnapshotFilename();
		Optional<String> expected = Optional.of( LIQUIBASE_SNAPSHOTS_2_1_X_LIQUIBASE_CORE_DATA_XML );
		assertEquals( expected, actual );
	}
	
	@Test
	public void shouldGetLiquibaseUpdateFilenames() {
		List<String> actual = versionFinder.getApplicableLiquibaseUpdateFileNames( VERSION_2_0_X );
		List<String> expected = Arrays.asList(
			LIQUIBASE_UPDATES_2_1_X_LIQUIBASE_UPDATE_TO_LATEST_XML,
			LIQUIBASE_UPDATES_2_2_X_LIQUIBASE_UPDATE_TO_LATEST_XML );
		assertEquals( expected, actual );
	}
	
	@Test
	public void shouldGetLiquibaseUpdateFilenamesForOpenMRSVersion() {
		List<String> actual = versionFinder.getApplicableLiquibaseUpdateFileNames( "2.1.0 SNAPSHOT Build 12ab34" );
		List<String> expected = Arrays.asList(
			LIQUIBASE_UPDATES_2_2_X_LIQUIBASE_UPDATE_TO_LATEST_XML );
		assertEquals( expected, actual );
	}

	@Test
	public void shouldGetLiquibaseUpdateFilenamesForOpenMRSShortVersion() {
		List<String> actual = versionFinder.getApplicableLiquibaseUpdateFileNames( "2.1.0-12ab34" );
		List<String> expected = Arrays.asList(
			LIQUIBASE_UPDATES_2_2_X_LIQUIBASE_UPDATE_TO_LATEST_XML );
		assertEquals( expected, actual );
	}
	
	@Test
	public void shouldGetLiquibaseSnapshotFilenames() {
		List<String> actual = versionFinder.getLiquibaseSnapshotFilenames( "1.2.3" );
		List<String> expected = Arrays.asList(
			"liquibase-snapshots/1.2.3/liquibase-schema-only.xml",
			"liquibase-snapshots/1.2.3/liquibase-core-data.xml"
		);

		assertEquals( expected, actual );
	}
	
	@Test
	public void shouldGetAllCombinationsOfLiquibaseChangeSets() {
		Set<List<String>> actual = versionFinder.getLiquibaseChangesetCombinations();
		
		List<String> liquibaseChangeSetsForSnapshot_1_9 = Arrays.asList(
			LIQUIBASE_SNAPSHOTS_1_9_X_LIQUIBASE_SCHEMA_ONLY_XML,
			LIQUIBASE_SNAPSHOTS_1_9_X_LIQUIBASE_CORE_DATA_XML,
			LIQUIBASE_UPDATES_2_0_X_LIQUIBASE_UPDATE_TO_LATEST_XML,
			LIQUIBASE_UPDATES_2_1_X_LIQUIBASE_UPDATE_TO_LATEST_XML,
			LIQUIBASE_UPDATES_2_2_X_LIQUIBASE_UPDATE_TO_LATEST_XML
		);

		List<String> liquibaseChangeSetsForSnapshot_2_1 = Arrays.asList(
			LIQUIBASE_SNAPSHOTS_2_1_X_LIQUIBASE_SCHEMA_ONLY_XML,
			LIQUIBASE_SNAPSHOTS_2_1_X_LIQUIBASE_CORE_DATA_XML,
			LIQUIBASE_UPDATES_2_2_X_LIQUIBASE_UPDATE_TO_LATEST_XML
		);

		Set<List<String>> expected = new HashSet<>();
		expected.add( liquibaseChangeSetsForSnapshot_1_9 );
		expected.add( liquibaseChangeSetsForSnapshot_2_1 );
		
		assertEquals( expected, actual );
	}
	
	@Test
	public void shouldReturnSubFolderNames() {
		Set<String> actual = versionFinder.getSubfolderNames( snapshotFolderMock );

		Set<String> expected = new HashSet<>(  );
		expected.add( VERSION_1_9_X );
		expected.add( VERSION_2_1_X );

		assertEquals( expected, actual );
	}
	
	@Test
	public void shouldHandleNullWhenEngangingWithFileSystem() {
		File folder = mock( File.class );
		when(folder.listFiles()).thenReturn( null );
		
		Set<String> actual = versionFinder.getSubfolderNames( folder );
		Set<String> expected = new HashSet<>();

		assertEquals( expected, actual );
	}
}
