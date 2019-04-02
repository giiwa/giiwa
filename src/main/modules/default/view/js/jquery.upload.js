(function($) {
	$.fn.upload = function(opts) {
		var e = $(this);
		if (e[0].tagName !== 'INPUT') {
			return;
		}
		var options = $.extend({
			tag : 'default',
			type : 'url',
			caption : 'Add file',
			msg_stop : 'Stop',
			msg_done : 'Done'
		}, opts);

		var that = $(e);

		var pp = $("<div class='fileupload'><div class='fileupload-btns'><span class='fileupload-file'></span><a href='javascript:;' class='fileupload-btn btn btn-success'>"
				+ options.caption
				+ "</a><input type='hidden' name='"
				+ that.attr('name')
				+ "'/></div><div class='fileupload-state'></div></div>");

		that.before(pp);
		that.attr('name', '');
		that.attr('type', 'file');

		pp.find('.fileupload-file').append(that);
		pp.find('.fileupload-btn').click(function() {
			that.trigger('click');
		});

		that
				.on(
						'change',
						function(e) {
							giiwa
									.upload(
											this.files,
											{
												url : '/upload',
												chunksize : 16 * 1024,
												onstart : function(e) {
													var s = '';
													s += "<div class='file-item'><span class='fileupload-filename'>"
															+ e.name
															+ "</span><span class='fileupload-progress'><span class='fileupload-bar-outter'><span class='fileupload-bar'></span></span><span class='fileupload-message'></span></span></div>";
													pp
															.find(
																	'.fileupload-state')
															.html(s);
												},
												ondone : function(e) {
													pp
															.find('.file-item')
															.addClass(
																	'upload-done');
													pp
															.find(
																	'.fileupload-message')
															.text(
																	options.msg_done);
													pp
															.find(
																	'.fileupload-message')
															.removeClass('red');

													pp
															.find(
																	'input[type=hidden]')
															.attr('value',
																	e.url);

													if (options.done
															&& typeof (options.done) == 'function') {
														options.done(e.url,
																e.repo, e.name);
													}
												},
												onprogress : function(e) {
													pp
															.find(
																	'.fileupload-message')
															.text(
																	e.pos
																			+ '('
																			+ giiwa
																					.size(e.pos)
																			+ '/'
																			+ giiwa
																					.size(e.size)
																			+ ')');

													pp
															.find(
																	'.fileupload-message')
															.removeClass('red');
													var progress = parseInt(
															e.pos / e.size
																	* 100, 10);

													pp
															.find(
																	'.fileupload-bar')
															.css(
																	'width',
																	progress
																			+ '%');
												},
												onerror : function(e) {
													pp
															.find(
																	'.fileupload-message')
															.text(
																	options.msg_stop);
												}
											})
						});

	}

})(jQuery);
