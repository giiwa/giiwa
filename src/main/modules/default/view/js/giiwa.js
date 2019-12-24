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
			uploaddone : false,
			_popup : false,

			loadimage : function(url, cb) {
				if(url && url != '') {
					var m = new Image();
					m.onload = function(){
						cb && cb(url);
					}
					m.src = url;
				}
			},
			
			popover : function (el, pop, opt){
				var pp = $("#popover");
				if(pp.length == 0) {
					pp = $("<div id='popover'><span class='top'></span><span class='right'></span><span  class='bottom'></span><span class='left'></span><div class='cc'></div></div>");
					$('body').append(pp);
				}
				
				el.each(function(i, e){
					e = $(e);
					e.on('mouseover', function(evt){

						pp.find('>span').hide();

						var e1 = pop.clone();
						opt && opt.onprepare && opt.onprepare(e, e1);

						e1.show();
						var cc = pp.find('>.cc');
						cc.empty();
						cc.append(e1);
						
						var hint = e.attr('data-hint');
						if(hint == 'top') {
							// top
							pp.find('>span.bottom').show();
							pp.css('top', (e.offset().top - pp.height() - 21) + 'px');
							pp.css('left', (e.offset().left + e.width()/2 - pp.width()/2) + 'px');
						} else if(hint == 'right') {
							// right
							pp.find('>span.left').show();
							pp.css('top', (e.offset().top + e.height()/2 - pp.height()/2) + 'px');
							pp.css('left', (e.offset().left + e.width()  + 21) + 'px');
						} else if(hint == 'left') {
							// left
							pp.find('>span.right').show();
							pp.css('top', (e.offset().top + e.height()/2 - pp.height()/2) + 'px');
							pp.css('left', (e.offset().left - pp.width() - 21) + 'px');
						} else {
							// bottom
							pp.find('>span.top').show();
							pp.css('top', (e.offset().top + e.height() + 21) + 'px');
							pp.css('left', (e.offset().left + e.width()/2 - pp.width()/2) + 'px');
						}
						
						pp.show();
					}).on('mouseout', function(e){
						pp.hide();
					})
				})
			},
			
			upload : function(files, opt) {
				opt = $.extend({
					chunksize : 32 * 1024
				}, opt);

				// console.log(files);
				$(files).each(
						function(i, file) {
							// console.log(file);
							var lastmodified = file.lastModified;
							var name = file.name;
							var size = file.size;
							var pos = 0;

							var xhr = new XMLHttpRequest();
							xhr.onreadystatechange = function(e) {
								if (xhr.readyState == 4) {
									// got response
									e = eval("(" + xhr.response + ")");

									opt.onprogress && opt.onprogress(e);
									pos = e.pos;
									if (pos < size) {
										_next();
									} else {
										opt.ondone && opt.ondone(e);
									}
								}
							}
							xhr.onerror = function(e) {
								console.log(xhr);
							}

							var _upload = function(bb) {

								xhr.open('POST', opt.url, true);
								// console.log(bb);
								xhr.setRequestHeader('lastModified',
										file.lastModified);
								xhr.setRequestHeader('Content-Range', 'bytes '
										+ pos + '-' + (pos + opt.chunksize + 1)
										+ '/' + file.size);
								var data = new FormData();
								data.append("filename", name);
								data.append("file", bb);
								xhr.send(data);
							}

							var _next = function() {
								var bb = false;
								if (file.webkitSlice) {
									bb = file.webkitSlice(pos, pos
											+ opt.chunksize + 1);
								} else if (file.mozSlice) {
									bb = file.mozSlice(pos, pos + opt.chunksize
											+ 1);
								} else if (file.slice) {
									bb = file.slice(pos, pos + opt.chunksize
											+ 1);
								}
								if (bb) {
									_upload(bb);
								} else {
									// not support post all in one
									_upload(file);
								}
							}
							opt.onstart && opt.onstart({
								name : name,
								pos : pos
							});

							_next();

						})
			},

			size : function(d) {
				var unit = '';
				if (d < 1024) {
				} else if (d < 1024 * 1024) {
					unit = "k";
					d /= 1024;
				} else if (d < 1024 * 1024 * 1024) {
					unit = "M";
					d /= 1024 * 1024;
				} else {
					unit = "G";
					d /= 1024 * 1024 * 1024;
				}
				return (parseInt(d) * 10 / 10) + unit;
			},

			link : function(e, cb) {
				e = $(e);
				var target = e.attr('target');
				if (target && target.length > 0) {
					// ignore
					return;
				}
				var on = e.attr('onclick');
				if (on && on.length > 0) {
					// ignore
					return;
				}

				var href = e.attr('href');
				if (!href) {
					return;
				}

				if (href.indexOf('javascript') != 0 && href.indexOf('#') != 0) {
					e.click(function(e1) {
						if (e1.done != 1) {
							// href may changed
							href = e.attr('href');
							cb && cb(href);
							// console.log(e1);
							e1.done = 1;
							e1.preventDefault();
							e1.stopPropagation();
						}
					});
				}
			},

			call : function(func, timeout) {
				setTimeout(function(){
					func();				
				}, timeout);
			},
			
			portlet : function(panel, cb) {
				panel.find('.portlet').each(function(i, e) {
					var pp = $(e);
					
					pp.bind('reload', function(){
						pp.attr('data-loaded', '0');
						reload();
					});
					
					reload();
					
					function reload(){
						if (pp.attr('data-loaded') != '1') {
							var url = pp.attr('data-url');
							if (url && url.length > 0) {
								load(url);
							}
						}
					}

					function load(url) {

						// console.log("portlet.load:" + url);

						$.get(url, function(d) {
							pp.attr('data-loaded', '1');
							if(d.length > 0) {
								pp.html(d);
								pp.fadeIn();
								
								// console.log(pp);

								hook(pp);

								cb && cb(pp);
							}
						})
					}

					function hook(p) {
						p.find('a').each(function(i, a) {
							giiwa.link(a, function(url) {
								load(url);
							});
						})
					}

				})
			},

			initmenu : function(menuid, settings) {
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
					var s = '<div class="';
					if (e.hasChildren || e.click || e.url || e.load) {
						// this is menu item
						s += 'item';
					}
					s += '"';
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
					s += ' data-title="' + e.text + '"> ';

					s += '<i class="icon ';
					if (e.classes) {
						s += e.classes;
					}
					s += '"></i>';
					// s += '<img class="icon" src="/images/loading.gif"/>';
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

								// title
								if (o.attr('title') && o.attr('title') != '') {
									// add title
									e.find('.item .title').each(function(i, e) {
										e = $(e);
										e.attr('title', e.attr('data-title'));
									});
								}

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
				var m1 = menu.clone();
				p1.append(m1);
				p1.css('left', xy[0] + 'px');
				p1.css('top', xy[1] + 'px');
				m1.show();
				p.find('.menuitem').on('click', function() {
					var that = this;
					if ($(that).hasClass('disabled')) {
						return;
					}
					p.hide();
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
						giiwa.history(__url);

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
					height : 200,
					z : 10001,
					moveable: true
				}, opt);

				var p = $('#dialog');
				if (p.length == 0) {
					p = $('<div id="dialog"><div class="dialogbg"></div><div class="dialog"><a class="close">X</a><div class="scroll"></div></div></div>');
					$('body').append(p);

					if (opt && opt.moveable) {
						$("#dialog .dialog").draggable();
					}

					$('#dialog .dialogbg, #dialog a.close').click(function(d) {
						p.fadeOut(100, function() {
							p.remove();
						});
					});
				} else if (giiwa._dialog) {
					giiwa._dialog.remove();
				}
				p.css("z-index", opt.z);
				
				var p1 = $('#dialog .dialog');
				p1.css('width', opt.width + 'px');
				p1.css('height', opt.height + 'px');
				p1.css('left', 'calc(50% - ' + opt.width/2 + 'px)');
				p1.css('top', 'calc(50% - ' + opt.height/2 + 'px');

				var pp = $('#dialog .dialog>.scroll');
				pp.empty();
				var p2 = panel.clone();
				pp.append(p2);
				p2.css('display', 'inline-block');

				opt && opt.prepare && opt.prepare(p2);
				
				p2.find('form').submit(function(e) {
					e.preventDefault();
					var form = e.target;

					giiwa.submit(form, {
						success : function(d) {
							p.fadeOut(100, function() {
								p.remove();
								opt.onclose && opt.onclose('success');
							});
							opt.onsubmit && opt.onsubmit(d);
						}
					});
				});

				p.fadeIn(100);

				giiwa._dialog = {
					panel : p2,
					close : function() {
						p.fadeOut(100, function() {
							p.remove();
						})
					},
					remove : function() {
					}
				};
				return giiwa._dialog;

			},

			popup : function(url, opt) {
				if (!url) {
					return giiwa._popup;
				}

				opt = $.extend({
					max : true,
					close : true,
					z : 10000,
					width: '70%',
					height: '70%',
					moveable: false
				}, opt);

				var p = $('#gwpopup');
				if (p.length == 0) {
					p = $('<div id="gwpopup"><div class="popupbg"></div><div class="popup"><a class="prev">&lt;</a><a class="max"><i class="icon-checkbox-unchecked"></i></a><a class="close">X</a><div class="scroll"></div></div></div>');
					$('body').append(p);

					$("#gwpopup>.popup").draggable();
						
					$('#gwpopup .popupbg, #gwpopup a.close').click(function(d) {
						try{
							opt.beforeclose && opt.beforeclose('close');
						}catch(e1){
							console.error(e1);
						}
						
						p.fadeOut(100, function() {
							p.remove();
							opt.onclose && opt.onclose('close');
						});
					});
					$('#gwpopup a.max').click(function(d) {
						// max or restore
						$('#gwpopup .popup').toggleClass('max');
						
						try{
							opt.beforeclose && opt.beforeclose('max');
						}catch(e1){
							console.error(e1);
						}
						
						giiwa._popup.reload();
					});

					$('#gwpopup a.prev').click(
							function(d) {
								if (giiwa.popuphistory
										&& giiwa.popuphistory.length > 1) {
									var h = giiwa.popuphistory.pop();
									h = giiwa.popuphistory.pop();

									try{
										opt.beforeclose && opt.beforeclose('back');
									}catch(e1){
										console.error(e1);
									}
									giiwa.popup(h);
								}
							});

					giiwa.popuphistory = [];
				}
				p.css("z-index", opt.z);

				if (giiwa.popuphistory && giiwa.popuphistory.length > 0) {
					p.find('a.prev').show();
				} else {
					p.find('a.prev').hide();
				}
				if (giiwa.popuphistory.length == 0
						|| url != giiwa.popuphistory[giiwa.popuphistory.length - 1]) {
					giiwa.popuphistory.push(url);
				}
				if (opt.max) {
					$("#gwpopup a.max").show();
				} else {
					$("#gwpopup a.max").hide();
				}

				// console.log(opt);
				
				var p1 = $('#gwpopup>.popup');
				if(!isNaN(opt.width)) {
					p1.css('width', opt.width + 'px');
					p1.css('left', 'calc(50% - ' + (opt.width/2) + 'px)');
				} else {
					p1.css('width', opt.width);
					p1.css('left', opt.left);
				}
				if(!isNaN(opt.height)) {
					p1.css('height', opt.height + 'px');
					p1.css('top', 'calc(50% - ' + (opt.height/2) + 'px');
				} else {
					p1.css('height', opt.height);
					p1.css('top', opt.top);
				}

				var pp = $('#gwpopup>.popup>.scroll');

				giiwa.processing.show();
				$.get(url, function(d) {
					giiwa.processing.hide();
					pp.html(d);

					hook(pp);

					function hook(pp) {
						pp.find('a').each(function(i, e) {
							giiwa.link(e, function(url) {
								giiwa.popup(url, opt);
							});
						});

						pp.find('form').submit(

						function(e) {
							e.preventDefault();

							var form = e.target;

							giiwa.submit(form, {
								success : function(d) {
									if(opt.onsubmit) {
										opt.onsubmit(d);	
									} else {
										p.fadeOut(100, function() {
											p.remove();
											opt.onclose && opt.onclose('success');
										});
									}
								}
							});

						});

						giiwa.portlet(pp, function(e) {
							// portlet already hook
							// hook($(e));
						})

					}

				})

				p.fadeIn(100);

				giiwa._popup = {
					close : function(s) {
						giiwa.popuphistory = [];
						if (p && p.length > 0 && p.css('display') != 'none') {

							try{
								opt.beforeclose && opt.beforeclose(s);
							}catch(e1){
								console.error(e1);
							}
							
							p.fadeOut(100, function() {
								p.remove();
								opt.onclose && opt.onclose(s);
							});
						}
					},
					data : function(s) {
						opt.ondata && opt.ondata(s);
					},
					isShowing : function() {
						return p && p.length > 0 && p.css('display') != 'none';
					},
					reload : function() {
						try{
							opt.beforeclose && opt.beforeclose('reload');
						}catch(e1){
							console.error(e1);
						}
						
						var h = giiwa.popuphistory.pop();
						giiwa.popup(h, opt);
					},
					back : function() {
						var h = giiwa.popuphistory.pop();
						var h = giiwa.popuphistory.pop();
						
						try{
							opt.beforeclose && opt.beforeclose('back');
						}catch(e1){
							console.error(e1);
						}
						
						giiwa.popup(h, opt);
					},
					moveable : function(b) {
						if (b) {
							$("#gwpopup>.popup").draggable("enable");
						} else {
							$("#gwpopup>.popup").draggable("disable");
						}
					}
				};

				giiwa._popup.opt = opt;
				giiwa._popup.moveable(opt && opt.moveable);
				
				return giiwa._popup;
			},

			download : function(url) {
				var d = $('a#download');
				if (d.length == 0) {
					d = $("<a id='download' download hidden></a>");
					$('body').append(d);
				}
				d.attr('href', url);
				d[0].click();
			},

			open : function(url) {
				var d = $('a#open');
				if (d.length == 0) {
					d = $("<a id='open' style='display:none' target='_blank'></a>");
					$('body').append(d);
				}
				d.attr('href', url);
				d[0].click();
			},

			processing : {
				show : function() {
					$('#panel').css('filter', 'blur(1px)')
				},
				hide : function() {
					$('#panel').css('filter', '')
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
			_equals : function(url1, url2) {
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
				if (url !== undefined && url.length > 0) {
					if (url[url.length - 1] == '?') {
						url = url.substring(0, url.length - 1);
					}
					var p = giiwa.__history.pop();
					if (p !== undefined) {
						giiwa.__history.push(p);
					}
					if (p === undefined || !giiwa._equals(p, url)) {
						giiwa.__history.push(url);
					}
					while (giiwa.__history.length > 100) {
						// remove the first
						giiwa.__history.shift();
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
					$('#panel').trigger('beforechange');
				}catch(e1){
					console.error(e1);
				}
				try {
					$('#panel .content').html(html);
				} catch (e) {
					console.error(e);
				}

				giiwa.hook($('#panel .content'));

				// resize();

			},

			hook : function(panel) {

				giiwa.portlet(panel, function(e) {
					giiwa.hook(e);
				})

				/**
				 * hook all the <a> tag
				 */
				panel.find('a').each(function(i, e) {
					giiwa.link(e, function(url) {
						giiwa.load(url);
					})
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
							var e = $(this);
							while (e.hasClass('link')) {
								e = e.prev();
							}
							e.addClass('hover');
							e = e.next();
							while (e.hasClass('link')) {
								e.addClass('hover');
								e = e.next();
							}
						}).bind('mouseleave', function() {
					var e = $(this);
					while (e.hasClass('link')) {
						e = e.prev();
					}
					e.removeClass('hover');
					e = e.next();
					while (e.hasClass('link')) {
						e.removeClass('hover');
						e = e.next();
					}
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
								// console.log(value + ", " + value.length + ",
								// "
								// + that.attr('max'));
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
				if (!uri)
					return;

				giiwa.processing.show();

				giiwa.popup() && giiwa.popup().close();
				giiwa.dialog() && giiwa.dialog().close();

				// giiwa.history(uri);

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
				if (panel.length > 0
						&& ((panel.width() != w - panel.offset().left) || (panel
								.height() != h - 92))) {
					panel.css('width', (w - panel.offset().left) + 'px');
					panel.css('height', (h - 92) + 'px');
					
					try {
						panel.trigger('panelresize', panel);
					}catch(e1) {
						console.error(e1);
					}
				}

				if (menu.length > 0) {
					menu.css('height', (h - 92) + 'px');
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
					m.fadeOut(100);
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
