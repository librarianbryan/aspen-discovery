package com.turning_leaf_technologies.symphony;

import com.turning_leaf_technologies.config.ConfigUtil;
import com.turning_leaf_technologies.indexing.IndexingProfile;
import com.turning_leaf_technologies.logging.LoggingUtil;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.marc4j.*;
import org.marc4j.marc.*;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.zip.CRC32;

public class SymphonyExportMain {
	private static Logger logger;

	private static IndexingProfile indexingProfile;

	private static Long lastSymphonyExtractTimeVariableId = null;

	private static boolean hadErrors = false;

	public static void main(String[] args){
		String serverName = args[0];

		// Set-up Logging //
		Date startTime = new Date();
		logger = LoggingUtil.setupLogging(serverName, "symphony_export");
		logger.info(startTime.toString() + ": Starting Symphony Extract");

		// Read the base INI file to get information about the server (current directory/cron/config.ini)
		Ini ini = ConfigUtil.loadConfigFile("config.ini", serverName, logger);

		//Connect to the aspen database
		Connection aspenConn = null;
		try{
			String databaseConnectionInfo = ConfigUtil.cleanIniValue(ini.get("Database", "database_aspen_jdbc"));
			aspenConn = DriverManager.getConnection(databaseConnectionInfo);

		}catch (Exception e){
			System.out.println("Error connecting to aspen database " + e.toString());
			System.exit(1);
		}

		// The time this export started
		long exportStartTime = startTime.getTime() / 1000;

		// The time the last export started
		long lastExportTime = getLastExtractTime(aspenConn);

		String profileToLoad = "ils";
		if (args.length > 1){
			profileToLoad = args[1];
		}
		indexingProfile = IndexingProfile.loadIndexingProfile(aspenConn, profileToLoad, logger);

		//Check for new marc out
		processNewMarcExports(lastExportTime, aspenConn);

		//Check for a new holds file
		processNewHoldsFile(aspenConn);

		//Check for new orders file(lastExportTime, aspenConn);
		processOrdersFile();

		//update the last export start time
		try {
			// Wrap Up
			if (!hadErrors) {
				//Update the last extract time
				if (lastSymphonyExtractTimeVariableId != null) {
					PreparedStatement updateVariableStmt = aspenConn.prepareStatement("UPDATE variables set value = ? WHERE id = ?");
					updateVariableStmt.setLong(1, exportStartTime);
					updateVariableStmt.setLong(2, lastSymphonyExtractTimeVariableId);
					updateVariableStmt.executeUpdate();
					updateVariableStmt.close();
					logger.debug("Updated last extract time to " + exportStartTime);
				} else {
					PreparedStatement insertVariableStmt = aspenConn.prepareStatement("INSERT INTO variables (`name`, `value`) VALUES ('last_symphony_extract_time', ?)");
					insertVariableStmt.setString(1, Long.toString(exportStartTime));
					insertVariableStmt.executeUpdate();
					insertVariableStmt.close();
					logger.debug("Set last extract time to " + exportStartTime);
				}
			} else {
				logger.error("There was an error updating during the extract, not setting last extract time.");
			}

			try{
				//Close the connection
				aspenConn.close();
			}catch(Exception e){
				System.out.println("Error closing connection: " + e.toString());
			}
		} catch (Exception e) {
			logger.error("MySQL Error: " + e.toString());
		}
	}

	private static void processOrdersFile() {
		File mainFile = new File(indexingProfile.getMarcPath() + "/fullexport.mrc");
		HashSet<String> idsInMainFile = new HashSet<>();
		if (mainFile.exists()){
			try {
				MarcReader reader = new MarcPermissiveStreamReader(new FileInputStream(mainFile), true, true);
				int numRecordsRead = 0;
				while (reader.hasNext()) {
					try {
						Record marcRecord = reader.next();
						numRecordsRead++;
						String id = getPrimaryIdentifierFromMarcRecord(marcRecord);
						idsInMainFile.add(id);
					}catch (MarcException me){
						logger.warn("Error processing individual record  on record " + numRecordsRead + " of " + mainFile.getAbsolutePath(), me);
					}
				}
			}catch (Exception e){
				logger.error("Error loading existing marc ids", e);
			}
		}

		//We have gotten 2 different exports a single export as CSV and a second daily version as XLSX.  If the XLSX exists, we will
		//process that and ignore the CSV version.
		File ordersFileMarc = new File(indexingProfile.getMarcPath() + "/Pika_orders.mrc");
		File ordersFile = new File(indexingProfile.getMarcPath() + "/PIKA-onorderfile.txt");
		convertOrdersFileToMarc(ordersFile, ordersFileMarc, idsInMainFile);

	}

	private static void convertOrdersFileToMarc(File ordersFile, File ordersFileMarc, HashSet<String> idsInMainFile) {
		if (ordersFile.exists()){
			long now = new Date().getTime();
			long ordersFileLastModified = ordersFile.lastModified();
			if (now - ordersFileLastModified > 7 * 24 * 60 * 60 * 1000){
				logger.warn("Orders File was last written more than 7 days ago");
			}
			//Always process since we only received one export and we are gradually removing records as they appear in the full export.
			try{
				MarcWriter writer = new MarcStreamWriter(new FileOutputStream(ordersFileMarc, false), "UTF-8", true);
				BufferedReader ordersReader = new BufferedReader(new InputStreamReader(new FileInputStream(ordersFile)));
				String line = ordersReader.readLine();
				int numOrderRecordsWritten = 0;
				int numOrderRecordsSkipped = 0;
				while (line != null){
					int firstPipePos = line.indexOf('|');
					if (firstPipePos != -1){
						String recordNumber = line.substring(0, firstPipePos);
						line = line.substring(firstPipePos + 1);
						if (recordNumber.matches("^\\d+$")) {
							if (!idsInMainFile.contains("a" + recordNumber)){
								if (line.endsWith("|")){
									line = line.substring(0, line.length() - 1);
								}
								int lastPipePosition = line.lastIndexOf('|');
								String title = line.substring(lastPipePosition + 1);
								line = line.substring(0, lastPipePosition);
								lastPipePosition = line.lastIndexOf('|');
								String author = line.substring(lastPipePosition + 1);
								line = line.substring(0, lastPipePosition);
								String ohohseven = line.replace("|", " ");
								//The marc record does not exist, create a temporary bib in the orders file which will get processed by record grouping
								MarcFactory factory = MarcFactory.newInstance();
								Record marcRecord = factory.newRecord();
								marcRecord.addVariableField(factory.newControlField("001", "a" + recordNumber));
								if (!ohohseven.equals("-")) {
									marcRecord.addVariableField(factory.newControlField("007", ohohseven));
								}
								if (!author.equals("-")){
									marcRecord.addVariableField(factory.newDataField("100", '0', '0', "a", author));
								}
								marcRecord.addVariableField(factory.newDataField("245", '0', '0', "a", title));
								writer.write(marcRecord);
								numOrderRecordsWritten++;
							}else{
								logger.info("Marc record already exists for a" + recordNumber);
								numOrderRecordsSkipped++;
							}
						}
					}
					line = ordersReader.readLine();
				}
				writer.close();
				logger.info("Finished writing Orders to MARC record");
				logger.info("Wrote " + numOrderRecordsWritten);
				logger.info("Skipped " + numOrderRecordsSkipped + " because they are in the main export");
			}catch (Exception e){
				logger.error("Error reading orders file ", e);
			}
		}else{
			logger.warn("Could not find orders file at " + ordersFile.getAbsolutePath());
		}
	}

	/**
	 * Check the marc folder to see if the holds files have been updated since the last export time.
	 *
	 * If so, load a count of holds per bib and then update the database.
	 *
	 * @param aspenConn       the connection to the database
	 */
	private static void processNewHoldsFile(Connection aspenConn) {
		HashMap<String, Integer> holdsByBib = new HashMap<>();
		boolean writeHolds = false;
		File holdFile = new File(indexingProfile.getMarcPath() + "/Pika_Holds.csv");
		if (holdFile.exists()){
			long now = new Date().getTime();
			long holdFileLastModified = holdFile.lastModified();
			if (now - holdFileLastModified > 2 * 24 * 60 * 60 * 1000){
				logger.warn("Holds File was last written more than 2 days ago");
			}else{
				writeHolds = true;
				String lastCatalogIdRead = "";
				try {
					BufferedReader reader = new BufferedReader(new FileReader(holdFile));
					String line = reader.readLine();
					while (line != null){
						int firstComma = line.indexOf(',');
						if (firstComma > 0){
							String catalogId = line.substring(0, firstComma);
							catalogId = catalogId.replaceAll("\\D", "");
							lastCatalogIdRead = catalogId;
							//Make sure the catalog is numeric
							if (catalogId.length() > 0 && catalogId.matches("^\\d+$")){
								if (holdsByBib.containsKey(catalogId)){
									holdsByBib.put(catalogId, holdsByBib.get(catalogId) +1);
								}else{
									holdsByBib.put(catalogId, 1);
								}
							}
						}
						line = reader.readLine();
					}
				}catch (Exception e){
					logger.error("Error reading holds file ", e);
					hadErrors = true;
				}
				logger.info("Read " + holdsByBib.size() + " bibs with holds, lastCatalogIdRead = " + lastCatalogIdRead);
			}
		}else{
			logger.warn("No holds file found at " + indexingProfile.getMarcPath() + "/Pika_Holds.csv");
			hadErrors = true;
		}

		File periodicalsHoldFile = new File(indexingProfile.getMarcPath() + "/Pika_Hold_Periodicals.csv");
		if (periodicalsHoldFile.exists()){
			long now = new Date().getTime();
			long holdFileLastModified = periodicalsHoldFile.lastModified();
			if (now - holdFileLastModified > 2 * 24 * 60 * 60 * 1000){
				logger.warn("Periodicals Holds File was last written more than 2 days ago");
			}else {
				writeHolds = true;
				try {
					BufferedReader reader = new BufferedReader(new FileReader(periodicalsHoldFile));
					String line = reader.readLine();
					String lastCatalogIdRead = "";
					while (line != null){
						int firstComma = line.indexOf(',');
						if (firstComma > 0){
							String catalogId = line.substring(0, firstComma);
							catalogId = catalogId.replaceAll("\\D", "");
							lastCatalogIdRead = catalogId;
							//Make sure the catalog is numeric
							if (catalogId.length() > 0 && catalogId.matches("^\\d+$")){
								if (holdsByBib.containsKey(catalogId)){
									holdsByBib.put(catalogId, holdsByBib.get(catalogId) +1);
								}else{
									holdsByBib.put(catalogId, 1);
								}
							}
						}
						line = reader.readLine();
					}
					logger.info(holdsByBib.size() + " bibs with holds (including periodicals) lastCatalogIdRead for periodicals = " + lastCatalogIdRead);
				}catch (Exception e){
					logger.error("Error reading periodicals holds file ", e);
					hadErrors = true;
				}
			}
		}else{
			logger.warn("No periodicals holds file found at " + indexingProfile.getMarcPath() + "/Pika_Hold_Periodicals.csv" );
			hadErrors = true;
		}

		//Now that we've counted all the holds, update the database
		if (!hadErrors && writeHolds){
			try {
				aspenConn.setAutoCommit(false);
				aspenConn.prepareCall("DELETE FROM ils_hold_summary").executeUpdate();
				logger.info("Removed existing holds");
				PreparedStatement updateHoldsStmt = aspenConn.prepareStatement("INSERT INTO ils_hold_summary (ilsId, numHolds) VALUES (?, ?)");
				for (String ilsId : holdsByBib.keySet()){
					updateHoldsStmt.setString(1, "a" + ilsId);
					updateHoldsStmt.setInt(2, holdsByBib.get(ilsId));
					int numUpdates = updateHoldsStmt.executeUpdate();
					if (numUpdates != 1){
						logger.info("Hold was not inserted " + "a" + ilsId + " " + holdsByBib.get(ilsId));
					}
				}
				aspenConn.commit();
				aspenConn.setAutoCommit(true);
				logger.info("Finished adding new holds to the database");
			}catch (Exception e){
				logger.error("Error updating holds database", e);
				hadErrors = true;
			}
		}
	}

	/**
	 * Check the updates folder for any files that have arrived since our last export, but after the
	 * last full export.
	 *
	 * If we get new files, load the MARC records from the file and compare what we have on disk.
	 * If the checksum has changed, we should mark the records as updated in the database and replace
	 * the current MARC with the new record.
	 *
	 * @param lastExportTime the last time the export was run
	 * @param aspenConn       the connection to the database
	 */
	private static void processNewMarcExports(long lastExportTime, Connection aspenConn) {
		File fullExportFile = new File(indexingProfile.getMarcPath() + "/fullexport.mrc");
		File fullExportDirectory = fullExportFile.getParentFile();
		File sitesDirectory = fullExportDirectory.getParentFile();
		File updatesDirectory = new File(sitesDirectory.getAbsolutePath() + "/marc_updates");
		File updatesFile = new File(updatesDirectory.getAbsolutePath() + "/Pika-hourly.mrc");
		if (!fullExportFile.exists()){
			logger.error("Full export file did not exist");
			hadErrors = true;
			return;
		}
		if (!updatesFile.exists()){
			logger.warn("Updates file did not exist");
			hadErrors = true;
			return;
		}
		if (updatesFile.lastModified() < fullExportFile.lastModified()){
			logger.debug("Updates File was written before the full export, ignoring");
			return;
		}
		if (updatesFile.lastModified() < lastExportTime){
			logger.info("Not processing updates file because it hasn't changed since the last run of the export process.");
			return;
		}

		//If we got this far we have a good updates file to process.
		try {
			PreparedStatement getChecksumStmt = aspenConn.prepareStatement("SELECT checksum FROM ils_marc_checksums where source = ? AND ilsId = ?");
			PreparedStatement updateChecksumStmt = aspenConn.prepareStatement("UPDATE ils_marc_checksums set checksum = ? where source = ? AND ilsId = ?");
			PreparedStatement getGroupedWorkIdStmt = aspenConn.prepareStatement("SELECT grouped_work_id from grouped_work_primary_identifiers WHERE type = ? AND identifier = ?");
			PreparedStatement updateGroupedWorkStmt = aspenConn.prepareStatement("UPDATE grouped_work set date_updated = ? where id = ?");

			MarcReader updatedMarcReader = new MarcStreamReader(new FileInputStream(updatesFile));
			while (updatedMarcReader.hasNext()){
				Record marcRecord = updatedMarcReader.next();
				//Get the id of the record
				String recordNumber = getPrimaryIdentifierFromMarcRecord(marcRecord);
				//Check to see if the checksum has changed
				getChecksumStmt.setString(1, indexingProfile.getName());
				getChecksumStmt.setString(2, recordNumber);
				ResultSet getChecksumRS = getChecksumStmt.executeQuery();
				if (getChecksumRS.next()){
					//If it has, write the file to disk and update the database
					long oldChecksum = getChecksumRS.getLong(1);
					long newChecksum = getChecksum(marcRecord);
					if (oldChecksum != newChecksum){
						getGroupedWorkIdStmt.setString(1, indexingProfile.getName());
						getGroupedWorkIdStmt.setString(2, recordNumber);
						ResultSet getGroupedWorkIdRS = getGroupedWorkIdStmt.executeQuery();
						if (getGroupedWorkIdRS.next()) {
							long groupedWorkId = getGroupedWorkIdRS.getLong(1);

							//Save the marc record
							File ilsFile = indexingProfile.getFileForIlsRecord(recordNumber);
							MarcStreamWriter writer2 = new MarcStreamWriter(new FileOutputStream(ilsFile,false), "UTF-8", true);
							writer2.setAllowOversizeEntry(true);
							writer2.write(marcRecord);
							writer2.close();

							//Mark the work as changed
							updateGroupedWorkStmt.setLong(1, new Date().getTime() / 1000);
							updateGroupedWorkStmt.setLong(2, groupedWorkId);
							updateGroupedWorkStmt.executeUpdate();

							//Save the new checksum so we don't reprocess
							updateChecksumStmt.setLong(1, newChecksum);
							updateChecksumStmt.setString(2, indexingProfile.getName());
							updateChecksumStmt.setString(3, recordNumber);
							updateChecksumStmt.executeUpdate();
						}else{
							logger.warn("Could not find grouped work for MARC " + recordNumber);
						}
					}else{
						logger.debug("Skipping MARC " + recordNumber + " because it hasn't changed");
					}
				}else{
					logger.debug("MARC Record " + recordNumber + " is new since the last full export");
				}

			}
		}catch (Exception e){
			logger.error("Error loading updated marcs", e);
			hadErrors = true;
		}
	}

	private static String getPrimaryIdentifierFromMarcRecord(Record marcRecord) {
		List<VariableField> recordNumberFields = marcRecord.getVariableFields(indexingProfile.getRecordNumberTag());
		String recordNumber = null;
		//Make sure we only get one ils identifier
		for (VariableField curVariableField : recordNumberFields) {
			if (curVariableField instanceof DataField) {
				DataField curRecordNumberField = (DataField) curVariableField;
				Subfield subfieldA = curRecordNumberField.getSubfield('a');
				if (subfieldA != null && (indexingProfile.getRecordNumberPrefix().length() == 0 || subfieldA.getData().length() > indexingProfile.getRecordNumberPrefix().length())) {
					if (curRecordNumberField.getSubfield('a').getData().substring(0, indexingProfile.getRecordNumberPrefix().length()).equals(indexingProfile.getRecordNumberPrefix())) {
						recordNumber = curRecordNumberField.getSubfield('a').getData().trim();
						break;
					}
				}
			} else {
				//It's a control field
				ControlField curRecordNumberField = (ControlField) curVariableField;
				recordNumber = curRecordNumberField.getData().trim();
				break;
			}
		}
		return recordNumber;
	}

	private static Long getLastExtractTime(Connection vufindConn) {
		Long lastSymphonyExtractTime = null;
		try {
			PreparedStatement loadLastSymphonyExtractTimeStmt = vufindConn.prepareStatement("SELECT * from variables WHERE name = 'last_symphony_extract_time'", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ResultSet lastSymphonyExtractTimeRS = loadLastSymphonyExtractTimeStmt.executeQuery();
			if (lastSymphonyExtractTimeRS.next()){
				lastSymphonyExtractTime           = lastSymphonyExtractTimeRS.getLong("value");
				SymphonyExportMain.lastSymphonyExtractTimeVariableId = lastSymphonyExtractTimeRS.getLong("id");
				logger.debug("Last extract time was " + lastSymphonyExtractTime);
			}else{
				logger.debug("Last extract time was not set in the database");
			}

			//Last Update in UTC
			Date now             = new Date();
			Date yesterday       = new Date(now.getTime() - 24 * 60 * 60 * 1000);
			// Add a small buffer (2 minutes) to the last extract time
			Date lastExtractDate = (lastSymphonyExtractTime != null) ? new Date((lastSymphonyExtractTime * 1000) - (120 * 1000)) : yesterday;

			if (lastExtractDate.before(yesterday)){
				logger.warn("Last Extract date was more than 24 hours ago.  Just getting the last 24 hours since we should have a full extract.");
				lastSymphonyExtractTime = yesterday.getTime();
			}else{
				lastSymphonyExtractTime = lastExtractDate.getTime();
			}

		} catch (Exception e) {
			logger.error("Error getting last Extract Time for CarlX", e);
		}
		return lastSymphonyExtractTime;
	}

	private static long getChecksum(Record marcRecord) {
		CRC32 crc32 = new CRC32();
		String marcRecordContents = marcRecord.toString();
		//There can be slight differences in how the record length gets calculated between ILS export and what is written
		//by MARC4J since there can be differences in whitespace and encoding.
		// Remove the text LEADER
		// Remove the length of the record
		// Remove characters in position 12-16 (position of data)
		marcRecordContents = marcRecordContents.substring(12, 19) + marcRecordContents.substring(24).trim();
		marcRecordContents = marcRecordContents.replaceAll("\\p{C}", "?");
		crc32.update(marcRecordContents.getBytes());
		return crc32.getValue();
	}
}
