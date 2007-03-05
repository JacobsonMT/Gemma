<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<title>Array Design Details</title><content tag="heading">Array Design Details</content>

<!--  Summary of array design associations -->
<c:if test="${ summary != ''}" >
<table class='datasummaryarea'>
<caption>
	<jsp:getProperty name="arrayDesign" property="name" />
		<a class="helpLink" href="?" onclick="showHelpTip(event, ''
		+ '		<p>'
		+ '	The platform/array design page shows a summary of our gene assignment analysis for the '
		+ '	current platform. This summary is broken down into categories described below.'
		+ '</p>'
		+ '<p>'
		+ '	A probe refers to a feature or a set of features on the array that form the primary analytical unit'
		+ '	and may in fact be made up of a set of probes (e.g. Affymetrix arrays). '
		+ '</p>'
		+ '<p>'
		+ '	A gene can be a known gene, a predicted gene, or a potential gene represented only by an alignment '
		+ '	(a probe-aligned region or PAR). A PAR is only assigned to a probe if it does not map to a known'
		+ '	or predicted gene.'
		+ '</p>'
		+ '<ul>'
		+ '	<li>'
		+ '		<p>'
		+ '		Probes with sequences:'
		+ '		</p>'
		+ '		<p>'
		+ '		The number of probes that have a provided sequence. For some array designs, sequences are missing'
		+ '		for some or all of its probes. Probes without sequences are excluded from further analysis.'
		+ '		</p>'
		+ '	</li>'
		+ '	<li>'
		+ '		<p>'
		+ '		Probes with genome alignments:'
		+ '		</p>'
		+ '		<p>'
		+ '		The number of probes whose sequences provide an alignment to the genome. Some probe-genome alignments do not'
		+ '		necessarily meet stringency requirements and are not included in the database.'
		+ '		</p>'
		+ '	</li>'
		+ '	<li>'
		+ '		<p>'
		+ '		Probes mapping to genes:'
		+ '		</p>'
		+ '		<p>'
		+ '		Of probes with alignments, how many are mapped to known genes, predicted genes, and probe-aligned regions'
		+ '		(a breakdown is provided). The details of the criteria used for determining if an alignment corresponds to '
		+ '		a gene (or predicted gene, or a PAR) will be documented elsewhere.'
		+ '		</p>'
		+ '	</li>'
		+ '	<li>'
		+ '		<p> '
		+ ' 	Unique genes represented'
		+ '		</p>'
		+ '		<p>'
		+ '		The number of distinct known genes, predicted genes, and probea-aligned regions represented in the array.'
		+ '		This number can be higher or lower than the number of probes mapping because some probes can map to '
		+ '		multiple genes, and some genes have more than one probe mapping.'
		+ '		</p>'
		+ '	</li>'
		+ ' </ul>' +
		''); return false">
		<img src="/Gemma/images/help.png" />
		</a>
</caption>
<tr>
<td>

<table class='datasummary'>
<tr> 
	<td colspan=2 align=center>
	</td>
</tr>
<authz:authorize ifAnyGranted="admin">
<tr>
	<td>Probes with sequences</td>
	<td>${summary.numProbeSequences}</td>
</tr>
<tr>
	<td>Probes with genome alignments</td>
	<td>${summary.numProbeAlignments}</td>
</tr>
</authz:authorize>
<tr>
	<td>Probes mapping to gene(s)</td>
	<td>${summary.numProbesToGenes}</td>
</tr>
	<tr>
		<td>&nbsp;&nbsp;Probes mapping to probe-aligned region(s)</td>
		<td>${summary.numProbesToProbeAlignedRegions}</td>
	</tr>
    <tr>
    	<td>&nbsp;&nbsp;Probes mapping to predicted genes</td>
       	<td>${summary.numProbesToPredictedGenes}</td>
    </tr>
    <tr>
       	<td>&nbsp;&nbsp;Probes mapping to known genes</td>
       	<td>${summary.numProbesToKnownGenes }</td>
    </tr>
    <tr>
    	<td>Unique genes represented</td>
    	<td>${summary.numGenes}</td>
    </tr>
    <tr>
       	<td colspan=2 align='center' class='small'>
       		(as of ${summary.dateCached})
       	</td>
    </tr>
</table>

</td>
</tr>
</table>
</c:if>
		 
		<table>
			<tr>
				<td class="label">
					Name
				</td>
				<td>
					<jsp:getProperty name="arrayDesign" property="name" />
				</td>
			</tr>
			<tr>
				<td class="label">
					Provider
				</td>
				<td>
					<%
					if ( arrayDesign.getDesignProvider() != null ) {
					%>
					<%=arrayDesign.getDesignProvider().getName()%>
					<%
					} else {
					%>
					(Not listed)
					<%
					}
					%>
				</td>
			</tr>
			<tr>
				<td class="label">
					Species
				</td>
				<td>
					<c:out value="${taxon}" />
				</td>
			</tr>
			<tr>
				<td class="label">
					Number of probes
				</td>
				<td>
					<c:out value="${numCompositeSequences}" />  <a href="/Gemma/arrays/showCompositeSequenceSummary.html?id=<jsp:getProperty name="arrayDesign" property="id" />"><img src="/Gemma/images/magnifier.png" /></a>
 
				</td>
			</tr>
			<tr>
				<td class="label">
					Accessions
				</td>
				<td>
					<%
					  if ( ( arrayDesign.getExternalReferences() ) != null
					                    && ( arrayDesign.getExternalReferences().size() > 0 ) ) {
					%>
					<c:forEach var="accession"
						items="${ arrayDesign.externalReferences }">
        				<Gemma:databaseEntry
            				databaseEntry="${accession}" />
						<br />
					</c:forEach>
					<%
					  }
					%>
				</td>
			</tr>
			<tr>
				<td class="label">
					Expression experiments using this array
				</td>
				<td>
					<c:out value="${numExpressionExperiments}" />
					<a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=<c:out value="${expressionExperimentIds}" />">
						<img src="/Gemma/images/magnifier.png" />
					</a>
				</td>
			</tr>
			<tr>
				<td class="label">
					Type
				</td>
				<td>
					<c:out value="${technologyType}" />
				</td>
			</tr>
			
			<tr>
				<td class="label">
					Description
				</td>
				<td>
					<%
					if ( arrayDesign.getDescription() != null && arrayDesign.getDescription().length() > 0 ) {
					%>
						<div class="clob"> <jsp:getProperty name="arrayDesign" property="description" /> </div>
					<%
					} else {
					%>
					(None provided)
					<%
					}
					%>
				</td>
			</tr>
			
			

			<table>
			<tr>
				<td colspan="2">
					<hr />
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div align="left">
						<input type="button" onclick="location.href='showAllArrayDesigns.html'" value="Back">
					</div>
				</td>
			<authz:authorize ifAnyGranted="admin">
					<td COLSPAN="2">
						<div align="left">
							<input type="button"
								onclick="location.href='/Gemma/arrayDesign/editArrayDesign.html?id=<%=request.getAttribute( "id" )%>'"
								value="Edit">
						</div>
					</td>
				</authz:authorize>
			</tr>
		</table>
		
