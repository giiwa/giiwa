#!/usr/bin/python

import os, sys
import getpass,pexpect

cmd = ''
host = ''
port = '22'

if len(sys.argv) > 2:
    ss = sys.argv[1].split(':')
    host = ss[0];
    if(len(ss) > 1):
        port = ss[1]
        
    cmd = sys.argv[2]
        
else:
    print "Error...\r\n[Usage] clone.py host:port command [modules]"
    print "host:port"
    print "\tthe SSH host and port"
    print "command:"
    print "\t-i - install jdk and giiwa"
    print "\t-u - update modules"
    print "modules:"
    print "\tdefault,atool,..."
    sys.exit()

jdk = 'jdk1.8.0_162'
jdk1 = 'jdk-8u162-linux-x64'
giiwa = 'giiwa-1.6'

# install jdk
def inst_jdk():
    print ''
    print 'Installing JDK ...'
    
    try:
        scp = pexpect.spawn('scp ' + username + '@' + host + ':' + port +':/opt/' + jdk1 + '.tar.gz /opt/')
        e = scp.expect([r'assword:',r'yes/no'],timeout=30)
        if e == 0:
            scp.sendline(passwd)
            print scp.read()
        elif e == 1:
            scp.sendline('yes')
            scp.expect('assword:',timeout=30)
            scp.sendline(passwd)
            print scp.read()
        
        sys.popen('tar xzf /opt/' + jdk1 + '.tar.gz -C /opt/')
        
        # set profile
        found = False
        profile = open('/etc/profile', 'r+')
        for line in profile:
            if "JAVA_HOME" in line:
                found = True
                print line
                break
        
        if not found:
            profile.write('export JAVA_HOME=/opt/' + jdk + '\r\n')
            profile.write('export PATH=$JAVA_HOME/bin:$PATH\r\n')
        
        profile.close()
    except Exception as err:
        print "Error, please try again."
        sys.exit()

# install giiwa
def inst_giiwa():
    print ''
    print 'Installing giiwa ...'

    try:
        scp = pexpect.spawn('scp ' + username + '@' + host + ':' + port +':/opt/' + giiwa + '.tgz /opt/')
        e = scp.expect([r'assword:',r'yes/no'],timeout=30)
        if e == 0:
            scp.sendline(passwd)
            print scp.read()
        elif e == 1:
            scp.sendline('yes')
            scp.expect('assword:',timeout=30)
            scp.sendline(passwd)
            print scp.read()
        
        sys.popen('tar xzf /opt/' + giiwa + '.tgz -C /opt/')
    
        # remove node.id
        p1 = open('/opt/giiwa/giiwa.old', 'r+')
        p2 = open('/opt/giiwa/giiwa.properties', 'w+')
        for line in p1:
            if not "node.id" in line:
                p2.write(line)
        p1.close()
        p2.close()
    except Exception as err:
        print "Error, please try again."
        sys.exit()

# install modules
def inst_module(mo):
    print ''
    print 'Updating module [' + mo + '] ...'

    try:
        os.popen("rm -rf /opt/giiwa/giiwa/modules/" + mo);
        scp = pexpect.spawn('scp -r -P ' + port + ' ' + username + '@' + host + ':/opt/giiwa/giiwa/modules/' + mo + ' /opt/giiwa/giiwa/modules/')
        e = scp.expect([r'assword:',r'yes/no'],timeout=30)
        if e == 0:
            scp.sendline(passwd)
            print scp.read()
        elif e == 1:
            scp.sendline('yes')
            scp.expect('assword:',timeout=30)
            scp.sendline(passwd)
            print scp.read()
    except Exception as err:
        print "Error, please try again."
        sys.exit()
        
username = getpass.getuser()
print 'Username[' +  username + ']: ',
s = sys.stdin.readline().strip()
if s != '':
    username = s
passwd = getpass.getpass(prompt='Password: ', stream=None)

if (cmd == '-i'):
    inst_jdk()
    inst_giiwa()
    print 'Done.'
    print 'Please start the giiwa by: \'giiwa/giiwa.sh start\''
elif (cmd == '-u'):
    if len(sys.argv) < 4:
        print ''
        print "no module updated."
        sys.exit()
        
    for m in sys.argv[3:]:
        inst_module(m)
    print ''
    print 'Done.'
    print 'Please restart the giiwa ...'
