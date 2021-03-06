<?php

require_once ROOT_DIR . '/sys/DB/DataObject.php';
class RecordToInclude extends DataObject{
	public $id;
	public $indexingProfileId;
	public $location;
	public $subLocation;
	public $iType;
	public $audience;
	public $format;
	public $includeHoldableOnly;
	public $includeItemsOnOrder;
	public $includeEContent;
	//The next 3 fields allow inclusion or exclusion of records based on a marc tag
	public $marcTagToMatch;
	public $marcValueToMatch;
	public $includeOnlyMatches;
	//The next 2 fields determine how urls are constructed
	public $urlToMatch;
	public $urlReplacement;

	public $weight;

	static function getObjectStructure(){
		$indexingProfiles = array();
		require_once ROOT_DIR . '/sys/Indexing/IndexingProfile.php';
		$indexingProfile = new IndexingProfile();
		$indexingProfile->orderBy('name');
		$indexingProfile->find();
		while ($indexingProfile->fetch()){
			$indexingProfiles[$indexingProfile->id] = $indexingProfile->name;
		}
		$structure = array(
			'id' => array('property'=>'id', 'type'=>'label', 'label'=>'Id', 'description'=>'The unique id'),
			'weight' => array('property'=>'weight', 'type'=>'integer', 'label'=>'Weight', 'description'=>'The sort order', 'default' => 0),
			'indexingProfileId' => array('property' => 'indexingProfileId', 'type' => 'enum', 'values' => $indexingProfiles, 'label' => 'Indexing Profile Id', 'description' => 'The Indexing Profile this map is associated with'),
			'location' => array('property'=>'location', 'type'=>'text', 'label'=>'Location', 'description'=>'A regular expression for location codes to include', 'maxLength' => '100', 'required' => true,'forcesReindex' => true),
			'subLocation' => array('property'=>'subLocation', 'type'=>'text', 'label'=>'Sub Location', 'description'=>'A regular expression for sublocation codes to include', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'iType' => array('property'=>'iType', 'type'=>'text', 'label'=>'iType', 'description'=>'A regular expression for item types to include', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'audience' => array('property'=>'audience', 'type'=>'text', 'label'=>'Audience', 'description'=>'A regular expression for audiences to include', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'format' => array('property'=>'format', 'type'=>'text', 'label'=>'Format', 'description'=>'A regular expression for formats to include', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'includeHoldableOnly' => array('property'=>'includeHoldableOnly', 'type'=>'checkbox', 'label'=>'Include Holdable Only', 'description'=>'Whether or not non-holdable records are included','forcesReindex' => true),
			'includeItemsOnOrder' => array('property'=>'includeItemsOnOrder', 'type'=>'checkbox', 'label'=>'Include Items On Order', 'description'=>'Whether or not order records are included', 'default' => 1,'forcesReindex' => true),
			'includeEContent' => array('property'=>'includeEContent', 'type'=>'checkbox', 'label'=>'Include e-content Items', 'description'=>'Whether or not e-Content should be included', 'default' => 1,'forcesReindex' => true),
			'marcTagToMatch' => array('property'=>'marcTagToMatch', 'type'=>'text', 'label'=>'Tag To Match', 'description'=>'MARC tag(s) to match', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'marcValueToMatch' => array('property'=>'marcValueToMatch', 'type'=>'text', 'label'=>'Value To Match', 'description'=>'The value to match within the MARC tag(s) if multiple tags are specified, a match against any tag will count as a match of everything', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'includeExcludeMatches' => array('property'=>'includeExcludeMatches', 'type'=>'enum', 'values' => array('1'=>'Include Matches','0'=>'Exclude Matches'), 'label'=>'Include Matches?', 'description'=>'Whether or not matches are included or excluded', 'default'=>1,'forcesReindex' => true),
			'urlToMatch' => array('property'=>'urlToMatch', 'type'=>'text', 'label'=>'URL To Match', 'description'=>'URL to match when rewriting urls', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
			'urlReplacement' => array('property'=>'urlReplacement', 'type'=>'text', 'label'=>'URL Replacement', 'description'=>'The replacement pattern for url rewriting', 'maxLength' => '100', 'required' => false,'forcesReindex' => true),
		);
		return $structure;
	}
}