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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openmrs.module.VersionComparator;

/**
 * Provides information about available Liquibase snapshot and update change logs.
 */
public class ChangeLogVersionFinder {

	static final String BASE_FOLDER_NAME = "org" + File.separator + "openmrs" + File.separator + "liquibase";
	static final String CORE_DATA_FOLDER_NAME = BASE_FOLDER_NAME + File.separator + "snapshots" + File.separator + "core-data";
	static final String SCHEMA_ONLY_FOLDER_NAME = BASE_FOLDER_NAME + File.separator + "snapshots" + File.separator + "schema-only";
	static final String UPDATES_FOLDER_NAME = BASE_FOLDER_NAME + File.separator + "updates";
	
	static final String CORE_DATA_BASE_NAME = "liquibase-core-data-";
	static final String SCHEMA_ONLY_BASE_NAME = "liquibase-schema-only-";
	static final String UPDATE_TO_LATEST_BASE_NAME = "liquibase-update-to-latest-";

	private static final String DOT_XML = ".xml";
	private static final String LOWER_CASE_X = "x";

	private ChangeLogVersions changeLogVersions;

	/**
	 * The default constructor initialises the default provider of change log versions.
	 */
	public ChangeLogVersionFinder() {
		this.changeLogVersions = new ChangeLogVersions();
	}

	/**
	 * Allows to inject a mock provider of change log versions for unit testing.
	 * 
	 * @param changeLogVersions a provider of change log versions.
	 */
	public ChangeLogVersionFinder( ChangeLogVersions changeLogVersions ) {
		this.changeLogVersions = changeLogVersions;
	}

	public Map<String, List<String>> getChangeLogCombinations() {
		Map<String, List<String>> changeLogCombinations = new HashMap<>();

		for ( String snapshotVersion : getSnapshotVersions() ) {
			List<String> changeLogFilenames = new ArrayList<>();

			changeLogFilenames.addAll( getSnapshotFilenames( snapshotVersion ) );

			changeLogFilenames.addAll( getUpdateFileNames(
				getUpdateVersionsGreaterThan( snapshotVersion )
			) );

			changeLogCombinations.put( snapshotVersion, changeLogFilenames );
		}

		return changeLogCombinations;
	}

	public Map<String, List<String>> getSnapshotCombinations() {
		Map<String, List<String>> changeLogCombinations = new HashMap<>();

		for ( String snapshotVersion : getSnapshotVersions() ) {
			List<String> changeLogFilenames = new ArrayList<>();

			changeLogFilenames.addAll( getSnapshotFilenames( snapshotVersion ) );

			changeLogCombinations.put( snapshotVersion, changeLogFilenames );
		}

		return changeLogCombinations;
	}

	public List<String> getSnapshotFilenames( String version ) {
		String versionAsDotX = getVersionAsDotX( version );
		return Arrays.asList(
			SCHEMA_ONLY_FOLDER_NAME + File.separator + SCHEMA_ONLY_BASE_NAME + versionAsDotX + DOT_XML,
			CORE_DATA_FOLDER_NAME + File.separator + CORE_DATA_BASE_NAME + versionAsDotX + DOT_XML
		);
	}

	public Optional<String> getLatestSnapshotVersion() {
		return getSnapshotVersions()
			.stream()
			.max( new VersionComparator() );
	}

	public Optional<String> getLatestSchemaSnapshotFilename() {
		Optional<String> snapshotVersion = getLatestSnapshotVersion();
		if ( snapshotVersion.isPresent() ) {
			return Optional.of(
				SCHEMA_ONLY_FOLDER_NAME + File.separator + SCHEMA_ONLY_BASE_NAME + snapshotVersion.get() + DOT_XML
			);
		}
		return Optional.empty();
	}

	public Optional<String> getLatestCoreDataSnapshotFilename() {
		Optional<String> snapshotVersion = getLatestSnapshotVersion();
		if ( snapshotVersion.isPresent() ) {
			return Optional.of(
				CORE_DATA_FOLDER_NAME + File.separator + CORE_DATA_BASE_NAME  + snapshotVersion.get() + DOT_XML
			);
		}
		return Optional.empty();
	}

	// TODO TRUNK-4830 when is 'equal to or greater than' actually needed ?
	//
	public List<String> getUpdateVersionsEqualToOrGreaterThan( String otherVersion ) {
		String shortestVersion = getVersionAsDotX( otherVersion );
		List<String> result = new ArrayList<>();

		if ( ! getUpdateVersions().contains( shortestVersion )) {
			throw new IllegalArgumentException(
				String.format("liquibase update version '%s' does not exist", shortestVersion )
			);
		}

		result.add( shortestVersion );
		result.addAll( getUpdateVersionsGreaterThan( shortestVersion ) );
		return result;
	}

	public List<String> getUpdateVersionsGreaterThan( String otherVersion ) {
		String versionAsDotX = getVersionAsDotX( otherVersion );
		VersionComparator versionComparator = new VersionComparator();

		return getUpdateVersions()
			.stream()
			.filter( updateVersion -> versionComparator.compare( updateVersion, versionAsDotX ) > 0 )
			.sorted( versionComparator )
			.collect( Collectors.toList() );
	}

	public List<String> getUpdateFileNames( List<String> versions ) {
		return versions
			.stream()
			.map( version -> UPDATES_FOLDER_NAME + File.separator + UPDATE_TO_LATEST_BASE_NAME + version + DOT_XML )
			.collect( Collectors.toList());
	}

	List<String> getSnapshotVersions() {
		return changeLogVersions.getSnapshotVersions();
	}

	List<String> getUpdateVersions() {
		return changeLogVersions.getUpdateVersions();
	}

	String getVersionAsDotX( String version ) {
		Matcher matcher = Pattern
			.compile("(\\d+\\.\\d+\\.)")
			.matcher(version);

		if ( matcher.find()) {
			return matcher.group( 1 ) + LOWER_CASE_X;
		}
		throw new IllegalArgumentException(
			String.format("version string '%s' does not match 'major.minor.' pattern", version )
		);
	}
}
