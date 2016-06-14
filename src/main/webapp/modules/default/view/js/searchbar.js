/**
 * 
 */
(function($) {
	$.fn.searchbar = function() {
		var ss = this;
		if (ss.length > 0) {
			var sbar = ss.find('.s1tab');
			sbar.click(function() {
				if (sbar.hasClass('up')) {
					// up
					ss.find('form').slideUp(function() {
						sbar.removeClass('up');
						sbar.addClass('down');

						resize();
					});
				} else {
					// down
					ss.find('form').slideDown(function() {
						sbar.removeClass('down');
						sbar.addClass('up');

						resize();
					});
				}
			});
			sbar.bind('mouseenter', function() {
				sbar.addClass('hover');
			});
			sbar.bind('mouseleave', function() {
				sbar.removeClass('hover');
			});
		}
	}
})(jQuery);
