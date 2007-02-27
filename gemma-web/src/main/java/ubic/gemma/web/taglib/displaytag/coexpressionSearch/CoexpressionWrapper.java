package ubic.gemma.web.taglib.displaytag.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 *
 * @author jsantos
 * @version $Id $
 *  
 */
public class CoexpressionWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );
    
    /**
     * @return String
     */
    public String getNameLink() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        StringBuffer link = new StringBuffer();
       
        // tmm-style coexpression search link

        // gene link to gemma
        link.append( "<a href=\"/Gemma/gene/showGene.html?id=" );
        link.append( object.getGeneId() );
        link.append( "\">" );
        link.append( StringUtils.abbreviate( object.getGeneName(), 20) );
        link.append( "</a>" );

        // build the GET parameters
        link.append( getCoexpressionLink(object, "<img src=\"/Gemma/images/logo/gemmaTiny.gif\" />")  );

        return link.toString();
    }
    
    /**
     * Function to return the number of data sets for a coexpression match
     * @return the data set count column
     */
    public String getDataSetCount() {
        String count;
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        count = (new Integer(object.getExpressionExperimentValueObjects().size())).toString();
        return count; 
    }
    
    /**
     * Function to return the number of data sets for a coexpression match
     * @return the data set count column
     */
    public String getLinkCount() {
        String count = "";
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        Integer positiveLinks = object.getPositiveLinkCount();
        Integer negativeLinks = object.getNegativeLinkCount();
        
        if (positiveLinks != null && positiveLinks != 0) {
            count += "<span class='positiveLink' >";
            count += positiveLinks.toString();
            
           if ( !object.getContributingExpressionExperiments().isEmpty())
            count += getNonSpecificString( object.getNonspecificEE(), object.getEEContributing2PositiveLinks() );
           
            count += "</span>";
        }
        
        if (negativeLinks != null && negativeLinks != 0) {
            if (count.length() > 0) {
                count += "/";
            }
            count += "<span class='negativeLink' >";
            count += negativeLinks.toString();
            if ( !object.getContributingExpressionExperiments().isEmpty())
                count += getNonSpecificString( object.getNonspecificEE(), object.getEEContributing2NegativeLinks() );
     
            
            count += "</span>";
        }
        return count; 
    }
    
    private String getNonSpecificString(Collection<Long> allNonSpecific, Collection<Long> contributingEE ){
        int nonSpecific = 0;
        String nonSpecificList = "";
        
        for (Long id: contributingEE){
            if (allNonSpecific.contains( id )){
                nonSpecific++;
                nonSpecificList += id + " ";
            }
        }
        
        if (nonSpecific == 0)
            return "";
        
        return "<i> <span style = 'font-size:smaller' title='" +  nonSpecificList + "' >  ( " + nonSpecific + " )  </span> </i>";
    }
    
    /**
     * Function to return the data sets for a coexpression match
     * @return the data set column
     */
    @SuppressWarnings("unchecked")
    public String getDataSets() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        Collection<ExpressionExperimentValueObject> ees = object.getExpressionExperimentValueObjects();
        Collection<String> dsLinks = new ArrayList<String>();
        for ( ExpressionExperimentValueObject ee : ees ) {
            String link = "<a " +
                    "title=\"" +
                    ee.getName() +
                    "\" " +
                    "href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id=" +
                    ee.getId() + 
                    "\">" + 
                    ee.getShortName() + 
                    "</a>";
            dsLinks.add(link);
        }
        return StringUtils.join( dsLinks.toArray(), "," ); 
    }
    
    /**
     * Function to build a GET coexpression link, given a coexpressionValueObect 
     * @param object the CoexpressionValueObject to build the link for
     * @param innerHTML The html to show as a link. This is typically an image link or a gene name.
     * @return
     */
    public String getCoexpressionLink(CoexpressionValueObject object, String innerHTML) {
        StringBuffer link = new StringBuffer();
        Collection<String> paramList = new ArrayList<String>();
        Collection<String> includeList = new ArrayList<String>();
        // include just taxon params
        includeList.add( "taxon" );
        includeList.add( "stringency" );
        extractParameters( paramList, includeList );
        // add in the current gene with exactSearch
        paramList.add( "searchString=" + object.getGeneName() );
        paramList.add( "exactSearch=on" );
        
        // put in the tmm link
        link.append( "&nbsp;" );
        link.append( "<a href=\"/Gemma/searchCoexpression.html?" );
        link.append( StringUtils.join( paramList.toArray(), "&" ) );
        link.append( "\">" + innerHTML + "</a>" );
        
        return link.toString();
    }
    
    /**
     * Function to build a GET coexpression link
     * @return
     */
    public String getCoexpressionLink() {
        CoexpressionValueObject object = ( CoexpressionValueObject ) getCurrentRowObject();
        return getCoexpressionLink(object, "<img src=\"/Gemma/images/logo/gemmaTiny.gif\" />");
    }

    /**
     * Function for extracting the GET parameter list, excluding the parameters in excludeList
     * @param paramList
     * @param excludeList
     */
    private void extractParameters( Collection<String> paramList, Collection<String> excludeList ) {
        Set params = this.getPageContext().getRequest().getParameterMap().entrySet();
        for ( Object param : params ) {
            Map.Entry entry =  (Map.Entry) param;
            if (!isInIncludeList( excludeList, entry )) {
                continue;
            }
            String[] values = (String[]) entry.getValue();
            for ( String string : values ) {
                paramList.add( entry.getKey() + "=" + string ); 
                // just use one parameter value
                break;
            }
        }
    }

    /**
     * Helper function. Checks to see if the entry is in the given exclude list
     * @param excludeList
     * @param entry
     * @return
     */
    private boolean isInExcludeList( Collection<String> excludeList, Map.Entry entry ) {
        for ( String exclude : excludeList ) {
            if ( ((String)entry.getKey()).equals( exclude ) ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Helper function. Checks to see if the entry is in the given include list
     * @param excludeList
     * @param entry
     * @return
     */
    private boolean isInIncludeList( Collection<String> includeList, Map.Entry entry ) {
        for ( String include : includeList ) {
            if ( ((String)entry.getKey()).equals( include ) ) {
                return true;
            }
        }
        return false;
    }
}
