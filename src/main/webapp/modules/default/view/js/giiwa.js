/**
 * 
 */

window.giiwa = {};

giiwa.extend = function(m) {
	for ( var k in m) {
		giiwa[k] = m[k];
	}
};

giiwa
		.extend({
			__history : [],
			menuapi : false,
			panelapi : false,
			uploaddone : false,
			_popup : false,

			popupmenu : function(menu, xy, cb) {
				// popup menu
				var p = $("#popupmenu");
				if (p.length == 0) {
					p = $("<div id='popupmenu'><div class='bg'></div><div class='menu'></div></div>");
					$('body').append(p);
				}
				p.find('.bg').on('click', function(e) {
					p.hide();
				});

				var p1 = p.find('>.menu');
				p1.empty();
				p1.append(menu);
				p1.css('left', xy[0] + 'px');
				p1.css('top', xy[1] + 'px');
				menu.show();
				p.find('.menuitem').on('click', function() {
					p.hide();
					var that = this;
					setTimeout(function() {
						cb(that)
					}, 100);
				});
				p.show();

			},

			submit : function(form, opt) {
				var beforesubmit = $(form).attr('beforesubmit');
				if (typeof window[beforesubmit] === 'function') {
					if (!window[beforesubmit](form)) {
						return;
					}
				}

				/**
				 * check the bad flag
				 */
				var bad = $(form).find(
						"input[bad=1], textarea[bad=1], select[bad=1]");
				if (bad.length > 0) {
					bad[0].focus();
					return;
				}
				var bb = $(form).find(
						"input[required=true], select[required=true]");
				for (i = 0; i < bb.length; i++) {
					var e = $(bb[i]);
					if (e.val() == '') {
						e.focus();
						return;
					}
				}

				var url = form.action;

				if (form != undefined && url != undefined) {

					giiwa.processing.show();

					if (form.method == 'get') {
						var data = $(form).serialize();

						var __url = '';
						if (url.indexOf('?') > 0) {
							__url = url + '&' + data;
						} else {
							__url = url + '?' + data;
						}
						if (giiwa.__history.length > 0
								&& giiwa
										._compare(
												giiwa.__history[giiwa.__history.length - 1],
												__url)) {
							giiwa.__history.pop();
						}
						giiwa.__history.push(__url);

						if (__url.indexOf('?') > 0) {
							__url += '&' + new Date().getTime();
						} else {
							__url += '?' + new Date().getTime();
						}

						$.get(__url, {}, function(d) {
							giiwa.processing.hide();
							opt && opt.success && opt.success(d);
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
									giiwa.processing.hide();
									opt && opt.success
											&& opt.success(xhr.responseText);
								}
							}
						}

					}
				}
			},

			dialog : function(panel, opt) {

				if (!panel || panel.length == 0) {
					return giiwa._dialog;
				}

				opt = $.extend({
					width : 400,
					height : 200
				}, opt);

				var p0 = panel.parent();
				var p = $('#dialog');
				if (p.length == 0) {
					p = $('<div id="dialog"><div class="dialogbg"></div><div class="dialog"><a class="close">X</a><div class="scroll"></div></div></div>');
					$('body').append(p);

					$('#dialog .dialogbg, #dialog a.close').click(function(d) {
						p.fadeOut(function() {
							panel.hide();
							p0.append(panel);
							p.remove();
						});
					});
				}
				var p1 = $('#dialog .dialog');
				p1.css('width', opt.width + 'px');
				p1.css('height', opt.height + 'px');
				p1.css('left', '50%');
				p1.css('top', '50%');
				p1.css('margin-left', (-opt.width / 2) + 'px');
				p1.css('margin-top', (-opt.height / 2) + 'px');

				var pp = $('#dialog .dialog>.scroll');
				pp.empty();
				pp.append(panel);
				panel.css('display', 'inline-block');

				p.fadeIn();

				giiwa._dialog = {
					close : function() {
						p.fadeOut(function() {
							panel.hide();
							p0.append(panel);
							p.remove();
						})
					}
				};
				return giiwa._dialog;

			},

			popup : function(url, opt) {
				if (!url) {
					return giiwa._popup;
				}

				opt = $.extend({}, opt);

				var p = $('#popup');
				if (p.length == 0) {
					p = $('<div id="popup"><div class="popupbg"></div><div class="popup"><a class="prev">&lt;</a><a class="close">X</a><div class="scroll"></div></div></div>');
					$('body').append(p);

					$("#popup .popup").draggable();
					$('#popup .popupbg, #popup a.close').click(function(d) {
						p.fadeOut(function() {
							p.remove();
						});
					});

					$('#popup a.prev').click(function(d) {
						if (giiwa.popuphistory.length > 1) {
							var h = giiwa.popuphistory.pop();
							h = giiwa.popuphistory.pop();
							giiwa.popup(h);
						}
					});

					giiwa.popuphistory = [];
				}
				if (giiwa.popuphistory.length > 0) {
					p.find('a.prev').show();
				} else {
					p.find('a.prev').hide();
				}
				giiwa.popuphistory.push(url);

				var pp = $('#popup .popup>.scroll');
				pp.empty();
				giiwa.processing.show();
				$
						.get(
								url,
								function(d) {
									giiwa.processing.hide();
									pp.html(d);

									hook();

									function hook() {
										pp
												.find('a')
												.each(

														function(i, e) {
															e = $(e);
															var href = e
																	.attr('href');
															var target = e
																	.attr('target');
															if (target == undefined
																	&& href != undefined
																	&& (href
																			.indexOf('javascript') == -1)
																	&& (href
																			.indexOf('#') != 0)) {

																e
																		.click(function(
																				e1) {
																			var href = $(
																					this)
																					.attr(
																							'href');
																			if (href != undefined) {
																				giiwa
																						.popup(href);
																			}

																			e1
																					.preventDefault();
																		});
															}
														});

										pp.find('form').submit(

										function(e) {
											e.preventDefault();

											var form = e.target;

											giiwa.submit(form, {
												success : function(d) {
													p.fadeOut(function() {
														p.remove();
													});
													giiwa.show(d);
												}
											});

										});
									}

								})

				p.fadeIn();

				giiwa._popup = {
					close : function() {
						p.fadeOut(function() {
							p.remove();
						});
					}
				};
				return giiwa._popup;
			},

			download : function(url, opt) {
				giiwa.processing.show();
				$
						.post(
								url,
								opt,
								function(d) {
									giiwa.processing.hide();
									if (d.state == 200) {
										var d = $('iframe#download');
										if (d.length == 0) {
											d = $("<iframe id='download' style='display:none'></iframe>");
											$('body').append(d);
										}
										d.attr('src', d.file);
									}
								});
			},

			processing : {
				show : function() {
					var p = $('#processing');
					if (p.length == 0) {
						p = $('<div id="processing" style="display: none;"><div class="bg"></div><div class="img"><img src="/images/loading2.gif"></div></div>');
						$('body').append(p);
					}
					p.show();
				},
				hide : function() {
					$('#processing').hide();
				}
			},

			css : function(urls) {
				if (urls && urls.length > 0) {
					var ss = urls.split(',');
					var ll = $(document).find('link');
					$(ss).each(
							function(i, f) {
								f = f.trim();
								for (var i = 0; i < ll.length; i++) {
									if (ll[i].href
											&& ll[i].href.indexOf(f) >= 0) {
										return;
									}
								}
								$('head').append(
										'<link href="' + f
												+ '" rel="stylesheet" />');
							});
				}
			},

			js : function(urls) {
				if (urls && urls.length > 0) {
					var ss = urls.split(',');
					var ll = $(document).find('script');
					$(ss)
							.each(
									function(i, f) {
										f = f.trim();
										for (var i = 0; i < ll.length; i++) {
											if (ll[i].src
													&& ll[i].src.indexOf(f) >= 0) {
												return;
											}
										}
										$('body').append(
												'<script type="text/javascript" src="'
														+ f + '"></script>');
									});
				}
			},

			_format : function(url) {
				var p = {};
				var i = url.indexOf('?');
				if (i >= 0) {
					var uri = url.substring(0, i);
					var query = url.substring(i + 1);
				} else {
					i = url.indexOf('&');
					if (i >= 0) {
						var uri = url.substring(0, i);
						var query = url.substring(i + 1);
					} else {
						i = url.indexOf('#');
						if (i >= 0) {
							var uri = url.substring(0, i);
							var query = url.substring(i + 1);
						} else {
							uri = url;
							query = '';
						}
					}
				}
				i = uri.indexOf('//');
				if (i >= 0) {
					var s = uri.substring(i + 2);
					i = s.indexOf('/');
					if (i >= 0) {
						p.uri = s.substring(i);
					} else {
						p.uri = '/';
					}
				} else {
					p.uri = uri;
				}
				if (p.uri[0] != '/') {
					p.uri = '/' + p.uri;
				}
				if (p.uri.length > 1 && p.uri[p.uri.length - 1] == '/') {
					p.uri = p.uri.substring(0, p.uri.length - 1);
				}

				i = query.indexOf('&');
				while (i > 0) {
					var s = query.substring(0, i);
					query = query.substring(i + 1);

					i = s.indexOf('=');
					if (i > 0) {
						var s1 = s.substring(0, i);
						var s2 = s.substring(i + 1);
						if (s1.length > 0 && s2.length > 0) {
							p[s1] = s2;
						}
					}
					i = query.indexOf('&');
				}
				s = query;
				i = s.indexOf('=');
				if (i > 0) {
					var s1 = s.substring(0, i);
					var s2 = s.substring(i + 1);
					if (s1.length > 0 && s2.length > 0) {
						p[s1] = s2;
					}
				}
				return p;
			},
			_compare : function(url1, url2) {
				var p1 = giiwa._format(url1);
				var p2 = giiwa._format(url2);
				// console.log(url1);
				// console.log(p1);
				// console.log(url2);
				// console.log(p2);

				for ( var key in p1) {
					if (p1[key] != p2[key]) {
						return false;
					}
				}
				for ( var key in p2) {
					if (p1[key] != p2[key]) {
						return false;
					}
				}
				return true;
			},
			history : function(url) {
				if (url && url.length > 0) {
					if (url[url.length - 1] == '?') {
						url = url.substring(0, url.length - 1);
					}
					if (!giiwa._compare(
							giiwa.__history[giiwa.__history.length - 1], url)) {
						giiwa.__history.push(url);
					}
					// console.log(giiwa.__history);
				}
			},
			back : function() {

				if (giiwa.__history.length > 1) {

					var h = giiwa.__history.pop();
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

				giiwa.hook($('#panel .content'));

				// resize();

			},

			hook : function(panel) {

				/**
				 * hook all the <a> tag
				 */
				panel.find('a').each(

						function(i, e) {
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

									e1.preventDefault();
								});
							}
						});

				/**
				 * hook all <form> to smooth submit
				 */
				panel.find('form').submit(

				function(e) {
					e.preventDefault();

					var form = e.target;

					giiwa.submit(form, {
						success : function(d) {
							giiwa.show(d);
						}
					});

				});

				panel.find('table th.checkbox').click(function(e) {
					var ch = $(this).find('input[type=checkbox]');
					if (ch.length > 0) {
						var en = ch[0].checked;
						var t = $(this);
						while (t.length > 0 && t[0].nodeName !== 'TABLE') {
							t = t.parent();
						}
						t.find('td input[type=checkbox]').each(function(i, e) {
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
				panel
						.find('select[parentnode=true]')
						.change(

								function(e) {
									var ch = $(this);
									if (ch.length > 0) {
										var value = ch.val();
										var subnode = ch.attr('subnode');
										var n1 = $('select[name=' + subnode
												+ ']');
										/**
										 * initialize the options
										 */
										if (options[ch.attr('name')]) {
											n1.html(options[ch.attr('name')]);
										} else {
											options[ch.attr('name')] = n1
													.find('option');
										}

										var valid = false;
										var best = undefined;
										n1
												.find('option')
												.each(

														function(i, e) {
															e = $(e);
															if (e
																	.attr('parent') == value
																	|| e
																			.attr('parent') == undefined) {
																e.show();
																if (best === undefined) {
																	best = e
																			.val();
																}
																if (!valid
																		&& e
																				.val() == n1
																				.val()) {
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
				panel.find('select').each(function(i, e) {
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
				panel.find('table.tablesorter tr').bind('mouseenter',
						function() {
							$(this).addClass('hover');
						}).bind('mouseleave', function() {
					$(this).removeClass('hover');
				});

				/**
				 * hook td.hover
				 */
				panel.find('table.tablesorter td').bind('mouseenter',
						function() {
							$(this).addClass('hover');
						}).bind('mouseleave', function() {
					$(this).removeClass('hover');
				});

				/**
				 * setting all searchbar
				 */
				panel.find('div.search').searchbar();

				panel.find('input').bind('focus', function() {
					$(this).parent().addClass('focus');
				}).bind(
						'blur',
						function() {
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

				setTimeout(function() {
					panel.find('.blink').each(function(i, e) {
						$(e).removeClass('blink');
					});
				}, 100);
			},

			reload : function() {
				if (giiwa.__history.length > 1) {
					giiwa.load(giiwa.__history.pop());
					return true;
				}

				return false;
			},

			load : function(uri) {
				giiwa.processing.show();

				if (giiwa.__history.length > 0
						&& giiwa._compare(
								giiwa.__history[giiwa.__history.length - 1],
								uri)) {
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
						giiwa.processing.hide();
						window.location.href = "/";
					},
					success : function(d, status, xhr) {
						giiwa.processing.hide();
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
				giiwa.processing.show();

				// $('#page').attr('src', uri);
				if (uri.indexOf('?') > 0) {
					uri += '&' + new Date().getTime();
				} else {
					uri += '?' + new Date().getTime();
				}
				var s = '<iframe src="' + uri + '"></iframe>';
				giiwa.processing.hide();
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
			},

			resize : function(e) {
				var h = $(window).height();
				var w = $(window).width();
				var menu = $('#menu');
				var panel = $('#panel');
				if ((panel.width() != w - panel.offset().left)
						|| (panel.height() != h - 92)) {
					panel.css('width', (w - panel.offset().left) + 'px');
					panel.css('height', (h - 92) + 'px');
					panel.trigger('panelresize', panel);
				}

				if (menu.length > 0) {
					menu.css('height', (h - 92) + 'px');

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
							var note = that.parent().parent().find('.note');
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
			},

			//
			message : function(message, onclick) {
				$('#error.leanmodal').hide();
				var m = $('#message.leanmodal');
				if (m.length == 0) {
					m = $("<div id='message' class='leanmodal'><div class='leanmodal-header'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.find('.leanmodal-content').html(message);
				var overlay = $('#lean_overlay');
				if (overlay.length == 0) {
					overlay = $("<div id='lean_overlay'></div>");
					$("body").append(overlay);
				}
				overlay.css({
					'display' : 'block',
					opacity : 0.5
				});
				m.css({
					'display' : 'block'
				});
				overlay.click(function() {
					overlay.hide();
					$('.leanmodal').hide();

					onclick && onclick();
				});
				m.click(function() {
					overlay.click();
				});
			},

			hint : function(message, delay) {
				if (!delay) {
					delay = 2000;
				}
				$('#error.leanmodal').hide();
				$('#message.leanmodal').hide();
				var m = $('#hint.leanmodal');
				if (m.length == 0) {
					m = $("<div id='hint' class='leanmodal'><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.find('.leanmodal-content').html(message);
				m.css({
					'display' : 'block'
				});
				setTimeout(function() {
					m.fadeOut();
				}, delay);
			},
			warn : function(message, onclick) {
				$('#message.leanmodal').hide();
				var m = $('#error.leanmodal');
				if (m.length == 0) {
					m = $("<div id='warn' class='leanmodal'><div class='leanmodal-header'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.find('.leanmodal-content').html(message);
				var overlay = $('#lean_overlay');
				if (overlay.length == 0) {
					overlay = $("<div id='lean_overlay'></div>");
					$("body").append(overlay);
				}
				overlay.css({
					'display' : 'block',
					opacity : 0.5
				});
				m.css({
					'display' : 'block'
				});
				overlay.click(function() {
					overlay.hide();
					$('.leanmodal').hide();

					onclick && onclick();
				});
				m.click(function() {
					overlay.click();
				});
			},
			error : function(message, onclick) {
				$('#message.leanmodal').hide();
				var m = $('#error.leanmodal');
				if (m.length == 0) {
					m = $("<div id='error' class='leanmodal'><div class='leanmodal-header'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.find('.leanmodal-content').html(message);
				var overlay = $('#lean_overlay');
				if (overlay.length == 0) {
					overlay = $("<div id='lean_overlay'></div>");
					$("body").append(overlay);
				}
				overlay.css({
					'display' : 'block',
					opacity : 0.5
				});
				m.css({
					'display' : 'block'
				});
				overlay.click(function() {
					overlay.hide();
					$('.leanmodal').hide();

					onclick && onclick();
				});
				m.click(function() {
					overlay.click();
				});
			}

		});

$(function() {
	$(window).resize(function() {
		giiwa.resize();
	});
});
