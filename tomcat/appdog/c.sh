
#cleanup shell

find /Users/joe/d -maxdepth 3 -type d -mtime +200 -exec rm -rf {} \;
rm -rf /home/joe/core*
rm -rf /tmp/puppeteer*