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

			menu : function(menuid, settings) {
				settings = $.extend({
					root : 0,
					name : 'home'
				}, settings);

				var that = menuid;
				$.post(settings.url, {
					root : settings.root,
					name : settings.name
				}, function(d) {
					var s = '';
					$(d).each(function(i, e) {
						s += tohtml(e);
					});

					that.html(s);

					_hook(that);

					giiwa.resize();
				});

				function tohtml(e) {
					var s = '<div ';
					if (e.hasChildren || e.click || e.url || e.load) {
						// this is menu item
						s += ' class="item" ';
					}
					if (e.style) {
						s += ' style="' + e.style + '" ';
					}
					s += '>';
					s += '<div class="title ';
					if (e.hasChildren) {
						s += ' haschild ';
					}
					s += '" mid="' + e.id + '" ';
					s += ' seq="' + e.seq + '" ';
					if (e.load) {
						s += ' data-load=\'' + e.load + '\' ';
					} else if (e.click) {
						s += ' onclick=\'$("#menu .menu .selected").removeClass("selected");$(this).addClass("selected");'
								+ e.click + '\' ';
					}
					if (e.tag) {
						s += ' tag="' + e.tag + '" ';
					}
					s += '> ';

					s += '<i class="icon ';
					if (e.classes) {
						s += e.classes;
					}
					s += '"></i>';
					s += '<img class="icon" src="/images/loading.gif"/>';
					if (e.url) {
						s += '&nbsp;<span><a href="' + e.url
								+ '" target="_blank">' + e.text + '</a></span>';
					} else {
						s += '&nbsp;<span>' + e.text + '</span>';
					}
					if (e.hasChildren) {
						s += '<i class="arrow"></i>';
					}
					s += '</div>';

					if (e.hasChildren) {
						s += '<div class="children"></div>'
					}
					s += '</div>';
					return s;
				}

				function _hook(o) {
					var e = o.find('div.title');

					e.bind('click', function() {
						var that = $(this);
						// if has href, load the href
						if (that.attr('data-load')) {
							menuid.find(".selected").removeClass("selected");
							that.addClass("selected");
							giiwa.load(that.attr('data-load'));
						}

						// if hasclick, open this
						if (that.hasClass('haschild')) {
							_open(this);
						}
					})

					e.bind('mouseenter', function() {
						$(this).addClass('hover');
					});
					e.bind('mouseleave', function() {
						$(this).removeClass('hover');
					});

				}

				function _open(o) {
					o = $(o);
					var parent = o.parent();
					if (o.hasClass('open')) {
						o.removeClass('open');
						$(parent.find('.children')[0]).slideUp(500, function() {
							giiwa.resize();
						});
					} else {
						o.addClass('open');
						var c = parent.find('.children .item');
						if (c.length == 0) {
							parent.addClass('loading');
							$.post(settings.url, {
								root : o.attr('mid')
							}, function(d) {
								var s = '';
								$(d).each(function(i, e) {
									s += tohtml(e);
								})
								var e = parent.find('.children');
								e.html(s);

								_hook(e);

								$(parent.find('.children')[0]).slideDown(500,
										'easeOutQuad', function() {
											parent.removeClass('loading');

											giiwa.resize();
										});
							})
						} else {
							$(parent.find('.children')[0]).slideDown(500,
									'easeOutQuad', function() {
										giiwa.resize();
									});
						}
					}
				}
			},

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
							opt && opt.success && opt.success(d, 'get');
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
									opt
											&& opt.success
											&& opt.success(xhr.responseText,
													'post');
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

					if (!opt || !opt.moveable) {
						$("#dialog .dialog").draggable();
					}

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

				var p = $('#gwpopup');
				if (p.length == 0) {
					p = $('<div id="gwpopup"><div class="popupbg"></div><div class="popup"><a class="prev">&lt;</a><a class="close">X</a><div class="scroll"></div></div></div>');
					$('body').append(p);

					if (!opt || !opt.moveable) {
						$("#gwpopup .popup").draggable();
					}
					$('#gwpopup .popupbg, #gwpopup a.close').click(function(d) {
						p.fadeOut(function() {
							p.remove();
							opt && opt.onclose && opt.onclose();
						});
					});

					$('#gwpopup a.prev').click(
							function(d) {
								if (giiwa.popuphistory
										&& giiwa.popuphistory.length > 1) {
									var h = giiwa.popuphistory.pop();
									h = giiwa.popuphistory.pop();
									giiwa.popup(h);
								}
							});

					giiwa.popuphistory = [];
				}
				if (giiwa.popuphistory && giiwa.popuphistory.length > 0) {
					p.find('a.prev').show();
				} else {
					p.find('a.prev').hide();
				}
				giiwa.popuphistory.push(url);

				var pp = $('#gwpopup .popup>.scroll');
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
																						.popup(
																								href,
																								opt);
																			}

																			e1
																					.preventDefault();
																		});
															}
														});

										pp
												.find('form')
												.submit(

														function(e) {
															e.preventDefault();

															var form = e.target;

															giiwa
																	.submit(
																			form,
																			{
																				success : function(
																						d) {
																					p
																							.fadeOut(function() {
																								p
																										.remove();
																								opt
																										&& opt.onclose
																										&& opt
																												.onclose();

																							});
																					opt.onsubmit
																							&& opt
																									.onsubmit(d);
																				}
																			});

														});
									}

								})

				p.fadeIn();

				giiwa._popup = {
					close : function() {
						if (p && p.length > 0 && p.css('display') != 'none') {
							p.fadeOut(function() {
								p.remove();
								opt && opt.onclose && opt.onclose();

							});
						}
					},
					isShowing : function() {
						return p && p.length > 0 && p.css('display') != 'none';
					},
					reload : function() {
						var h = giiwa.popuphistory.pop();
						giiwa.popup(h);
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

				portlet.load(panel, function(e) {
					giiwa.hook(e);
				})

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
						success : function(d, method) {
							if (method == 'post') {
								try {
									d = eval("(" + d + ")");
								} catch (e) {
									console.error(e);
								}
								if (d.state == 200) {
									giiwa.back();
									giiwa.hint(d.message);
								} else if (d.message) {
									giiwa.error(d.message);
								}
							} else {
								giiwa.show(d);
							}
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

			reload : function(cb) {
				if (giiwa.__history.length > 1) {
					giiwa.load(giiwa.__history.pop(), cb);
					return true;
				}

				return false;
			},

			load : function(uri, cb) {
				giiwa.processing.show();

				giiwa.popup() && giiwa.popup().close();

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
						} else if (cb) {
							cb(d);
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
			message : function(message, cb) {

				var m = $('#leanmodal');
				if (m.length == 0) {
					m = $("<div id='leanmodal' class='hint'><div class='bg'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.attr('class', 'message');
				m.find('.leanmodal-content').html(message);
				m.css({
					'display' : 'block'
				});
				m.click(function() {
					m.hide();
				});
				m.click(function() {
					m.hide();
					cb && cb();
				});
			},

			hint : function(message, delay) {
				if (!delay) {
					delay = 2000;
				}
				var m = $('#leanmodal');
				if (m.length == 0) {
					m = $("<div id='leanmodal' class='hint'><div class='bg'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.attr('class', 'hint');
				m.find('.leanmodal-content').html(message);
				m.css({
					'display' : 'block'
				});
				m.click(function() {
					m.hide();
				});
				setTimeout(function() {
					m.fadeOut();
				}, delay);

			},

			warn : function(message, cb) {
				var m = $('#leanmodal');
				if (m.length == 0) {
					m = $("<div id='leanmodal' class='hint'><div class='bg'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.attr('class', 'warn');
				m.find('.leanmodal-content').html(message);
				m.css({
					'display' : 'block'
				});
				m.click(function() {
					m.hide();
					cb && cb();
				});
			},

			error : function(message, cb) {
				var m = $('#leanmodal');
				if (m.length == 0) {
					m = $("<div id='leanmodal' class='hint'><div class='bg'></div><div class='leanmodal-content'></div></div>");
					$('body').append(m);
				}
				m.attr('class', 'error');
				m.find('.leanmodal-content').html(message);
				m.css({
					'display' : 'block'
				});
				m.click(function() {
					m.hide();
					cb && cb();
				});
			},

			fullscreen : function(ele) {
				if (ele.requestFullscreen) {
					ele.requestFullscreen();
				} else if (ele.mozRequestFullScreen) {
					ele.mozRequestFullScreen();
				} else if (ele.webkitRequestFullscreen) {
					ele.webkitRequestFullscreen();
				} else if (element.msRequestFullscreen) {
					ele.msRequestFullscreen();
				}
			}

		});

$(function() {

	$(window).resize(function() {
		giiwa.resize();
	});
});
