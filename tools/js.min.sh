#!/bin/bash

java -jar yuicompressor-2.4.8.jar --preserve-semi --charset utf8 -o ../temp/giiwa.min.js ../temp/giiwa.js

java -jar yuicompressor-2.4.8.jar --charset utf8 -o ../temp/giiwa.min.css ../temp/giiwa.css

java -jar yuicompressor-2.4.8.jar --charset utf8 -o ../temp/icons.min.css ../temp/icons.css
