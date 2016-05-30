angular.module('dataset_view', ['ngMaterial'])
.directive('datasetView', function() {
	return {
		templateUrl: '/knowage/js/src/angular_1.4/tools/workspace/scripts/directive/dataset-view/dataset-view.html',
		controller: datasetViewControllerFunction,
		 priority: 10,
		scope: {
			ngModel:"=",
			showGridView:"=?",
			tableSpeedMenuOption:"=?",
			selectedDataset:"=?",
			selectDatasetAction:"&",
			shareDatasetAction:"&",
			shareDatasetEnabled:"=?",
			previewDatasetAction:"&",
			//cloneDocumentAction:"&",
			showQbeDatasetAction:"&",
			
			showQbeEnabled:"=?",
			showDetailDatasetAction:"&",
			showDetailDatasetEnabled:"=?",
			//executeDocumentAction:"&",
			orderingDatasetCards:"=?",
		},
		
		link: function (scope, elem, attrs) { 
			
			elem.css("position","static")
			 if(!attrs.tableSpeedMenuOption){
				 scope.tableSpeedMenuOption=[];
			 }
			
		}
	}
});

function datasetViewControllerFunction($scope,sbiModule_user,sbiModule_translate){
	$scope.clickDataset=function(item){
		
		 $scope.selectDatasetAction({ds: item});
		 
		 
	}
	
	$scope.translate=sbiModule_translate;
	
}