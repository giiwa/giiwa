jQuery
		.extend({
			upload : function(options) {
				options = $.extend({
					url : "/upload",
					obj : false,
					done : false,
					tag : 'file'
				}, options);

				if (!options.obj) {
					throw "obj missed";
				}

				if (options.obj.files.length > 0) {
					var file = options.obj.files[0];
					var reader = new FileReader();
					if (options.url.indexOf('?') > 0) {
						options.url += '&file=' + file.name + "&tag="
								+ options.tag;
					} else {
						options.url += '?file=' + file.name + "&tag="
								+ options.tag;
					}
					reader.onloadend = function() {
						if (reader.error) {
							console.log(reader.error);
						} else {
							var xhr = new XMLHttpRequest();
							xhr.open("POST", options.url);
							xhr.overrideMimeType("application/octet-stream");
							xhr.setRequestHeader('lastModified',
									file.lastModifiedDate);
							xhr.setRequestHeader('Content-Range', 'bytes 0-'
									+ file.size + '/' + file.size);

							var fd = new FormData();
							fd.append("file", file);
							xhr.send(fd);

							xhr.onreadystatechange = function() {
								if (xhr.readyState == 4) {
									if (xhr.status == 200) {
										if (options.done) {
											var r = eval('(' + xhr.responseText
													+ ')');
											options.done(r);
										}
									}
								}
							}
						}
					}
					reader.readAsBinaryString(file);
				}
			}
		});
