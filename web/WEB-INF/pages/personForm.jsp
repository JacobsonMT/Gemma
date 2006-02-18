<%@ include file="/common/taglibs.jsp"%>
<title><fmt:message key="personDetail.title" /></title>
<content tag="heading">
<h1><fmt:message key="personDetail.heading" /></h1>
</content>
<%--
<spring:bind path="person.*">
    <c:if test="${not empty status.errorMessages}">
    <div class="error">	
        <c:forEach var="error" items="${status.errorMessages}">
            <img src="<c:url value="/images/iconWarning.gif"/>"
                alt="<fmt:message key="icon.warning"/>" class="icon" />
            <c:out value="${error}" escapeXml="false"/><br />
        </c:forEach>
    </div>
    </c:if>
</spring:bind>

<form method="post" action="<c:url value="/welcome.jsp"/>" id="personForm"
    onsubmit="return validatePerson(this)">
--%>
<form method="post" action="">
<table class="detail">
	<tr>
		<th><Gemma:label key="person.firstName" /></th>

		<td alignment="right" width="31%"><strong>First Name</strong></td>
		<td><spring:bind path="person.firstName">
			<%--
                <input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" 
                    value="<c:out value="${status.value}"/>" />
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            --%>
			<input type="text" name="firstName"
				id="<c:out value="${status.expression}"/>"
				value="<c:out value="${status.value}"/>" />
			<span class="fieldError"><c:out value="${status.errorMessage}" /></span>
		</spring:bind></td>
	</tr>
	<%--
<spring:bind path="person.id">
<input type="hidden" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/> 
</spring:bind>
--%>

	<tr>
		<th><Gemma:label key="person.middleName" /></th>

		<td alignment="right" width="31%"><strong>Middle Name:</strong></td>
		<td><spring:bind path="person.middleName">
			<%--
                <input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" 
                    value="<c:out value="${status.value}"/>" />
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            --%>
			<input type="text" name="middleName"
				id="<c:out value="${status.expression}"/>"
				value="<c:out value="${status.value}"/>" />
			<span class="fieldError"><c:out value="${status.errorMessage}" /></span>
		</spring:bind></td>
	</tr>

	<tr>

		<th><Gemma:label key="person.lastName" /></th>

		<td align="right" width="31%"><strong>Last Name:</strong></td>
		<td><spring:bind path="person.lastName">
			<%--
                <input type="text" name="<c:out value="${status.expression}"/>" id="<c:out value="${status.expression}"/>" 
                    value="<c:out value="${status.value}"/>" />
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            --%>
			<input type="text" name="lastName"
				id="<c:out value="${status.expression}"/>"
				value="<c:out value="${status.value}"/>" />
			<span class="fieldError"><c:out value="${status.errorMessage}" /></span>
		</spring:bind></td>
	</tr>


	<tr>
		<td></td>
		<td align="right"></td>
		<td class="buttonBar"><input type="submit" class="button" name="save"
			onclick="bCancel=false" value="<fmt:message key="button.save"/>" />
		<input type="submit" class="button" name="delete"
			onclick="bCancel=true;return confirmDelete('Person')"
			value="<fmt:message key="button.delete"/>" /> <input type="submit"
			class="button" name="cancel" onclick="bCancel=true"
			value="<fmt:message key="button.cancel"/>" /></td>
	</tr>
</table>
</form>


<validate:javascript formName="person" cdata="false"
	dynamicJavascript="true" staticJavascript="false" />
<script type="text/javascript"
	src="<c:url value="/scripts/validator.jsp"/>"></script>
<script type="text/javascript">
    document.forms["person"].elements["firstName"].focus();
</script>
