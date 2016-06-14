(function($) {
	$.fn.upload = function(opts) {
		var e = $(this);
		if (e[0].tagName !== 'INPUT') {
			return;
		}
		var options = $.extend({
					tag : 'default',
					type : 'url',
					caption : '添加文件',
					hint : '选择文件',
					autoretry : true,
					maxFileSize : 1024 * 1024 * 1024 * 100// 100G
				}, opts);

		var that = $(e);

		function _startfileupload() {
			// console.log(options.uploader);

			options.uploader && options.uploader.submit();
		}

		options.uploader = false;
		var pp = $("<div class='fileupload'><div class='fileupload-btns btns hint hint--bottom' data-hint='"
				+ options.hint
				+ "'><span class='fileupload-file'></span><a href='javascript:;' class='fileupload-btn'>"
				+ options.caption
				+ "</a><input type='hidden' name='"
				+ that.attr('name')
				+ "'/></div><div class='fileupload-state'></div></div>");

		that.before(pp);
		that.attr('name', 'file');
		that.attr('type', 'file');

		pp.find('.fileupload-file').append(that);
		pp.find('.fileupload-btn').click(function() {

					that.trigger('click');
					options.autoretry = true;
				});

		that
				.fileupload({
							url : "/upload?tag=" + options.tag,
							dataType : 'json',
							autoUpload : false,
							maxFileSize : options.maxFileSize,
							limitConcurrentUploads : 1,
							sequentialUploads : true,
							progressInterval : 100,
							maxChunkSize : 16384
						})
				.on('fileuploadadd', function(e, data) {
					try {
						if (typeof(options.beforeupload) == 'function') {
							if (!options.beforeupload(data.files[0].name,
									data.files[0].size)) {
								return;
							}
						}

						options.uploader && options.uploader.abort();

						$(options.note).hide();

						var s = '';
						s += "<div class='file-item'><span class='fileupload-filename'>"
								+ data.files[0].name
								+ "</span><span class='fileupload-progress'><span class='fileupload-bar-outter'><span class='fileupload-bar'></span></span><span class='fileupload-message'></span></span><a href='javascript:;' class='fileupload-cancel'>终止</a><a href='javascript:;' class='fileupload-resume'>上传</a></div>";
						pp.find('.fileupload-state').html(s);
						pp.find('.fileupload-cancel').click(function() {
									options.autoretry = false;
									options.uploader
											&& options.uploader.abort();

									var action = $(this).text();
									// console.log(action);
									if (action == '删除') {
										var p = $(this).parent();
										while (!p.hasClass('file-item')) {
											p = p.parent();
										}

										if (options.repo) {
											$.post('/repo/delete', {
														repo : options.repo
													});
										}
										/**
										 * add input back to btn
										 */
										p.remove();
									} else if (action == '终止') {
										$(this).text('删除');
										pp.find('.fileupload-resume').show();
									}
								});
						pp.find('.fileupload-resume').click(function() {
									options.autoretry = true;
									options.uploader
											&& options.uploader.submit();
									pp.find('.fileupload-resume').hide();
									pp.find('.fileupload-cancel').text('终止');
								});

						options.autoretry = true;
						options.uploader = data;
						options.uploader.submit();

					} catch (err) {
						console.log(err);
					}
				})
				.on('fileuploadprogressall', function(e, data) {

					var filename = (options.uploader._response && options.uploader._response.result)
							? options.uploader._response.result.name
							: false;

					pp.find('.fileupload-message').text(data.loaded + '('
							+ size(data.loaded) + '/' + size(data.total) + ')');

					pp.find('.fileupload-message').removeClass('red');
					var progress = parseInt(data.loaded / data.total * 100, 10);

					pp.find('.fileupload-bar').css('width', progress + '%');
				})
				.on('fileuploadchunkfail', function(e, data) {
					try {

						var filename = (options.uploader._response && options.uploader._response.result)
								? options.uploader._response.result.name
								: false;

						if (data.result && data.result.message) {
							pp.find('.fileupload-message')
									.text(data.result.message);
						} else {
							pp.find('.fileupload-message').text('停止');
						}

						if (data.result && data.result.error > 0) {
							options.uploader && options.uploader.abort();
						} else if (options.autoretry) {
							setTimeout(_startfileupload, 1000);
						}
					} catch (e) {
						console.log(e);
					}
				})
				.on('fileuploadfail', function(e, data) {
					try {

						var filename = (options.uploader._response && options.uploader._response.result)
								? options.uploader._response.result.name
								: false;

						if (data.result && data.result.message) {
							pp.find('.fileupload-message')
									.text(data.result.message);
						}

						if (data.result && data.result.error > 0) {
							options.uploader && options.uploader.abort();
						} else if (options.autoretry) {
							setTimeout(_startfileupload, 1000);
						}
					} catch (e) {
						console.log(e);
					}
				})
				.on('fileuploaddone', function(e, data) {

					var filename = (options.uploader._response && options.uploader._response.result)
							? options.uploader._response.result.name
							: false;

					pp.find('.file-item').addClass('upload-done');
					pp.find('.fileupload-message').text('完成');
					pp.find('.fileupload-message').removeClass('red');
					pp.find('.fileupload-cancel').text('删除');

					pp.find('input[type=hidden]')
							.attr('value', data.result.url);

					if (options.done && typeof(options.done) == 'function') {
						if (data.result) {
							options.done(data.result.url, data.result.repo);
						} else {
							options.done();
						}
					}

				}).prop('disabled', !$.support.fileInput).parent()
				.addClass($.support.fileInput ? undefined : 'disabled');

		function size(len) {
			if (len < 1024) {
				return len;
			}
			len /= 1024;
			if (len < 1024) {
				return parseInt(len * 10) / 10 + "K";
			}
			len /= 1024;
			if (len < 1024) {
				return parseInt(len * 10) / 10 + "M";
			}
			len /= 1024;
			return parseInt(len * 10) / 10 + "G";
		}

	}

})(jQuery);
