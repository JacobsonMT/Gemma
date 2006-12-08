<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>

<!--  Summary of array design associations -->
<%-- Admin only --%>
<authz:authorize ifAnyGranted="admin">
<c:if test="${summaryString != null }" >
${summaryString}
</c:if>
</authz:authorize>

		<h1>
		Platforms
		</h1>
		<title>Platforms</title>


		<h3>
			Displaying <c:out value="${numArrayDesigns}" /> Platforms
		</h3>

				<display:table name="arrayDesigns" sort="list" class="list" requestURI="" id="arrayDesignList"
				pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
					<display:column property="name" sortable="true" href="showArrayDesign.html" paramId="id" paramProperty="id"
						titleKey="arrayDesign.name" />
					<display:column property="shortName" sortable="true" titleKey="arrayDesign.shortName" />
					<display:column property="expressionExperimentCountLink" sortable="true" title="Expts" />
					<authz:authorize ifAnyGranted="admin">
						<display:column property="color" sortable="true" titleKey="arrayDesign.technologyType" />
					</authz:authorize>
					<display:column property="summaryTable" title="Probe Summary" />
					<display:setProperty name="basic.empty.showtable" value="true" />
	
				</display:table>




