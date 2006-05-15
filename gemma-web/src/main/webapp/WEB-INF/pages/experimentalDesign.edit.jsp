<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="experimentalDesign" scope="request"
    class="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<spring:bind path="experimentalDesign.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error"><c:forEach var="error"
			items="${status.errorMessages}">
			<img src="<c:url value="/images/iconWarning.gif"/>"
				alt="<fmt:message key="icon.warning"/>" class="icon" />
			<c:out value="${error}" escapeXml="false" />
			<br />
		</c:forEach></div>
	</c:if>
</spring:bind>

<form method="post" action="<c:url value="/experimentalDesign/editExperimentalDesign.html"/>">


<h2>
	<fmt:message key="experimentalDesign.title" />
</h2>
	
<table cellspacing="10">
    <tr>
       <td valign="top">
        	<b>
        	<fmt:message key="experimentalDesign.name" />
            </b>
        </td>
        <td>
        	<spring:bind path="experimentalDesign.name">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
    </tr>
    
    <tr>    
	    <td valign="top">
        	<b>
        		<fmt:message key="experimentalDesign.description" />
            </b>
        </td>
        <td>
        	<spring:bind path="experimentalDesign.description">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        <%--<textarea rows=8 cols=30 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"></textarea>--%>
	        </spring:bind>
	    </td>
	</tr>
	
	<tr>    
	    <td valign="top">
        	<b>
        	<fmt:message key="experimentalDesign.replicateDescription" />
            </b>
        </td>
        <td>
        	<spring:bind path="experimentalDesign.replicateDescription">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
	</tr>
	
	<tr>    
	    <td valign="top">
        	<b>
        	<fmt:message key="experimentalDesign.qualityControlDescription" />
            </b>
        </td>
        <td>
        	<spring:bind path="experimentalDesign.qualityControlDescription">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
	</tr>
	
	<tr>    
	    <td valign="top">
        	<b>
        	<fmt:message key="experimentalDesign.normalizationDescription" />
            </b>
        </td>
        <td>
        	<spring:bind path="experimentalDesign.normalizationDescription">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
	</tr>		
		
</table>
		<h3>
            <fmt:message key="experimentalFactors.title" />
        </h3>
        <display:table name="experimentalDesign.experimentalFactors" class="list" requestURI="" id="experimentalFactorList"
        export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExperimentalDesignWrapper">
        	<display:column property="id" sortable="true" href="/Gemma/experimentalFactor/showExperimentalFactor.html" paramId="id" paramProperty="id"/>
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
            <display:column property="factorValuesLink" sortable="true" maxWords="100" titleKey="experimentalDesign.factorValues"  />
        </display:table>
		
        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=experimentalDesign.getAuditTrail()%>" />
            
        <br />
			
		<table>
		<tr>
        <td>
	    	<input type="submit" class="button" name="save" value="<fmt:message key="button.save"/>" />
            <input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
       	</td>
       	</tr>
       	</table>
       
   
</form>
<%-- TODO
<validate:javascript formName="experimentalDesignForm" staticJavascript="false"/>
<script type="text/javascript"
      src="<c:url value="/scripts/validator.jsp"/>"></script>
--%>