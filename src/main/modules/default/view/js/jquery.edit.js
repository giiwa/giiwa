/**
 * 
 */
(function($) {

	$.fn.extend({
		edit : function(settings) {
			settings = $.extend({
						post : undefined,
						lookup : undefined
					}, settings);

			var that = this;
			that.each(function(i, e) {
						e = $(e);
						var t = $('<div class="data"></div>');
						t.append(e.html());
						e.html(t);
					});
			that.click(function(e) {
				that.removeClass('focused');
				var t = $(this);
				t.addClass('focused');
				var e1 = t.find('div.editor');
				var e2 = t.find('div.data');
				if (e1.length == 0) {
					var editor = t.attr('data-editor');
					if (editor == 'checkbox') {
						e1 = $('<div class="editor"><input type="checkbox"/></div>');
						e1.find('input').prop('checked', e2.text() == 'on');
					} else if (editor == 'number') {
						e1 = $('<div class="editor"><input type="number" value="'
								+ e2.text() + '"/></div>');
					} else if (editor == 'select') {
						if (settings.lookup
								&& typeof(settings.lookup) == 'function') {
							e1 = $('<div class="editor"><select></select></div>');
							settings.lookup({
										name : t.attr('data-name')
									}, function(list) {
										var l1 = e1.find('select');
										$(list).each(function(i, e) {
											if (e.value == e2.text()) {
												l1.append('<option value="'
																+ e.value
																+ '" selected >'
																+ e.label
																+ '</option>');
											} else {
												l1.append('<option value="'
																+ e.value
																+ '">'
																+ e.label
																+ '</option>');
											}
										});
									});
						} else {
							e1 = $('<div class="editor"><input type="text" value="'
									+ e2.text() + '"/></div>');
						}
					} else {
						e1 = $('<div class="editor"><input type="text" value="'
								+ e2.text() + '"/></div>');
					}
					t.append(e1);
				}
				e1.show();
				e2.hide();
				var input = e1.find('input');
				if (input.length > 0) {
					input.focus();
					input.bind('blur', function() {
								e1.hide();
								e2.show();

								var editor = t.attr('data-editor');
								if (editor == 'checkbox') {
									var t1 = $(this).prop('checked')
											? 'on'
											: 'off';
								} else {
									var t1 = $(this).val();
								}
								var t2 = e2.text();
								if (t1 != t2) {
									e2.text(t1);
									if (settings.post
											&& typeof(settings.post) == 'function') {
										var data = {};
										data.id = t.attr('data-id');
										data.name = t.attr('data-name');
										data.editor = editor;
										data.value = t1;
										settings.post(data);
									}
								}
							});
				} else {
					// select
					var input = e1.find('select');
					if (input.length > 0) {
						input.focus();
						input.bind('blur', function() {
							e1.hide();
							e2.show();

							var editor = t.attr('data-editor');
							var t1 = $(this).val();
							var t2 = e2.text();
							if (t1 != t2) {
								e2.text(t1);
								if (settings.post
										&& typeof(settings.post) == 'function') {
									var data = {};
									data.id = t.attr('data-id');
									data.name = t.attr('data-name');
									data.editor = editor;
									data.value = t1;
									settings.post(data);
								}
							}
						});
					}
				}
			});
		}
	})
})(jQuery);
