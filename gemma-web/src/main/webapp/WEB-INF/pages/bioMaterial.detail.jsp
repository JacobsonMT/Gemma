<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioMaterial" scope="request" class="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" />

<head>
	<title><fmt:message key="bioMaterial.details" />
	</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/bmFactorValues.js' />


	<script type='text/javascript'>
		Ext.namespace('Gemma');
		Ext.onReady(function() {
		Ext.QuickTips.init();
	
	var bmId = dwr.util.getValue("bmId");
	var bmClass = dwr.util.getValue("bmClass");
	var admin = dwr.util.getValue("hasAdmin");
	var grid = new Gemma.AnnotationGrid( { renderTo : "bmAnnotations",
				readMethod :BioMaterialController.getAnnotation,
				readParams : [{
					id : bmId,
					classDelegatingFor : bmClass
				}],
				writeMethod : OntologyService.saveBioMaterialStatement,
				removeMethod : OntologyService.removeBioMaterialStatement,
				entId : bmId,
				
				editable : admin,
				mgedTermKey : "experiment"
			});
}); 
	  </script>


</head>

<security:authorize ifAnyGranted="GROUP_ADMIN">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize ifNotGranted="GROUP_ADMIN">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="false" />
</security:authorize>

<h2>
	BioMaterial: ${bioMaterial.name} from
	<a title="${expressionExperiment.name}" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}">${expressionExperiment.shortName}</a>
</h2>
<table width="50%" cellspacing="5">

	<tr>
		<td valign="top">
			<b> <fmt:message key="bioMaterial.description" /> </b>
		</td>
		<td>
			<c:choose>
				<c:when test="${not empty bioMaterial.description}">${bioMaterial.description}</c:when>
				<c:otherwise>Description not available</c:otherwise>
			</c:choose>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="taxon.title" /> </b>
		</td>
		<td>
			<c:choose>
				<c:when test="${not empty bioMaterial.sourceTaxon}">${bioMaterial.sourceTaxon.scientificName}</c:when>
				<c:otherwise>Taxon not available</c:otherwise>
			</c:choose>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="databaseEntry.title" /> </b>
		</td>

		<td>
			<c:choose>
				<c:when test="${not empty bioMaterial.externalAccession}">${bioMaterial.externalAccession.accession}</c:when>
				<c:otherwise>No external identifier</c:otherwise>
			</c:choose>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b>Assays used in</b>
		</td>
		<td>
			<ul>

				<c:forEach items="${bioMaterial.bioAssaysUsedIn}" var="assay">
					<li>
						<a href="/Gemma/bioAssay/showBioAssay.html?id=${assay.id}">${assay.name}</a>
					</li>
				</c:forEach>
			</ul>
		</td>
	</tr>


</table>

<h3>
	<fmt:message key="treatments.title" />
</h3>
<display:table name="bioMaterial.treatments" defaultsort="1" class="list" requestURI="" id="treatmentList" pagesize="30"
	decorator="ubic.gemma.web.taglib.displaytag.expression.biomaterial.BioMaterialWrapper">
	<display:column sortable="true" property="name" maxWords="20" />
	<display:column sortable="true" property="description" maxWords="100" />
	<display:column sortable="true" property="orderApplied" maxWords="100" />
</display:table>


<h3>
	<fmt:message key="experimentalDesign.factorValues" />
</h3>

<div id="bmFactorValues" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width: 650px;"></div>

<h3>
	Annotations
</h3>

<div id="bmAnnotations"></div>
<input type="hidden" name="bmId" id="bmId" value="${bioMaterial.id}" />
<input type="hidden" name="bmClass" id="bmClass" value="${bioMaterial.class.name}" />



<table>
	<tr>
		<td COLSPAN="2">
			<DIV align="left">
				<input type="button" onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
					value="Back">
			</DIV>
		</TD>
		<security:accesscontrollist domainObject="${bioMaterial}" hasPermission="1,6">
			<TD COLSPAN="2">
				<DIV align="left">
					<input type="button" onclick="location.href='/Gemma/bioMaterial/editBioMaterial.html?id=${bioMaterial.id}"
						value="Edit">
				</DIV>
			</td>
		</security:accesscontrollist>
	</tr>
</table>
