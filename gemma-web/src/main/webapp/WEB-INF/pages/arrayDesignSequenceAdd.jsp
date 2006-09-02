<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="arrayDesignSequenceAddCommand" scope="request"
	class="ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignSequenceAddCommand" />

<h1>
	Associate sequences with an array design
</h1>

<form method="post"
	action="<c:url value="/arrayDesign/associateSequences.html"/>"
	enctype="multipart/form-data">

	<table>
		<tr>
			<td>

				<spring:bind path="arrayDesignSequenceAddCommand.*">
					<c:if test="${not empty status.errorMessages}">
						<div class="error">
							<c:forEach var="error" items="${status.errorMessages}">
								<img src="<c:url value="/images/iconWarning.gif"/>"
									alt="<fmt:message key="icon.warning"/>" class="icon" />
								<c:out value="${error}" escapeXml="false" />
								<br />
							</c:forEach>
						</div>
					</c:if>
				</spring:bind>

			</td>
		</tr>
		<tr>
			<td>

				<spring:bind path="arrayDesignSequenceAddCommand.arrayDesign">
					<select name="${status.expression}">
						<c:forEach items="${arrayDesigns}" var="arrayDesign">
							<spring:transform value="${arrayDesign}" var="name" />
							<option value="${name}"
								<c:if test="${status.value == name}">selected</c:if>>
								${name}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>

			</td>
		</tr>
		<tr>
			<td>

				<spring:bind path="arrayDesignSequenceAddCommand.sequenceFile.file">
					<input type="file" size=30
						name="<c:out value="${status.expression}" />"
						value="<c:out value="${status.value}" />" />
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>

			</td>
		</tr>
		<tr>
			<td>


				<spring:bind path="arrayDesignSequenceAddCommand.sequenceType">
					<select name="${status.expression}">
						<c:forEach items="${sequenceTypes}" var="sequenceType">
							<option value="${sequenceType}"
								<c:if test="${status.value == sequenceType}">selected</c:if>>
								${sequenceType}
							</option>
						</c:forEach>
					</select>
					<span class="fieldError">${status.errorMessage}</span>
				</spring:bind>

			</td>
		</tr>
		<tr>
			<td>

				<input type="submit" class="button" name="submit"
					value="<fmt:message key="button.submit"/>" />
				<input type="submit" class="button" name="cancel"
					value="<fmt:message key="button.cancel"/>" />

			</td>
		</tr>
	</table>

</form>

