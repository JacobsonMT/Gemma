<%@ include file="/common/taglibs.jsp"%>


<title>Expression Experiment Link Summary</title>


<h3>
	Expression Experiment Summaries
	<br>
	Displaying
	<b> <c:out value="${numExpressionExperiments}" /> </b> Datasets
</h3>
<a class="helpLink" href="?"
	onclick="showHelpTip(event, 'Summarizes multiple expression experiments.'); return false">Help</a>

<authz:authorize ifAnyGranted="admin">
	<p>
		<a
			href="<c:url value="/expressionExperiment/generateExpressionExperimentLinkSummary.html"/>"
			onclick="return confirm('Regenerate reports for all experiments?');">
			Regenerate Expression Experiment Link Summaries </a>

	</p>
	<display:table pagesize="50" name="expressionExperiments" sort="list" defaultsort="8" defaultorder="descending"
		class="list" requestURI="" id="expressionExperimentList"
		decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
	
		<display:column property="nameLink" sortable="true" sortProperty="name"
			titleKey="expressionExperiment.name"
			comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
		<display:column property="status" sortable="true"
			titleKey="expressionExperiment.status" style="text-align:center; vertical-align:middle;"
			comparator="ubic.gemma.web.taglib.displaytag.StringComparator"
			defaultorder="descending" />
		<display:column property="shortName" sortable="true"
			titleKey="expressionExperiment.shortName"
			comparator="ubic.gemma.web.taglib.displaytag.StringComparator" />
		<display:column property="coexpressionLinkCount" sortable="true"
			titleKey="expressionExperiment.coexpressionLinkCount"
			comparator="ubic.gemma.web.taglib.displaytag.NumberComparator"
			defaultorder="descending" />
		<display:column property="preferredDesignElementDataVectorCount"
			sortable="true" defaultorder="descending"
			comparator="ubic.gemma.web.taglib.displaytag.NumberComparator"
			titleKey="expressionExperiment.preferredDesignElementDataVectorCount" />
		<display:column property="bioMaterialCount" sortable="true"
			defaultorder="descending"
			titleKey="expressionExperiment.bioMaterialCount"
			comparator="ubic.gemma.web.taglib.displaytag.NumberComparator" />
		<display:column property="dateCachedNoTime" sortable="true"
			defaultorder="descending" title="Cached" />
		<display:column property="dateCreatedNoTime" sortable="true"
			defaultorder="descending" title="Created"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="dateMissingValueAnalysisNoTime"
			sortable="true" defaultorder="descending" title="MV Analysis"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="dateRankComputationNoTime" sortable="true"
			defaultorder="descending" title="Rank Computed"
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="dateLastArrayDesignUpdatedNoTime"
			sortable="true" defaultorder="descending" title="Array Des. Mod."
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="dateLinkAnalysisNoTime" sortable="true"
			defaultorder="descending" title="Link Analyzed" 
			comparator="ubic.gemma.web.taglib.displaytag.DateStringComparator" />
		<display:column property="refreshReport" title="Refresh" />
		<display:setProperty name="basic.empty.showtable" value="true" />
		
	</display:table>
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<p>
		Permission denied.
	</p>
</authz:authorize>