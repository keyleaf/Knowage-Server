<%--
Knowage, Open Source Business Intelligence suite
Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

Knowage is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

Knowage is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>


<!-- 
	The main JSP page for the management of the LOV catalog.
	
	Author: Danilo Ristovski (danristo, danilo.ristovski@mht.net) 
-->

<%@ page language="java" pageEncoding="utf-8" session="true"%>

<%-- ---------------------------------------------------------------------- --%>
<%-- JAVA IMPORTS															--%>
<%-- ---------------------------------------------------------------------- --%>
<%@include file="/WEB-INF/jsp/commons/angular/angularResource.jspf"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html ng-app="lovsManagementModule">
	
	<head>
	
		<!-- HTML meta data -->
		<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
		
		<!-- JSP files included -->
		<%@include file="/WEB-INF/jsp/commons/angular/angularImport.jsp"%>
		
		<!-- Style files included -->
<%-- 		<link rel="stylesheet" type="text/css" href="/knowage/themes/glossary/css/generalStyle.css"> --%>
		<link rel="stylesheet" type="text/css"    href="${pageContext.request.contextPath}/themes/commons/css/customStyle.css">
		
		<!-- Javascript files included -->
<%-- 
		<script type="text/javascript" src="/knowage/js/src/angular_1.4/tools/commons/angular-table/AngularTable.js"></script>
		<script type="text/javascript" src="/knowage/js/src/angular_1.4/tools/catalogues/lovsManagement.js"></script>		 
--%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/src/angular_1.4/tools/catalogues/lovsManagement.js"></script>		
		
		<title>LOVS Management</title>
		
	</head>
	
	<body class="bodyStyle kn-layerCatalogue" ng-controller="lovsManagementController as LOVSctrl" >
	<angular-list-detail show-detail="showMe">
 		<list label='translate.load("sbi.behavioural.lov.title")' new-function="createLov"> 

			 	
				
				 <angular-table
			flex
			id="listOfLovs_id" 
			ng-model="listOfLovs"
			columns='[
					  {"label":"Label","name":"label"},
					  {"label":"Description","name":"description"},
					  {"label":"Type","name":"itypeCd"}
					]'
			show-search-bar=true
			highlights-selected-item=true
			speed-menu-option="lovsManagementSpeedMenu"
			click-function="itemOnClick(item)">
		     </angular-table>
		

		</list>
		
		<extra-button>
			  <md-button class="md-flat" ng-click="testLOV()" ng-show="showMe" >{{translate.load("sbi.datasource.testing")}}</md-button>
		</extra-button>
		
		<detail label=' selectedLov.label==undefined? "" : selectedLov.label'  save-function="saveLov"
		cancel-function="cancel"
		disable-save-button="!attributeForm.$valid"
		show-save-button="showMe" show-cancel-button="showMe">
		<form name="attributeForm" ng-submit="attributeForm.$valid && saveLov()">
	
			           <md-card layout-padding  ng-show="showMe">
					<div layout="row" layout-wrap>
						<div flex=100>
							<md-input-container class="md-block">
							<label>{{translate.load("sbi.ds.label")}}</label>
							<input name="lbl" ng-model="selectedLov.label" ng-required="true"
							ng-maxlength="20" ng-change="setDirty()">
							
							<div  ng-messages="attributeForm.lbl.$error" ng-show="selectedLov.label== null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>
							
							 </md-input-container>
						</div>
					</div>
					
					<div layout="row" layout-wrap>
						<div flex=100>
							<md-input-container class="md-block">
							<label>{{translate.load("sbi.ds.name")}}</label>
							<input name="name" ng-model="selectedLov.name"  ng-required = "true"
						    ng-maxlength="40" ng-change="setDirty()">
						    
						    <div  ng-messages="attributeForm.name.$error" ng-show="selectedLov.name== null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>
						    
						    
						     </md-input-container>
						</div>
					</div>
					
					<div layout="row" layout-wrap>
						<div flex=100>
							<md-input-container class="md-block">
							<label>{{translate.load("sbi.ds.description")}}</label>
							<input ng-model="selectedLov.description"
					        ng-maxlength="160" ng-change="setDirty()"> </md-input-container>
						</div>
					</div>
				
				<div layout="row" layout-wrap>
      				<div flex=100>
				       <md-input-container class="md-block" > 
				       <label>{{translate.load("sbi.modalities.check.details.check_type")}}</label>
				       <md-select  aria-label="dropdown" placeholder ="Check Type"
				       	name ="dropdown" 
				        ng-required = "true"
				        ng-model="selectedLov.itypeCd"
				        ng-change="changeType('lov',selectedLov.itypeCd)"
				        > <md-option 
				        ng-repeat="l in listOfInputTypes track by $index" value="{{l.VALUE_CD}}">{{l.VALUE_NM}} </md-option>
				       </md-select>
				       <div  ng-messages="attributeForm.dropdown.$error" ng-show="selectedLov.itypeCd == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>   
				        </md-input-container>
				   </div>
			</div>
	
	</md-card>
		      
	<md-card>
				    <md-content layout-padding>
					<md-toolbar class="secondaryToolbar">
				      <div class="md-toolbar-tools">
				        <h2>
				          <span>{{toolbarTitle}}</span>
				        </h2>
				   		<span flex></span>
				   		
				        <md-button class="md-icon-button" aria-label="Info" ng-click="openInfoFromLOV()">
				          <md-icon md-font-icon="fa fa-info-circle" class="fa fa-2x"></md-icon>
				        </md-button>
				        <md-button class="md-icon-button" aria-label="Profiles" ng-click="openAttributesFromLOV()">
				          <md-icon md-font-icon="fa fa-users" class="fa fa-2x"></md-icon>
				        </md-button>
				   		
				      </div>
				    </md-toolbar>
			<div ng-if="selectedLov.itypeCd == 'SCRIPT'">    
				    <div layout="row" layout-wrap>
      				<div flex=100>
				       <md-input-container class="md-block" > 
				       <label>{{translate.load("sbi.functionscatalog.language")}}</label>
				       <md-select  aria-label="dropdown" placeholder ="Script Type"
				       	name ="dropdown" 
				        ng-required = "true"
				        ng-model="selectedScriptType.language"
				        ng-change="changeType('script',selectedScriptType.language)"
				        > <md-option 
				        ng-repeat="l in listOfScriptTypes track by $index" value="{{l.VALUE_CD}}">{{l.VALUE_NM}} </md-option>
				       </md-select>
				       <div  ng-messages="attributeForm.dropdown.$error" ng-show="selectedScriptType.language == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>   
				        </md-input-container>
				   </div>
			</div>
			 <md-input-container class="md-block">
		          <label>{{translate.load("sbi.functionscatalog.script")}}</label>
		          <textarea ng-model="selectedScriptType.text" md-maxlength="500" rows="12" md-select-on-focus></textarea>
        	</md-input-container>
		</div>
		
		<div ng-if="selectedLov.itypeCd == 'QUERY'">    
				    <div layout="row" layout-wrap>
      				<div flex=100>
				       <md-input-container class="md-block" > 
				       <label>{{translate.load("sbi.datasource.label")}}</label>
				       <md-select  aria-label="dropdown" placeholder ="Select Datasource"
				       	name ="dropdown" 
				        ng-required = "true"
				        ng-model="selectedQuery.datasource"
				        ng-change="changeType('datasource',selectedQuery.datasource)"
				        > <md-option 
				        ng-repeat="l in listOfDatasources track by $index" value="{{l.dsId}}">{{l.label}} </md-option>
				       </md-select>
				       <div  ng-messages="attributeForm.dropdown.$error" ng-show="selectedQuery.datasource == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>   
				        </md-input-container>
				   </div>
			</div>
			 <md-input-container class="md-block">
		          <label>{{translate.load("sbi.tools.dataset.qbedatasetswizard.query")}}</label>
		          <textarea ng-model="selectedScriptType.query" md-maxlength="500" rows="12" md-select-on-focus></textarea>
        	</md-input-container>
		</div>
		
		<div ng-if="selectedLov.itypeCd == 'FIX_LOV'">    
			
						<div layout="row" layout-wrap>
						<div flex=100>
							<md-input-container class="md-block">
							<label>{{translate.load("sbi.generic.value")}}</label>
							<input name="lbl" ng-model="selectedFIXLov.value" ng-required="true"
							ng-maxlength="20" ng-change="setDirty()">
							
							<div  ng-messages="attributeForm.lbl.$error" ng-show="selectedFIXLov.value == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>
							
							 </md-input-container>
						</div>
					</div>
					
					<div layout="row" layout-wrap>
						<div flex=100>
							<md-input-container class="md-block">
							<label>{{translate.load("sbi.generic.descr")}}</label>
							<input name="name" ng-model="selectedFIXLov.description"  ng-required = "true"
						    ng-maxlength="40" ng-change="setDirty()">
						    
						    <div  ng-messages="attributeForm.name.$error" ng-show="selectedFIXLov.description == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>
						    
						    
						     </md-input-container>
						</div>
					</div>
					<md-button class="md-raised md-primary" ng-click="addNewFixValueParam()">{{translate.load("sbi.attributes.add");}}</md-button>
					
				   
		</div>
		
		<div ng-if="selectedLov.itypeCd == 'JAVA_CLASS'">    
			
			<div layout="row" layout-wrap>
						<div flex=100>
							<md-input-container class="md-block">
							<label>{{translate.load("sbi.ds.jclassName")}}</label>
							<input name="lbl" ng-model="selectedJavaClass" ng-required="true"
							ng-maxlength="20" ng-change="setDirty()">
							
							<div  ng-messages="attributeForm.lbl.$error" ng-show="selectedJavaClass == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>
							
							 </md-input-container>
						</div>
					</div>
				   
		</div>
		
		<div ng-if="selectedLov.itypeCd == 'DATASET'">    
			
			<div layout="row" layout-wrap>
      				<div flex=100>
				       <md-input-container class="md-block" > 
				       <label>{{translate.load("sbi.datasource.label")}}</label>
				       <md-select  aria-label="dropdown" placeholder ="Select Dataset"
				       	name ="dropdown" 
				        ng-required = "true"
				        ng-model="selectedDataset.name"
				        ng-change="changeType('dataset',selectedDataset.name)"
				        > <md-option 
				        ng-repeat="l in listOfDatasets track by $index" value="{{l.name}}">{{l.label}} </md-option>
				       </md-select>
				       <div  ng-messages="attributeForm.dropdown.$error" ng-show="selectedDataset.name == null">
				        <div ng-message="required">{{translate.load("sbi.catalogues.generic.reqired");}}</div>
				      </div>   
				        </md-input-container>
				   </div>
			</div>
		
				   
		</div>
					    
		 	</md-content>
		 	</md-card>
		
		</form>
		</detail>
	</angular-list-detail>
</body>
	
</html>
