<?php

require_once ROOT_DIR . "/Action.php";

require_once 'Home.php';

class MyAccount_Edit extends Action
{
	function launch($msg = null)
	{
		global $interface;

		if (!UserAccount::isLoggedIn()) {
			require_once ROOT_DIR . '/services/MyAccount/Login.php';
			$launchAction = new MyAccount_Login();
			$launchAction->launch();
			exit();
		}else{
			$user = UserAccount::getLoggedInUser();
		}

		// Save Data
		$listId = isset($_REQUEST['listId']) ? $_REQUEST['listId'] : null;
		if (is_array($listId)){
			$listId = array_pop($listId);
		}
		if (!empty($listId) && is_numeric($listId)) {
			require_once ROOT_DIR . '/sys/LocalEnrichment/UserList.php';
			$userList     = new UserList();
			$userList->id = $listId;
			if ($userList->find(true)) {
				$userObj = UserAccount::getActiveUserObj();
				if ($userObj == false){
					$interface->assign('error', 'You must be logged in to edit list entries, please login again.');
				}else {
					$userCanEdit = $userObj->canEditList($userList);
					if (!$userCanEdit){
						$interface->assign('error', 'Sorry, you don\'t have permissions to edit this list.');
					}else{
						if (isset($_POST['submit'])) {
							$this->saveChanges();

							// After changes are saved, send the user back to an appropriate page;
							// either the list they were viewing when they started editing, or the
							// overall favorites list.
							if (isset($listId)) {
								$nextAction = 'MyList/' . $listId;
							} else {
								$nextAction = 'Home';
							}
							header('Location: /MyAccount/' . $nextAction);
							exit();
						}

						$interface->assign('list', $userList);

						$listEntryId = $_REQUEST['listEntryId'];
						if (!empty($listEntryId)) {

							// Retrieve saved information about record
							require_once ROOT_DIR . '/sys/LocalEnrichment/UserListEntry.php';
							$userListEntry = new UserListEntry();
							$userListEntry->id = $listEntryId;
							if ($userListEntry->find(true)) {
								$interface->assign('listEntry', $userListEntry);
								$interface->assign('recordDriver', $userListEntry->getRecordDriver());
							} else {
								$interface->assign('error', 'The item you selected is not part of the selected list.');
							}
						} else {
							$interface->assign('error', 'No ID for the list item.');
						}
					}
				}
			} else {
				$interface->assign('error', "List {$listId} was not found.");
			}
		} else {
			$interface->assign('error', 'Invalid List ID.');
		}
		$this->display('editListTitle.tpl', 'Edit List Entry');
	}

	private function saveChanges()
	{
		require_once ROOT_DIR . '/sys/LocalEnrichment/UserListEntry.php';
		$userListEntry = new UserListEntry();
		$userListEntry->id = $_REQUEST['listEntry'];
		if ($userListEntry->find(true)){
			$userListEntry->notes = strip_tags($_REQUEST['notes']);
			$userListEntry->update();
		}
	}
}

