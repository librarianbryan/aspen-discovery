package com.turning_leaf_technologies.cron.marc_manipulation;

import org.apache.logging.log4j.Logger;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.regex.Pattern;

class MarcSplitOption {
	private MarcWriter marcWriter;
	private Pattern locationsToIncludePattern;
	private String itemTag;
	private char locationSubfield;

	void setFilename(String basePath, String filename, Logger logger) throws FileNotFoundException {
		if (basePath.endsWith("/")) {
			basePath = basePath.substring(0, basePath.length() -1);
		}
		File basePathFile = new File(basePath);
		if (!basePathFile.exists()){
			if (!basePathFile.mkdirs()){
				logger.error("Error creating directories for file " + basePath);
			}
		}
		marcWriter = new MarcStreamWriter(new FileOutputStream(basePath + "/" + filename), "UTF-8", true);
	}

	void setLocationsToInclude(String locationsToInclude) {
		locationsToIncludePattern = Pattern.compile(locationsToInclude);
	}

	void setItemTag(String itemTag) {
		this.itemTag = itemTag;
	}

	void setLocationSubfield(char locationSubfield) {
		this.locationSubfield = locationSubfield;
	}

	public void close() {
		marcWriter.close();
	}

	void processRecord(Record curBib) {
		//Check to see if the bib is valid for this splitter
		List<DataField> itemFields = curBib.getDataFields(itemTag);
		boolean validBib = false;
		for (DataField curItem : itemFields){
			Subfield locationSubfieldInst = curItem.getSubfield(locationSubfield);
			if (locationSubfieldInst != null){
				String locationCode = locationSubfieldInst.getData().trim();
				if (locationsToIncludePattern.matcher(locationCode).matches()){
					validBib = true;
					break;
				}
			}
		}

		if (validBib) {
			MarcFactory factory = MarcFactory.newInstance();
			//if we have a valid bib, make a copy and write it to the split marc
			Record marcCopy = factory.newRecord();
			marcCopy.setLeader(curBib.getLeader());
			for (ControlField curField : curBib.getControlFields()) {
				marcCopy.addVariableField(curField);
			}
			for (DataField curField : curBib.getDataFields()) {
				boolean addField = true;
				if (curField.getTag().equals(itemTag)) {
					Subfield locationSubfieldInst = curField.getSubfield(locationSubfield);
					if (locationSubfieldInst != null) {
						String locationCode = locationSubfieldInst.getData();
						addField = locationsToIncludePattern.matcher(locationCode).matches();
					}
				}
				if (addField) {
					marcCopy.addVariableField(curField);
				}
			}
			marcWriter.write(marcCopy);
		}
	}
}
