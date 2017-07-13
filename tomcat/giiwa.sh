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
		f = open('/tmp/catalina.pid', 'r');
		pid = f.readline().strip();
		f.close()
		os.popen("ps -p " + pid).readlines()[1]
		return pid
	except:
		return -1
def _stopx():
	pid=_pidx()
	if pid>0:
		subprocess.call("kill " + str(pid), shell=True)

def _start():
	global running,interval,home

	pid=os.fork()
	if pid>0:
		pidfile="/tmp/giiwa.pid"
		try:
			f=open(pidfile, "wb+")
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
		f = open('/tmp/giiwa.pid', 'r');
		pid = f.readline().strip();
		f.close()
		os.popen("ps -p " + pid).readlines()[1]
		subprocess.call("kill " + pid, shell=True)
		print "Stopped."
	except Exception, e:
		print "Not running."
	
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
	else:
		print "Help: \n\tgiiwa [start|stop]\n"
