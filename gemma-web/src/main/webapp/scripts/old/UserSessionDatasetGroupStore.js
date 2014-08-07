Ext.namespace( 'Gemma' );
/**
 * Holds the list of available db backed and session backed ExpressionExperiment groups from. This is a separate class
 * so it can be more easily shared between components. It also knows which record is selected, unlike the default store.
 * 
 * @class Gemma.UserSessionDatasetGroupStore
 * @extends Ext.data.Store
 * @see DatasetGroupCombo
 * @deprecated not used
 */
Gemma.UserSessionDatasetGroupStore = function( config ) {

   /*
    * Leave this here so copies of records can be constructed.
    */
   this.record = Ext.data.Record.create( [ {
      name : "id",
      type : "int"
   }, {
      name : "name",
      type : "string"
   }, {
      name : "description",
      type : "string"
   }, {
      name : "size",
      type : "int"
   }, {
      name : "memberIds"
   }, {
      name : "taxonId",
      type : "int"
   }, {
      name : "taxonName"
   }, {
      name : "modifiable",
      type : 'boolean'
   }, {
      name : "isPublic",
      type : 'boolean'
   }, {
      name : "isShared",
      type : 'boolean'
   }, {
      name : "userCanWrite",
      type : 'boolean'
   }, {
      name : "session",
      type : 'boolean'
   } ] );

   // todo replace with JsonReader.
   this.reader = new Ext.data.ListRangeReader( {}, this.record );

   Gemma.UserSessionDatasetGroupStore.superclass.constructor.call( this, config );

};

/**
 * 
 * @class Gemma.DatasetGroupStore
 * @extends Ext.data.Store
 */
Ext.extend( Gemma.UserSessionDatasetGroupStore, Ext.data.Store, {

   autoLoad : true,
   autoSave : false,
   selected : null,

   proxy : new Ext.data.DWRProxy( {
      apiActionToHandlerMap : {
         read : {
            dwrFunction : ExpressionExperimentSetController.loadAllUserAndSessionGroups
         },
         create : {
            dwrFunction : ExpressionExperimentSetController.addUserAndSessionGroups
         },
         update : {
            dwrFunction : ExpressionExperimentSetController.updateUserAndSessionGroups
         },
         destroy : {
            dwrFunction : ExpressionExperimentSetController.removeUserAndSessionGroups
         }
      }
   } ),

   writer : new Ext.data.JsonWriter( {
      writeAllFields : true
   } ),

   getSelected : function() {
      return this.selected;
   },

   setSelected : function( rec ) {
      this.previousSelection = this.getSelected();
      if ( rec ) {
         this.selected = rec;
      }
   },

   getPreviousSelection : function() {
      return this.previousSelection;
   },

   clearSelected : function() {
      this.selected = null;
      delete this.selected;
   },

   listeners : {
      exception : function( proxy, type, action, options, res, arg ) {
         if ( type === 'remote' ) {
            Ext.Msg.show( {
               title : 'Error',
               msg : res.message,
               icon : Ext.MessageBox.ERROR
            } );
         } else {
            Ext.Msg.show( {
               title : 'Error',
               msg : arg.message ? arg.message : arg,
               icon : Ext.MessageBox.ERROR
            } );
         }
      }

   }

} );
