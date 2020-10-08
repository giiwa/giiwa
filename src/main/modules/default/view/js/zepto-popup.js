(function($) {
  'use strict';

  var ZeptoPopup = function(ele, options) {
    return {
      options: {
        direction: 'bottom',
        height: '80%',
        bodyStyle: {}
      },
      init: function() {
        this.options = $.extend(true, this.options, options);
        ele.addClass('z-popup').css('max-height', this.options.height);
      },
      wrap: function() {
        var eleParent = ele.parent();
        if (eleParent.hasClass('zepto-popup')) {
          this.options.wrapElement = eleParent;
        } else {
          var random = Math.random() * 1000000;
          this.options.id = 'zepto-popup-' + random;
          ele.wrap('<div id="' + this.options.id + '" class="zepto-popup"></div>');
          this.options.wrapElement = $(document.getElementById(this.options.id));
        }

        var wrapEle = this.options.wrapElement.get(0);
        this.options.wrapElement.off('touchmove').on('touchmove', function(ev){
          if(ev.target === wrapEle) {
            ev.preventDefault();
            ev.stopPropagation();
          }
        });
      },
      show: function() {
        this.options.bodyStyle.overflow = document.body.style.overflow;
        this.options.bodyStyle.position = document.body.style.position;

        document.body.style.overflow = 'hidden';
        document.body.style.position = 'relative';
        this.options.wrapElement.addClass('show');
        ele.show();
      },
      close: function(){
        document.body.style.overflow = this.options.bodyStyle.overflow;
        document.body.style.position = this.options.bodyStyle.position;
        this.options.wrapElement.removeClass('show');
      },
      showToast: function(msg, timeout) {
        ele.addClass('show');
        ele.text(msg);

        this.options.toastTimeout && clearTimeout(this.options.toastTimeout);

        this.options.toastTimeout = setTimeout(function() {
          ele.removeClass('show');
        }, timeout || 4500);
      }
    };
  };

  $.fn.ZPopup = function(options) {
    if (options === 'toast') {
      return new ZeptoPopup($(this));
    } else {
      var zeptoPopup = new ZeptoPopup($(this), options);
      zeptoPopup.init();
      zeptoPopup.wrap();
      return zeptoPopup;
    }
  };


})(Zepto || jQuery);
