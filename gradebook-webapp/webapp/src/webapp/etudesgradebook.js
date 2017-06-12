tool_obj =
{
	title: "GRADEBOOK",
	showReset: true,
	currentMode: 0,
	modeBarElementId: "gradebook_mode_bar",
	siteId: null,
	sortPreference : null,	
	lastSortData : null,
	lastFixedColumn: 7,
	lastShowColumn : 0,
	showColumnsIterSize: 5,
	preferences: new Object(),
	modes:
		[
			{
				title: "Overview",
				elementId: "gradebook_assessments",
				element: null,
				toolItemTableElementId: "assessments_table",
				icon: "gb-assessments.png",		
				headers:
				[
				 	{title: "", type: null, sort: null},
					{title: "Title", type: "tablesorter-header", click: function(){tool_obj.changeSort(tool_obj, $(this));return false;},sort: true},
					{title: "", type: null, sort: null},
					{title: "Open", type: "tablesorter-header", click: function(){tool_obj.changeSort(tool_obj, $(this));return false;}, sort: true},
					{title: "Due", type: "tablesorter-header", click: function(){tool_obj.changeSort(tool_obj, $(this));return false;}, sort: true},
					{title: "Students", type: null, sort: null},
					{title: "Average", type: null, sort: null},
					{title: "Points", type: null, sort: null},
					{title: "", type: "reorderWidth", sort: null}
				 ],
				navBarElementId: ["gradebook_assessments_navbar_top","gradebook_assessments_navbar_bottom"],
				navbar:
		   		[		   		 	
		   		 {title: "Save", icon: "save.png", access: "s", popup: "Save", click: function(){tool_obj.saveCategoryItemMap(tool_obj);return false;}}			 
			 	],
				start: function(obj, mode)
				{
					obj.sortPreference = null;
					obj.lastSortData = null;
		
					if (obj.lastOverviewSortData != null)
					{
						obj.lastSortData = obj.lastOverviewSortData;
						obj.lastOverviewSortData = null;
					}
					if (obj.itemId == null || obj.itemId == undefined || "null".match(obj.itemId))
					{
 						obj.loadAssessments(obj, obj.lastSortData);	
						obj.callFromPage = undefined;
						obj.showBestSubmissionOnly = undefined;
					}
					else 
					{
						obj.saveOverrideList =[];
						obj.loadAssessmentDetails(obj, obj.itemId);						
					}
				},
				reset: function(obj, mode)
				{
					obj.itemId = null;
					obj.lastOverviewSortData = null;
					obj.assessmentCategories = null;
					obj.studentAssessmentOverview = null;
					obj.assessmentOverview = null;
					obj.detailedItem = null;
					obj.showBestSubmissionOnly = undefined;
					obj.saveOverrideList =[];
				},
				stop: function(obj)
				{
					//obj.itemId = null;
				//	obj.lastOverviewSortData = null;
					if (obj.itemId != null && obj.itemId != undefined) obj.saveOverrideValues(obj, false, "etudesgradebook_assessmentDetails");
					if (obj.studentAssessmentOverview != null && obj.studentAssessmentOverview != undefined) obj.saveOverrideValues(obj, false, "etudesgradebook_indvidualStudentGrades");
					obj.assessmentCategories = null;
					obj.studentAssessmentOverview = null;
					obj.assessmentOverview = null;
					obj.showBestSubmissionOnly = undefined;
					obj.detailedItem = null;
					obj.saveOverrideList =[];
					$("#gradebook_assessments").trigger("destroy");
					$("#gradebook_assessment_detail").trigger("destroy");
				}
			},
			{
				title: "Itemized Grades",
				elementId: "gradebook_student_itemized_grades",
				element: null,
				toolItemTableElementId: "students_all_assessments_grades_table",
				icon: "gb-grade-students.png",		
				headers:
				[	
				],
				navBarElementId: ["gradebook_student_itemized_grades_navbar_top","gradebook_student_itemized_grades_navbar_bottom"],
				navbar:
		   		[		   		 	   		 			 
			 	],				
				start: function(obj, mode)
				{						
					if (obj.callFromPage != null && obj.callFromPage != undefined && obj.callFromPage.match("returnBack"))
					{
						//keep all other values
						obj.callFromPage = undefined;
					}
					else if (obj.callFromPage != null && obj.callFromPage != undefined && obj.callFromPage.match("grades"))
					{
						obj.callFromPage = undefined;
						obj.lastSortData = obj.lastGradesSortData;
						obj.selectedType = obj.gradesAssessmentType;
						obj.selectedSection = obj.gradesSection;
					}
					else
					{
						obj.lastSortData = null;
						obj.selectedType = undefined;
						obj.selectedSection = undefined;
					}
					obj.viewAssessmentGrades="2";
					obj.itemId = null;
					obj.studentAssessmentOverview = null;
					obj.saveOverrideList =[];
					obj.studentGrades = null;
					obj.showBestSubmissionOnly = undefined;
					obj.loadStudentsGrade(obj);
				},
				reset: function(obj, mode)
				{
					
				},
				stop: function(obj)
				{						
					if (tool_obj.studentAssessmentOverview != null && tool_obj.studentAssessmentOverview != undefined) tool_obj.saveOverrideValues(tool_obj,false, "etudesgradebook_indvidualStudentGrades");	
					obj.saveOverrideList =[];
					obj.showBestSubmissionOnly = undefined;
					obj.selectedSection = undefined;
					obj.studentGrades = undefined;
					$("#gradebook_student_itemized_grades").trigger("destroy");
					$("#gradebook_student_full_details").trigger("destroy");	
				}
			},
			{
				title: "Overall Grades",
				elementId: "gradebook_student_grades",
				element: null,
				toolItemTableElementId: "students_overall_grades_table",
				icon: "gb-grade-students.png",		
				headers:
				[
					{title: "Name", type: "tablesorter-header", click: function(){tool_obj.changeGradesSort(tool_obj, $(this));return false;}, sort: true},
					{title: "", type: null, sort: null},
					{title: "", type: null, sort: null},
					{title: "Section", type: "tablesorter-header", click: function(){tool_obj.changeGradesSort(tool_obj, $(this));return false;}, sort: true},
					{title: "Status", type: "tablesorter-header", click: function(){tool_obj.changeGradesSort(tool_obj, $(this));return false;}, sort: true},
					{title: "Score", type: "tablesorter-header", click: function(){tool_obj.changeGradesSort(tool_obj, $(this));return false;}, sort: true},
					{title: "Out of", type: null, sort: null},
					{title: "Extra Credit", type: null, sort: null},
					{title: "Overall Grade", type: "tablesorter-header", click: function(){tool_obj.changeGradesSort(tool_obj, $(this));return false;}, sort: true},
					{title: "Log", type: null, sort: null},
					{title: "Grade Override", type: null, sort: null}
				],
				navBarElementId: ["gradebook_studentgrades_navbar_top","gradebook_studentgrades_navbar_bottom"],
				navbar:
		   		[
		   		 	{title: "Save", icon: "save.png", access: "s", popup: "Save", click: function(){tool_obj.saveOverrideValues(tool_obj, true, "etudesgradebook_studentGrades"); return false;}}	   		 			 
			 	],				
				start: function(obj, mode)
				{	
					obj.viewAssessmentGrades=null;
					obj.lastSortData = null;
					obj.studentGrades = null;		
					obj.itemId = null;
					obj.studentAssessmentOverview = null;
					obj.saveOverrideList =[];
					obj.selectedSection = undefined;
					obj.showBestSubmissionOnly = undefined;
					obj.loadStudentsGrade(obj);
				},
				reset: function(obj, mode)
				{
					
				},
				stop: function(obj)
				{
					if ((tool_obj.studentAssessmentOverview == null || tool_obj.studentAssessmentOverview == undefined) && (tool_obj.viewAssessmentGrades == null || tool_obj.viewAssessmentGrades == undefined || tool_obj.viewAssessmentGrades == '1')) tool_obj.saveOverrideValues(tool_obj, false, "etudesgradebook_studentGrades");
					if (tool_obj.studentAssessmentOverview != null) tool_obj.saveOverrideValues(tool_obj,false, "etudesgradebook_indvidualStudentGrades");
					
					obj.saveOverrideList =[];
					obj.showBestSubmissionOnly = undefined;
					obj.selectedSection = undefined;
					obj.studentGrades = undefined;
					$("#gradebook_student_grades").trigger("destroy");
					$("#gradebook_student_full_details").trigger("destroy");	
				}
			},
			{
				title: "Options",
				elementId: "gradebook_grade_options",
				element: null,
				toolItemTableElementId: "grade_options_table",
				icon: "gb-grade-options.png",
				actions:
				[	
				],			
				headers:
				[			
				],
				navBarElementId: ["grade_options_actions"],
				navbar:
				[
				],				
				start: function(obj, mode)
				{
					obj.loadGradeOptions(obj, true);
				},
				reset: function(obj, mode)
				{
					obj.gradeOptions = null;
					obj.gradeOptionsCategories = null;
				},
				stop: function(obj)
				{
					obj.gradeOptions = null;
					obj.gradeOptionsCategories = null;
				}
			},			
			{
				title: "Categories",
				elementId: "gradebook_categories",
				element: null,
				toolItemTableElementId: "categories_table",
				icon: "gb-categories.png",
				actions:
				[	
				],			
				headers:
				[			
				],
				navBarElementId: ["categories_actions"],
				navbar:
				[
				],				
				start: function(obj, mode)
				{
					obj.assessmentsLastSortData = undefined;
					obj.loadCategories(obj);
				},
				reset: function(obj, mode)
				{					
				},
				stop: function(obj)
				{
					obj.assessmentsLastSortData = undefined;
					$("#categories_table").trigger("destroy");
				}
			}

		],
	// start the tool
	start: function(obj, data)
	{
		setTitle(obj.title);
		obj.siteId = data.siteId;
		
		var params = new Object();
		params.siteId = obj.siteId;
		
		// export grades
		setupDialog("student_grades_export_dialog", "Done", function(){obj.JSONToCSVConvertor(obj, obj.studentGrades.exportStudentOverallGrades, obj.studentGrades.exportFileName, true);return false;});
		
		//student grades mode
		$("input[name='view_grades_option']").unbind("change").change(function() {obj.showGradesTable(obj, this.value);return false;});
		$(".colNext").unbind("click").click(function(){obj.showGradesNextColumn(obj);return false;});
		$(".colPrev").unbind("click").click(function(){obj.showGradesPrevColumn(obj);return false;}); 
		$(".colFirst").unbind("click").click(function(){obj.showGradesFirstColumns(obj);return false;});
		$(".colLast").unbind("click").click(function(){obj.showGradesLastColumns(obj);return false;});
		$("#navigateToOptions").unbind("click").click(function(){selectToolMode(3, obj);});
				
		populateToolModes(obj);
		startHeartbeat();
		obj.clear(obj, false, undefined);
		
		// categories type
		setupDialog("category_type_dialog", "Done", function(){return obj.saveCategoriesType(obj);});
		$("#preferences_sort_order_link").unbind("click").click(function(){obj.selectCategoryType(obj);return false;});
		
		// edit categories		
		setupDialog("categories_edit_dialog", "Done", function(){return obj.saveCategories(obj);});
		
		$("#categories_configure_weights_link").unbind("click").click(function(){obj.editCategories(obj, obj.preferences.gradebook.manageType, obj.preferences.gradebook.categoriesList);return false;});
		$("#custom_categories_configure_weights_link").unbind("click").click(function(){obj.editCategories(obj, obj.preferences.gradebook.manageType, obj.preferences.gradebook.categoriesList);return false;});
		$("#categories_configure_add").unbind("click").click(function(){obj.addCustomCategory(obj);return false;});
				
		// grade options grade display 
		setupDialog("preferences_gradeCalculation_dialog", "Done", function(){return obj.setOverallGradeCalculationPref(obj);});
		$("#grade_options_grade_display_link").unbind("click").click(function(){obj.openGradeCalculationDialog(obj);return false;});
		
		// bgrade options drop lowest score		
		setupDialog("grade_options_drop_score_dialog", "Done", function(){return obj.saveDropLowestScores(obj);});
		$("#grade_options_drop_score_link").unbind("click").click(function(){obj.openDropLowestScoreDialog(obj, "options", obj.gradeOptionsCategories.categoriesList, obj.gradeOptions.dropScore);return false;});
		$("#grade_options_drop_score_checked").unbind("click").click(function(){obj.setDropLowestScore(obj);return false;});
		$("#gotoDropScores").unbind("click").click(function(){obj.saveOverrideValues(obj, true, "etudesgradebook_studentGrades");obj.openDropLowestScoreDialog(obj, "grades", obj.studentGrades.categories.categoriesList, obj.studentGrades.dropScore);return false;});
		
		// grade options grading scale
		setupDialog("grade_options_grading_scale_dialog", "Done", function(){return obj.saveGradingOptionsGradingScale(obj);});
		$("#grade_options_grading_scale_link").unbind("click").click(function(){obj.gradeOptionsGradingScale(obj);return false;});
		
		// print
		$("#studentPrint").unbind("click").click(function(){obj.printStudentGrades(obj);return false;});
		
		// item details best submission only		
		$("#item_details_allSubmission").unbind("click").click(function(){obj.showAllSubmissions(obj, "assessmentDetails", "false");return false;});
		$("#item_details_best").unbind("click").click(function(){obj.showAllSubmissions(obj, "assessmentDetails", "true");return false;});
		$("#student_details_allSubmission").unbind("click").click(function(){obj.showAllSubmissions(obj, "studentDetails", "false");return false;});
		$("#student_details_best").unbind("click").click(function(){obj.showAllSubmissions(obj, "studentDetails", "true");return false;});
		
		//notes
		setupDialog("notesOnStudents", "Done", function(){return obj.saveInstructorNotes(obj);});
		$("#student_details_notes").unbind("click").click(function(){obj.populateNoteComments(obj, obj.studentAssessmentOverview.notes, obj.studentAssessmentOverview.notesLog, obj.studentAssessmentOverview.notesDate, obj.studentAssessmentOverview.studentName, "indv_summary", obj.studentAssessmentOverview.studentId);$("#notesOnStudents").dialog('open');return false;});
		
	//	setupDialog("openPMDialog", "Done", function(){return true;});
		$("#student_details_pm").unbind("click").click(function(){obj.openSendPM(obj, obj.studentAssessmentOverview.sendPMLink);return false;});
	},
	
	stop: function(obj, save)
	{
		stopHeartbeat();
	},

	reset: function(obj)
	{
		populateToolModes(obj);
	},
	
	addCustomCategory: function(obj)
	{				
		var tr = $("<tr />");
		var trExtraCredit = $("#categories_configure_weights_table tbody tr.extraCreditRow");
		if (trExtraCredit == null || trExtraCredit == undefined ) 
			$("#categories_configure_weights_table tbody").append(tr);
		else trExtraCredit.before(tr); 
			
		createIconTd(tr, "custom-category.png", "Custom category");
		 
		createTextEditTd(tr, "");
		
		createTextTd(tr, "");
		
		var td = createTextEditTd(tr, "",3, "itemWeight").append("<span> %</span>");
		$(td).addClass("weight");			
		$(td).attr("distribution", 0);
		$(td).unbind("change").change(function(){obj.updateSubTotal(obj, $(this));return false;});
		$(td).unbind("mouseout").mouseout(function(){obj.mouseUpdateSubTotal(obj, $(this));return false;});
		
		var distributionOptions = [{value:0,title:''},{value:1,title:'Equally'},{value:2,title:'By Points'}];
		createDropDownTd(tr, 0, distributionOptions, 1).addClass("distributionTd");
		
		createIconTd(tr, "delete.png", "delete", function(){obj.deleteCustomCategory(obj, $(this).parent()); return false;});
					
		// reorder categories
		createReorderIconTd(tr);
		
		//zebra color
		$("#categories_configure_weights_table tr:even()").addClass("evenRow");
		adjustForNewHeight();
	},
	
	addToOverrideList : function(obj, t, callFromScreen)
	{		
		var fields = $(t).find("input[type=text]");
		var studentId = $(t).attr("userId");
		var loadTime= $(t).attr("loadTime");
		var submissionId= $(t).attr("submissionId");
		
		var val = fields.eq(0).val();
		
		var releaseFields = $(t).find("input[type=checkbox]");
		var releaseBox = releaseFields.eq(0);
		var valRelease = $(releaseBox).is(':checked').toString();
			
		var a = {};
		if ("gradeOverride".match(callFromScreen))
			a = {userId:studentId, loadTime: loadTime, changedGrade:val};
		if ("scoreOverride".match(callFromScreen))
		{
			var overrideAssessmentId = $(t).attr("assessmentId");
			a = {userId:studentId, loadTime: loadTime, overrideAssessmentId: overrideAssessmentId, overrideSubmissionId: submissionId, changedScore:val, overrideRelease: valRelease};
		}
		if (obj.saveOverrideList == null || obj.saveOverrideList == undefined) obj.saveOverrideList = [];
		
		obj.saveOverrideList.push(a);			
		
		if ("true".match(valRelease) || valRelease == true) $(releaseBox).prop("checked", true);	
	},
	
	adjustNextPrev: function(obj, curPos)
	{
		$(".counts").empty().html(curPos);
	},
	
	callAssessmentDetailCdp: function (obj ,itemId, lastOverviewSortData, lastSortData, sectionFilter, nextPrevAction, params)
	{
		if (params == null || params == undefined) params = new Object();
		params.siteId = obj.siteId;
		params.itemId = itemId;
		if (lastOverviewSortData != null) params.lastOverviewSortData = lastOverviewSortData;
		if (lastSortData != null) params.lastSortData = lastSortData;
		if (sectionFilter != null) params.selectedSection = sectionFilter;				
		if (nextPrevAction != null) params.nextPrevAction = nextPrevAction;
		//transfer grades data
		if (obj.callFromPage != null) params.callFromPage = obj.callFromPage;
		if (obj.lastGradesSortData != null) params.lastGradesSortData = obj.lastGradesSortData;
		if (obj.gradesAssessmentType != null) params.gradesAssessmentType = obj.gradesAssessmentType;
		if (obj.gradesSection != null) params.gradesSection = obj.gradesSection;
		// best submission 
		if (obj.showBestSubmissionOnly != null) params.showBestSubmissionOnly = obj.showBestSubmissionOnly.toString();
		obj.clear(obj, true, undefined);
	
		obj.saveOverrideParams(obj, params, "etudesgradebook_assessmentDetails");
		
		requestCdp("etudesgradebook_assessmentDetails", params, function(data)
   		{
			obj.itemId = data.itemId;
			obj.lastOverviewSortData = data.lastOverviewSortData;
   			obj.lastSortData = data.lastSortData;
   			obj.sortPreference = data.sortPreference;
   			obj.sortOrder = data.sortOrder;
   			obj.saveOverrideList = [];
   			obj.detailedItem = data;	   
   			obj.showBestSubmissionOnly = data.showBestSubmissionOnly;
   	   			
   			if (data.callFromPage != null) obj.callFromPage = data.callFromPage;
   			if (data.lastGradesSortData != null) obj.lastGradesSortData = data.lastGradesSortData;
   			if (data.gradesAssessmentType != null) obj.gradesAssessmentType = data.gradesAssessmentType;
   			if (data.gradesSection != null) obj.gradesSection = data.gradesSection;
   			obj.clearDetails(obj);     			
   			showAllToolModes(obj);
   			
   			navBarElementId2 = ["item_details_navbar_top","item_details_navbar_bottom"];
			navbar2 = [		   		 	
	   		 	{id: "assessment_item_next", title: "Next", icon: "next.png", iconRight: true, right: true, access: "n", popup: "Assessment Next", additionalClass:"maskGrade e3_hot", click: function(){tool_obj.doNext(tool_obj);return false;}},
	   		 	{id: "assessment_item_counts", right: true, text: "1 of 1", additionalClass:"counts maskGrade"},
		 		{id: "assessment_item_prev", title: "Prev", icon: "previous.png", right: true, access: "p", popup: "Assessment Previous", additionalClass:"maskGrade e3_hot", click: function(){tool_obj.doPrev(tool_obj);return false;}},
		 		{id: "assessment_item_count_title", right: true, text: "Assessments:", additionalClass:"e3_bold maskGrade"},
		 		{title: "Return", icon: "return.png", access: "r", popup: "Return", click: function(){tool_obj.returnBackfromDetails(tool_obj);return false;}}
		 	];
			var saveElement = {title: "Save", icon: "save.png", access: "s", popup: "Save", click: function(){tool_obj.saveOverrideValues(tool_obj, true, "etudesgradebook_assessmentDetails"); return false;}};
			if (obj.showBestSubmissionOnly != undefined && "false".match(obj.showBestSubmissionOnly)) navbar2.push(saveElement);
			populateToolNavbar(obj,navBarElementId2, navbar2);
   			addSelectFilterToolNavbar(obj, "item_details_navbar_top", "View:", "assessment_item_sections", "1", function(){obj.changeSectionsFilter(obj, "assessmentDetails", $(this).val());return false;});
   			
   			obj.populateItem(obj);	   		
   			if (obj.callFromPage != null && obj.callFromPage != undefined && obj.callFromPage.match("grades"))
			{
   				$(".maskGrade").addClass("e3_offstage");
   			}
   			obj.itemId = null;
   			obj.adjustNextPrev(obj, obj.detailedItem.assessmentItem.showAssessmentCount);
   			obj.clear(obj, true, "#gradebook_assessment_detail");
   		});
   	
   		adjustForNewHeight();
	},	
	
	callStudentGradesCdp : function(obj, params)
	{
		if (params == null || params == undefined)
		{
			params = new Object();
			params.siteId = obj.siteId;
		}

		if(obj.viewAssessmentGrades != undefined && "2".match(obj.viewAssessmentGrades))
			obj.callStudentItemizedGradesCdp(obj, params);
		else obj.callStudentOverallGradesCdp(obj, params);
	},
	
	callStudentItemizedGradesCdp : function(obj, params)
	{
		if (params == null || params == undefined)
		{
			params = new Object();
			params.siteId = obj.siteId;
		}
		
		// hide assessments extra screens
		obj.clear(obj, true, undefined);
		requestCdp("etudesgradebook_studentGrades", params, function(data)
		{	
			obj.saveOverrideList = [];
			
			if (data.lastSortData != null) obj.lastSortData = data.lastSortData;
			obj.viewAssessmentGrades = data.viewAssessmentGrades;
			obj.sortOrder = data.SortOrder;
			obj.sortPreference = data.sortPreference;
			obj.selectedSection = data.selectedSection;
			obj.selectedType = data.selectedType;
			obj.studentGrades = data;
			
			$("#item_option_instructions").html(data.overallCalculationOptions);
			populateToolNavbar(obj,obj.navBarElementId, obj.navbar);			
			addSelectFilterToolNavbar(obj, "gradebook_student_itemized_grades_navbar_top", "View:", "student_itemized_grades_sections", "1", function(){obj.changeSectionsFilter(obj, "grades", $(this).val());return false;});
			obj.populateStudentsGradeHeader(obj, "#itemized_student_max_points", "#itemized_student_extra_max_points", "#itemized_student_class_avg", "#student_itemized_grades_sections");
			
			$("#itemized_grades_overview_table").attr("width", "60%");
		
			// type filter
			addSelectFilterToolNavbar(obj, "gradebook_student_itemized_grades_navbar_top", "Type:", "student_grades_assessmentTypes", "1", function(){obj.changeAssessmentTypeFilter(obj,$(this).val());return false;});
			
			obj.populateStudentsAssessmentsGrade(obj);	
			obj.showGradesFirstColumns(obj);
		
			//show student grade screen
			obj.clear(obj, true, "#gradebook_student_itemized_grades");
			adjustForNewHeight();
		});	
	},
	
	callStudentOverallGradesCdp : function(obj, params)
	{
		if (params == null || params == undefined)
		{
			params = new Object();
			params.siteId = obj.siteId;
		}
		
		obj.saveOverrideParams(obj, params, "etudesgradebook_studentGrades");
	
		// hide assessments extra screens
		obj.clear(obj, true, undefined);
		requestCdp("etudesgradebook_studentGrades", params, function(data)
		{	
			obj.saveOverrideList = [];
			
			if (data.lastSortData != null) obj.lastSortData = data.lastSortData;
			obj.viewAssessmentGrades = data.viewAssessmentGrades;
			obj.sortOrder = data.SortOrder;
			obj.sortPreference = data.sortPreference;
			obj.selectedSection = data.selectedSection;
			obj.selectedType = data.selectedType;
			obj.studentGrades = data;
			
			populateToolNavbar(obj,obj.navBarElementId, obj.navbar);			
			addSelectFilterToolNavbar(obj, "gradebook_studentgrades_navbar_top", "View:", "student_grades_sections", "1", function(){obj.changeSectionsFilter(obj, "grades", $(this).val());return false;});
			obj.populateStudentsGradeHeader(obj, "#student_max_points", "#student_extra_max_points", "#student_class_avg", "#student_grades_sections");
			
			$("#overall_option_instructions").html(data.overallCalculationOptions);
			
			// instructions on view 1 screen
			$("#drop_score").empty();
			if (data.dropLowestNumber != null && data.dropLowestNumber != undefined && data.dropScore)
				$("#drop_score").html(data.dropLowestNumber + " low score\(s\) have been dropped");
			
			if (data.dropLowestNumber != null && data.dropLowestNumber != undefined && data.dropLowestNumber != 0 && !data.dropScore)
				$("#drop_score").html(data.dropLowestNumber + " low score\(s\) will be dropped when enabled");
			
			if (data.dropLowestNumber != null && data.dropLowestNumber != undefined && data.dropLowestNumber == 0 && !data.dropScore)
				$("#drop_score").html(data.dropLowestNumber + " low score\(s\) will be dropped");
			
			$("#grades_overview_table").attr("width", "95%");
				
			// boost user grades
			$("#boost_number").val(data.boostUserGradesBy);
			if (data.boostUserGradesTypeCode == "1")
			{
				$("#boost_type").val("1");
			}
			else if (data.boostUserGradesTypeCode == "2")
			{
				$("#boost_type").val("2");
			}
			
			$("#apply_grades_boost").unbind("click").click(function()
			{
				obj.saveOverrideValues(obj, true, "etudesgradebook_studentGrades");
				obj.saveBoostUserGrades(obj);
			});
			
			obj.populateStudentsOverallGrade(obj);	
				
			//show student grade screen
			obj.clear(obj, true, "#gradebook_student_grades");
			adjustForNewHeight();
		});	
	},
	
	callStudentFullDetailCdp : function (obj, params)
	{
		if (params == null || params == undefined)
		{
			params = new Object();
			params.siteId = obj.siteId;
		}
		
		obj.clear(obj, true, undefined);
	 	var navBar2ElementId=["gradebook_student_full_details_top_navbar","gradebook_student_full_details_navbar"];
		var navbar2 = [
			{id: "grades_student_next", title: "Next", icon: "next.png", iconRight: true, right: true, access: "n", popup: "Next Student", additionalClass:"e3_hot", click: function(){obj.loadStudentFullDetailsGrade(obj, obj.studentAssessmentOverview.studentId, "Next");return false;}},
			{id: "grades_student_counts", right: true, text: "1 of 1", additionalClass:"gradeCounts"},
	 		{id: "grades_student_prev", title: "Prev", icon: "previous.png", right: true, access: "p", popup: "Previous Student", additionalClass:"e3_hot", click: function(){obj.loadStudentFullDetailsGrade(obj, obj.studentAssessmentOverview.studentId, "Prev");return false;}},
	 		{id: "grades_student_title", right: true, text: "Students:", additionalClass:"e3_bold"},
	 		{title: "Return", icon: "return.png", access: "r", popup: "Return", click: function(){obj.returnBackFromIndvSummary(obj); return false;}}
	 		];
		var saveElement = {title: "Save", icon: "save.png", access: "s", popup: "Save", click: function(){obj.saveOverrideValues(obj,true, "etudesgradebook_indvidualStudentGrades");return false;}};
		if (obj.showBestSubmissionOnly != undefined && "false".match(obj.showBestSubmissionOnly)) navbar2.push(saveElement);
	
		showAllToolModes(obj);
		populateToolNavbar(obj,navBar2ElementId, navbar2);
		obj.saveOverrideParams(obj, params, "etudesgradebook_indvidualStudentGrades");
		
		requestCdp("etudesgradebook_indvidualStudentGrades", params, function(data)
		{	
			obj.saveOverrideList = [];
			obj.studentAssessmentOverview = data;
			//returning page settings
			obj.sortTypePreference = data.sortTypePreference;
			obj.selectedSection = data.selectedSection;
			obj.selectedType = data.selectedType;
			obj.viewAssessmentGrades = data.viewAssessmentGrades;
			obj.showBestSubmissionOnly = data.showBestSubmissionOnly;
			obj.lastSortData = data.lastSortData;
			if (data.lastGradeSortData != undefined) obj.lastSortData = data.lastGradeSortData;
			var returnSetting = "/"+ data.viewAssessmentGrades +"/"+data.lastSortData+"/"+data.selectedSection+"/"+data.selectedType;
			obj.populateStudentNameandStatus(obj, "instructor","student_details_gradetoDate_letterpoints","student_details_gradetoDate_points","student_details_gradetoDate_header","student_details_points","student_details_points_row");
			obj.populateStudents(obj, "student_details_table", returnSetting, true, false);
			obj.clear(obj, true, "#gradebook_student_full_details");
		
		});
		
		adjustForNewHeight();
},
	
	clear:function(obj, showModebar, screenName)
	{
		$("#gradebook_mode_bar").addClass("e3_offstage");
		$("#gradebook_assessments").addClass("e3_offstage");
		$("#gradebook_student_assessments").addClass("e3_offstage");
		$("#gradebook_assessment_detail").addClass("e3_offstage");
		$("#gradebook_student_full_details").addClass("e3_offstage");
		$("#gradebook_student_itemized_grades").addClass("e3_offstage");
		$("#gradebook_student_grades").addClass("e3_offstage");
		$("#gradebook_grade_options").addClass("e3_offstage");
		$("#gradebook_categories").addClass("e3_offstage");
		
		if (showModebar == true) $("#gradebook_mode_bar").removeClass("e3_offstage");
		if (screenName != undefined && screenName != null) $(screenName).removeClass("e3_offstage");
	},	
	
	// clear table
	clearDetails: function(obj)
	{
		$("#item_details_table thead th:contains('Name')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Status')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Finished')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Reviewed')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Score')").removeClass().addClass("tablesorter-header");
		
		$("#details_feature_title").empty();
		$("#assessment_item_points").empty();
		$("#assessment_item_status").empty().removeClass("e3_alert_text");
		$("#assessment_item_open").empty();
		$("#assessment_item_due").empty();
		$("#assessment_item_accept").empty(); 
		$("#assessment_item_counts").empty();
		$("#assessment_item_sections").empty();
	},	
	
 	changeAssessmentDetailsSort : function (obj, t)
	{
		if (t.hasClass("tablesorter-headerAsc")) 
		{
			obj.lastSortData =  t.index() + ",1";
		}
		else
		{
			obj.lastSortData =  t.index() + ",0";
		}
		
		$("#item_details_table thead th:contains('Name')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Status')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Finished')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Reviewed')").removeClass().addClass("tablesorter-header");
		$("#item_details_table thead th:contains('Score')").removeClass().addClass("tablesorter-header");

		obj.callAssessmentDetailCdp(obj,obj.detailedItem.itemId, obj.lastOverviewSortData, obj.lastSortData, null, null);
	},	
	
	changeAssessmentTypeFilter: function(obj,assessmentType)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.selectedType = assessmentType;		
		if (obj.viewAssessmentGrades != null) params.viewAssessmentGrades = obj.viewAssessmentGrades;
		if (obj.lastSortData != null) params.lastSortData = obj.lastSortData;
		if (obj.selectedSection != null) params.selectedSection = obj.selectedSection;
		obj.callStudentGradesCdp(obj,params);
		adjustForNewHeight();
	},
	
	changeSectionsFilter: function(obj, from, sectionFilter)
	{
		if (from.match("assessmentDetails")) 
		{
			obj.callAssessmentDetailCdp(obj,obj.detailedItem.itemId, obj.lastOverviewSortData, obj.lastSortData, sectionFilter, null);	
		}
		if (from.match("grades"))
		{
			var params = new Object();
			params.siteId = obj.siteId;
			params.selectedSection = sectionFilter;
			if (obj.lastSortData != null) params.lastSortData = obj.lastSortData;			
			if (obj.viewAssessmentGrades != null) params.viewAssessmentGrades = obj.viewAssessmentGrades;
			if (obj.selectedType != null) params.selectedType = obj.selectedType;
			obj.callStudentGradesCdp(obj,params);
		}
	},
	
	changeSort : function (obj, t)
	{
		if (t != undefined && t.hasClass("tablesorter-headerAsc")) 
		{
			obj.lastSortData =  t.index() + ",1";
		}
		else 
		{
			obj.lastSortData =  t.index() + ",0";
		}
		
		$("table thead th:contains('Title')").removeClass().addClass("tablesorter-header");
		$("table thead th:contains('Open')").removeClass().addClass("tablesorter-header");
		$("table thead th:contains('Due')").removeClass().addClass("tablesorter-header");
		
		obj.loadAssessments(obj, obj.lastSortData);
	},

	changeGradesSort : function (obj, t)
	{
		if (t == null || t == undefined) 
		{
			obj.lastSortData =  "2,0";
		}
		else
		{
			if (t.hasClass("tablesorter-headerAsc")) obj.lastSortData =  t.index() + ",1";
			else obj.lastSortData =  t.index() + ",0";
		}	
		$("#students_overall_grades_table thead th:contains('Name')").removeClass().addClass("tablesorter-header");
		$("#students_overall_grades_table thead th:contains('Status')").removeClass().addClass("tablesorter-header");
		$("#students_overall_grades_table thead th:contains('Section')").removeClass().addClass("tablesorter-header");
		$("#students_overall_grades_table thead th:contains('Score')").removeClass().addClass("tablesorter-header");
		$("#students_overall_grades_table thead th:contains('Overall')").removeClass().addClass("tablesorter-header");
		
		$("#students_all_assessments_grades thead th:contains('Name')").removeClass().addClass("tablesorter-header");
		$("#students_all_assessments_grades thead th:contains('Status')").removeClass().addClass("tablesorter-header");
		$("#students_all_assessments_grades thead th:contains('Section')").removeClass().addClass("tablesorter-header");
		$("#students_all_assessments_grades thead th:contains('Score')").removeClass().addClass("tablesorter-header");
		obj.loadStudentsGrade(obj);
	},	
	
	deleteCustomCategory: function(obj, check, target)
	{
		if (check > 0)
		{
			openAlert("delete_category_alert");
			return false;
		}
		else
		{
			var prev = $(target).prev('tr')[0];
			var next = $(target).next('tr')[0];
	
			$(target).remove();
		
			if (prev != null)
			{
				$(prev).focus();
			}
			else if (next != null)
			{
				$(next).focus();
			}
		}
	},	

	doNext: function(obj)
	{		
		obj.callAssessmentDetailCdp(obj,obj.detailedItem.itemId, obj.lastOverviewSortData, obj.lastSortData, obj.detailedItem.selectedSection, "Next");	
	},
	
	doPrev: function(obj)
	{		
		obj.callAssessmentDetailCdp(obj,obj.detailedItem.itemId, obj.lastOverviewSortData, obj.lastSortData, obj.detailedItem.selectedSection, "Prev");
	},
	
	// edit categories dialog
	editCategories: function(obj, customType, categoriesList, assessmentsSortData)
	{
		obj.manageType = customType;
		if (assessmentsSortData != null && assessmentsSortData != undefined) 
		{
			obj.assessmentsLastSortData = assessmentsSortData;
	// save map and refresh categories doesn't happen in this order. refresh categories is always first..so no autosave
	//		obj.saveCategoryItemMap(obj);
	//		obj.reloadCategories(obj);
	//		categoriesList = obj.categories;
		}		
		
		var custom = (customType == 2) ? true : false;
		if (custom) $("#edit_category_instructions").empty().html("You may add weight percentages and weight distribution for the items in each category. You may also add new categories, customize titles and delete unused categories, except Extra Credit.");
		else $("#edit_category_instructions").empty().html("You may add weight percentages and weight distribution for the items in each category.You may also customize the titles, except Extra Credit.");
		
		if (custom) $("#edit_category_dialog_header").html("Custom Values");
		else  $("#edit_category_dialog_header").html("Standard Values");
		
		if (custom) $("#edit_categories_action").removeClass("e3_offstage");
		else $("#edit_categories_action").addClass("e3_offstage");
		
		if (categoriesList != null)
		{
			var subT = 0;
			var withCredit = 0;
			var subPoints = 0;
			var pointsCredit = 0;
			$("#categories_configure_weights_table tbody").empty();
			$.each(categoriesList, function(index, value)
			{
				var tr = $("<tr />");
				$(tr).attr("id",value.id);
				
				$("#categories_configure_weights_table tbody").append(tr);
				
				var typeIcon = obj.findCategoryIcon(obj, value.code);
				if (!"".match(typeIcon)) createIconTd(tr, typeIcon, value.title);
				else createEmptyTd(tr);
				
			
				if (value.extraCategory) createTextEditTd(tr, value.title).addClass("extraTitleTd");			
				else createTextEditTd(tr, value.title); 
				
				// points calculation
				createTextTd(tr, (value.categoryPoints == undefined || "".match(value.categoryPoints)) ? "-" : isNaN(value.categoryPoints) ? value.categoryPoints : value.categoryPoints.toFixed(2));
				var p = 0;
				if (!"".match(value.categoryPoints)) p = (isNaN(value.categoryPoints)) ? 0 : value.categoryPoints.toFixed(2);
				if (value.extraCategory)
				{
					pointsCredit = parseFloat(p);	
				}
				else 
				{
					if (p > 0) subPoints += parseFloat(p);		
				}
				
				//weight calculation
				var td = createTextEditTd(tr, value.weight,3, "itemWeight").append("<span> %</span>");
				var i = 0;
				if (!"".match(value.weight)) i = (isNaN(value.weight)) ? 0 : value.weight;	
			
				if (value.extraCategory)
				{
					$(tr).addClass("extraCreditRow");
					$(td).addClass("extraCredit");
					withCredit = parseFloat(i);	
				}
				else 
				{
					$(td).addClass("weight");
					if (i > 0) subT += parseFloat(i);		
				}			
			
				var weightImage = $("<img/>");
				weightImage.attr("id", "weightAlert"+index);
				weightImage.attr("src", "support/icons/error.png");
				weightImage.attr("alt", "Please enter a positive value.");
				weightImage.attr("title", "Please enter a positive value.");
				weightImage.addClass("e3_offstage");
				weightImage.addClass("detailsHeaderImg");
				weightImage.click(function(){openAlert("negative_value_alert"); return false;});
				$(td).append(weightImage);
				
				$(td).attr("distribution", value.distribution);
				$(td).unbind("change").change(function(){obj.updateSubTotal(obj, $(this));return false;});
				 $(td).unbind("mouseout").mouseout(function(){obj.mouseUpdateSubTotal(obj, $(this));return false;});
				 
				var distributionOptions = [{value:1,title:'Equally'},{value:2,title:'By Points'}];
				var distributionTd = createDropDownTd(tr, value.distribution, distributionOptions, 1);
				$(distributionTd).addClass("distributionTd");
				
				// custom categories other than extra credit are allowed to be deleted
				var deleteIcon = (value.emptyCategory > 0) ? "delete-grey.png" : "delete.png";
				if (custom && !value.extraCategory) createIconTd(tr, deleteIcon, "delete", function(){obj.deleteCustomCategory(obj, value.emptyCategory, $(this).parent()); return false;});
				else if (custom && value.extraCategory) createIconTd(tr, "delete-grey.png", "delete", function(){obj.openExtraCreditDeleteCategoryMessage(obj); return false;});
				else createEmptyTd(tr);
				
				// reorder categories
				createReorderIconTd(tr);
				
			});
			$(".extraTitleTd input").prop("disabled", true);
			$("#categories_configure_weights_table tr:even()").addClass("evenRow");
			$("#categories_configure_weights_table tbody").sortable({axis:"y", containment:"#categories_configure_weights_table tbody", handle:".e3_reorder", tolerance:"pointer"});
		}		
		
		if (subT > 0) 
		{
			obj.writeTotalWeights(obj, subT, withCredit, "#weight-edit-subtotal", "#weight-edit-total");
			$(".distributionTd select").prop("disabled", false);
		}
		else
		{
			var c = (isNaN(pointsCredit)) ? parseFloat(subPoints) : parseFloat(subPoints) + parseFloat(pointsCredit);
			$("#weight-edit-total").empty().text(parseFloat(c).toFixed(2) + " points");				
			$("#weight-edit-subtotal").empty().text(parseFloat(subPoints).toFixed(2) + " points");
			$(".distributionTd select").prop("disabled", true);
		}
		
		obj.hideSubTotalAlert(obj, subT, "#subTotal-header", true, "#subTotalFootnote");
		
		// show dialog
		$("#categories_edit_dialog").dialog('open');
	},
		
	findAssessmentIcon : function (obj, toolTitle, displayTitle)
	{
		displayTitle=$.trim(displayTitle);
		if (toolTitle.match("Discussions"))	return "jforum.png";				
		if (displayTitle.match("Surveys") || displayTitle.match("Survey")) return "survey_type.png";
		if (displayTitle.match("Offline")) return "offline_type.png";			
		if (displayTitle.match("Assignments") || displayTitle.match("Assignment")) return "assignment_type.png";				
		if (displayTitle.match("Tests") || displayTitle.match("Test")) return "test_type.png";
		return "";
	},
	
	findAssessmentStatus : function(obj, status)
	{
		if (status.match("Closed")) return "closed.gif";
		if (status.match("Hidden")) return "invisible.png";
		if (status.match("Not Yet Open")) return "closed.gif";	
		if (status.match("unpublished")) return "remove.png";	
		return "";
	},
		
	findCategoryIcon : function (obj, originalType)
	{
		originalType=$.trim(originalType);
		if (originalType.match("4"))	return "jforum.png";				
		if (originalType.match("3")) return "offline_type.png";			
		if (originalType.match("1")) return "assignment_type.png";				
		if (originalType.match("2")) return "test_type.png";
		if (originalType.match("5")) return "extra-credit.png";
		else return "custom-category.png";
	},	

	findCompletionIcon : function (obj, complete)
	{
		if (complete == undefined || "na".match(complete))return undefined;
		
		if (complete.match("complete")) return "finish.gif";
		if (complete.match("belowMastery"))	return "not-mastered.png";
		if (complete.match("belowCount") || complete.match("inProgress")) return "status_away.png";
		if (complete.match("missedNoSubAvailable"))	return "missed-try-again.png";
		if (complete.match("missedNoSub") || complete.match("missed")) return "exclamation.png";		
		else return undefined;	
	},
	
	findCompletionTitle : function (obj, complete)
	{
		if (complete == undefined) return "";
		if ("na".match(complete))return "NA";
		if (complete.match("complete")) return "Complete";
		if (complete.match("belowMastery"))	return "Below Mastery";
		if (complete.match("inProgress") || complete.match("belowCount")) return "In Progress";
		if (complete.match("missed")) return "Missed";		
		else return "";	
	},
	
	goToHeaderDetails : function(obj, itemId)
	{	
		// save view2 settings
		obj.lastGradesSortData = obj.lastSortData;
		obj.gradesAssessmentType = obj.selectedType;
		obj.gradesSection = obj.selectedSection;
		obj.callFromPage = "grades";
		
		obj.clear(obj, true, undefined);
		
		// reset for details page
		obj.lastSortData = null;
		obj.itemId = itemId;		
		selectToolMode(0, obj);
  		adjustForNewHeight();
	}, 
	
	// grade options grading scale dialog
	gradeOptionsGradingScale: function(obj)
	{
		// remove any message from grading scale change
		$("#grade_options_grading_scale_message").css("display", "none");
		$("#grade_options_grading_scale_message_alert").empty();
		
		// show dialog
		$("#grade_options_grading_scale_dialog").dialog('open');
	},
	
	hideSubTotalAlert : function(obj, total, totalField, showFootnote, footnoteField)
	{
		if ((parseFloat(total)).toFixed(1) != (parseFloat(100)).toFixed(1) && (parseFloat(total)).toFixed(1) != (parseFloat(0)).toFixed(1)) 
		{
			$(totalField).addClass("e3_alert");
			if (showFootnote) $(footnoteField).removeClass("e3_offstage");
	//		$(".e3_primary_dialog_button").attr("disabled", "disabled").addClass("ui-state-disabled");
		}
		else 
		{
			$(totalField).removeClass("e3_alert");
			$(footnoteField).addClass("e3_offstage");
	//		$(".e3_primary_dialog_button").removeAttr("disabled").removeClass("ui-state-disabled");
		}
	},
	
	initializeShowColumns : function (obj)
	{
		var totalColumns = $(".addedHeader").length;
		// hide all added columns
		$(".addedHeader").hide();
		$(".addedFooter").hide();
		$(".addedScoreCol").hide();	 	
	},
	
	loadAssessments: function(obj, lastSortData)
	{
		// hide assessments extra screens
		$("#assessments_table tbody").empty();
		
		var params = new Object();
		params.siteId = obj.siteId;
		if (obj.sortPreference != null)	params.sortPreference = obj.sortPreference;
		if (lastSortData != null) params.lastSortData = lastSortData;
		requestCdp("etudesgradebook_assessments", params, function(data)
		{
			obj.assessmentCategories = data.categories;
		
			if ("instructor".match(data.userRole))
			{
				// instructor returns back from view work on Mneme and Jforum
				if (data.redirectStudentGrade != undefined && data.studentId != undefined)
				{	
					obj.loadStudentFullDetailsGradeRedirect(obj, data.studentId, data.viewAssessmentGrades, data.lastGradeSortData, data.selectedSection, data.selectedType);
					return;
				}
				
				if (data.redirectStudentGrade != undefined && data.itemId != undefined)
				{	
					obj.loadAssessmentDetailsRedirect(obj, data.itemId, data.lastOverviewSortData, data.lastItemDetailsSortData, data.selectedSection, data.callFromPage);
					return;
				}
				
				obj.sortPreference = data.sortPreference;
				obj.lastSortData = data.lastSortData;
				obj.sortOrder = data.sortOrder;
				obj.assessmentOverview = data;
				obj.populateAssessments(obj);
				obj.clear(obj, true, "#gradebook_assessments");	
			}
			if ("student".match(data.userRole))
			{
				obj.studentAssessmentOverview = data;
				$("#student_feature_title").empty().text("Grade report for " + data.userName);	
									
				obj.populateStudentNameandStatus(obj, "student", "student_gradetoDate_letterpoints","student_gradetoDate_points", "student_gradetoDate_header", "student_points", "student_points_row");			
				obj.populateStudents(obj, "student_assessments_table", null, false, false);
				obj.clear(obj, false, "#gradebook_student_assessments");				
			}
		});			
	},
	
	// assessment details
   	loadAssessmentDetails: function(obj, itemId)
   	{
  		obj.callAssessmentDetailCdp(obj,itemId, obj.lastSortData, null, null, null);
   	},
   	
	// assessment details
   	loadAssessmentDetailsRedirect: function(obj, itemId, lastOverviewSort, lastSort,selectedSection, callfromPage)
   	{
   		obj.callFromPage = callfromPage;
  		obj.callAssessmentDetailCdp(obj,itemId, lastOverviewSort, lastSort, selectedSection, null);
   	},
	
	// preferences mode
	loadCategories : function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		
		requestCdp("etudesgradebook_categories", params, function(data)
		{
			// hide assessments extra screens
			obj.clear(obj, true, "#gradebook_categories");
			
			obj.populateCategories(obj, data.gradebook);
			adjustForNewHeight();			
		});
	},

	loadStudentsItemizedGrade: function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		if (obj.lastSortData != null) params.lastSortData = obj.lastSortData;
		if (obj.viewAssessmentGrades != null) params.viewAssessmentGrades = obj.viewAssessmentGrades;
		if (obj.selectedSection != null) params.selectedSection = obj.selectedSection;
		if (obj.selectedType != null) params.selectedType = obj.selectedType;	
		obj.saveOverrideList = new Object();
		
		$("#students_all_assessments_grades_table tbody").empty();
		obj.callStudentItemizedGradesCdp(obj, params);		
		adjustForNewHeight();
	},	
	
	// Student Grades mode
	loadStudentsGrade: function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		if (obj.lastSortData != null) params.lastSortData = obj.lastSortData;
		if (obj.viewAssessmentGrades != null) params.viewAssessmentGrades = obj.viewAssessmentGrades;
		if (obj.selectedSection != null) params.selectedSection = obj.selectedSection;
		if (obj.selectedType != null) params.selectedType = obj.selectedType;	
		obj.saveOverrideList = new Object();
		
		$("#students_overall_grades_table tbody").empty();
		$("#students_all_assessments_grades_table tbody").empty();
		obj.callStudentGradesCdp(obj, params);		
		adjustForNewHeight();
	},	
	
	loadStudentFullDetailsGrade: function(obj, studentId, prevNextAction)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.studentId = studentId;
		if (obj.lastSortData != null && obj.lastSortData != undefined) params.lastSortData = obj.lastSortData;
		if (obj.viewAssessmentGrades != null && obj.viewAssessmentGrades != undefined) params.viewAssessmentGrades = obj.viewAssessmentGrades;	
		if (obj.selectedSection != null && obj.selectedSection != undefined) params.selectedSection = obj.selectedSection;
		if (obj.selectedType != null && obj.selectedType != undefined) params.selectedType = obj.selectedType;			
		if (prevNextAction != null && prevNextAction != undefined) params.prevNextAction = prevNextAction;
		// best submission 
		if (obj.showBestSubmissionOnly != null) params.showBestSubmissionOnly = obj.showBestSubmissionOnly.toString();		
		obj.callStudentFullDetailCdp(obj,params);
	},		
	
	loadStudentFullDetailsGradeRedirect: function(obj, studentId, viewAssessmentGrades, lastGradeSortData, selectedSection, selectedType)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.studentId = studentId;
		if (lastGradeSortData != null && lastGradeSortData != undefined) params.lastSortData = lastGradeSortData;
		if (viewAssessmentGrades != null && viewAssessmentGrades != undefined) params.viewAssessmentGrades = viewAssessmentGrades;
		if (selectedSection != null && selectedSection != undefined) params.selectedSection = selectedSection;
		if (selectedType != null && selectedType != undefined) params.selectedType = selectedType;			
		obj.callStudentFullDetailCdp(obj,params);
	},		
		
	// grade options mode
	loadGradeOptions : function(obj, clearMessage)
	{
		if (clearMessage)
		{
			$("#grade_options_grading_scale_message").css("display", "none");
			$("#grade_options_grading_scale_message_alert").empty();
		}
		var params = new Object();
		params.siteId = obj.siteId;

		requestCdp("etudesgradebook_gradeoptions", params, function(data)
		{
			// hide assessments extra screens
			obj.clear(obj, true, "#gradebook_grade_options");	
			
			obj.gradeOptions = data.gradebook;
			obj.gradeOptionsCategories = data.categories;
			obj.populateGradeOptions(obj, data.gradebook, data.categories);
			adjustForNewHeight();			
		});
	},	

	mouseUpdateSubTotal: function(obj, t)
	{	
		// add distribution value to be by points
		var fields = $(t).find("input[type=text]");
		var val = fields.eq(0).val();
		if (val != undefined && !"".match(val))
		{
			$(".distributionTd select").prop("disabled", false);
			return;
		}
		
		if (val != undefined && val < 0)
		{			
			$(t).find("img").removeClass("e3_offstage");
			return false;
		}
		
		if (val != undefined && val > 0)
		{			
			$(t).find("img").addClass("e3_offstage");
			obj.updateSubTotal(obj, t);
		}
	},

	openGradeCalculationDialog : function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
	
		$("input[name='release_grades_option'][value='"+ obj.gradeOptions.releaseGrades +"']").prop('checked', true);
		$("input[name='display_letter_grade'][value='"+ obj.gradeOptions.showLetterGrade +"']").prop('checked', true);				
		
		$("#preferences_gradeCalculation_dialog").dialog('open');
	},
	
	openDropLowestScoreDialog: function(obj, from, categoriesList, dropScore)
	{	
		obj.callDropLowestFrom = from;
		
		if (categoriesList != null)
		{
			$("#grade_options_edit_lowscore_table tbody").empty();
			$.each(categoriesList, function(index, value)
			{
				var tr = $("<tr />");
				$(tr).attr("id",value.id);
				$(tr).attr("itemCount",value.emptyCategory);
				$(tr).attr("dropCount",value.dropLowest);
				$("#grade_options_edit_lowscore_table tbody").append(tr);
				
				var typeIcon = obj.findCategoryIcon(obj, value.code);
				if (!"".match(typeIcon)) createIconTd(tr, typeIcon, value.title);
				else createEmptyTd(tr);
				
				createTextTd(tr, value.title);
				createTextTd(tr, value.emptyCategory);
				var tdDrop = createTextEditTd(tr, value.dropLowest, 2);
				var dropError = $("<img/>");
				dropError.attr("id", "dropError"+index);
				dropError.attr("src", "support/icons/error.png");
				dropError.attr("alt", "Drop value is higher than available items in category.");
				dropError.attr("title", "Drop value is higher than available items in category.");
				dropError.addClass("e3_offstage");
				dropError.addClass("detailsHeaderImg");
				dropError.click(function(){openAlert("higher_value_alert"); return false;});
				$(tdDrop).append(dropError);
			});
			$("#grade_options_edit_lowscore_table tr:even()").css("background-color", "#FAFAFA" );
		}		
		if (dropScore == 1) $("#grade_options_edit_drop_score_checked").prop("checked", true);

		// show dialog
		$("#grade_options_drop_score_dialog").dialog('open');
	},	

	openExportDialog : function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		
		requestCdp("etudesgradebook_sections", params, function(data)
		{
			// fill up sections dropdown
		});
		
		$("#student_grades_export_dialog").dialog('open');
	},

	openExtraCreditDeleteCategoryMessage : function(obj)
	{
		openAlert("extraCredit_delete_category_alert");
		return false;
	},
	
	openSendPM : function(obj, pmLink)
	{	
		var features = "height=540,width=670,left=60,top=40,toolbar=no,directories=no,status=no,location=no,menubar=no,scrollbars=yes,resizable=no";
		var winHandle = window.open(pmLink, null, features);
		winHandle.focus();
	},
	
	populateAssessments: function(obj)
	{
		var totalAssessments = 0;
		
		if ($("#assessments_table").hasClass("tablesorter")) $("#assessments_table").trigger("destroy");
		$("#assessments_table tbody").empty();
		var toggleClass = (obj.sortOrder.match("asc")) ? "tablesorter-headerAsc" : "tablesorter-headerDesc";
		$("table thead th:contains('"+ obj.sortPreference+"')").removeClass("tablesorter-header").addClass(toggleClass);
		
		if (obj.assessmentOverview.assessments != null)
			{
				var showTypeHeadings = ("Category".match(obj.sortPreference)) ? true : false;
				var recentType = null;
				var typeRowCount = 1;
				
				var assessmentsLength = obj.assessmentOverview.assessmentsLength;				
				var reorderOptions = [];
				
				for(var i=1; i<= assessmentsLength; i++)
				{
				  var o = {value:i,title:i.toString()};
				  reorderOptions.push(o);
				}
				
				$.each(obj.assessmentOverview.assessments, function(index, value)
				{					
					if (showTypeHeadings && value.isCategory != undefined && value.isCategory.match("yes"))
					{
						var trTypeHeading= $("<tr />");
						$(trTypeHeading).attr("categoryId", value.categoryId);
						if(value.emptyCategory == 0) $(trTypeHeading).addClass("typeEmptyHeaderRow");
						else $(trTypeHeading).addClass("typeHeaderRow");
						$("#assessments_table tbody").append(trTypeHeading);
					
						typeRowCount = 1;
					
						var headingTitle = value.categoryTitle;
						if (obj.assessmentCategories.showWeight && !value.categoryWeight.match("-")) headingTitle = headingTitle + (" (" + value.categoryWeight + ")");
						else if (!obj.assessmentCategories.showWeight && !value.categoryPoints.match("-")) headingTitle = headingTitle + (" (" + value.categoryPoints + ")");
						
						var tdHeading = createTextTd(trTypeHeading, headingTitle , null);	
						$(tdHeading).attr("colSpan", "8");
						
						var tdHeadingIcon = createHotTd(trTypeHeading, "Edit", function(){obj.editCategories(obj, obj.assessmentCategories.manageType, obj.assessmentCategories.categoriesList, obj.assessmentOverview.lastSortData); return false;});
						$(tdHeadingIcon).addClass("category_edit");
						$(tdHeadingIcon).addClass("reorderWidth");
						return true;
					} 
					else 
					{						
						typeRowCount = typeRowCount + 1;					
					}
				
					var tr = $("<tr />");
					$(tr).addClass("assessmentTableExtraSpacing");
					$(tr).attr("itemId", value.id);
					$("#assessments_table tbody").append(tr);
					
					// show tool icon and title
					var assessmentIconType = obj.findAssessmentIcon(obj, value.toolTitle, value.toolDisplayTitle);		
					createIconTd(tr, assessmentIconType, value.toolDisplayTitle, null);
					
					var td = createTextTd(tr, value.title);
					td.addClass("hot");
					td.click(function(){obj.loadAssessmentDetails(obj,value.id);return false;});
					
					// status icon				
					var assessment_status_icon = obj.findAssessmentStatus(obj, value.status);
					if (assessment_status_icon != "") createIconTd(tr, assessment_status_icon, value.status, null);
					else createEmptyTd(tr);
								
					// open date
					td = createTextTd(tr, value.open);
					$(td).addClass("date");
					
					td = createTextTd(tr, value.due);
					$(td).addClass("date");
					
					createTextTd(tr, value.submissionCount);
					
					createTextTd(tr, value.averageScore);
					
					createTextTd(tr, value.points);
					
					if (showTypeHeadings) createReorderIconDropDownTd(tr, "", "", value.displayOrder, reorderOptions, 1, "reorderCombo_"+(index + 1), "reorderCombo", function () {obj.sortRowAtNewPosition(obj, value.displayOrder, $(this).val());return true;});
					else createEmptyTd(tr);
					
					if (showTypeHeadings && (typeRowCount % 2 == 0)) $(tr).removeClass().addClass("evenRow");
						
				});
				
				totalAssessments = obj.assessmentOverview.assessmentsLength;
			    if (showTypeHeadings == false) $("#assessments_table tr:even()").css("background-color", "#FAFAFA" );
			}
		$("#assessments_table tbody").sortable({axis:"y", containment:"#assessments_table tbody", handle:".e3_reorder", tolerance:"pointer", sort: function(event, ui){	$(".reorderCombo").remove();}});
	
		$("#assessments_count").empty().text(totalAssessments.toString() + " assessments");	
		
		//total points
		$("#assessment_total_points").empty();			
		$("#assessment_extra_max_points").empty();
		if (!obj.assessmentCategories.showWeight)
		{
			if (obj.assessmentOverview.classMax != null && obj.assessmentOverview.classMax != undefined) $("#assessment_total_points").empty().text(obj.assessmentOverview.classMax.toString() + " total points");
					
			if (obj.assessmentOverview.extraCreditMax != null && obj.assessmentOverview.extraCreditMax != undefined) 
				$("#assessment_extra_max_points").empty().text(obj.assessmentOverview.extraCreditMax.toString() + " with extra credit");
		}
		else
		{
			if (obj.assessmentOverview.totalMaxWeight != null && obj.assessmentOverview.totalMaxWeight != undefined) 
				$("#assessment_extra_max_points").empty().text(obj.assessmentOverview.totalMaxWeight.toString() + "% total weight");
		}
		
		adjustForNewHeight();
	},
	
	populateCategories: function(obj, gradebookData)
	{
		obj.preferences.gradebook = gradebookData;
		
		if (gradebookData.manageType == 2)
		{
			$("#prefs_sort_setting").text("Custom");
			$("#standard_weights_title").addClass("e3_offstage");
			$("#standard_instructions").addClass("e3_offstage");
			$("#custom_weights_title").removeClass("e3_offstage");
			$("#custom_instructions").removeClass("e3_offstage");
		}
		else
		{
			$("#prefs_sort_setting").text("Standard");
			$("#standard_weights_title").removeClass("e3_offstage");
			$("#standard_instructions").removeClass("e3_offstage");
			$("#custom_weights_title").addClass("e3_offstage");
			$("#custom_instructions").addClass("e3_offstage");
		}	
				
		// fill categories
		if (gradebookData.categoriesList != null)
		{
			var subT = 0;
			var withCredit = 0;
			$("#categories_weights_table tbody").empty();
			$.each(gradebookData.categoriesList, function(index, value)
			{
				var tr = $("<tr />");
				$("#categories_weights_table tbody").append(tr);
				
				var typeIcon = obj.findCategoryIcon(obj, value.code);
				if (!"".match(typeIcon)) createIconTextTd(tr, typeIcon, value.title, value.title);
				else createEmptyTd(tr);
				
				var weight_points = 0;
				if (gradebookData.showWeight == null || gradebookData.showWeight == undefined || gradebookData.showWeight == false || "false".match(gradebookData.showWeight))
					weight_points = value.categoryPoints;
				else weight_points = value.weight;
				
				var i=0;
				if (!"".match(weight_points)) i = (isNaN(weight_points)) ? 0 : weight_points;
				if (value.extraCategory || "true".match(value.extraCategory))
				{
					withCredit = parseFloat(i);	
				}
				else 
				{
					if (i > 0) subT += parseFloat(i);						
				}	
				
				if (weight_points != undefined && !"".match(weight_points) && (gradebookData.showWeight || "true".match(gradebookData.showWeight))) 
					weight_points = weight_points + "%";
		
				createLabelTd(tr, ("".match(weight_points)) ? "-" : isNaN(weight_points) ? weight_points : weight_points.toFixed(2));
				
				if (value.distribution == 1) createLabelTd(tr, "Equally");
				else createLabelTd(tr, "By points");
			});
			
			if (!"".match(withCredit))
			{
				var c = (isNaN(withCredit)) ? 0 : withCredit;
				withCredit = (parseFloat(subT)+parseFloat(c)).toFixed(2);
			}
			else withCredit = parseFloat(subT).toFixed(2);
			
			if (gradebookData.showWeight || "true".match(gradebookData.showWeight)) 
			{
				$("#categories_weights_table thead th:contains('Weight')").empty().text("Weight %");
				$("#weight-total").html(withCredit + "%");				
				$("#weight-subtotal").empty().text((parseFloat(subT)).toFixed(2) + "%");
			} 
			else
			{
				$("#categories_weights_table thead th:contains('Weight')").empty().text("Weight");
				$("#weight-total").html(withCredit + " points");				
				$("#weight-subtotal").empty().text((parseFloat(subT)).toFixed(2) + " points");
			}		
			
			if (gradebookData.showWeight) obj.hideSubTotalAlert(obj, subT, "#subTotalList-header", false);			
		}
		
	},
	
 	populateItem: function(obj)
   	{
   		var item = obj.detailedItem.assessmentItem;
   
   		$("#item_details_export").attr("href","/access/etudesgb/item_detail/" + obj.siteId + "/"+obj.detailedItem.exportFileName + "/?item_id=" + obj.itemId + "&sortBy="+obj.sortPreference+obj.sortOrder +"&section="+obj.detailedItem.selectedSection);
   		$("#item_details_table tbody").empty();   		
   		$(".tablesorter-header").unbind("click").click(function(){obj.changeAssessmentDetailsSort(obj, $(this));return false;});
		var toggleClass = ("_a".match(obj.sortOrder)) ? "tablesorter-headerAsc" : "tablesorter-headerDesc";
		$("#item_details_table thead th:contains('"+ obj.sortPreference+"')").removeClass("tablesorter-header").addClass(toggleClass);
		
		if ("true".match(obj.detailedItem.showBestSubmissionOnly) || obj.detailedItem.showBestSubmissionOnly == undefined)
		{
			$("#item_details_best").addClass("e3_action_current");
			$("#item_details_allSubmission").removeClass("e3_action_current");
		}			
		else 
		{
			$("#item_details_best").removeClass("e3_action_current");
			$("#item_details_allSubmission").addClass("e3_action_current");
		}
		
		var assessmentIconType = obj.findAssessmentIcon(obj, item.toolTitle, item.toolDisplayTitle);
		$("#details_feature_title").empty().append("<img src='support/icons/"+ assessmentIconType +"' class='detailsHeaderImg'/> ");
   		$("#details_feature_title").append("Grades for: " + item.title);
   		$("#assessment_item_points").empty().text(item.points);
   		
   		$("#assessment_item_status").empty().text(item.status);
   		
   		if (!"Open".match(item.status)) $("#assessment_item_status").addClass("e3_alert_text e3_bold");
   		else $("#assessment_item_status").removeClass("e3_alert_text e3_bold");
   		
   		$("#assessment_item_open").empty().text(item.open);
   		$("#assessment_item_due").empty().text(item.due);
   		$("#assessment_item_accept").empty().text(item.accept);     
   		
   		// for loop to fill sections dropdown. Need selectedSection too to fill selected part.	
   		if (obj.detailedItem.itemSections != null)
		{   
   			$.each(obj.detailedItem.itemSections, function(index, value)
			{
   				var option = $("<option>").attr("value", value.sectionId).text(value.sectionTitle);
   				if (value.sectionId.match(obj.detailedItem.selectedSection)) option.attr("selected", "selected");
   				$("#assessment_item_sections").append(option);
			});
		}
	
   		if (obj.detailedItem.itemParticipants != null)
		{
			var x=1;
			var releaseColumn = $("#item_details_table thead th:contains('Released')");
			if ("false".match(obj.detailedItem.showBestSubmissionOnly))
			{
				if (releaseColumn.html() == undefined)
				{
					$("#item_details_table tr:first").append("<th>Released</th>");
				}			
			}
			else
			{
				if (releaseColumn.html() == "Released")
				{
					$(releaseColumn).remove();
				}					
			}
			
			$.each(obj.detailedItem.itemParticipants, function(index, value)
			{
				var tr = $("<tr />");
				$(tr).addClass("assessmentTableExtraSpacing");
			
				if (x % 2 == 0) $(tr).addClass("evenRow");
				$("#item_details_table tbody").append(tr);
				
				//name
				createTextTd(tr, value.name);				
				
				//pm
				if (value.sendPMLink != undefined && value.sendPMLink != null)
				{
					var td = createIconTd(tr, "pm.png", "Send Private Message", null);
					$(td).addClass("hot");
					$(td).unbind("click").click(function(){obj.openSendPM(obj, value.sendPMLink);return false;});
				} else createEmptyTd(tr);
				
				//notes
				var noteIcon = "notes_plain.png";
				if (value.notes == null || value.notes == undefined) noteIcon = "notes_plain.png";
				else noteIcon ="notes_text.png";
				td = createIconTd(tr, noteIcon, "Add note", null).addClass("hot notes");
				$(td).unbind("click").click(function(){obj.populateNoteComments(obj, value.notes, value.notesLog, value.notesDate, value.name, "assessmentDetails", value.id);$("#notesOnStudents").dialog('open');return false;});
				
				// section number
				createTextTd(tr, value.section);
				
				//enrolled/dropped
				td = createTextTd(tr, value.enrolledStatus);
				if ("Dropped".match(value.enrolledStatus)) $(td).addClass("e3_alert_text");
				if ("Blocked".match(value.enrolledStatus))
				 {
					if (x % 2 == 0) $(tr).removeClass("evenRow");
					$(tr).addClass("blockedRow");				
				 }				
						
				//finish date
				if (value.inProgressStatus == true)
				{
					td = createTextTd(tr, "in-progress");
					$(td).addClass("tdItalic");
				}
				else
				{	
					td = createTextTd(tr, value.finishDate);
					if (value.lateSubmission != undefined && "true".match(value.lateSubmission)) 
					{
						var lateText = $("<div class='e3_alert_text'>- late</div>");		
						$(td).append(lateText);
					}
					
					if (value.autoSubmission != undefined && "true".match(value.autoSubmission)) 
					{
						var autoText = $("<div class='e3_alert_text'>- auto</div>");		
						$(td).append(autoText);
					}					
				}
				
				// reviewed status icon and review date
				if (value.reviewStatus != undefined)
				{
					var reviewIconType = (value.reviewStatus) ? "eval-reviewed.png" : "eval-not-reviewed.png";
					var reviewPopup = (value.reviewStatus) ? "Evaluation reviewed" : "Evaluation not reviewed";
					td = createIconTextTd(tr, reviewIconType, value.reviewDate, reviewPopup);
				}
				else
				{
					td = createTextTd(tr, value.reviewDate);
				}			
				
				var score = value.userScore;
				var returnSetting = "/"+item.id + "/" +obj.detailedItem.lastOverviewSortData + "/"+obj.detailedItem.lastSortData+ "/"+obj.detailedItem.selectedSection + "/"+obj.detailedItem.callFromPage;
				var gradeScoreLink = obj.setGradeLink(obj, true, returnSetting, value.gradeLink);
								
				obj.setScorewithReviewLink(obj, true, obj.detailedItem.showBestSubmissionOnly, gradeScoreLink, undefined, value.scoreReleased, score, value.inProgressStatus, value.enrolledStatus, tr, x, value.id, value.loadTime, item.id, value.submissionId);
			
				x=x+1;
			});
		}
   
		adjustForNewHeight();
   	},  
	
	populateStudentsGradeHeader : function (obj, maxPoints, extraPoints, avg, gradeSections)
	{
		// class average etc		
		$(maxPoints).empty().text(obj.studentGrades.classMax + " total points");	
	
		if (!obj.studentGrades.showWeight)
			$(extraPoints).empty().text(obj.studentGrades.extraCreditMax + " with extra credit");
		else 
			$(extraPoints).empty().text(obj.studentGrades.totalMaxWeight + "% with extra credit");
		$(avg).empty().text(obj.studentGrades.classAvg + "% average score");
		
		$(gradeSections).empty();

		sections = obj.studentGrades.itemSections;
		if ( sections != null)
		{
   			$.each(sections, function(index, value)
			{
   				var option = $("<option>").attr("value", value.sectionId).text(value.sectionTitle);
   				if (value.sectionId.match(obj.studentGrades.selectedSection)) option.attr("selected", "selected");
   				$(gradeSections).append(option);
			});
		}
	},
	
	populateStudentsOverallGrade: function(obj)
	{
		$("#students_overall_grades_table tbody").empty();		
		var toggleClass = ("asc".match(obj.studentGrades.sortOrder)) ? "tablesorter-headerAsc" : "tablesorter-headerDesc";
		$("#students_overall_grades_table thead th:contains('"+ obj.studentGrades.sortPreference+"')").removeClass("tablesorter-header").addClass(toggleClass);
		
		///etudesgb/student_grades/siteId/exportFileName/?itemType=&sortBy=&sortOrder=&section=&viewAssessments&showExtraCredit
		var exportHref= "/access/etudesgb/student_grades/" + obj.siteId + "/"+obj.studentGrades.exportFileName + "/?sortBy="+obj.studentGrades.sortPreference+"&sortOrder="+obj.studentGrades.sortOrder +"&section="+obj.studentGrades.selectedSection+"&showExtraCredit="+obj.studentGrades.showExtraCredit;
		$("#student_grades_export").attr("href", exportHref);
			
		if (obj.studentGrades.participants != null)
		{
			var x=1;
			$.each(obj.studentGrades.participants, function(index, value)
			{
				var tr = $("<tr />");
				$(tr).addClass("assessmentTableExtraSpacing");
			
				if (x % 2 == 0) $(tr).addClass("evenRow");
				$("#students_overall_grades_table tbody").append(tr);
				
				//name
				var td = createTextTd(tr, value.name);
				td.addClass("hot");				
				td.click(function(){obj.saveOverrideValues(obj, false, "etudesgradebook_studentGrades");obj.loadStudentFullDetailsGrade(obj,value.id, null); return false;});
			
				//pm
				if (value.sendPMLink != undefined && value.sendPMLink != null)
				{
					var td = createIconTd(tr, "pm.png", "Send Private Message", null);
					$(td).addClass("hot");
					$(td).unbind("click").click(function(){obj.openSendPM(obj, value.sendPMLink);return false;});
				} else createEmptyTd(tr);
								
				//notes
				var noteIcon = "notes_plain.png";
				if (value.notes == null || value.notes == undefined) noteIcon = "notes_plain.png";
				else noteIcon ="notes_text.png";
				td = createIconTd(tr, noteIcon, "Add note", null).addClass("hot notes");
				$(td).unbind("click").click(function(){obj.populateNoteComments(obj, value.notes, value.notesLog, value.notesDate, value.name, "overall_grades" , value.id);$("#notesOnStudents").dialog('open');return false;});
				
				// section number
				createTextTd(tr, value.section);
				
				//enrolled/dropped
				var td = createTextTd(tr, value.enrolledStatus);
				if ("Dropped".match(value.enrolledStatus)) $(td).addClass("e3_alert_text");
				if ("Blocked".match(value.enrolledStatus))
				 {
					if (x % 2 == 0) $(tr).removeClass("evenRow");
					$(tr).addClass("blockedRow");				
				 }				
			
				// score
				createTextTd(tr, value.finalScore);
				//out of
				createTextTd(tr, value.totalPoints);
				//extra points
				createTextTd(tr, value.extraScore);
				// overall grade
				createTextTd(tr, value.totalGrade);
				
				//log history
				if (value.logComment == null) createEmptyTd(tr);
				else
				{
					var td = createIconTd(tr, "log.png", "", null).addClass("hot overrideLog");
					$(td).unbind("click").click(function(){obj.populateLogComments(obj, value.logComment, value.name);openAlert("logComments");return false;});
				}
				//grade override
				if (!"Dropped".match(value.enrolledStatus))
				{
					var td=createTextEditTd(tr, value.gradeOverride, 5, "override", null);
					$(td).addClass("center");
					$(td).attr("userId", value.id);
					$(td).attr("loadTime", value.loadTime);
					$(td).addClass("gradeOverride");
					$(td).unbind("change").change(function(){obj.addToOverrideList(obj, $(this), "gradeOverride");return false;});
				}
				else  createEmptyTd(tr);
				
				x= x+1;
			});
		}	
		
		//show extra credit
		if (!obj.studentGrades.showExtraCredit) 
		{
			var i = $("#students_overall_grades_table th:contains('Extra Credit')").index()+1;
			$('#students_overall_grades_table td:nth-child('+i+')').hide();
			$("#students_overall_grades_table th:contains('Extra Credit')").hide();
		}
		
		//if log column empty then hide it
		var hideEmptyLog = $(".overrideLog").length;

		var i = $("#students_overall_grades_table th:contains('Log')").index()+1;
		if (hideEmptyLog == 0) 
		{
			$('#students_overall_grades_table td:nth-child('+i+')').hide();
			$("#students_overall_grades_table th:contains('Log')").hide();
		}
		else
		{
			$('#students_overall_grades_table td:nth-child('+i+')').show();
			$("#students_overall_grades_table th:contains('Log')").show();
		}	
	
	},
	
	populateNoteComments : function (obj, noteContent, noteBy, notesDate, name, callfrom, id)
	{
		var table = $("#noteStudentTable");
		$(table).empty();
		$(table).parent().attr("title", "Notes on " + name);
		$("#notesOnStudents").dialog('option', 'title', 'Notes on ' + name);
		
		var tr = $("<tr />");
		$(table).append(tr);			
		createTextTd(tr, noteBy).addClass("td_bold");
	
		tr = $("<tr />");
		$(table).append(tr);	
		if (noteContent == null || noteContent == undefined) noteContent = "";
		var td = createTextareaEditTd(tr, noteContent, 6, 60, "note_stud", null);
		$(td).attr("notesDate", notesDate);
		$(td).attr("notesCallfrom", callfrom);
		$(td).attr("notesStudentId", id);
	},
	
	populateLogComments : function (obj, t, name)
	{
		var table = $("#logCommentsTable");
		$(table).empty();
		$(table).parent().attr("title", "Grade Log of " + name);
		$.each(t, function(index, value)
		{
			var tr = $("<tr />");
			$(table).append(tr);		
			
			createTextTd(tr, value.newGradeActionTitle).addClass("td_bold");
			createTextTd(tr, value.newGradeInfo + " on " + value.overrideTime);
		});		
	},
	
	populateAssessmentGradeTableHeaderFooter : function (obj, headerElement, makeSortable)
	{
		headerElement.empty();
		var headerRow = $("<tr />");
		headerElement.append(headerRow);
		
		$.each(obj.studentGrades.view2FixedHeaders, function(index, value)
		{
			var th = $('<th />');
			$(th).text(value);
			if (makeSortable && (value.match("Name") || value.match("Section") || value.match("Status") || value.match("Score"))) 
			{
				$(th).addClass("tablesorter-header");
				$(th).unbind("click").click(function(){obj.changeGradesSort(obj, $(this));return false;});
			}
			$(headerRow).append(th);
		});
	
		$.each(obj.studentGrades.assessmentColHeaders, function(index, value)
		{
			var th = $('<th />');
			var trimValue10characters = value.title;
			if (value.title.length > 10) trimValue10characters = value.title.slice(0,9)+"...";
			$(th).text(trimValue10characters);			
			$(th).attr("fullTitle", value.title);
			//tipsy comment above line and uncomment this		$(th).attr("title", value.title);
			$(th).attr("id", value.id);
			$(th).hover( 
				  function (){$(this).css("background","#FFFFFF"); $(this).append('<div class="tooltip"><div class="tooltipTitle">'+ value.title +'</div> <div class="tooltipBody">'+ value.points +' points </div></div>');}, 
				  function () {$(this).css("background",""); $('div.tooltip').remove(); });
				$(th).addClass("hot");
				$(th).click(function(){tool_obj.goToHeaderDetails(tool_obj, value.id); return false;});
				
			if (makeSortable)
			{
				$(th).addClass("addedHeader");
			}
			else 
			{
				$(th).addClass("addedFooter");
			}
			$(headerRow).append(th);
		});
	},
	
	populateStudentsAssessmentsGrade: function(obj)
	{	
		var exportParams = "/?sortBy="+obj.studentGrades.sortPreference+"&sortOrder="+obj.studentGrades.sortOrder +"&section="+obj.studentGrades.selectedSection+"&showExtraCredit="+obj.studentGrades.showExtraCredit;
		exportParams = exportParams.concat("&viewAssessments=2&itemType=" + obj.studentGrades.selectedType);
		$("#student_itemized_grades_export").attr("href","/access/etudesgb/student_grades/" + obj.siteId + "/"+obj.studentGrades.exportFileName + exportParams);
			
		$("#student_grades_assessmentTypes").empty();
		$("#student_grades_assessmentTypes_title").removeClass("e3_offstage");
		$("#student_grades_assessmentTypes").removeClass("e3_offstage");
		
		if (obj.studentGrades.itemTypes != null)
		{
			$.each(obj.studentGrades.itemTypes, function(index, value)
			{
				var option = $("<option>").attr("value", value.id).text(value.title);
				if (value.id.match(obj.studentGrades.selectedType)) option.attr("selected", "selected");
				$("#student_grades_assessmentTypes").append(option);
			});		
		}
		
		if (obj.studentGrades.assessmentColHeaders != null && obj.studentGrades.assessmentColHeaders != undefined)
		{
			var headerElement = $("#students_all_assessments_grades_table thead");
			obj.populateAssessmentGradeTableHeaderFooter(obj, headerElement, true);
			
			var footerElement = $("#students_all_assessments_grades_table tfoot");
			obj.populateAssessmentGradeTableHeaderFooter(obj, footerElement, false);
					
			var toggleClass = ("asc".match(obj.studentGrades.sortOrder)) ? "tablesorter-headerAsc" : "tablesorter-headerDesc";
			$("#students_all_assessments_grades_table thead th:contains('"+ obj.studentGrades.sortPreference+"')").removeClass("tablesorter-header").addClass(toggleClass);	
		}

		// load participants
		$("#students_all_assessments_grades_table tbody").empty();	
		if (obj.studentGrades.participants != null)
		{
			var x=1;
			$.each(obj.studentGrades.participants, function(index, value)
			{
				var tr = $("<tr />");
				$(tr).addClass("assessmentTableExtraSpacing");
			
				if (x % 2 == 0) $(tr).addClass("evenRow");
				$("#students_all_assessments_grades_table tbody").append(tr);

				//name
				var td = createTextTd(tr, value.name);
				td.addClass("hot");
				td.click(function(){obj.loadStudentFullDetailsGrade(obj,value.id, null); return false;});
			
				//pm
				if (value.sendPMLink != undefined && value.sendPMLink != null)
				{
					var td = createIconTd(tr, "pm.png", "Send Private Message", null);
					$(td).addClass("hot");
					$(td).unbind("click").click(function(){obj.openSendPM(obj, value.sendPMLink);return false;});
				} else createEmptyTd(tr);
				
				//notes
				var noteIcon = "notes_plain.png";
				if (value.notes == null || value.notes == undefined) noteIcon = "notes_plain.png";
				else noteIcon ="notes_text.png";
				td = createIconTd(tr, noteIcon, "Add note", null).addClass("hot notes");
				$(td).unbind("click").click(function(){obj.populateNoteComments(obj, value.notes, value.notesLog, value.notesDate, value.name, "all_grades", value.id);$("#notesOnStudents").dialog('open');return false;});
				
				// section number
				createTextTd(tr, value.section);
				
				//enrolled/dropped
				var td = createTextTd(tr, value.enrolledStatus);
				if ("Dropped".match(value.enrolledStatus)) $(td).addClass("e3_alert_text");
				if ("Blocked".match(value.enrolledStatus))
				 {
					if (x % 2 == 0) $(tr).removeClass("evenRow");
					$(tr).addClass("blockedRow");				
				 }			
				
				// score
				createTextTd(tr, value.finalScore);
				//out of
				createTextTd(tr, value.totalPoints);
				//extra points
		//		createTextTd(tr, value.extraScore);
				
				//assessments score
				if(obj.studentGrades.assessmentColHeaders != null && obj.studentGrades.assessmentColHeaders != undefined)
				{
					$.each(obj.studentGrades.assessmentColHeaders, function(index, v)
					{
						var td = createTextTd(tr, value[v.id]);
						td.addClass("addedScoreCol");
					});
				}
				x= x+1;
			});
		}		
	},
	
	// indvidual student summary
	populateStudentNameandStatus : function (obj, role, gradetoDateletterPoints, gradetoDatePoints, gradetoDateHeader, points, pointsRow)
	{
		if ("instructor".match(role))
		{
			$("#student_name").empty().text(obj.studentAssessmentOverview.studentName);
			$("#student_nameiid").empty().text("(" +obj.studentAssessmentOverview.studentIid +")");		
			$("#student_status").empty().removeClass().addClass("e3_configure_set_entry_field").text(obj.studentAssessmentOverview.studentStatus);	
			if ("Dropped".match(obj.studentAssessmentOverview.studentStatus)) $("#student_status").addClass("e3_alert_text");
			if ("Blocked".match(obj.studentAssessmentOverview.studentStatus)) $("#student_status").addClass("blockedText");
			
			$("#detail_feature_title").empty().text("Grade report for " + obj.studentAssessmentOverview.studentName);		
			$(".gradeCounts").empty().text(obj.studentAssessmentOverview.showStudentIndex);
		}
		
		if ("student".match(role))
		{
			$("#indv_student_export").addClass("e3_offstage");
		}	
		if (obj.studentAssessmentOverview.letterGrade != null && !"null".match(obj.studentAssessmentOverview.letterGrade))
		{
			$("#"+gradetoDateletterPoints).empty().text(obj.studentAssessmentOverview.letterGrade);	
			$("#"+gradetoDateletterPoints).addClass("e3_bold");
			$("#"+gradetoDatePoints).empty().text(obj.studentAssessmentOverview.pointsText);
			$("#"+gradetoDateHeader).empty().text(obj.studentAssessmentOverview.pointsHeaderText);	
		}
		else
		{
			$("#"+gradetoDateletterPoints).empty().text(obj.studentAssessmentOverview.pointsText);	
			$("#"+gradetoDatePoints).empty();
			$("#"+gradetoDateHeader).empty().text(obj.studentAssessmentOverview.pointsHeaderText);
		}
		
		if (obj.studentAssessmentOverview.releasedGrades == 2) 
		{
			$("#"+points).empty().text(obj.studentAssessmentOverview.totalCategoryPoints + " (" + obj.studentAssessmentOverview.toGradePercentage +" of the total points possible for the entire course have been graded and released)");
			$("#"+pointsRow).removeClass("e3_offstage");
		}
		else 
		{
			$("#"+points).empty();
			$("#"+pointsRow).addClass("e3_offstage");
		}
	},
	
	// Student landing screen 
	populateStudents: function (obj, tableName, returnSetting, editScore, printLink)
	{	
		//student_assessments_table
		$("#" + tableName+" tbody").empty();
		var bestScore = obj.studentAssessmentOverview.showBestSubmissionOnly;
		$("#student_details_best").addClass("e3_action_current");
		// // /etudesgb/indv_student_grades/siteId/exportFileName/?studentUserId=&studentIId&pointsHeaderText&pointsText
		if (editScore)
		{
			var exportParams = "/?studentUserId="+ obj.studentAssessmentOverview.studentId + "&pointsHeaderText=" + obj.studentAssessmentOverview.pointsHeaderText + "&pointsText="+ obj.studentAssessmentOverview.pointsText;
			$("#student_details_export").attr("href","/access/etudesgb/indv_student_grades/" + obj.siteId + "/"+obj.studentAssessmentOverview.exportIndvStudentFileName + exportParams);
			
			if (obj.studentAssessmentOverview.notes == null || obj.studentAssessmentOverview.notes == undefined) 
				$("#student_details_notes").attr("style", "background-image:url(support/icons/notes_plain.png)");
			else 
				$("#student_details_notes").attr("style", "background-image:url(support/icons/notes_text.png)");
			
			if (obj.studentAssessmentOverview.sendPMLink == null || obj.studentAssessmentOverview.sendPMLink == undefined) 
				$("#student_details_pm").addClass("e3_offstage");
			else 
				$("#student_details_pm").removeClass("e3_offstage");
			
			var releaseColumn = $("#student_details_table thead th:contains('Released')");
			
			if (bestScore == undefined || "true".match(bestScore))
			{
				$("#student_details_best").addClass("e3_action_current");
				$("#student_details_allSubmission").removeClass("e3_action_current");
				if (releaseColumn != undefined && releaseColumn.html() == "Released")
				{
					$(releaseColumn).remove();
				}		
			}
			else 
			{
				$("#student_details_best").removeClass("e3_action_current");
				$("#student_details_allSubmission").addClass("e3_action_current");
				if (releaseColumn != undefined && releaseColumn.html() == undefined)
				{
					$("#student_details_table tr:first").append("<th>Released</th>");
				}	
			}	
		}
		
		if (obj.studentAssessmentOverview.userGradebookList != null)
			{
				var showTypeHeadings = true;
				var recentType = null;
				var typeRowCount = 1;
				$.each(obj.studentAssessmentOverview.userGradebookList, function(index, value)
				{					
					if (showTypeHeadings && value.isCategory != undefined && value.isCategory.match("yes"))
					{
						var trTypeHeading= $("<tr />");
						$(trTypeHeading).addClass("typeHeaderRow");
						$("#" + tableName+" tbody").append(trTypeHeading);
					
						typeRowCount = 1;
						createEmptyTd(trTypeHeading);
					//	createEmptyTd(trTypeHeading);
						
						var headingTitle = value.categoryTitle;
						if (value.showWeight && !value.categoryWeight.match("-")) headingTitle = headingTitle + (" (" + value.categoryWeight + ")");
						else if (!value.showWeight && !value.categoryPoints.match("-")) headingTitle = headingTitle + (" (" + value.categoryPoints + ")");
						var tdHeading = createTextTd(trTypeHeading, headingTitle, null);	
						if (editScore) $(tdHeading).attr("colSpan", "11");
						else $(tdHeading).attr("colSpan", "10");	
						
						return true;
					} else typeRowCount = typeRowCount + 1;
					
					var tr = $("<tr />");
					$(tr).addClass("assessmentTableExtraSpacing");
					
					$("#" + tableName+" tbody").append(tr);
					
					//completion icon	
					var progressStatusIcon = (value.progressStatus != undefined && value.progressStatus != null) ? obj.findCompletionIcon(obj, value.progressStatus) :undefined;
					if (value.progressStatus != undefined && progressStatusIcon != undefined)
					{
						var progressStatusTitle = obj.findCompletionTitle(obj, value.progressStatus);
						createIconTd(tr, progressStatusIcon, progressStatusTitle, null);
					}
					else createEmptyTd(tr);					
					
					// status icon		
					var assessment_status_icon = "information-closed.png";
					if (value.status.match("Open")) assessment_status_icon = "information.png";
					createIconTd(tr, assessment_status_icon, value.status, null);
										
					// show tool icon and title
					var assessmentIconType = obj.findAssessmentIcon(obj, value.toolTitle, value.toolDisplayTitle);		
					createIconTd(tr, assessmentIconType, value.toolDisplayTitle, null);
					
					createTextTd(tr, value.title);
							
					// open
					createTextTd(tr, value.open);
									
					//due
					createTextTd(tr, value.due);
								
					//finish date
					if (value.inProgressStatus)
					{
						td = createTextTd(tr, "in-progress");
						$(td).addClass("tdItalic");
					}
					else
					{	
						td = createTextTd(tr, value.finishDate);
						if (value.lateSubmission != undefined && "true".match(value.lateSubmission)) 
						{
							var lateText = $("<div class='e3_alert_text'>- late</div>");		
							if (editScore) $(td).append(lateText);
						}
						
						if (value.autoSubmission != undefined && "true".match(value.autoSubmission)) 
						{
							var autoText = $("<div class='e3_alert_text'>- auto</div>");		
							if (editScore) $(td).append(autoText);
						}					
					}
										
					// show review icon and reviewed date
					if (value.reviewStatus != undefined)
					{
						var reviewIconType = (value.reviewStatus) ? "eval-reviewed.png" : "eval-not-reviewed.png";
						var reviewPopup = (value.reviewStatus) ? "Evaluation reviewed" : "Evaluation not reviewed";
						td = createIconTextTd(tr, reviewIconType, value.reviewDate,  reviewPopup);
					}
					else
					{
						td = createTextTd(tr, value.reviewDate);
					}					
																			
					createTextTd(tr, value.points);
					
					// score - icon and text box for instructors and score and review link for students
					var score = value.userScore;
					if (value.dropScore) score = "*" + score + "*";
					var gradeScoreLink = obj.setGradeLink(obj, editScore, returnSetting, value.gradeLink);
					var reviewScoreLink = obj.setReviewLink(obj, editScore, returnSetting, value.reviewLink);
					
					if (printLink) reviewScoreLink = undefined;
					
					obj.setScorewithReviewLink(obj, editScore, bestScore, gradeScoreLink, reviewScoreLink, value.releasedScore, score, value.inProgressStatus, obj.studentAssessmentOverview.studentStatus, tr, typeRowCount, obj.studentAssessmentOverview.studentId, value.loadTime, value.id, value.submissionId);
				
					if (showTypeHeadings && (typeRowCount % 2 == 0)) $(tr).removeClass().addClass("evenRow");	
				});					
			}
		 if (showTypeHeadings == false) $("#" + tableName+" tr:even()").css("background-color", "#FAFAFA" );			
		 
		adjustForNewHeight();
	},
	
	populateGradeOptions: function(obj, gradebookData, categories)
	{
		
		$("#grade_display_released_all").empty();
		$("#grade_display_show_letter_grade").empty();		
		
		$("#grade_types_type").empty();
		$("#grade_scale_table tbody").empty();
		
		// grading scale dialog
		$("#grade_types_type_dialog").empty();
		$("#grade_scale_table_dialog tbody").empty();
		
		// grade display - current grade based on released all or all
		var includeGrade = "Released assessments only";
		if (gradebookData.releaseGrades === 1)
		{
			includeGrade = "All assessments, including not released ones (blank entries are treated as zeros)";
		}		 
		createIconTextTd($("#grade_display_released_all"), "bullet_black.png", includeGrade);
		
		// grade display - letter grade
		var showLetterGrade = "No letter grade for points earned";
		if (gradebookData.showLetterGrade == 1)
		{
			showLetterGrade = "Letter grade for points earned";
		}
				
		createIconTextTd($("#grade_display_show_letter_grade"), "bullet_black.png", showLetterGrade);
		
		// final grade calculation
		if (gradebookData.dropScore == 1)
		{
			$("#grade_options_drop_score_checked").prop("checked", true);
		}
		else 
		{
			$("#grade_options_drop_score_checked").prop("checked", false);
		}
		
		var checkDropScore = 0;
		if (categories.categoriesList != null && categories.categoriesList != undefined)
		{
			$("#categories_drop_table tbody").empty();
			$.each(categories.categoriesList, function(index, value)
			{
				var tr = $("<tr />");
				$(tr).attr("dropCount", value.dropLowest);
				$(tr).attr("itemCount", value.emptyCategory);
				$("#categories_drop_table tbody").append(tr);
					 
				createIconTextTd(tr, "bullet_black.png", value.title);
				createLabelTd(tr, value.emptyCategory).addClass("tdCenter");
			
				var tdDrop = createLabelTd(tr, value.dropLowest).addClass("tdCenter");
				var dropError = $("<img/>");
				dropError.attr("id", "dropError"+index);
				dropError.attr("src", "support/icons/error.png");
				dropError.attr("alt", "Drop value is higher than available items in category.");
				dropError.attr("title", "Drop value is higher than available items in category.");
				dropError.addClass("e3_offstage");
				dropError.addClass("detailsHeaderImg");
				dropError.click(function(){openAlert("higher_value_alert"); return false;});
				$(tdDrop).append(dropError);				
				
				if (checkDropScore == 0 && value.dropLowest > 0) checkDropScore = value.dropLowest;				
			});		
		}	
				
		// grade types
		var contextGradingScalesList = gradebookData.contextGradingScalesList;
		if (contextGradingScalesList != null && contextGradingScalesList != undefined)
		{
			$.each(contextGradingScalesList, function(index, value)
			{
				if (gradebookData.selectedGradebookScale.id === value.id)
				{
					 $("#grade_types").text(value.name);
					 
					 // grading scale dialog
					 $("#grade_types_type_dialog").append($("<option>").attr("id", value.id).attr("value", value.scaleType).attr("selected", "selected").text(value.name));
					
					var selectedGradebookScalePercentList = gradebookData.selectedGradebookScale.selectedGradebookScalePercentList;
					
					if (selectedGradebookScalePercentList != null && selectedGradebookScalePercentList != undefined)
					{
						$.each(selectedGradebookScalePercentList, function(index, value)
						{
							var tr = $("<tr />");
							$("#grade_scale_table tbody").append(tr);
							createTextTd(tr, value.letterGrade);
							
							// grading scale dialog
							var trDialog = $("<tr />");
							$("#grade_scale_table_dialog tbody").append(trDialog);
							createTextTd(trDialog, value.letterGrade);
							
							
							if (value.letterGrade.match("I") || value.letterGrade.match("i"))
							{
								createTextTd(tr, "-");									
								createTextTd(trDialog, "-");
							}
							else if (value.letterGrade.match("NP") || value.letterGrade.match("np"))
							{
								createTextTd(tr, "0");									
								createTextTd(trDialog, "0");
							}
							else
							{
								createTextTd(tr, value.percent);
								createTextEditTd(trDialog, value.percent, 10, "grade_scale_"+ value.letterGrade, value.scaleId);
							}							
						});
					}
				}
				else
				{
					// dialog
					$("#grade_types_type_dialog").append($("<option>").attr("id", value.id).attr("value", value.scaleType).text(value.name));
				}
			});
			
			/* buttons */
  			populateToolNavbar(obj,obj.navBarElementId, obj.navbar);
				
			// grade type change - show related grading scales
    		$("#grade_types_type_dialog").change(function()
			{
				var selectedGradingScaleId = $(this).val();
				
				var params = new Object();
				params.siteId = obj.siteId;
				if (selectedGradingScaleId != null) params.gradingScaleId = selectedGradingScaleId;
				params.action="grading_scale_changed";				
				requestCdp("etudesgradebook_gradeoptions", params, function(data)
				{
					obj.populateGradingScalePercentages(obj, data);
					adjustForNewHeight();
				});
			});
		}
	},
	
	populateGradingScalePercentages: function(obj, data)
	{
		$("#grade_scale_table_dialog tbody").empty();
		
		var selectedGradebookScalePercentList = data.gradebook.selectedGradebookScale.selectedGradebookScalePercentList;
		
		if (selectedGradebookScalePercentList != null)
		{
			$.each(selectedGradebookScalePercentList, function(index, value)
			{
				var tr = $("<tr />");
				$("#grade_scale_table_dialog tbody").append(tr);
				createTextTd(tr, value.letterGrade);
				
				if (value.letterGrade != null && (value.letterGrade.match("I") || value.letterGrade.match("i")))
				{
					createTextTd(tr, "-");
				}
				else if (value.letterGrade != null && (value.letterGrade.match("NP") || value.letterGrade.match("np")))
				{
					createTextTd(tr, "0");
				}
				else
				{
					createTextEditTd(tr, value.percent, 10, "grade_scale_"+ value.letterGrade, value.scaleId);
				}
			});
		}
		
	},	
	
	printStudentGrades: function (obj)
	{
		$("#print_feature_title").empty().text("Grade report for " + obj.studentAssessmentOverview.userName);	
		obj.populateStudentNameandStatus(obj, "student", "printStudent_gradetoDate_letterpoints","printStudent_gradetoDate_points","printStudent_gradetoDate_header","printStudent_points","printStudent_points_row");			
		obj.populateStudents(obj, "print_assessments_table", null, false, true);
		
		openAlert("student_print_dialog");
	},

	reloadCategories: function (obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;			
		requestCdp("etudesgradebook_categories", params, function(data)
		{
			obj.categories = data.gradebook.categoriesList;
		});
	},	

	returnBackfromDetails: function (obj)
	{
		$("#gradebook_assessment_detail").addClass("e3_offstage");
		obj.itemId = null;
		if (obj.callFromPage == null || obj.callFromPage == undefined || obj.callFromPage.length == 0)
		{
			obj.detailedItem = null;
			obj.lastSortData = obj.lastOverviewSortData;
			obj.clearDetails(obj);
			selectToolMode(0, obj);
		}
		else if (obj.callFromPage != undefined && obj.callFromPage.match("grades"))
		{
			selectToolMode(1, obj);
		}		
	},

	returnBackFromIndvSummary: function (obj)
	{
		obj.callFromPage="returnBack";
		obj.saveOverrideValues(obj,false, "etudesgradebook_indvidualStudentGrades");
		
		if(obj.viewAssessmentGrades != undefined && "2".match(obj.viewAssessmentGrades))
			selectToolMode(1,obj);
		else selectToolMode(2,obj);
	},
	
	saveBoostUserGrades : function (obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.boostNumber = $("#boost_number").val();
		params.boostType = $("#boost_type").val();
		
		requestCdp("etudesgradebook_boostGrades", params, function(data)
		{
			obj.loadStudentsGrade(obj);
		});
		return true;	
	},	

	saveCategories: function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		
		var i = 0;
		$("#categories_configure_weights_table tbody tr").each(function()
		{
		    var fields = $(this).find("input[type=text],select");
		    var cid = $(this).attr("id");		
		    if (cid != null && cid != undefined) params["categoryId"+i] = cid;
		    params["order"+i]=i.toString(); 
	        params["newTitle"+i] =fields.eq(0).val();	
    	    params["newWeight"+i] =fields.eq(1).val();
  	        params["newDistribution"+i]=fields.eq(2).val();    	    
  	        i+=1;				   
		});
			
		params.categoriesCount = $("#categories_configure_weights_table tbody tr").length.toString();
		params.action="categories_changed";				
		if (obj.assessmentsLastSortData != undefined) params.assessmentsLastSortData = obj.assessmentsLastSortData;
		
		var subTotal = 0;
		$('.weight input').each(function()
		{
			if (!"".match(this.value))
			{
				var i = (isNaN(this.value)) ? 0 : this.value;
				if (i > 0) subTotal += parseFloat(i);		
			}
		});
		
		if ((parseFloat(subTotal)).toFixed(1) != (parseFloat(100)).toFixed(1) && (parseFloat(subTotal)).toFixed(1) != (parseFloat(0)).toFixed(1))
		{
			openAlert("subtotal_category_alert");
			return false;
		}
	
		var credit = $('.extraCredit input').val();
		
		if (credit != undefined && !"".match(credit) && !"0".match(credit) && (parseFloat(subTotal)).toFixed(1) != (parseFloat(100)).toFixed(1))
		{
			openAlert("subtotal_category_alert");
			return false;
		}
			
		requestCdp("etudesgradebook_categories", params, function(data)
		{
			if (data.assessmentsLastSortData != null && data.assessmentsLastSortData != undefined)
			{
				obj.loadAssessments(obj, data.assessmentsLastSortData);	
				adjustForNewHeight();	
			}
			else
			{
				obj.loadCategories(obj);
			}
		});		
		
		return true;
	},

	saveCategoryItemMap: function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
	
		var i = 1;
		$("#assessments_table tr").each(function()
		{
		   var id = "";
		   if ($(this).attr("categoryId") != undefined)  id= "Category-" + $(this).attr("categoryId");
		   else if ($(this).attr("itemId") != undefined) id= "Item-" + $(this).attr("itemId"); 
		   
		   if (id.length != 0)
		   {
		    params["saveMap"+i] = id;
  	        i+=1;		    
		   }
		});
		
		params.mapCount = $("#assessments_table tr").length.toString();
		
		requestCdp("etudesgradebook_assessments", params, function(data)
		{
			obj.loadAssessments(obj, data.lastSortData);
		});
		
		return true;
	},	
	
	// save preferences sort order
	saveCategoriesType: function(obj)
	{
		var selectedSortType = $('input[name="category_sort_option"]:radio:checked').val();
		var params = new Object();
		params.siteId = obj.siteId;
		params.sortType = selectedSortType;
		params.action="grading_manage_type_changed";				
		requestCdp("etudesgradebook_categories", params, function(data)
		{
			// hide the dialog and refresh the preferences
			selectToolMode(4, obj);
		});
		
		return true;
	},
	
	saveDropLowestScores: function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.action = "grading_dropLowest_save";
		params.callDropLowestFrom = obj.callDropLowestFrom;
	
		var i = 0;
		$("#grade_options_edit_lowscore_table tbody tr").each(function()
		{
		    var fields = $(this).find("input[type=text]");		
		    params["categoryId"+i] = $(this).attr("id");
		    params["dropLowest"+i]=fields.eq(0).val();    	    
  	        i+=1;		    
		});
		
		params.categoriesCount = $("#grade_options_edit_lowscore_table tbody tr").length.toString();
		
		params.setLowest = $("#grade_options_edit_drop_score_checked").is(":checked").toString();
		
		var checkAlert = obj.setDropHigherAlert(obj, params, "grade_options_edit_lowscore_table", "#grade_options_edit_drop_score_checked", "#grade_options_edit_drop_score_footnote");
		if (!checkAlert) return false;
		
		requestCdp("etudesgradebook_gradeoptions", params, function(data)
		{
			obj.clear(obj, true, "#gradebook_grade_options");
			if (data.callDropLowestFrom != null && data.callDropLowestFrom != undefined && "grades".match(data.callDropLowestFrom))
			{
				obj.loadStudentsGrade(obj);					
			}
			else
			{
				obj.loadGradeOptions(obj, true);
			}
			
			adjustForNewHeight();			
		});
		
		return true;
	},
	
	saveInstructorNotes : function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		
		params.instructorNotes = $("#note_stud").val();
		params.notesDate = $("#note_stud").parent().attr("notesDate");
		var requestPath = $("#note_stud").parent().attr("notesCallfrom");
		params.notesStudentId = $("#note_stud").parent().attr("notesStudentId");
		
		if ("all_grades".match(requestPath) || "overall_grades".match(requestPath)) 
		{
			if (obj.lastSortData != null) params.lastSortData = obj.lastSortData;
			if (obj.viewAssessmentGrades != null) params.viewAssessmentGrades = obj.viewAssessmentGrades;
			if (obj.selectedSection != null) params.selectedSection = obj.selectedSection;
			if (obj.selectedType != null) params.selectedType = obj.selectedType;
			obj.callStudentGradesCdp(obj, params);
		}
		else if ("assessmentDetails".match(requestPath)) 
		{	
			obj.callAssessmentDetailCdp(obj ,obj.detailedItem.assessmentItem.id, obj.detailedItem.lastOverviewSortData, obj.detailedItem.lastSortData, obj.detailedItem.selectedSection, null, params);
		}			
		else if ("indv_summary".match(requestPath)) 
		{
			params.studentId = obj.studentAssessmentOverview.studentId;
			if (obj.viewAssessmentGrades != null && obj.viewAssessmentGrades != undefined) params.viewAssessmentGrades = obj.viewAssessmentGrades;	
			if (obj.selectedType != null && obj.selectedType != undefined) params.selectedType = obj.selectedType;
			obj.callStudentFullDetailCdp(obj ,params);
		}

		adjustForNewHeight();
		return true;
	},
	
	saveOverrideValues : function(obj, stayPage, requestPath)
	{	
		var params = new Object();
		params.siteId = obj.siteId;
		if (obj.selectedSection != undefined && obj.selectedSection != null) params.selectedSection = obj.selectedSection;
		if (obj.lastSortData != undefined && obj.lastSortData != null) params.lastSortData = obj.lastSortData;	
		if (obj.showBestSubmissionOnly != null) params.showBestSubmissionOnly = obj.showBestSubmissionOnly.toString();
		params.gradesOverrideCount = "0";		
		
		if (stayPage)
		{
			if ("etudesgradebook_studentGrades".match(requestPath)) obj.callStudentGradesCdp(obj, params);
			else if ("etudesgradebook_assessmentDetails".match(requestPath)) 
			{	
				obj.callAssessmentDetailCdp(obj ,obj.detailedItem.assessmentItem.id, obj.detailedItem.lastOverviewSortData, obj.detailedItem.lastSortData, obj.detailedItem.selectedSection, null);
			}			
			else if ("etudesgradebook_indvidualStudentGrades".match(requestPath)) 
			{
				params.studentId = obj.studentAssessmentOverview.studentId;
				if (obj.viewAssessmentGrades != null && obj.viewAssessmentGrades != undefined) params.viewAssessmentGrades = obj.viewAssessmentGrades;	
				if (obj.selectedType != null && obj.selectedType != undefined) params.selectedType = obj.selectedType;
				obj.callStudentFullDetailCdp(obj ,params);
			}
			adjustForNewHeight();
		}
		else
		{
			obj.saveOverrideParams(obj, params, requestPath);

			requestCdp(requestPath , params, function(data)
			{
				// do nothing
			});	
		}
		obj.saveOverrideList = [];
		return true;
	},
	
	saveOverrideParams: function(obj, params, requestPath)
	{
		params.gradesOverrideCount = "0";			
	
		if (obj.saveOverrideList != null && obj.saveOverrideList != undefined && params != null)
		{
			var len = obj.saveOverrideList.length;
			//IE fix
			if (len != undefined && len != null)
			{
				if ("etudesgradebook_indvidualStudentGrades".match(requestPath)) 
				{
					if (obj.studentAssessmentOverview != null && obj.studentAssessmentOverview != undefined) params.studentId = obj.studentAssessmentOverview.studentId;
					if (obj.viewAssessmentGrades != null && obj.viewAssessmentGrades != undefined) params.viewAssessmentGrades = obj.viewAssessmentGrades;	
					if (obj.selectedType != null && obj.selectedType != undefined) params.selectedType = obj.selectedType;
				}
				for (var i=0; i< len; i++)
				{
					params["user"+i] = obj.saveOverrideList[i].userId;
					if (obj.saveOverrideList[i].loadTime != null) params["loadTime"+i] = obj.saveOverrideList[i].loadTime;
					//overall grades
					if ("etudesgradebook_studentGrades".match(requestPath))
					{
						if (obj.saveOverrideList[i].changedGrade != null) params["newGrade"+i] = obj.saveOverrideList[i].changedGrade;		
					}
					//override score from assessment detail and indv student detail
					if ("etudesgradebook_assessmentDetails".match(requestPath) || "etudesgradebook_indvidualStudentGrades".match(requestPath))
					{
						if (obj.saveOverrideList[i].changedScore != null) params["newScore"+i] = obj.saveOverrideList[i].changedScore;
						if (obj.saveOverrideList[i].overrideAssessmentId != null) params["overrideAssessmentId"+i] = obj.saveOverrideList[i].overrideAssessmentId;
						if (obj.saveOverrideList[i].overrideRelease != null) params["overrideRelease"+i] = obj.saveOverrideList[i].overrideRelease;
						if (obj.saveOverrideList[i].overrideSubmissionId != null) params["overrideSubmissionId"+i] = obj.saveOverrideList[i].overrideSubmissionId;
					}					
				}		
				params.gradesOverrideCount = len.toString();
			}		
		}	
	},
	
	// save grading options grading scale
	saveGradingOptionsGradingScale: function(obj)
	{
		/* save preferences */
		var selectedGradingScaleId = $("#grade_types_type_dialog").val();
				
		var params = new Object();
		params.siteId = obj.siteId;
		if (selectedGradingScaleId != null) params.gradingScaleId = selectedGradingScaleId;
		params.action = "grading_scale_save";
		
		// add modified grades to params
		$("input[type=text]").each(function()
		{
			var id = this.id;
			var val = $(this).val();
			
			params[id] = val;
		});
		
		requestCdp("etudesgradebook_gradeoptions", params, function(data)
		{
			obj.clear(obj, true, "#gradebook_grade_options");
			if (data.dataChange === "CanNotModify")
			{
				$("#grade_options_grading_scale_message").css("display", "block");
				$("#grade_options_grading_scale_message_alert").empty();
				$("#grade_options_grading_scale_message_alert").css("color", "red");
				$("#grade_options_grading_scale_message_alert").text("Please remove all grade overrides before changing the grading scale");
			}
			
			obj.loadGradeOptions(obj, false);
		});
		
		return true;
	},	
	
	selectCategoryType :function (obj)
	{
		if (obj.preferences.gradebook.manageType == 2) 
			$("#category_sort_option_2").prop("checked",true);
		else 
			$("#category_sort_option_1").prop("checked",true);
		// show dialog
		$("#category_type_dialog").dialog('open');
	},	
	
	setDropHigherAlert : function (obj, params, tableName, checkboxName, footnoteName)
	{
		if (params.setLowest.match("true") || params.setLowest.match("1"))
		{
			var i = 0;
			var allDropValid = true;
			$("#" + tableName+" tbody tr").each(function()
			{
			    var catAvailableItems = $(this).attr("itemCount");
			    var catDropNumber;
			    if ("grade_options_edit_lowscore_table".match(tableName))
			   	{
			    	var fields = $(this).find("input[type=text]");	
			    	catDropNumber=fields.eq(0).val();  
			   	}
			    else catDropNumber= $(this).attr("dropCount");
	
			    if (catDropNumber != undefined && Number(catDropNumber) > Number(catAvailableItems))
			    {
			    	$(this).find("img").removeClass("e3_offstage");
				   	allDropValid = false;
			    }
	  	        i+=1;		    
			});
		
			if (!allDropValid)
			{
				params.setLowest = "0";
				$(checkboxName).prop("checked", false);
				$(footnoteName).removeClass("e3_offstage");
				return false;
			}			
		}
		$("#grade_options_edit_drop_score_footnote").addClass("e3_offstage");		
		$("#grade_options_drop_score_footnote").addClass("e3_offstage");
		return true;
	},
	
	setDropLowestScore : function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.action = "grading_dropLowest_set";
		params.setLowest = $("#grade_options_drop_score_checked").is(":checked").toString();
		
		var checkAlert = obj.setDropHigherAlert(obj, params, "categories_drop_table", "#grade_options_drop_score_checked", "#grade_options_drop_score_footnote");
		if (!checkAlert) return false;
		
		requestCdp("etudesgradebook_gradeoptions", params, function(data)
		{
			obj.clear(obj, true, "#gradebook_grade_options");
			obj.loadGradeOptions(obj, true);
		});
		return true;
	},
	
	setOverallGradeCalculationPref : function(obj)
	{
		var params = new Object();
		params.siteId = obj.siteId;	
		params.action="overallGrades_type_changed";
		
		// release grades - save the option
		params.releaseGradesType =  $("input[name='release_grades_option']:checked").val();
		params.showLetterGrades =  $("input[name='display_letter_grade']:checked").val();
		
		requestCdp("etudesgradebook_gradeoptions", params, function(data)
		{
			obj.clear(obj, true, "#gradebook_grade_options");
			obj.loadGradeOptions(obj, true);
		});
		return true;	
	},

	setGradeLink : function (obj, editScore, returnSetting, rlink)
	{
		var gradeScoreLink = undefined;
		
		if (!("-").match(rlink)) 
		{						
			if (editScore) gradeScoreLink = $("<a><img src='support/icons/grade_student.png' style='border:0;vertical-align:text-bottom' title='View Graded Submission'/></a>");
			if (editScore && returnSetting != null && returnSetting != undefined) rlink = rlink.concat(returnSetting);
			gradeScoreLink.attr("href", rlink);		
			gradeScoreLink.attr("target", "_top");					
		}
		return gradeScoreLink;	
	},
	
	setReviewLink : function (obj, editScore, returnSetting, rlink)
	{
		var reviewScoreLink = undefined;
		
		if (!("-").match(rlink)) 
		{						
			reviewScoreLink = $("<a>Review</a>");
				
			if (editScore && returnSetting != null && returnSetting != undefined) rlink = rlink.concat(returnSetting);
			reviewScoreLink.attr("href", rlink);		
			reviewScoreLink.attr("target", "_top");					
		}
		return reviewScoreLink;	
	},
	
	setScorewithReviewLink : function (obj, editScore, bestScore, gradeScoreLink, reviewScoreLink, scoreReleased, score, inProgress, studentStatus, tr, trIndex, forStudentId, loadTime, assessmentId, submissionId)
	{
		// show editable field for instructor and when submission is complete
		var instructorEditScore = "false".match(bestScore) && (inProgress == undefined || !"true".match(inProgress));
	
		// user is instructor but submission is in progress so non-editable score
		var instructorNotEditScore = (!"false".match(bestScore) || "true".match(inProgress));
	
		// if student is blocked or Dropped and has no score then keep it non-editable

		var blockedDroppedStudent = ("Dropped".match(studentStatus) || "Blocked".match(studentStatus));
	
		if (editScore && instructorEditScore && !blockedDroppedStudent)
		{
			var reviewTd = createTextTd(tr,"");
			if (gradeScoreLink != undefined) $(reviewTd).append(gradeScoreLink);
			
			$(tr).attr("userId", forStudentId);
			$(tr).attr("loadTime", loadTime);
			$(tr).attr("assessmentId", assessmentId);
			$(tr).attr("submissionId", submissionId);
			
			var scoreTd = createTextEditTd(tr, score, 5, "itemStudentScore", null);	
			$(scoreTd).unbind("change").change(function(){obj.addToOverrideList(obj, tr, "scoreOverride");return false;});
			
			var scoreRelease = createCheckboxTd(tr, scoreReleased, "releasedScore" + trIndex);
			$(scoreRelease).unbind("change").change(function(){obj.addToOverrideList(obj, tr, "scoreOverride");return false;});
			if (scoreReleased == 1) $(scoreRelease).prop("checked", true);
			else $(scoreRelease).prop("checked", false);
		}
		else 
		{
			if (editScore && (instructorNotEditScore || blockedDroppedStudent))
			{
				var reviewTd = createTextTd(tr,"");
				if (gradeScoreLink != undefined) $(reviewTd).append(gradeScoreLink);
			}
			
			var scoreTd = createTextTd(tr,"");
			var scoreText = $("<div>" + score +" </div>");			
			if (reviewScoreLink != undefined) $(scoreText).addClass("reviewScore");
			$(scoreTd).append(scoreText);
	
			if (reviewScoreLink != undefined) $(scoreTd).append(reviewScoreLink);
			if (!editScore) $(scoreTd).attr("colspan","2");
		}
		
	},
	
	sortRowAtNewPosition: function (obj, old_index, new_index)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		params.action="sort_assessment";
		params.oldIdx = old_index.toString();
		params.newIdx = new_index.toString();
		
		requestCdp("etudesgradebook_assessments", params, function(data)
		{
			obj.loadAssessments(obj, obj.assessmentOverview.lastSortData);
		});
		
		return true;
		
	},	   	
  
	showGradesLastColumns : function (obj)
	{
		obj.initializeShowColumns(obj);
		// show last 5 added columns
		var totalColumns = $(".addedHeader").length;
		var lastColumn = totalColumns - obj.showColumnsIterSize;
		obj.showTdTh(obj, lastColumn, totalColumns);	
		obj.lastShowColumn = totalColumns;	
	},
	
	showGradesNextColumn: function (obj)
	{
		var lastColumn = obj.lastShowColumn;		
		obj.initializeShowColumns(obj);
		
		// new last Column 
		var totalColumns = $(".addedHeader").length;
		var newLastColumn = lastColumn + 5;
		if (newLastColumn >= totalColumns) 
		{
			obj.showGradesLastColumns(obj);
		}
		else 
		{
			obj.lastShowColumn = newLastColumn;				
			obj.showTdTh(obj, lastColumn, obj.lastShowColumn);
		}	
	},
	
	showGradesPrevColumn : function (obj)
	{		
		obj.initializeShowColumns(obj);

		var newLastColumn = obj.lastShowColumn - obj.showColumnsIterSize;
		var startColumn = newLastColumn - obj.showColumnsIterSize;
		if (startColumn < 0)
		{
			obj.showGradesFirstColumns(obj);
		}
		else
		{
			obj.lastShowColumn = newLastColumn;
			obj.showTdTh(obj, startColumn, obj.lastShowColumn);
		}
	},
	
	showTdTh: function (obj, startIndex, endIndex)
	{
		var totalColumns = $(".addedHeader").length;

		var fixedColNumber = obj.lastFixedColumn;
		if (startIndex < 0) startIndex = 0;
		for (var i = startIndex; i < endIndex; i++)
		{
			$("#students_all_assessments_grades_table th:eq(" + (i+fixedColNumber) + ")").show();
			$("#students_all_assessments_grades_table tfoot th:eq(" + (i+fixedColNumber) + ")").show();
		}	
		
		for (var i = startIndex + 1; i <= endIndex; i++)
		{
			$("#students_all_assessments_grades_table td:nth-child(" + (i+fixedColNumber) + ")").show();
		}

		//nav title header
		var t = obj.studentGrades.selectedType;
		if (t != null && "2".match(t))
		{
			$(".nav_headers_title").empty().text("Discussions:");
		}
		else if (t != null && "1".match(t)) 
		{
			$(".nav_headers_title").empty().text("Assignments:");
		}
		else if (t != null && "5".match(t))
		{
			$(".nav_headers_title").empty().text("Tests:");
		}
		else if (t != null && "7".match(t))
		{
			$(".nav_headers_title").empty().text("Offline Items:");
		}
		else 
		{
			$(".nav_headers_title").empty().text("Assessments:");
		}
		
		// viewing statement
		if (endIndex > totalColumns) endIndex = totalColumns;
		if (totalColumns == 0)
			$(".nav_headers_text").empty().text("Viewing \(0 - 0\) of 0");
		else
			$(".nav_headers_text").empty().text("Viewing \(" + (startIndex + 1) + " - " + endIndex+"\) of " + totalColumns);
		
		adjustForNewHeight();
	},
	
	showGradesFirstColumns : function (obj)
	{
		obj.initializeShowColumns(obj);
		// show first 5 added columns
		obj.showTdTh(obj, 0, obj.showColumnsIterSize);	
		obj.lastShowColumn = obj.showColumnsIterSize;
	},	

	showGradesTable : function(obj, viewGrade)
	{
		var params = new Object();
		params.siteId = obj.siteId;
		
		if (viewGrade == '2') params.viewAssessmentGrades="2";

		obj.callStudentGradesCdp(obj, params);	
		adjustForNewHeight();
	},
	
	showAllSubmissions: function(obj, from, v)
	{
		obj.showBestSubmissionOnly = v.toString();
		if (from.match("assessmentDetails")) 
		{			
			obj.callAssessmentDetailCdp(obj,obj.detailedItem.itemId, obj.lastOverviewSortData, obj.lastSortData, obj.detailedItem.selectedSection, null);	
		}
		if (from.match("studentDetails"))
		{
			var params = new Object();
			params.siteId = obj.siteId;
			params.studentId = obj.studentAssessmentOverview.studentId;
			if (obj.lastSortData != null && obj.lastSortData != undefined) params.lastSortData = obj.lastSortData;
			if (obj.viewAssessmentGrades != null && obj.viewAssessmentGrades != undefined) params.viewAssessmentGrades = obj.viewAssessmentGrades;	
			if (obj.selectedSection != null && obj.selectedSection != undefined) params.selectedSection = obj.selectedSection;
			if (obj.selectedType != null && obj.selectedType != undefined) params.selectedType = obj.selectedType;			
			
			// best submission 
			if (obj.showBestSubmissionOnly != null) params.showBestSubmissionOnly = obj.showBestSubmissionOnly.toString();		
			obj.callStudentFullDetailCdp(obj,params);
		}
	},
	
	uncheckBestSubmissionOnly: function(obj, from, bestOnly)
	{
		obj.showBestSubmissionOnly = bestOnly.is(":checked").toString();
		if (from.match("assessmentDetails")) 
		{			
			obj.callAssessmentDetailCdp(obj,obj.detailedItem.itemId, obj.lastOverviewSortData, obj.lastSortData, obj.detailedItem.selectedSection, null);	
		}
		if (from.match("studentDetails"))
		{
			var params = new Object();
			params.siteId = obj.siteId;
			params.studentId = obj.studentAssessmentOverview.studentId;
			if (obj.lastSortData != null && obj.lastSortData != undefined) params.lastSortData = obj.lastSortData;
			if (obj.viewAssessmentGrades != null && obj.viewAssessmentGrades != undefined) params.viewAssessmentGrades = obj.viewAssessmentGrades;	
			if (obj.selectedSection != null && obj.selectedSection != undefined) params.selectedSection = obj.selectedSection;
			if (obj.selectedType != null && obj.selectedType != undefined) params.selectedType = obj.selectedType;			
			
			// best submission 
			if (obj.showBestSubmissionOnly != null) params.showBestSubmissionOnly = obj.showBestSubmissionOnly.toString();		
			obj.callStudentFullDetailCdp(obj,params);
		}
	},
	
	updateSubTotal: function(obj, t)
	{	
		// add distribution value to be by points
		var distribution= $(t).attr("distribution");
		var fields = $(t).find("input[type=text]");
		var val = fields.eq(0).val();
		if (val != undefined && !"".match(val))
		{
			$(".distributionTd select").prop("disabled", false);
		}
		
		if (val != undefined && val < 0)
		{			
			$(t).find("img").removeClass("e3_offstage");
			return false;
		}
		else
		{			
			$(t).find("img").addClass("e3_offstage");
		}		
		
		if ("0".match(distribution))
		{
			$(t).next().find("select").val("2");
		}

		var subTotal = 0;
		$("#weight-edit-total").empty();
		$("#weight-edit-subtotal").empty();
		
		$('.weight input').each(function()
		{
			if (!"".match(this.value))
			{
				var i = (isNaN(this.value)) ? 0 : this.value;
				if (i > 0) subTotal += parseFloat(i);		
			}
		});
		//find extra credit value 
		var extraCredit = $(".extraCredit input[type='text']").first().val();	
		
		//write subtotal
		obj.writeTotalWeights(obj, subTotal, extraCredit, "#weight-edit-subtotal", "#weight-edit-total");		
		obj.hideSubTotalAlert(obj, subTotal, "#subTotal-header", true, "#subTotalFootnote");				
	},
		
	writeTotalWeights : function(obj, subTotal, extraCredit, subTotalField, extraCreditField)
	{
		if (!"".match(extraCredit))
		{
			var c = (isNaN(extraCredit)) ? 0 : extraCredit;
			$(extraCreditField).html((parseFloat(subTotal)+parseFloat(c)).toFixed(1) + "%");
		} else $(extraCreditField).html((parseFloat(subTotal)).toFixed(1) + "%");
		
		$(subTotalField).empty().text((parseFloat(subTotal)).toFixed(1) + "%");
	},

	JSONToCSVConvertor : function(obj, JSONData, ReportTitle, ShowLabel)
	 {
		if (JSONData == null || JSONData == undefined) return;
	    //If JSONData is not an object then JSON.parse will parse the JSON string in an Object
		try{
			var arrData = typeof JSONData != 'object' ? JSON.parse(JSONData) : JSONData;
	    } catch(e){window.console && console.log("csv export:"+e.message);return;}
	    
	    if (arrData == null || arrData == undefined) return;
	    
	    var CSV = '';    

	    //This condition will generate the Label/Header
	    if (ShowLabel) {
	        var row = "";
	        
	        //This loop will extract the label from 1st index of on array
	        for (var index in arrData[0]) {
	            
	            //Now convert each value to string and comma-seprated
	            row += index + ',';
	        }
	
	        row = row.slice(0, -1);
	        
	        //append Label row with line break
	        CSV += row + '\r\n';
	    }
	    
	    //1st loop is to extract each row
	    for (var i = 0; i < arrData.length; i++) {
	        var row = "";
	        
	        //2nd loop will extract each column and convert it in string comma-seprated
	        for (var index in arrData[i]) {
	            row += '"' + arrData[i][index] + '",';
	        }
	
	        row.slice(0, row.length - 1);
	        
	        //add a line break after each row
	        CSV += row + '\r\n';
	    }
	
	    if (CSV == '') {        
	        alert("Invalid data");
	        return;
	    }   
	    
	    //Generate a file name
	    //this will remove the blank-spaces from the title and replace it with an underscore
	    var fileName = ReportTitle.replace(/ /g,"_");   
		   
	    // ie doesn't support download attr ..needs to be window open
	    if (navigator.appName.match("Microsoft Internet Explorer"))
	    	{
	    	 var csvIframe = document.getElementById('csvDownload');
	    	 csvIframe = csvIframe.contentWindow || csvIframe.contentDocument;
	    	 CSV = 'sep=,\r\n' + CSV;
	    	  
	    	 csvIframe.document.open("text/csv", "replace");
	    	 csvIframe.document.writeln(CSV);
	    	 csvIframe.document.close();
	    	 csvIframe.focus();
	    	 csvIframe.document.execCommand('SaveAs', true, fileName + '.csv'); 	    
	    	}
	    else
	    	{
	        //Initialize file format you want csv or xls
		    var uri = 'data:text/csv;charset=utf-8,' + escape(CSV);
		
		    //this trick will generate a temp <a /> tag
		    var link = document.createElement("a");    
		    $(link).attr({
		    	'href': uri,
		        'target': '_blank',
		        'download': fileName + '.csv'
		    });
		    
		    //set the visibility hidden so it will not effect on your web-layout
		    link.style = "visibility:hidden";
		    
		    //this part will append the anchor tag and remove it after automatic click
		    document.body.appendChild(link);
		    link.click();
		    document.body.removeChild(link);
	    	}
	}
};

completeToolLoad();
