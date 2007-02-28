<%@ include file="/common/taglibs.jsp"%>


<authz:authorize ifAnyGranted="admin">

	<p>
		Maintenance mode simply puts a notice on every page that
		things might be broken.
	</p>

	<input type="button" style="color:BB3333" name="start"
		onclick="location.href='maintenanceMode.html?start=1'"
		value="Enter Maintenance Mode">

	<input type="button" style="color:BB3333" name="start"
		onclick="location.href='maintenanceMode.html?stop=1'"
		value="Exit Maintenance Mode">
		
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<p>
		Permission denied.
	</p>
</authz:authorize>
