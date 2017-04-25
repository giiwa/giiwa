/**
 * 
 */

window.giiwa = {};

giiwa.extend = function(m) {
	for (var k in m) {
		giiwa[k] = m[k];
	}
};

giiwa.extend({
			__history : [],
			menuapi : false,
			panelapi : false,
			uploaddone : false,

			history : function(url) {
				if (url && url.length > 0) {
					if (giiwa.__history[giiwa.__history.length - 1] !== url) {
						giiwa.__history.push(url);
					}
				}
			},
			back : function() {
				if (giiwa.__history.length > 2) {

					var h = giiwa.__history.pop();
					h = giiwa.__history.pop();
					h = giiwa.__history.pop();

					giiwa.load(h);
				}
			},
			show : function(html) {

				giiwa.uploaddone = false;
				try {
					$('#panel .content').html(html);
				} catch (e) {
					console.error(e);
				}

				giiwa.hook();

				// resize();

			},

			hook : function() {
				/**
				 * hook all the <a> tag
				 */
				$('#panel .content a').each(function(i, e) {
					e = $(e);
					var href = e.attr('href');
					var target = e.attr('target');
					if (target == undefined && href != undefined
							&& (href.indexOf('javascript') == -1)
							&& (href.indexOf('#') != 0)) {

						e.click(function(e1) {
									var href = $(this).attr('href');
									if (href != undefined) {
										giiwa.load(href);
									}

									// console.log(href);
									e1.preventDefault();
								});
					}
				});

				/**
				 * hook all <form> to smooth submit
				 */
				$('#panel form').submit(function(e) {
					e.preventDefault();

					var form = e.target;

					var beforesubmit = $(form).attr('beforesubmit');
					if (typeof window[beforesubmit] === 'function') {
						if (!window[beforesubmit](form)) {
							return;
						}
					}

					/**
					 * check the bad flag
					 */
					var bad = $(form)
							.find("input[bad=1], textarea[bad=1], select[bad=1]");
					if (bad.length > 0) {
						bad[0].focus();
						return;
					}
					var bb = $(form)
							.find("input[required=true], select[required=true]");
					for (i = 0; i < bb.length; i++) {
						var e = $(bb[i]);
						if (e.val() == '') {
							e.focus();
							return;
						}
					}

					var url = form.action;

					if (form != undefined && url != undefined) {

						processing && processing.show();

						if (form.method == 'get') {
							var data = $(form).serialize();

							var __url = '';
							if (url.indexOf('?') > 0) {
								__url = url + '&' + data;
							} else {
								__url = url + '?' + data;
							}
							if (giiwa.__history.length > 0
									&& giiwa.__history[giiwa.__history.length - 1] == __url) {
								giiwa.__history.pop();
							}
							giiwa.__history.push(__url);

							if (__url.indexOf('?') > 0) {
								__url += '&' + new Date().getTime();
							} else {
								__url += '?' + new Date().getTime();
							}

							$.get(__url, {}, function(d) {
										giiwa.show(d);
										processing && processing.hide();
									});

						} else {
							var data = new FormData(form);

							var xhr = new XMLHttpRequest();
							xhr.open("POST", url);
							xhr.overrideMimeType("multipart/form-data");
							xhr.send(data);

							xhr.onreadystatechange = function() {
								if (xhr.readyState == 4) {
									if (xhr.status == 200) {
										giiwa.show(xhr.responseText);
										processing && processing.hide();
									}
								}
							}

						}
					}

				});

				$('#panel table th.checkbox').click(function(e) {
							var ch = $(this).find('input[type=checkbox]');
							if (ch.length > 0) {
								var en = ch[0].checked;
								var t = $(this);
								while (t.length > 0
										&& t[0].nodeName !== 'TABLE') {
									t = t.parent();
								}
								t.find('td input[type=checkbox]').each(
										function(i, e) {
											if (!e.disabled) {
												e.checked = en;
											}
										});
							}
						});

				var options = options || {};

				/**
				 * hook all <select> associated group
				 */
				$('#panel select[parentnode=true]').change(function(e) {
					var ch = $(this);
					if (ch.length > 0) {
						var value = ch.val();
						var subnode = ch.attr('subnode');
						var n1 = $('select[name=' + subnode + ']');
						/**
						 * initialize the options
						 */
						if (options[ch.attr('name')]) {
							n1.html(options[ch.attr('name')]);
						} else {
							options[ch.attr('name')] = n1.find('option');
						}

						var valid = false;
						var best = undefined;
						n1.find('option').each(function(i, e) {
							e = $(e);
							if (e.attr('parent') == value
									|| e.attr('parent') == undefined) {
								e.show();
								if (best === undefined) {
									best = e.val();
								}
								if (!valid && e.val() == n1.val()) {
									valid = true;
								}
							} else {
								// e.hide();
								e.remove();
							}
						});

						if (!valid) {
							n1.val(best);
							n1.trigger('change');
						}
					}
				});

				/**
				 * hook all <select> to make
				 */
				$('#panel select').each(function(i, e) {
							if (e.value == '') {
								$(e).removeClass('setted');
							} else {
								$(e).addClass('setted');
							}
						}).change(function(e) {
							if (this.value == '') {
								$(this).removeClass('setted');
							} else {
								$(this).addClass('setted');
							}
						});

				/**
				 * hook tr.hover
				 */
				$('#panel table.tablesorter tr').bind('mouseenter', function() {
							$(this).addClass('hover');
						}).bind('mouseleave', function() {
							$(this).removeClass('hover');
						});

				/**
				 * hook td.hover
				 */
				$('#panel table.tablesorter td').bind('mouseenter', function() {
							$(this).addClass('hover');
						}).bind('mouseleave', function() {
							$(this).removeClass('hover');
						});

				/**
				 * setting all searchbar
				 */
				$('#panel div.search').searchbar();

				$('#panel input').bind('focus', function() {
							$(this).parent().addClass('focus');
						}).bind('blur', function() {
					var that = $(this);
					that.parent().removeClass('focus');
					if (that.attr('verify')) {
						// verify
					}
					if (that.attr('max')) {
						// check max
						var value = that.val();
						console.log(value + ", " + value.length + ", "
								+ that.attr('max'));
					}
				});

			},

			reload : function() {
				if (giiwa.__history.length > 1) {
					giiwa.load(giiwa.__history.pop());
					return true;
				}

				return false;
			},

			load : function(uri) {
				processing && processing.show();

				if (giiwa.__history.length > 0
						&& giiwa.__history[giiwa.__history.length - 1] == uri) {
					giiwa.__history.pop();
				}
				giiwa.__history.push(uri);

				// $('#page').attr('src', uri);
				if (uri.indexOf('?') > 0) {
					uri += '&' + new Date().getTime();
				} else {
					uri += '?' + new Date().getTime();
				}
				$.ajax({
							url : uri,
							type : 'GET',
							data : {},
							error : function(d) {
								processing && processing.hide();
								window.location.href = "/";
							},
							success : function(d, status, xhr) {
								processing && processing.hide();
								var resp = {
									"status" : xhr.getResponseHeader('status')
								};
								if (resp.status == '401') {
									window.location.href = "/";
								} else {
									giiwa.show(d);
								}
							}
						})
			},

			load1 : function(uri) {
				processing && processing.show();

				// $('#page').attr('src', uri);
				if (uri.indexOf('?') > 0) {
					uri += '&' + new Date().getTime();
				} else {
					uri += '?' + new Date().getTime();
				}
				var s = '<iframe src="' + uri + '"></iframe>';
				processing && processing.hide();
				giiwa.show(s);
			},
			_post : function(o, table, max) {
				var s = '';
				var name = '';
				var selected = $(table + ' td input:checked');
				if (selected.length == 0) {
					return;
				}

				selected.each(function(i, e) {
							if (s.length > 0)
								s += ',';
							s += e.value;
							name = e.name;
						});

				var p = {};
				p[name] = s;

				if (o.indexOf('?') > 0) {
					o += '&' + new Date().getTime();
				} else {
					o += '?' + new Date().getTime();
				}
				$.get(o, p, function(d) {
							giiwa.show(d);
						});
			}

			,
			resize : function() {
				var menu = $('#menu');
				if (menu.length > 0) {
					var h = $(window).height();
					menu.css('height', (h - 120) + 'px');

					if (!giiwa.menuapi) {
						giiwa.menuapi = menu.jScrollPane().data('jsp');
					} else {
						giiwa.menuapi.reinitialise();
					}
				}
			},
			verify : function(obj, url) {
				var that = $(obj);
				$.post(url, {
							name : that.attr('name'),
							value : that.val()
						}, function(d) {
							if (d.state == 200) {
								/**
								 * ok, good
								 */
								that.attr('bad', "0");
								if (d.value) {
									that.val(d.value);
								}
								that.removeClass('bad').addClass('good');
								that.parent().parent().find('.note').hide();
							} else if (d.state == 400) {
								/**
								 * need confirm
								 */
								if (confirm(d.message)) {
									that.attr('bad', "0");
									if (d.value) {
										that.val(d.value);
									}
									that.removeClass('bad').addClass('good');
									that.parent().parent().find('.note').hide();
								} else {
									that.attr('bad', "1");
									that.removeClass('good').addClass('bad');
									var note = that.parent().parent()
											.find('.note');
									if (note.length == 0) {
										note = $('<div class="note"></div>');
										that.parent().parent().append(note);
									}
									note.html(d.error).show();
								}
							} else {
								that.attr('bad', "1");
								that.removeClass('good').addClass('bad');
								var note = that.parent().parent().find('.note');
								if (note.length == 0) {
									note = $('<div class="note"></div>');
									that.parent().parent().append(note);
								}
								note.html(d.message).show();
							}
						});
			}
		});

$(function() {
			$(window).resize(function() {
						giiwa.resize();
					});

			var e = $('#ss_tab');
			e.bind('mouseenter', function() {
						var e1 = $(this);
						if (e1.hasClass('info_but_close')) {
							e1.addClass('info_but_close_on');
						} else {
							e1.addClass('info_but_open_on');
						}
					});

			e.bind('mouseleave', function() {
						var e1 = $(this);
						e1.removeClass('info_but_close_on');
						e1.removeClass('info_but_open_on');
					});

			e.click(function() {
						var bar = $(this);
						var menu = $('#menu');
						var panel = $('#content');
						var toolbar = $('#toolbar');

						if (bar.hasClass('info_but_close')) {
							bar.addClass('info_but_open');
							bar.removeClass('info_but_close');
							bar.removeClass('info_but_close_on');

							panel.addClass('full');
							toolbar.addClass('full');
							menu.hide();

							giiwa.resize();
						} else {
							bar.addClass('info_but_close');
							bar.removeClass('info_but_open');
							bar.removeClass('info_but_open_on');

							panel.removeClass('full');
							toolbar.removeClass('full');

							menu.show();

							giiwa.resize();
						}
					});

		});

var processing = {
	show : function() {
		$('#processing').show();
	},
	hide : function() {
		$('#processing').hide();
	}
};
