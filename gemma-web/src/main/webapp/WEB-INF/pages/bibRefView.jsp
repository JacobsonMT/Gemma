<%@ include file="/common/taglibs.jsp"%>

<%-- $Id$ --%>
<%-- Shows the results of a search for pubmed references. --%>

<jsp:useBean id="bibliographicReference" scope="request"
	class="ubic.gemma.model.common.description.BibliographicReferenceImpl"></jsp:useBean>

<title>Bibliographic Reference Search Results</title>


<h2>
	Bibliographic Reference Search Results
</h2>

<c:if test="${!requestScope.existsInSystem}">
	<p>
		This reference was obtained from PubMed; it is not in the Gemma
		system. You can add it to Gemma by clicking the button on the bottom
		of the page, or do a
		<a href="<c:url value="/bibRef/bibRefSearch.html"/>">new search</a>.
	</p>
</c:if>

<spring:hasBindErrors name="bibliographicReference">
	<div class="error">
		There were the following error(s) with your submission:
		<ul>
			<c:forEach var="errMsgObj" items="${errors.allErrors}">
				<li>
					<spring:message code="${errMsgObj.code}"
						text="${errMsgObj.defaultMessage}" />
				</li>
			</c:forEach>
		</ul>
	</div>
</spring:hasBindErrors>

<Gemma:bibref bibliographicReference="${bibliographicReference}" />


<table>
	<tr>
		<td align="left">
			<c:if test="${!requestScope.existsInSystem}">
				<div align="left">
					<form method="GET" action="<c:url value="/bibRef/bibRefAdd.html"/>"
						<input type="hidden" name="acc"
							value="${bibliographicReference.pubAccession.accession}">
						<input type="submit" value="Add to Gemma Database">
					</form>
				</div>
			</c:if>
		</td>
		<td>
			<c:if test="${requestScope.existsInSystem}">
				<authz:acl domainObject="${bibliographicReference}"
					hasPermission="1,6">
					<div align="right">
						<form method="get"
							action="<c:url value="/bibRef/deleteBibRef.html"/>" 
							<input type="hidden"  name="acc" value="${bibliographicReference.pubAccession.accession}"
							<input type="submit"  
										value="Delete from Gemma" /></form>
					</div>
				</authz:acl>
			</c:if>
		</td>


		<td>
			<c:if test="${requestScope.existsInSystem}">
				<authz:acl domainObject="${bibliographicReference}"
					hasPermission="1,6">

					<div align="right">
						<form method="GET" action="<c:url value="/bibRefEdit.html"/>">
							<input type="submit" value="Edit" />
							<input type="hidden" name="id"
								value="${bibliographicReference.id}">
						</form>
					</div>
				</authz:acl>
			</c:if>
		</td>

	</tr>



</table>

<div align="left">
	<a href="<c:url value="/bibRefSearch.html"/>">New Search</a>
</div>


