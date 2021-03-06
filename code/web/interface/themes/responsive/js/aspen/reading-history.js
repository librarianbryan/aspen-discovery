AspenDiscovery.Account.ReadingHistory = (function(){
	return {
		deleteEntry: function (patronId, id){
			if (confirm('The item will be irreversibly deleted from your reading history.  Proceed?')){
				let url = Globals.path + "/MyAccount/AJAX?method=deleteReadingHistoryEntry&patronId=" + patronId + "&permanentId=" + id;
				$.getJSON(url, function(data){
					if (data.success){
						$("#readingHistoryEntry" + id).hide();
					}else{
						AspenDiscovery.showMessage(data.title, data.message);
					}
				}).fail(AspenDiscovery.ajaxFail);
			}
			return false;
		},

		deletedMarkedAction: function (){
			if (confirm('The marked items will be irreversibly deleted from your reading history.  Proceed?')){
				$('#readingHistoryAction').val('deleteMarked');
				$('#readingListForm').submit();
			}
			return false;
		},

		deleteAllAction: function (){
			if (confirm('Your entire reading history will be irreversibly deleted.  Proceed?')){
				$('#readingHistoryAction').val('deleteAll');
				$('#readingListForm').submit();
			}
			return false;
		},

		optOutAction: function (){
			if (confirm('Opting out of Reading History will also delete your entire reading history irreversibly.  Proceed?')){
				$('#readingHistoryAction').val('optOut');
				$('#readingListForm').submit();
			}
			return false;
		},

		optInAction: function (){
			$('#readingHistoryAction').val('optIn');
			$('#readingListForm').submit();
			return false;
		},

		exportListAction: function (){
			let url = Globals.path + "/MyAccount/AJAX?method=exportReadingHistory";
			document.location.href = url;
			return false;
		}
	};
}(AspenDiscovery.Account.ReadingHistory || {}));
