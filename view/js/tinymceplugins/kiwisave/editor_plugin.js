(function() {
	tinymce.create('tinymce.plugins.KiWiSavePlugin', {

		init : function(ed, url) {
		
			ed.addCommand('mceKiWiSave', function() {
				kiwiSave();
			});
			
			ed.addButton('kiwisave', {
				title : 'Save',
				cmd : 'mceKiWiSave',
				'class': 'mce_save'
			});
		}
	});

	/* Register plugin */
	tinymce.PluginManager.add('kiwisave', tinymce.plugins.KiWiSavePlugin);
})();
