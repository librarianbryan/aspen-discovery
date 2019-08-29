package com.turning_leaf_technologies.grouping;

import com.turning_leaf_technologies.indexing.IndexingProfile;
import org.apache.logging.log4j.Logger;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.sql.Connection;
import java.util.List;

class HooplaRecordGrouper extends MarcRecordGrouper {
	/**
	 * Creates a record grouping processor that saves results to the database.
	 *
	 * @param dbConnection   - The Connection to the database
	 * @param profile        - The profile that we are grouping records for
	 * @param logger         - A logger to store debug and error messages to.
	 * @param fullRegrouping - Whether or not we are doing full regrouping or if we are only grouping changes.
	 */
	HooplaRecordGrouper(String serverName, Connection dbConnection, IndexingProfile profile, Logger logger, boolean fullRegrouping) {
		super(serverName, dbConnection, profile, logger, fullRegrouping);
	}

	protected String setGroupingCategoryForWork(Record marcRecord, String loadFormatFrom, char formatSubfield, String specifiedFormatCategory, GroupedWorkBase workForTitle) {
		//Load the format (broad format for grouping book, music, movie) we can get these from the 099
		List<DataField> fields099 = getDataFields(marcRecord, "099");
		String groupingFormat = "";
		for (DataField cur099 : fields099){
			String format = cur099.getSubfield('a').getData();
			if (format.equalsIgnoreCase("eAudiobook hoopla") || format.equalsIgnoreCase("eComic hoopla") || format.equalsIgnoreCase("eBook hoopla")){
				groupingFormat = "book";
				break;
			}else if (format.equalsIgnoreCase("eVideo hoopla")){
				groupingFormat = "movie";
				break;
			}else if (format.equalsIgnoreCase("eMusic hoopla")){
				groupingFormat = "music";
				break;
			}else{
				logger.warn("Unknown Hoopla format " + format);
			}
		}
		workForTitle.setGroupingCategory(groupingFormat);
		return groupingFormat;
	}

}
