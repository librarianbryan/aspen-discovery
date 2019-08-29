<?php

require_once ROOT_DIR . '/sys/Indexing/TranslationMap.php';
require_once ROOT_DIR . '/sys/Indexing/FormatMapValue.php';
require_once ROOT_DIR . '/sys/Indexing/StatusMapValue.php';
class SideLoad extends DataObject{
	public $__table = 'sideloads';    // table name

	public $id;
	public $name;
	public $marcPath;
	public $filenamesToInclude;
	public $marcEncoding;
	public $individualMarcPath;
	public $numCharsToCreateFolderFrom;
	public $createFolderFromLeadingCharacters;
	public $groupingClass;
	public $indexingClass;
	public $recordDriver;
	public $recordUrlComponent;
	public $recordNumberTag;
	public $recordNumberSubfield;
	public $recordNumberPrefix;

	public $suppressItemlessBibs;
	public $itemTag;
	public $itemRecordNumber;
	public $location;
	public $locationsToSuppress;
	public $itemUrl;
	public $format;

	public $formatSource;
	public $specifiedFormat;
	public $specifiedFormatCategory;
	public $specifiedFormatBoost;

	public $runFullUpdate;
    public $lastUpdateOfChangedRecords;
    public $lastUpdateOfAllRecords;

    static function getObjectStructure(){
		$translationMapStructure = TranslationMap::getObjectStructure();
		unset($translationMapStructure['indexingProfileId']);

		$sierraMappingStructure = SierraExportFieldMapping::getObjectStructure();
		unset($sierraMappingStructure['indexingProfileId']);

	    $statusMapStructure = StatusMapValue::getObjectStructure();
	    unset($statusMapStructure['indexingProfileId']);

	    $formatMapStructure = FormatMapValue::getObjectStructure();
	    unset($formatMapStructure['indexingProfileId']);

	    global $serverName;
		$structure = array(
			'id' => array('property'=>'id', 'type'=>'label', 'label'=>'Id', 'description'=>'The unique id within the database'),
			'name' => array('property' => 'name', 'type' => 'text', 'label' => 'Name', 'maxLength' => 50, 'description' => 'A name for this side load', 'required' => true),
			'marcPath' => array('property' => 'marcPath', 'type' => 'text', 'label' => 'MARC Path', 'maxLength' => 100, 'description' => 'The path on the server where MARC records can be found', 'required' => true, 'default' => "/data/aspen-discovery/{$serverName}/{sideload_name}/marc"),
			'filenamesToInclude' => array('property' => 'filenamesToInclude', 'type' => 'text', 'label' => 'Filenames to Include', 'maxLength' => 250, 'description' => 'A regular expression to determine which files should be grouped and indexed', 'required' => true, 'default' => '.*\.ma?rc'),
			'marcEncoding' => array('property' => 'marcEncoding', 'type' => 'enum', 'label' => 'MARC Encoding', 'values' => array('MARC8' => 'MARC8', 'UTF8' => 'UTF8', 'UNIMARC' => 'UNIMARC', 'ISO8859_1' => 'ISO8859_1', 'BESTGUESS' => 'BESTGUESS'), 'default' => 'UTF8'),
			'individualMarcPath' => array('property' => 'individualMarcPath', 'type' => 'text', 'label' => 'Individual MARC Path', 'maxLength' => 100, 'description' => 'The path on the server where individual MARC records can be found', 'required' => true, 'default' => "/data/aspen-discovery/{$serverName}/{sideload_name}/marc_recs"),
			'numCharsToCreateFolderFrom' => array('property' => 'numCharsToCreateFolderFrom', 'type' => 'integer', 'label' => 'Number of characters to create folder from', 'maxLength' => 50, 'description' => 'The number of characters to use when building a sub folder for individual marc records', 'required' => false, 'default' => '4'),
			'createFolderFromLeadingCharacters' => array('property'=>'createFolderFromLeadingCharacters', 'type'=>'checkbox', 'label'=>'Create Folder From Leading Characters', 'description'=>'Whether we should look at the start or end of the folder when .', 'hideInLists' => true, 'default' => 0),

			'groupingClass' => array('property' => 'groupingClass', 'type' => 'text', 'label' => 'Grouping Class', 'maxLength' => 50, 'description' => 'The class to use while grouping the records', 'required' => true, 'hideInLists' => true, 'default' => 'SideLoadedRecordGrouper'),
			'indexingClass' => array('property' => 'indexingClass', 'type' => 'text', 'label' => 'Indexing Class', 'maxLength' => 50, 'description' => 'The class to use while indexing the records', 'required' => true, 'hideInLists' => true, 'default' => 'SideLoadedEContentProcessor'),
			'recordDriver' => array('property' => 'recordDriver', 'type' => 'text', 'label' => 'Record Driver', 'maxLength' => 50, 'description' => 'The record driver to use while displaying information in Pika', 'required' => true, 'hideInLists' => true, 'default' => 'SideLoadedRecord'),

			'recordUrlComponent' => array('property' => 'recordUrlComponent', 'type' => 'text', 'label' => 'Record URL Component', 'maxLength' => 50, 'description' => 'The Module to use within the URL', 'required' => true, 'default' => 'Change based on name'),

			'recordNumberTag' => array('property' => 'recordNumberTag', 'type' => 'text', 'label' => 'Record Number Tag', 'maxLength' => 3, 'description' => 'The MARC tag where the record number can be found', 'required' => true, 'default' => '001'),
            'recordNumberSubfield' => array('property' => 'recordNumberSubfield', 'type' => 'text', 'label' => 'Record Number Subfield', 'maxLength' => 1, 'description' => 'The subfield where the record number is stored', 'required' => true, 'default'=>'a'),

			'itemSection' => ['property'=>'itemSection', 'type' => 'section', 'label' =>'Item Information', 'hideInLists' => true, 'properties' => [
				'suppressItemlessBibs' => array('property' => 'suppressItemlessBibs', 'type' => 'checkbox', 'label' => 'Suppress Itemless Bibs', 'description' => 'Whether or not Itemless Bibs can be suppressed', 'default' => false),
				'itemTag' => array('property' => 'itemTag', 'type' => 'text', 'label' => 'Item Tag', 'maxLength' => 3, 'description' => 'The MARC tag where items can be found'),
				'itemRecordNumber' => array('property' => 'itemRecordNumber', 'type' => 'text', 'label' => 'Item Record Number', 'maxLength' => 1, 'description' => 'Subfield for the record number for the item'),
				'location' => array('property' => 'location', 'type' => 'text', 'label' => 'Location', 'maxLength' => 1, 'description' => 'Subfield for location'),
				'locationsToSuppress' => array('property' => 'locationsToSuppress', 'type' => 'text', 'label' => 'Locations To Suppress', 'maxLength' => 255, 'description' => 'A regular expression for any locations that should be suppressed'),
				'itemUrl' => array('property' => 'itemUrl', 'type' => 'text', 'label' => 'Item URL', 'maxLength' => 1, 'description' => 'Subfield for a URL specific to the item'),
				'format' => array('property' => 'format', 'type' => 'text', 'label' => 'Format', 'maxLength' => 1, 'description' => 'The subfield to use when determining format based on item information'),
			]],

			'formatSection' => ['property'=>'formatMappingSection', 'type' => 'section', 'label' =>'Format Information', 'hideInLists' => true, 'properties' => [
				'formatSource' => array('property' => 'formatSource', 'type' => 'enum', 'label' => 'Load Format from', 'values' => array('bib' => 'Bib Record', 'item' => 'Item Record', 'specified'=> 'Specified Value'), 'default' => 'bib'),
				'specifiedFormat' => array('property' => 'specifiedFormat', 'type' => 'text', 'label' => 'Specified Format', 'maxLength' => 50, 'description' => 'The format to set when using a defined format', 'required' => false, 'default' => ''),
				'specifiedFormatCategory' => array('property' => 'specifiedFormatCategory', 'type' => 'enum', 'values' => array('', 'Books' => 'Books', 'eBook' => 'eBook', 'Audio Books' => 'Audio Books', 'Movies' => 'Movies', 'Music' => 'Music', 'Other' => 'Other'), 'label' => 'Specified Format Category', 'maxLength' => 50, 'description' => 'The format category to set when using a defined format', 'required' => false, 'default' => ''),
				'specifiedFormatBoost' => array('property' => 'specifiedFormatBoost', 'type' => 'integer', 'label' => 'Specified Format Boost', 'maxLength' => 50, 'description' => 'The format boost to set when using a defined format', 'required' => false, 'default' => '8'),
			]],

            'runFullUpdate' => array('property' => 'runFullUpdate', 'type' => 'checkbox', 'label' => 'Run Full Update', 'description'=>'Whether or not a full update of all records should be done on the next pass of indexing', 'default'=>0),
            'lastUpdateOfChangedRecords' => array('property' => 'lastUpdateOfChangedRecords', 'type' => 'integer', 'label' => 'Last Update of Changed Records', 'description'=>'The timestamp when just changes were loaded', 'default'=>0),
            'lastUpdateOfAllRecords' => array('property' => 'lastUpdateOfAllRecords', 'type' => 'integer', 'label' => 'Last Update of All Records', 'description'=>'The timestamp when all records were loaded from the API', 'default'=>0),

		);

		return $structure;
	}

	public function update(){
		$ret = parent::update();
		if ($ret !== FALSE ){
			if (!file_exists($this->marcPath)){
				mkdir($this->marcPath,0770, true);
			}
			if (!file_exists($this->individualMarcPath)){
				mkdir($this->individualMarcPath,0770, true);
			}
		}
		return true;
	}

	public function insert(){
		$ret = parent::insert();
		if ($ret !== FALSE ){
			if (!file_exists($this->marcPath)){
				mkdir($this->marcPath,0770, true);
			}
			if (!file_exists($this->individualMarcPath)){
				mkdir($this->individualMarcPath,0770, true);
			}
		}
		return $ret;
	}
}