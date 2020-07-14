AspenDiscovery.Searches = (function(){
	$(document).ready(function(){
		AspenDiscovery.Searches.initAutoComplete();

		// Add Browser-stored showCovers setting to the search form if there is a stored value set, and
		// this is not a OPAC Machine, and the user is not logged in, and there is not a hidden value
		// already set in the search form.
		// This allows a preset showCovers setting to be sent back with the first search without requiring login or
		// a page reload on the search results page.
		if (!Globals.opac && !Globals.loggedIn && AspenDiscovery.hasLocalStorage() && $('input[name="showCovers"]').length === 0){
			let showCovers = window.localStorage.getItem('showCovers') || false;
			if (showCovers.length > 0) {
				$("<input>").attr({
					type: 'hidden',
					name: 'showCovers',
					value: showCovers
				}).appendTo('#searchForm');
			}
		}
	});
	return{
		searchGroups: [],
		curPage: 1,
		displayMode: 'list', // default display Mode for results
		displayModeClasses: { // browse mode to css class correspondence
			covers:'home-page-browse-thumbnails',
			list:''
		},

		getCombinedResults: function(fullId, shortId, source, searchTerm, searchType, numberOfResults){
			let url = Globals.path + '/Union/AJAX';
			let params = '?method=getCombinedResults&source=' + source + '&numberOfResults=' + numberOfResults + "&id=" + fullId + "&searchTerm=" + searchTerm + "&searchType=" + searchType;
			if ($('#hideCovers').is(':checked')){
				params += "&showCovers=off";
			}else{
				params += "&showCovers=on";
			}
			$.getJSON(url+params, function(data){
				if (data.success === 'false'){
					AspenDiscovery.showMessage("Error loading results", data.error);
				}else{
					$('#combined-results-section-results-' + shortId).html(data.results);
				}
			}).fail(AspenDiscovery.ajaxFail);
			return false;
		},

		combinedResultsDefinedOrder: [],
		reorderCombinedResults: function () {
			if ($('#combined-results-column-0').is(':visible')) {
				if ($('.combined-results-column-0', '#combined-results-column-0').length === 0){
					$('.combined-results-column-0').detach().appendTo('#combined-results-column-0');
					$('.combined-results-column-1').detach().appendTo('#combined-results-column-1');
				}
			} else {
				if ($('.combined-results-section', '#combined-results-all-column').length === 0) {
					$.each(AspenDiscovery.Searches.combinedResultsDefinedOrder, function (i, id) {
						el = $(id).parents('.combined-results-section').detach().appendTo('#combined-results-all-column');
					});
				}
			}
			return false;
		},

		getPreferredDisplayMode: function(){
			if (!Globals.opac && AspenDiscovery.hasLocalStorage()){
				temp = window.localStorage.getItem('searchResultsDisplayMode');
				if (AspenDiscovery.Searches.displayModeClasses.hasOwnProperty(temp)) {
					AspenDiscovery.Searches.displayMode = temp; // if stored value is empty or a bad value, fall back on default setting ("null" is returned from local storage when not set)
					$('input[name="view"]','#searchForm').val(AspenDiscovery.Searches.displayMode); // set the user's preferred search view mode on the search box.
				}
			}
		},

		toggleDisplayMode : function(selectedMode){
			let mode = this.displayModeClasses.hasOwnProperty(selectedMode) ? selectedMode : this.displayMode, // check that selected mode is a valid option
					searchBoxView = $('input[name="view"]','#searchForm'), // display mode variable associated with the search box
					paramString = AspenDiscovery.replaceQueryParam('page', '', AspenDiscovery.replaceQueryParam('view',mode)); // set view in url and unset page variable
			this.displayMode = mode; // set the mode officially
			this.curPage = 1; // reset js page counting
			if (searchBoxView) searchBoxView.val(this.displayMode); // set value in search form, if present
			if (!Globals.opac && AspenDiscovery.hasLocalStorage() ) { // store setting in browser if not an opac computer
				window.localStorage.setItem('searchResultsDisplayMode', this.displayMode);
			}
			if (mode === 'list') $('#hideSearchCoversSwitch').show(); else $('#hideSearchCoversSwitch').hide();
			location.replace(location.pathname + paramString); // reloads page without adding entry to history
		},

		getMoreResults: function(){
			let url = Globals.path + '/Search/AJAX',
					params = AspenDiscovery.replaceQueryParam('page', this.curPage+1)+'&method=getMoreSearchResults',
					divClass = this.displayModeClasses[this.displayMode];
			params = AspenDiscovery.replaceQueryParam('view', this.displayMode, params); // set the view url parameter just in case.
			if (params.search(/[?;&]replacementTerm=/) !== -1) {
				let searchTerm = location.search.split('replacementTerm=')[1].split('&')[0];
				params = AspenDiscovery.replaceQueryParam('lookfor', searchTerm, params);
			}
			$.getJSON(url+params, function(data){
				if (data.success === 'false'){
					AspenDiscovery.showMessage("Error loading search information", "Sorry, we were not able to retrieve additional results.");
				}else{
					let newDiv = $(data.records).hide();
					$('.'+divClass).filter(':last').after(newDiv);
					newDiv.fadeIn('slow');
					if (data.lastPage) $('#more-browse-results').hide(); // hide the load more results
					else AspenDiscovery.Searches.curPage++;
				}
			}).fail(AspenDiscovery.ajaxFail);
			return false;
		},

		initAutoComplete: function(){
			try{
				let searchTermInput = $("#lookfor");
				if (searchTermInput.length){
					searchTermInput.autocomplete({
						source:function(request,response){
							let url=Globals.path+"/Search/AJAX?method=getAutoSuggestList&searchTerm=" + $("#lookfor").val() + "&searchIndex=" + $("#searchIndex").val() + "&searchSource=" + $("#searchSource").val();
							$.ajax({
								url:url,
								dataType:"json",
								success:function(data){
									response(data);
								}
							});
						},
						position:{
							my:"left top",
							at:"left bottom",
							of:"#lookfor",
							collision:"none"
						},
						minLength:4,
						delay:600
					}).data('ui-autocomplete')._renderItem = function( ul, item ) {
						return $( "<li></li>" )
							.data( "ui-autocomplete-item", item.value )
							.append( '<a>' + item.label + '</a>' )
							.appendTo( ul );
					};
				}

			}catch(e){
				alert("error during autocomplete setup"+e);
			}
		},

		sendEmail: function(){
			if (Globals.loggedIn){
				let from = $('#from').val();
				let to = $('#to').val();
				let message = $('#message').val();
				let sourceUrl = window.location.href;

				let url = Globals.path + "/Search/AJAX";
				$.getJSON(url,
						{ // pass parameters as data
							method     : 'sendEmail'
							,from      : from
							,to        : to
							,message   : message
							,sourceUrl : sourceUrl
						},
						function(data) {
							if (data.result) {
								AspenDiscovery.showMessage("Success", data.message);
							} else {
								AspenDiscovery.showMessage("Error", data.message);
							}
						}
				);
			}
			return false;
		},

		loadSearchTypes: function(){
			let searchTypeElement = $("#searchSource");
			let catalogType = "catalog";
			if (searchTypeElement){
				let selectedSearchType = $(searchTypeElement.find(":selected"));
				if (selectedSearchType){
					catalogType = selectedSearchType.data("catalog_type");
				}
			}
			let url = "/Search/AJAX";
			$.getJSON(url,
				{ // pass parameters as data
					method : 'getSearchIndexes',
					searchSource : catalogType
				},
				function(data) {
					if (data.success) {
						let searchIndexElement = $("#searchIndex");
						if (searchIndexElement) {
							//Clear the existing options and load with the new ones
							searchIndexElement.empty();
							for(let searchIndex in data.searchIndexes) {
								let selected = "";
								if (searchIndex === data.selectedIndex){
									selected = " selected"
								}
								let defaultSearch = "";
								if (searchIndex === data.defaultSearchIndex){
									defaultSearch = " id='default_search_type'";
								}
								searchIndexElement.append("<option value='" + searchIndex + "'" + selected + defaultSearch + ">" + data.searchIndexes[searchIndex] + "</option>")
							}
						}
					}
				}
			);
		},

		resetSearchType: function(){
			if ($("#lookfor").val() === ""){
				$("#searchSource").val($("#default_search_type").val());
			}
			return true;
		},

		loadExploreMoreBar: function(section, searchTerm){
			let url = Globals.path + "/Search/AJAX";
			let params = "method=loadExploreMoreBar&section=" + encodeURIComponent(section);
			params += "&searchTerm=" + encodeURIComponent(searchTerm);
			let fullUrl = url + "?" + params;
			$.getJSON(fullUrl,
				function(data) {
					if (data.success === true){
						$("#explore-more-bar-placeholder").html(data.exploreMoreBar);
						AspenDiscovery.initCarousels();
					}
				}
			);
		},

		lockFacet: function (clusterName) {
			event.stopPropagation();
			let url = Globals.path + "/Search/AJAX";
			let params = "method=lockFacet&facet=" + encodeURIComponent(clusterName);
			let fullUrl = url + "?" + params;
			$.getJSON(fullUrl,
				function(data) {
					if (data.success === true){
						$("#facetLock_lockIcon_" + clusterName).hide();
						$("#facetLock_unlockIcon_" + clusterName).show();
					}else{
						AspenDiscovery.showMessage('Error', data.message, true);
					}
				}
			);
			return false;
		},

		unlockFacet: function (clusterName) {
			event.stopPropagation();
			let url = Globals.path + "/Search/AJAX";
			let params = "method=unlockFacet&facet=" + encodeURIComponent(clusterName);
			let fullUrl = url + "?" + params;
			$.getJSON(fullUrl,
				function(data) {
					if (data.success === true){
						$("#facetLock_lockIcon_" + clusterName).show();
						$("#facetLock_unlockIcon_" + clusterName).hide();
					}else{
						AspenDiscovery.showMessage('Error', data.message, true);
					}
				}
			);
			return false;
		},
	}
}(AspenDiscovery.Searches || {}));