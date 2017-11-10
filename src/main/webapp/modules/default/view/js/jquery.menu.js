/**
 * 
 */
(function($) {

	$.fn.extend({
		menu : function(settings) {
			settings = $.extend({
						root : 0,
						name : 'home'
					}, settings);

			var that = this;
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
					s += '<span><a href="' + e.url + '" target="_blank">' + e.text
							+ '</a></span>';
				} else {
					s += '<span>' + e.text + '</span>';
				}
				if (e.hasChildren) {
					s += '<i class="arrow"></i>';
				}
				// if (e.content) {
				// s += '<div class="extra">' + e.content + '</div>';
				// }
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
								$("#menu .menu .selected")
										.removeClass("selected");
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

									$(parent.find('.children')[0]).slideDown(
											500, 'easeOutQuad', function() {
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
		}
	})
})(jQuery);

$(function(){
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
		var panel = $('#panel');
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

})