@echo off

@echo compressing /src/main/webapp/modules/default/view/css/admin.css
java -jar yuicompressor-2.4.6.jar --charset utf8 -o ../src/main/webapp/modules/default/view/css/admin.css ../src/main/webapp/modules/default/view/css/admin.css

@echo compressing /src/main/webapp/modules/default/view/js/jquery.fileupload.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/jquery.fileupload.min.js ../src/main/webapp/modules/default/view/js/jquery.fileupload.js

@echo compressing /src/main/webapp/modules/default/view/js/jquery.filedownload.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/jquery.filedownload.min.js ../src/main/webapp/modules/default/view/js/jquery.filedownload.js

@echo compressing /src/main/webapp/modules/default/view/js/jquery.easing.1.3.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/jquery.easing.min.js ../src/main/webapp/modules/default/view/js/jquery.easing.1.3.js


@echo compressing /src/main/webapp/modules/default/view/js/jquery.dictpicker.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/jquery.dictpicker.min.js ../src/main/webapp/modules/default/view/js/jquery.dictpicker.js


@echo compressing /src/main/webapp/modules/default/view/js/fileuploader.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/fileuploader.min.js ../src/main/webapp/modules/default/view/js/fileuploader.js

@echo compressing /src/main/webapp/modules/default/view/js/jquery.menu.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/jquery.menu.min.js ../src/main/webapp/modules/default/view/js/jquery.menu.js

@echo compressing /src/main/webapp/modules/default/view/js/jquery.datetimepicker.min.js
java -jar yuicompressor-2.4.6.jar --preserve-semi --charset utf8 -o ../src/main/webapp/modules/default/view/js/jquery.datetimepicker.min.js ../src/main/webapp/modules/default/view/js/jquery.datetimepicker.js


@echo compressing /src/main/webapp/modules/default/view/css/giiwa.css
java -jar yuicompressor-2.4.6.jar --charset utf8 -o ../src/main/webapp/modules/default/view/css/giiwa.min.css ../src/main/webapp/modules/default/view/css/giisoo.css

@echo compressing /temp.css
java -jar yuicompressor-2.4.6.jar --charset utf8 -o temp.css temp.css


