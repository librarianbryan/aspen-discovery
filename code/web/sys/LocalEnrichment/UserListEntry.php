<?php

require_once ROOT_DIR . '/sys/DB/DataObject.php';
class UserListEntry extends DataObject{
	public $__table = 'user_list_entry';     // table name
	public $id;                              // int(11)  not_null primary_key auto_increment
	public $source;
	public $sourceId;          // int(11)  not_null multiple_key
	public $listId;                          // int(11)  multiple_key
	public $notes;                           // blob(65535)  blob
	public $dateAdded;                       // timestamp(19)  not_null unsigned zerofill binary timestamp
	public $weight;                          //Where to position the entry in the overall list

	/**
	 * @param bool $updateBrowseCategories
	 * @return bool
	 */
	function insert($updateBrowseCategories = true)
	{
		$result = parent::insert();
		if ($result && $updateBrowseCategories) {
			$this->flushUserListBrowseCategory();
			global $memCache;
			$memCache->delete('user_list_data_' . UserAccount::getActiveUserId());
		}
		return $result;
	}

	/**
	 * @param bool $updateBrowseCategories
	 * @return bool|int|mixed
	 */
	function update($updateBrowseCategories = true)
	{
		$result = parent::update();
		if ($result && $updateBrowseCategories) {
			$this->flushUserListBrowseCategory();
			global $memCache;
			$memCache->delete('user_list_data_' . UserAccount::getActiveUserId());
		}
		return $result;
	}

	/**
	 * @param bool $useWhere
	 * @param bool $updateBrowseCategories
	 * @return bool|int|mixed
	 */
	function delete($useWhere = false, $updateBrowseCategories = true)
	{
		$result = parent::delete($useWhere);
		if ($result && $updateBrowseCategories) {
			$this->flushUserListBrowseCategory();
			global $memCache;
			$memCache->delete('user_list_data_' . UserAccount::getActiveUserId());
		}
		return $result;
	}

	private function flushUserListBrowseCategory(){
		// Check if the list is a part of a browse category and clear the cache.
		require_once ROOT_DIR . '/sys/Browse/BrowseCategory.php';
		$userListBrowseCategory = new BrowseCategory();
		$userListBrowseCategory->sourceListId = $this->listId;
		if ($userListBrowseCategory->find()) {
			while ($userListBrowseCategory->fetch()) {
				$userListBrowseCategory->deleteCachedBrowseCategoryResults();
			}
		}
		$userListBrowseCategory->__destruct();
		$userListBrowseCategory = null;
	}

	public function getRecordDriver()
	{
		if ($this->source == 'GroupedWork'){
			require_once ROOT_DIR . '/RecordDrivers/GroupedWorkDriver.php';
			$recordDriver = new GroupedWorkDriver($this->sourceId);
			return $recordDriver;
		}elseif ($this->source == 'OpenArchives'){
			require_once ROOT_DIR . '/RecordDrivers/OpenArchivesRecordDriver.php';
			$recordDriver = new OpenArchivesRecordDriver($this->sourceId);
			return $recordDriver;
		}elseif ($this->source == 'Lists'){
			require_once ROOT_DIR . '/RecordDrivers/ListsRecordDriver.php';
			$recordDriver = new ListsRecordDriver($this->sourceId);
			return $recordDriver;
		}else{
			return null;
		}
	}
}
