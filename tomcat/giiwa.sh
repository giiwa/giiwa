#!/usr/bin/python

# chkconfig:   2345 90 10
# description:  giiwa is a apps daemon tool
# version: v0.2
# author: joe
# required: above python2.6

import os, sys, time, subprocess, signal
#import re, os, sys, time, threading, subprocess, signal

global running,app
running=True
home=sys.path[0]
app=home + "/bin/startup.sh"
interval=1
giiwapid = "/tmp/giiwa.pid"
catalinapid = "/tmp/catalina.pid"

def onkill(a,b):
	global running
	running=False
	
def _startx():
	global app, home
	print "Starting: " + app
	try:
		subprocess.Popen(app, shell=True, cwd=home)
	except Exception, e:
		print e
		print "Failed:" + app
		
def _pidx():
	try:
		f = open(catalinapid, 'r');
		pid = f.readline().strip();
		f.close()
		os.popen("ps -p " + pid).readlines()[1]
		return pid
	except:
		return -1
	
def _stopx():
	pid = _pidx()
	if pid>0:
		subprocess.call("kill " + str(pid), shell=True)

def _start():
	global running,interval,home
	pid = os.fork()
	if pid>0:
		try:
			f=open(giiwapid, "wb+")
			print >>f, pid
			f.close()
			sys.exit(0)
		except Exception, e:
			print e
			sys.exit(0)
	while running:
		if _pidx()>0:
			time.sleep(interval)
		else:
			_startx()

			if interval>5:
				time.sleep(interval)
			else:
				time.sleep(10)
	else:
		_stopx()

def _stop():
	global home
	try:
		f = open(giiwapid, 'r');
		pid = f.readline().strip();
		f.close()
		os.popen("ps -p " + pid).readlines()[1]
		subprocess.call("kill " + pid, shell=True)
		print "Stopped."
	except Exception, e:
		print "Not running."
	
def _install():
	print "installing appdog ..."
	print "copying appdog to /etc/init.d"
	print "copying apps.conf to /etc/appdog/"
	print "setup appdog" 
	print "install success."
	print "Please view /etc/appdog/apps.conf to get more."
	
if __name__=="__main__":
	if len(sys.argv) > 1:
		a=sys.argv[1]
	else:
		a=""
	if a=="start":
		if _pidx()>0:
			print "Already running."
		else:
			signal.signal(signal.SIGTERM, onkill)
			_start()
	elif a=="stop":
		_stop()
	elif a=="restart":
		_stop()
		_start()
	elif a=="install":
		_install()
	else:
		print "Help: \n\tgiiwa [start|stop|restart|install]\n"
