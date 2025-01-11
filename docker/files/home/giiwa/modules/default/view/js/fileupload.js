$(function() {
			initfileupload();
		})

var uploader = uploader | false;
var _autoretry = true;

function _startfileupload() {
	_autoretry = true;
	uploader && uploader.submit();
}

function _stopfileupload() {
	_autoretry = false;
	uploader && uploader.abort();
}

function initfileupload() {
	if ($('#fileupload').length > 0) {

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

		uploadButton = $('#uploadButton');
		$('#fileupload')
				.fileupload({
							url : "/f/upload?tag=default",
							dataType : 'json',
							autoUpload : false,
							maxFileSize : 1024 * 1024 * 1024 * 100, // 100G
							limitConcurrentUploads : 1,
							sequentialUploads : true,
							progressInterval : 100,
							maxChunkSize : 32*1024
						})
				.on('fileuploadadd', function(e, data) {
					try {
						uploader = data;
						
						if (typeof(beforeupload) == 'function') {
							if (beforeupload(data.files[0].name,
									data.files[0].size)) {
								$('.fileupload .notes').hide();
								$('.fileupload .state').show();
								$('.fileupload .state .name')
										.text(data.files[0].name);
								_autoretry = true;

								$('.fileupload a.stop').text('pause');
								$('.fileupload a.stop').removeClass('disabled');

								uploader.submit();
							}
						} else {
							$('.fileupload .notes').hide();
							$('.fileupload .state').show();
							$('.fileupload .state .name')
									.text(data.files[0].name);
							_autoretry = true;

							$('.fileupload a.stop').text('pause');
							$('.fileupload a.stop').removeClass('disabled');

							uploader.submit();
						}
					} catch (err) {

					}

				}).on('fileuploadprogressall', function(e, data) {
					$('#error').text(data.loaded + '(' + size(data.loaded)
							+ ')/' + size(data.total));
					$('#error').removeClass('red');
					var progress = parseInt(data.loaded / data.total * 100, 10);
					$('#progress .bar').css('width', progress + '%');
				}).on('fileuploadchunkfail', function(e, data) {
							if (data.result) {
								$('#error').text(data.result.error);
							} else {
								$('#error').text('stop');
							}
							$('#error').addClass('red');

							if (_autoretry) {
								setTimeout(_startfileupload, 1000);
							}
						}).on('fileuploadfail', function(e, data) {
							$('#error').text('stop');
							$('#error').addClass('red');

							if (_autoretry) {
								setTimeout(_startfileupload, 1000);
							}
						}).on('fileuploaddone', function(e, data) {

							$('#error').text('done');
							$('#error').removeClass('red');
							$('.fileupload a.stop').text('done');
							$('.fileupload a.stop').addClass('disabled');

							uploaddone && uploaddone(data.result.url);
							uploader = false;

						}).prop('disabled', !$.support.fileInput).parent()
				.addClass($.support.fileInput ? undefined : 'disabled');
	}
}
