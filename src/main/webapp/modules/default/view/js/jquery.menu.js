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

						resize();
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
								load(that.attr('data-load'));
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
								resize();
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

												resize();
											});
								})
					} else {
						$(parent.find('.children')[0]).slideDown(500,
								'easeOutQuad', function() {
									resize();
								});
					}
				}
			}
		}
	})
})(jQuery);
