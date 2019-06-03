AspenDiscovery.OverDrive = (function(){
	return {
		cancelOverDriveHold: function(patronId, overdriveId){
			if (confirm("Are you sure you want to cancel this hold?")){
				let ajaxUrl = Globals.path + "/OverDrive/AJAX?method=cancelHold&patronId=" + patronId + "&overDriveId=" + overdriveId;
				$.ajax({
					url: ajaxUrl,
					cache: false,
					success: function(data){
						if (data.success){
							AspenDiscovery.showMessage("Hold Cancelled", data.message, true);
							//remove the row from the holds list
							$("#overDriveHold_" + overdriveId).hide();
						}else{
							AspenDiscovery.showMessage("Error Cancelling Hold", data.message, false);
						}
					},
					dataType: 'json',
					async: false,
					error: function(){
						AspenDiscovery.showMessage("Error Cancelling Hold", "An error occurred processing your request in OverDrive.  Please try again in a few minutes.", false);
					}
				});
			}
			return false;
		},

		getCheckOutPrompts: function(overDriveId){
			let url = Globals.path + "/OverDrive/" + overDriveId + "/AJAX?method=getCheckOutPrompts";
			let result = true;
			$.ajax({
				url: url,
				cache: false,
				success: function(data){
					result = data;
					if (data.promptNeeded){
						AspenDiscovery.showMessageWithButtons(data.promptTitle, data.prompts, data.buttons);
					}
				},
				dataType: 'json',
				async: false,
				error: function(){
					alert("An error occurred processing your request in OverDrive.  Please try again in a few minutes.");
					AspenDiscovery.closeLightbox();
				}
			});
			return result;
		},

		checkOutTitle: function(overDriveId){
			if (Globals.loggedIn){
				//Get any prompts needed for placing holds (email and format depending on the interface.
				let promptInfo = AspenDiscovery.OverDrive.getCheckOutPrompts(overDriveId, 'hold');
				if (!promptInfo.promptNeeded){
					AspenDiscovery.OverDrive.doOverDriveCheckout(promptInfo.patronId, overDriveId);
				}
			}else{
				AspenDiscovery.Account.ajaxLogin(null, function(){
					AspenDiscovery.OverDrive.checkOutTitle(overDriveId);
				});
			}
			return false;
		},

		processOverDriveCheckoutPrompts: function(){
			let overdriveCheckoutPromptsForm = $("#overdriveCheckoutPromptsForm");
			let patronId = $("#patronId").val();
			let overdriveId = overdriveCheckoutPromptsForm.find("input[name=overdriveId]").val();
			AspenDiscovery.OverDrive.doOverDriveCheckout(patronId, overdriveId);
		},

		doOverDriveCheckout: function(patronId, overdriveId){
			if (Globals.loggedIn){
				let ajaxUrl = Globals.path + "/OverDrive/AJAX?method=checkOutTitle&patronId=" + patronId + "&overDriveId=" + overdriveId;
				$.ajax({
					url: ajaxUrl,
					cache: false,
					success: function(data){
						if (data.success === true){
							AspenDiscovery.showMessageWithButtons("Title Checked Out Successfully", data.message, data.buttons);
						}else{
							if (data.noCopies === true){
								AspenDiscovery.closeLightbox();
								ret = confirm(data.message);
								if (ret === true){
									AspenDiscovery.OverDrive.placeHold(overdriveId);
								}
							}else{
								AspenDiscovery.showMessage("Error Checking Out Title", data.message, false);
							}
						}
					},
					dataType: 'json',
					async: false,
					error: function(){
						alert("An error occurred processing your request in OverDrive.  Please try again in a few minutes.");
						//alert("ajaxUrl = " + ajaxUrl);
						AspenDiscovery.closeLightbox();
					}
				});
			}else{
				AspenDiscovery.Account.ajaxLogin(null, function(){
					AspenDiscovery.OverDrive.checkOutTitle(overdriveId);
				}, false);
			}
			return false;
		},

		doOverDriveHold: function(patronId, overDriveId, overdriveEmail, promptForOverdriveEmail){
			let url = Globals.path + "/OverDrive/AJAX?method=placeHold&patronId=" + patronId + "&overDriveId=" + overDriveId + "&overdriveEmail=" + overdriveEmail + "&promptForOverdriveEmail=" + promptForOverdriveEmail;
			$.ajax({
				url: url,
				cache: false,
				success: function(data){
					if (data.availableForCheckout){
						AspenDiscovery.OverDrive.doOverDriveCheckout(patronId, overdriveId);
					}else{
						AspenDiscovery.showMessage("Placed Hold", data.message, true);
					}
				},
				dataType: 'json',
				async: false,
				error: function(){
					AspenDiscovery.showMessage("Error Placing Hold", "An error occurred processing your request in OverDrive.  Please try again in a few minutes.", false);
				}
			});
		},

		followOverDriveDownloadLink: function(patronId, overDriveId, formatId){
			let ajaxUrl = Globals.path + "/OverDrive/AJAX?method=getDownloadLink&patronId=" + patronId + "&overDriveId=" + overDriveId + "&formatId=" + formatId;
			$.ajax({
				url: ajaxUrl,
				cache: false,
				success: function(data){
					if (data.success){
						//Reload the page
						let win = window.open(data.downloadUrl, '_blank');
						win.focus();
						//window.location.href = data.downloadUrl ;
					}else{
						alert(data.message);
					}
				},
				dataType: 'json',
				async: false,
				error: function(){
					alert("An error occurred processing your request in OverDrive.  Please try again in a few minutes.");
					AspenDiscovery.closeLightbox();
				}
			});
		},

		getOverDriveHoldPrompts: function(overDriveId){
			let url = Globals.path + "/OverDrive/" + overDriveId + "/AJAX?method=getHoldPrompts";
			let result = true;
			$.ajax({
				url: url,
				cache: false,
				success: function(data){
					result = data;
					if (data.promptNeeded){
						AspenDiscovery.showMessageWithButtons(data.promptTitle, data.prompts, data.buttons);
					}
				},
				dataType: 'json',
				async: false,
				error: function(){
					alert("An error occurred processing your request in OverDrive.  Please try again in a few minutes.");
					AspenDiscovery.closeLightbox();
				}
			});
			return result;
		},

		placeHold: function(overDriveId){
			if (Globals.loggedIn){
				//Get any prompts needed for placing holds (email and format depending on the interface.
				let promptInfo = AspenDiscovery.OverDrive.getOverDriveHoldPrompts(overDriveId, 'hold');
				if (!promptInfo.promptNeeded){
					AspenDiscovery.OverDrive.doOverDriveHold(promptInfo.patronId, overDriveId, promptInfo.overdriveEmail, promptInfo.promptForOverdriveEmail);
				}
			}else{
				AspenDiscovery.Account.ajaxLogin(null, function(){
					AspenDiscovery.OverDrive.placeHold(overDriveId);
				});
			}
			return false;
		},

		processOverDriveHoldPrompts: function(){
			let overdriveHoldPromptsForm = $("#overdriveHoldPromptsForm");
			let patronId = $("#patronId").val();
			let overdriveId = overdriveHoldPromptsForm.find("input[name=overdriveId]").val();
			let promptForOverdriveEmail;
			if (overdriveHoldPromptsForm.find("input[name=promptForOverdriveEmail]").is(":checked")){
				promptForOverdriveEmail = 0;
			}else{
				promptForOverdriveEmail = 1;
			}
			let overdriveEmail = overdriveHoldPromptsForm.find("input[name=overdriveEmail]").val();
			AspenDiscovery.OverDrive.doOverDriveHold(patronId, overdriveId, overdriveEmail, promptForOverdriveEmail);
		},

		returnCheckout: function (patronId, overDriveId){
			if (confirm('Are you sure you want to return this title?')){
				AspenDiscovery.showMessage("Returning Title", "Returning your title in OverDrive.  This may take a minute.");
				let ajaxUrl = Globals.path + "/OverDrive/AJAX?method=returnCheckout&patronId=" + patronId + "&overDriveId=" + overDriveId;
				$.ajax({
					url: ajaxUrl,
					cache: false,
					success: function(data){
						AspenDiscovery.showMessage("Title Returned", data.message);
						if (data.success){
							//Reload the page
							setTimeout(function(){
								AspenDiscovery.closeLightbox();
								//Refresh the page
								window.location.href = window.location.href ;
							}, 3000);
						}
					},
					dataType: 'json',
					async: false,
					error: function(){
						AspenDiscovery.showMessage("Error Returning Title", "An error occurred processing your request in OverDrive.  Please try again in a few minutes.");
					}
				});
			}
			return false;
		},

		selectOverDriveDownloadFormat: function(patronId, overDriveId){
			var selectedOption = $("#downloadFormat_" + overDriveId + " option:selected");
			var selectedFormatId = selectedOption.val();
			var selectedFormatText = selectedOption.text();
			if (selectedFormatId == -1){
				alert("Please select a format to download.");
			}else{
				if (confirm("Are you sure you want to download the " + selectedFormatText + " format? You cannot change format after downloading.")){
					var ajaxUrl = Globals.path + "/OverDrive/AJAX?method=selectOverDriveDownloadFormat&patronId=" + patronId + "&overDriveId=" + overDriveId + "&formatId=" + selectedFormatId;
					$.ajax({
						url: ajaxUrl,
						cache: false,
						success: function(data){
							if (data.success){
								//Reload the page
								window.location.href = data.downloadUrl;
							}else{
								AspenDiscovery.showMessage("Error Selecting Format", data.message);
							}
						},
						dataType: 'json',
						async: false,
						error: function(){
							AspenDiscovery.showMessage("Error Selecting Format", "An error occurred processing your request in OverDrive.  Please try again in a few minutes.");
						}
					});
				}
			}
			return false;
		}
	}
}(AspenDiscovery.OverDrive || {}));