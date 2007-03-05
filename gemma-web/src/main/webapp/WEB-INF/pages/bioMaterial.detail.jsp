<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioMaterial" scope="request"
    class="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title> <fmt:message key="bioMaterial.details" /> </title>

        <h2>
            <fmt:message key="bioMaterial.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioMaterial.name" />
                    </b>
                </td>
                <td>
                	<%if (bioMaterial.getName() != null){%>
                    	<jsp:getProperty name="bioMaterial" property="name" />
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>
        
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioMaterial.description" />
                    </b>
                </td>
                <td>
                	<%if (bioMaterial.getDescription() != null){%>
                    	<jsp:getProperty name="bioMaterial" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="taxon.title" />
                    </b>
                </td>
                <td>
                	<%if (bioMaterial.getSourceTaxon() != null){
                    	out.print(bioMaterial.getSourceTaxon().getScientificName());
                    }else{
                    	out.print("Taxon unavailable");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="databaseEntry.title" />
                    </b>
                </td>
                <td>
                	<%if (bioMaterial.getExternalAccession() != null){
                    	out.print(bioMaterial.getExternalAccession().getAccession() + "." + bioMaterial.getExternalAccession().getAccessionVersion());
                    }else{
                    	out.print("No accession");
                    }%>
                </td>
            </tr>
                 
         </table>  
        <h3>
            <fmt:message key="treatments.title" />
        </h3>
        <display:table name="bioMaterial.treatments"  defaultsort="1" class="list" requestURI="" id="treatmentList"
         pagesize="30" decorator="ubic.gemma.web.taglib.displaytag.expression.biomaterial.BioMaterialWrapper">
            <display:column sortable="true"  property="name" maxWords="20" />
            <display:column sortable="true"  property="description" maxWords="100" />
            <display:column sortable="true"  property="orderApplied" maxWords="100" />
        </display:table>
        
        <h3>
            <fmt:message key="characteristics.title" />
        </h3>
        <display:table name="bioMaterial.characteristics" defaultsort="1" class="list" requestURI="" id="characteristicList"
        pagesize="30" >
            <display:column sortable="true" property="category" maxWords="100" />
            <display:column sortable="true"  property="value" maxWords="100" />
        </display:table>
	    		
    
    <table>
    <tr>
    <td COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
            value="Back"></DIV>
            </TD>
        <authz:acl domainObject="${bioMaterial}" hasPermission="1,6">
            <TD COLSPAN="2">    
            <DIV align="left"><input type="button"
            onclick="location.href='/Gemma/bioMaterial/editBioMaterial.html?id=<%=bioMaterial.getId()%>'"
            value="Edit"></DIV>
            </td>
        </authz:acl>
    </tr>
    </table>