1.Required
  JDK 1.8+
  DB: any one of HSQLDB, PostgreSQL 9.3+, MYSQL 5.4+, Oracle 10i+ or Mongo 3.2+

2. Running giiwa directly
  giiwa.sh start
  the giiwa.sh is python script.

3. If using appdog, then start the giiwa using below command
  bin/startup.sh
  or 
  bin/startup.bat

4. giiwa.properties
  the basic configuration, which can be done by http://[host]/setup in first time

5. log4j.properties
  the log4j configuration
  
6. conf/server.xml
  The configuration of tomcat, you can change the port from 8080 to other
  