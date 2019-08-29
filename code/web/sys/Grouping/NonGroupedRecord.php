<?php

class NonGroupedRecord extends DataObject{
	public $__table = 'nongrouped_records';
	public $id;
	public $source;
	public $recordId;
	public $notes;

	static function getObjectStructure() {
		global $indexingProfiles;
		global $sideLoadSettings;
		$availableSources = array();
		foreach ($indexingProfiles as $profile){
			$availableSources[$profile->name] = $profile->name;
		}
		foreach ($sideLoadSettings as $profile){
			$availableSources[$profile->name] = $profile->name;
		}
		$availableSources['cloud_library'] = 'Cloud Library';
		$availableSources['hoopla'] = 'Hoopla';
		$availableSources['overdrive'] = 'Overdrive';
		$availableSources['rbdigital'] = 'RBdigital';

		$structure = array(
			array(
				'property' => 'id',
				'type' => 'label',
				'label' => 'Id',
				'description' => 'The unique id of the merged grouped work in the database',
				'storeDb' => true,
			),
			array(
				'property' => 'source',
				'type' => 'enum',
				'values' => $availableSources,
				'label' => 'Source of the Record Id',
				'description' => 'The source of the record to avoid merging.',
				'default' => 'ils',
				'storeDb' => true,
				'required' => true,
			),
			array(
				'property' => 'recordId',
				'type' => 'text',
				'size' => 36,
				'maxLength' => 36,
				'label' => 'Record Id',
				'description' => 'The id of the record that should not be merged.',
				'storeDb' => true,
				'required' => true,
			),
			array(
				'property' => 'notes',
				'type' => 'text',
				'size' => 255,
				'maxLength' => 255,
				'label' => 'Notes',
				'description' => 'Notes related to the record.',
				'storeDb' => true,
				'required' => true,
			),
		);
		return $structure;
	}

}