Ext.namespace( 'Gemma' );

/**
 * Combobox to display available taxa. This can be set to show only taxa that have genes, or only taxa that have data
 * sets.
 * 
 * @class Gemma.TaxonCombo
 * @extends Ext.form.ComboBox
 *
 */

TaxonRecord = Ext.data.Record.create( [ {
   name : "id",
   type : "int"
}, {
   name : "commonName",
   type : "string",
   convert : function( v, rec ) {
      if ( rec.commonName ) {
         return rec.commonName;
      }
      return rec.scientificName;
   }
}, {
   name : "scientificName",
   type : "string"
}, {
   name : "parentTaxon"
} ] );

Gemma.TaxonCombo = Ext.extend( Gemma.StatefulRemoteCombo, {

   name : "taxcomb",
   displayField : 'commonName',
   valueField : 'id',
   editable : false,
   loadingText : Gemma.StatusText.Loading.generic,
   triggerAction : 'all', // so selecting doesn't hide the others
   listWidth : 250,
   width : 120,
   stateId : "Gemma.TaxonCombo",
   /*
    * this controls whether or not the entry 'All taxa' is added at the top of the list; this option must be handled in
    * the function that responds to the combo box's selection event for example, adding "if(record.get('commonName') ===
    * "All taxa"){...}"
    */
   allTaxa : false,
   emptyText : 'Select a taxon',

   isDisplayTaxonSpecies : false,
   isDisplayTaxonWithGenes : false,
   isDisplayTaxonWithDatasets : false,
   isDisplayTaxonWithEvidence : false,

   record : TaxonRecord,

   /**
    * @memberOf Gemma.TaxonCombo
    */
   filter : function( taxon ) {
      this.store.clearFilter();
      this.store.filterBy( function( record, id ) {
         if ( taxon.id === record.get( "id" ) ) {
            return true;
         } else {
            return false;
         }
      } );
      this.setTaxon( taxon );
      this.onLoad();
   },

   initComponent : function() {

      var tmpl = new Ext.XTemplate(
         '<tpl for="."><div class="x-combo-list-item">{commonName} ({scientificName})</div></tpl>' );

      /*
       * Option to either display all taxa, those taxa that are a species or those taxa that have genes; or those which
       * have datasets.
       */
      if ( this.isDisplayTaxonSpecies ) {
         proxyTaxon = new Ext.data.DWRProxy( GenePickerController.getTaxaSpecies );
      } else if ( this.isDisplayTaxonWithDatasets ) {
         proxyTaxon = new Ext.data.DWRProxy( GenePickerController.getTaxaWithDatasets );
      } else if ( this.isDisplayTaxonWithGenes ) {
         proxyTaxon = new Ext.data.DWRProxy( GenePickerController.getTaxaWithGenes );
      } else if ( this.isDisplayTaxaWithArrays ) {
         proxyTaxon = new Ext.data.DWRProxy( GenePickerController.getTaxaWithArrays );
      } else if ( this.isDisplayTaxonWithEvidence ) {
         proxyTaxon = new Ext.data.DWRProxy( GenePickerController.getTaxaWithEvidence );
      } else {
         proxyTaxon = new Ext.data.DWRProxy( GenePickerController.getTaxa );
      }

      Ext.apply( this, {
         store : new Ext.data.Store( {
            proxy : proxyTaxon,
            reader : new Ext.data.ListRangeReader( {
               id : "id"
            }, this.record ),
            remoteSort : false,
            sortInfo : {
               field : 'commonName'
            }
         } ),
         tpl : tmpl
      } );

      Gemma.EVENTBUS.on( 'taxonchanged', function( taxonId ) {
         this.setTaxonById( taxonId );
      }, this );

      Gemma.TaxonCombo.superclass.initComponent.call( this );

      if ( this.allTaxa ) {
         this.store.load( {
            params : [],
            add : false,
            callback : function( allTaxa ) {
               // add an option so user can see data belonging to all taxa
               var allTaxaRecord = new TaxonRecord( {
                  'id' : '-1',
                  'commonName' : 'All taxa',
                  'scientificName' : 'All Taxa',
                  'parentTaxon' : '-1'
               } );
               this.insert( 0, [ allTaxaRecord ] );
               // using getStore().on('load', ...) will execute code before this call back has been run
               this.fireEvent( 'doneLoading' );
            }
         } );
      } else {
         this.store.load( {
            params : [],
            add : false,
            callback : function() {
               // for symmetry with the case when an 'all taxa' option is added
               this.fireEvent( 'doneLoading' );
            }
         } );
      }

   },

   getTaxon : function() {
      return (this.store.getById( this.getValue() ) || this.store.getAt( this.store.find( 'commonName',
         this.getValue(), 0, false ) ));

   },

   /**
    * To allow setting programmatically.. Do not fire the taxonchanged event, but do fire the select event.
    * 
    * @param {}
    *           taxon
    */
   setTaxon : function( taxon ) {
      if ( taxon.id ) {
         this.setValue( taxon.id );
      } else {
         this.setValue( taxon );
      }
      var r = this.store.findExact( this.getValue() );
      this.fireEvent( "select", this, r, this.store.indexOf( r ) );
   },

   /**
    * To allow setting programmatically. Do not fire the taxonchanged event, but do fire the select event.
    * 
    * @private
    * @param {}
    *           taxon
    */
   setTaxonById : function( taxonId ) {
      if ( taxonId ) {
         this.setValue( taxonId );
      }
      var r = this.store.findExact( this.getValue() );
      this.fireEvent( "select", this, r, this.store.indexOf( r ) );
   },

   /**
    * returns complete taxon object that matches the common name given if successful. Else return -1.. Do not fire the
    * taxonchanged event, but do fire the select event.
    * 
    * @param {commonName}
    *           the common name of the taxon
    * 
    */
   setTaxonByCommonName : function( commonName ) {
      var records = this.store.getRange();

      if ( !records || records.length < 1 ) {
         return -1;
      }
      var i = 0;
      for (i = 0; i < records.length; i++) {
         if ( records[i].data.commonName === commonName ) {
            this.setTaxon( records[i].data.id );
            return records[i].data;
         }
      }

      return -1;

   }
} );

Ext.reg( 'taxonCombo', Gemma.TaxonCombo );