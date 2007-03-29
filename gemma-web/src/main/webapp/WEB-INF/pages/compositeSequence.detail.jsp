<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="compositeSequence" scope="request"
	class="ubic.gemma.model.expression.designElement.CompositeSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<title><fmt:message key="compositeSequence.title" />
</title>

<table id="csTableList">
	<tr>
		<td>
			<h2>
				<fmt:message key="compositeSequence.title" />
			</h2>
			<table width="100%">
				<tr>
					<td valign="top">
						<b> <fmt:message key="compositeSequence.name" /> </b>
						<a class="helpLink" href="?"
							onclick="showHelpTip(event, 'Identifier for the probe, provided by the manufacturer or the data submitter.'); return false"><img
								src="/Gemma/images/help.png" /> </a>
					</td>
					<td>
						<%
						if ( compositeSequence.getName() != null ) {
						%>
						<jsp:getProperty name="compositeSequence" property="name" />
						<%
						                } else {
						                out.print( "No name available" );
						            }
						%>
					</td>
				</tr>
				<tr>
					<td valign="top">
						<b> <fmt:message key="compositeSequence.description" /> <a
							class="helpLink" href="?"
							onclick="showHelpTip(event, 'Description for the probe, usually provided by the manufacturer. It might not match the sequence annotation!'); return false"><img
									src="/Gemma/images/help.png" /> </a> </b>
					</td>
					<td>
						<%
						if ( compositeSequence.getDescription() != null ) {
						%>
						<jsp:getProperty name="compositeSequence" property="description" />
						<%
						                } else {
						                out.print( "No description available" );
						            }
						%>
					</td>
				</tr>
				<tr>
					<td valign="top">
					<a class="helpLink" href="?"
							onclick="showHelpTip(event, 'The array design this probe belongs to.'); return false"><img
							src="/Gemma/images/help.png" /> </a>
						<b> Array Design </b>
					</td>
					<td>
						<%
						if ( compositeSequence.getArrayDesign().getName() != null ) {
						%>
						${
						compositeSequence.arrayDesign.name}
						<%
						                } else {
						                out.print( "Array Design unavailable." );
						            }
						%>
					</td>
				</tr>
				<tr>
					<td valign="top">
						<b> Taxon </b>
					</td>
					<td>
						<%
						if ( compositeSequence.getBiologicalCharacteristic().getTaxon() != null ) {
						%>
						${
						compositeSequence.biologicalCharacteristic.taxon.scientificName}
						<%
						                } else {
						                out.print( "No taxon information available" );
						            }
						%>
					</td>
				</tr>
				<tr>
					<td valign="top">
						<b> Sequence Type </b>
						<a class="helpLink" href="?"
							onclick="showHelpTip(event, 'The type of this sequence as recorded in our system'); return false"><img
								src="/Gemma/images/help.png" /> </a>
					</td>
					<td>

						<c:choose>
							<c:when
								test="${compositeSequence.biologicalCharacteristic != null }">
								<spring:bind
									path="compositeSequence.biologicalCharacteristic.type">
									<spring:transform
										value="${compositeSequence.biologicalCharacteristic.type}">
									</spring:transform>
								</spring:bind>
							</c:when>
							<c:otherwise>
								<%="[Not available]"%>
							</c:otherwise>
						</c:choose>


					</td>
				</tr>
				<tr>
					<td valign="top">
						<b> Sequence name <a class="helpLink" href="?"
							onclick="showHelpTip(event, 'Name of the sequence in our system.'); return false"><img
									src="/Gemma/images/help.png" /> </a> </b>
					</td>
					<td>
						<%
						if ( compositeSequence.getBiologicalCharacteristic().getName() != null ) {
						%>
						${compositeSequence.biologicalCharacteristic.name }
						<%
						                } else {
						                out.print( "No name available" );
						            }
						%>
					</td>
				</tr>
					<tr>
						<td valign="top">
							<a class="helpLink" href="?"
								onclick="showHelpTip(event, 'Description of the sequence in our system.'); return false"><img
									src="/Gemma/images/help.png" /> </a>
							<b> Sequence description </b>
						</td>
						<td>
							<%
							                        if ( compositeSequence.getBiologicalCharacteristic() != null
							                        && compositeSequence.getBiologicalCharacteristic().getDescription() != null ) {
							%>
							${compositeSequence.biologicalCharacteristic.description }
							<%
							                    } else {
							                    out.print( "No description available" );
							                }
							%>
						</td>
					</tr>
				<tr>
					<td valign="top">
						<b> Sequence accession <a class="helpLink" href="?"
							onclick="showHelpTip(event, 'External accession for this sequence, if known'); return false"><img
									src="/Gemma/images/help.png" /> </a> </b>
					</td>
					<td>
						<%
						                if ( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null ) {
						                String organism = compositeSequence.getBiologicalCharacteristic().getTaxon().getCommonName();
						                String database = "hg18";
						                if ( organism.equalsIgnoreCase( "Human" ) ) {
						                    database = "hg18";
						                } else if ( organism.equalsIgnoreCase( "Rat" ) ) {
						                    database = "rn4";
						                } else if ( organism.equalsIgnoreCase( "Mouse" ) ) {
						                    database = "mm8";
						                }
						                // build position if the biosequence has an accession
						                // otherwise point to location
						                String position = compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry()
						                        .getAccession();
						                String link = position + " <a href='http://genome.ucsc.edu/cgi-bin/hgTracks?clade=vertebrate&org="
						                        + organism + "&db=" + database + "&position=+" + position
						                        + "&pix=620'>(Search UCSC Genome Browser)</a>";

						                out.print( link );

						            } else {
						                out.print( "No accession available" );
						            }
						%>
					</td>
				</tr>
				<tr>
					<td valign="top">
						<b> Sequence length </b>
					</td>
					<td>
						<%
						                if ( compositeSequence.getBiologicalCharacteristic().getSequence() != null ) {
						                out.print( compositeSequence.getBiologicalCharacteristic().getSequence().length() );
						            } else {
						                out.print( "No sequence available" );
						            }
						%>
					</td>
				</tr>
				<tr>
					<td valign="top">
						<b> Sequence </b>
					</td>
					<td>
						<%
						                if ( compositeSequence.getBiologicalCharacteristic().getSequence() != null ) {
						                String sequence = compositeSequence.getBiologicalCharacteristic().getSequence();
						                String formattedSequence = org.apache.commons.lang.WordUtils.wrap( sequence, 80, "<br />", true );
						%>
						<div class="clob">
							<%
							out.print( formattedSequence );
							%>
						</div>
						<%
						                } else {
						                out.print( "No sequence available" );
						            }
						%>
					</td>
				</tr>
			</table>
		</td>
	</tr>
	<tr>
		<td>
			<div>
				&nbsp;
			</div>
			<display:table name="blatResults" requestURI="" id="blatResult"
				style="width:100%;" pagesize="200"
				decorator="ubic.gemma.web.taglib.displaytag.expression.designElement.CompositeSequenceWrapper"
				class="scrollTable">
				<display:column property="blatResult" sortable="true"
					title="Alignment" />
				<display:column property="blatScore" sortable="true" title="S"
					defaultorder="descending" />
				<display:column property="blatIdentity" sortable="true" title="I"
					defaultorder="descending" />
				<display:column property="geneProducts" title="GeneProducts" />
				<display:column property="genes" title="Genes" />
				<display:setProperty name="basic.empty.showtable" value="true" />
			</display:table>
		</td>
	</tr>
</table>


</body>
</html>
