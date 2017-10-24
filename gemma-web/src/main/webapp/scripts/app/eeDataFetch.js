function handleFailure( data, e ) {

   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : ctxBasePath + '/images/icons/warning.png'
   } );
   Ext.DomHelper.append( "messages", {
      tag : 'span',
      html : "&nbsp;There was an error:<br/>" + data + " " + (e ? e : "")
   } );

   Ext.MessageBox.alert( "Error", data + " " );

}

function handleDoneGeneratingFile( url ) {
   window.location = url;
}

function fetchData( filter, eeId, formatType, qtId, eeDId ) {

   // Get the parameters from the form.
   var commandObj = {
      quantitationTypeId : qtId,
      filter : filter,
      expressionExperimentId : eeId,
      format : formatType,
      experimentalDesignId : eeDId
   };

   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : ctxBasePath + '/images/default/tree/loading.gif'
   } );
   Ext.DomHelper.append( "messages", "&nbsp;Fetching ..." );

   ExpressionExperimentDataFetchController.getDataFile( commandObj, {
      callback : function( taskId ) {
         var task = new Gemma.ObservableSubmittedTask( {
            'taskId' : taskId
         } );

         task.on( 'task-completed', function( url ) {
            handleDoneGeneratingFile( url );
         } );

         task.showTaskProgressWindow( {
            showLogButton : true
         } );

      },
      errorHandler : handleFailure
   } );
}

function fetchCoExpressionData( eeId ) {

   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : ctxBasePath + '/images/default/tree/loading.gif'
   } );
   Ext.DomHelper.append( "messages", "&nbsp;Fetching ..." );

   ExpressionExperimentDataFetchController.getCoExpressionDataFile( eeId, {
      callback : function( taskId ) {
         var task = new Gemma.ObservableSubmittedTask( {
            'taskId' : taskId
         } );

         task.on( 'task-completed', function( url ) {
            handleDoneGeneratingFile( url );
         } );

         task.showTaskProgressWindow( {
            showLogButton : true
         } );

      },
      errorHandler : handleFailure
   } );
}

function fetchDiffExpressionData( analysisId ) {

   Ext.DomHelper.overwrite( "messages", {
      tag : 'img',
      src : ctxBasePath + '/images/default/tree/loading.gif'
   } );
   Ext.DomHelper.append( "messages", "&nbsp;Fetching ..." );

   ExpressionExperimentDataFetchController.getDiffExpressionDataFile( analysisId, {
      callback : function( taskId ) {
         var task = new Gemma.ObservableSubmittedTask( {
            'taskId' : taskId
         } );

         task.on( 'task-completed', function( url ) {
            handleDoneGeneratingFile( url );
         } );

         task.showTaskProgressWindow( {
            showLogButton : true
         } );

      },
      errorHandler : handleFailure
   } );

}
