Ext.namespace('Gemma');

Gemma.NeurocartaStatistics = Ext.extend(Gemma.GemmaGridPanel, {
   title: 'Phenocarta Statistics',  
   tbar : [
            '->',
            '<a target="_blank" alt="Archived Files" href="http://www.chibi.ubc.ca/Gemma/phenocarta/ArchivedEvidenceExport"><b>Archives</b></a> ',
            { xtype: 'tbspacer', width: 40 }, 
          ],
   loadMask : true,
   record : Ext.data.Record.create([ {
      name : "name",
      type : "string"
   }, {
      name : "numEvidence",
      type : "int"
   }, {
      name : "numGenes",
      type : "int"
   }, {
      name : "numPhenotypes",
      type : "int"
   }, {
      name : "numPublications",
      type : "int"
   }, {
      name : "lastUpdated",
      type : "date"
   }, {
      name : "pathToDownloadFile",
      type : "string"
   } ]),
   initComponent : function() {

      Gemma.NeurocartaStatistics.superclass.initComponent.call(this);

      var store = new Ext.data.Store({
         autoLoad : true,

         proxy : new Ext.data.DWRProxy(
               PhenotypeController.calculateExternalDatabasesStatistics),

         reader : new Ext.data.JsonReader({
            fields : [ 'name', 'description', 'webUri', 'numEvidence',
                  'numGenes', 'numPhenotypes', 'numPublications', 'lastUpdated', 'pathToDownloadFile' ]
         })
      });

      function renderDatabase(val, metaData, record, row, col, store,
            gridView) {
         
         if( record.data.name.indexOf("Total") != -1){
            return  renderBold(val, metaData, record, row, col, store,gridView);
         }
         
         if( record.data.webUri==""){
            return val;
         }

         var imageSrc = '/Gemma/images/icons/externallink.png';
         
         return val+ ' <A HREF=\'' + record.data.webUri + '\' TARGET="_blank"><img src="' + imageSrc + '" /></A>';
      };
      
      
      function renderBold(val, metaData, record, row, col, store,
            gridView) {
         
         if( record.data.name.indexOf("Total") != -1){
            return "<b><i>"+val+"</i></b>";
         }
         else{
            return val;
         }
      };
      
      
      function renderDownload(val, metaData, record, row, col, store,
            gridView) {
         
         var imageSrc = '/Gemma/images/download.gif';
         var imageSrcErmineJ = '/Gemma/images/logo/ermineJ.png';
         
         if( record.data.name.indexOf("Total") != -1){
            // add ermineJ files
            return '<A HREF=\'' + val + '\' TARGET="_blank"><img src="' + imageSrc + '" ext:qtip="Download '+ record.data.name +' file" /></A> '+
            '<A href="http://www.chibi.ubc.ca/Gemma/phenocarta/LatestEvidenceExport/ErmineJ" TARGET="_blank"><img src="' + imageSrcErmineJ + '" ext:qtip="Download ErmineJ files" /></A> ';
         }
         
         if(record.data.name.indexOf("OMIM") != -1)
         {
            return '<A HREF="#"><img src="' + imageSrc + '" ext:qtip="Please see omim.org for licensing information and data access." /></A>';
         }     
         return '<A HREF=\'' + val + '\' TARGET="_blank"><img src="' + imageSrc + '" ext:qtip="Download '+ record.data.name +' file" /></A>';

      };

      Ext.apply(this, {
         store : store,
         colModel : new Ext.grid.ColumnModel({
            defaults : {
               sortable : true
            },
            columns : [ {

               header : "Data source",
               dataIndex : "name",
               renderer : renderDatabase,
               width : 0.55
            }, {

               header : "Description",
               dataIndex : "description",
               renderer : renderBold,
               width : 0.55
            }, {

               header : "Number of evidence",
               dataIndex : "numEvidence",
               renderer : renderBold,
               width : 0.55
            }, {

               header : "Number of genes",
               dataIndex : "numGenes",
               renderer : renderBold,
               width : 0.55
            }, {

               header : "Number of phenotypes",
               dataIndex : "numPhenotypes",
               renderer : renderBold,
               width : 0.55
            }, {

               header : "Number of publications",
               dataIndex : "numPublications",
               renderer : renderBold,
               width : 0.55
            }, {
               header : "Last updated",
               dataIndex : "lastUpdated",
               tooltip: "The date when we last imported a data source or checked it for updates. If a source itself has not been updated since we last imported it, we do not re-import it.",
               renderer : renderBold,
               width : 0.55,
               renderer : Ext.util.Format.dateRenderer('Y/M/d')
            }, {
               header : "Download",
               dataIndex : "pathToDownloadFile",
               renderer : renderDownload,
               width : 0.25,
            }
            ]
            
         }),
      });
   }
});
