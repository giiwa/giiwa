<h1><img height='46' src="http://giiwa.org/images/giiwa.png" alt="giiwa"/></h1>
<p>Giiwa is a Java Web framework, the aim is to <strong>simplify</strong> and <strong>develop fast</strong>, projects can be <strong>reused</strong> and <strong>overrided</strong>.</p>
<p>To achieve the goal, Giiwa use module management, and simplified MV development model. Please see <a href="http://giiwa.org">giiwa.org</a> to learn more about Giiwa.</p>
<p>View can be Velocity template, JSP page or FreeMaker template.</p>
<p><img src="http://giiwa.org/docs/images/mv.png" alt="MV developing"/></p>

<h2>Downloading</h2>
<p>Latest Giiwa version is available <a href="http://giiwa.org/archive/giiwa-1.0.2.tgz">giiwa-1.0.2.tgz</a>.</p>

<h2>Obtaining the Source</h2>
<p>The official Git repository is at:</p>
<pre>https://github.com/giiwa/giiwa.git</pre>
<p>Simply <code>git clone</code> the repo (or the repo you forked via the github website) and you will have the complete source.</p>
<h2>Build and pacake giiwa</h2>
<p><strong>Step 1</strong>), To build, by maven 2.0+, it will compile, package all dependences to giiwa.war</p>
<pre>mvn clean package install
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building giiwa 1.0.2
[INFO] ------------------------------------------------------------------------
[INFO]
...
[INFO] Building jar: /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2/WEB-INF/lib/giiwa-1.0.2.jar
[INFO] Webapp assembled in [216 msecs]
[INFO] Building war: /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2.war
...
[INFO] Installing /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2.war to /Users/wujun/.m2/repository/org/giiwa/giiwa/1.0.2/giiwa-1.0.2.war
[INFO] Installing /Users/wujun/d/workspace/giiwa/pom.xml to /Users/wujun/.m2/repository/org/giiwa/giiwa/1.0.2/giiwa-1.0.2.pom
[INFO] Installing /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2-sources.jar to /Users/wujun/.m2/repository/org/giiwa/giiwa/1.0.2/giiwa-1.0.2-sources.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
</pre>
<p><strong>step 2</strong>) Package, by ant 1.8+, it will generate giiwa_x.x.x.tgz (binary package) and giiwa_upgrade_x.x.x.zip (module zip file)<p>
<pre>ant
Buildfile: /Users/wujun/d/workspace/giiwa/build.xml

tar:
     [copy] Copying 58 files to /Users/wujun/d/workspace/giiwa/target/giiwa
     [copy] Copied 8 empty directories to 3 empty directories under /Users/wujun/d/workspace/giiwa/target/giiwa
     [copy] Copying 254 files to /Users/wujun/d/workspace/giiwa/target/giiwa/giiwa
     [copy] Copied 55 empty directories to 2 empty directories under /Users/wujun/d/workspace/giiwa/target/giiwa/giiwa
      [tar] Building tar: /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2.tar
     [gzip] Building: /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2-1607240950.tgz
   [delete] Deleting: /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2.tar
     [copy] Copying 1 file to /Users/wujun/d/workspace/archives

zip:
     [copy] Copying 254 files to /Users/wujun/d/workspace/giiwa/target/upgrade
     [copy] Copied 55 empty directories to 2 empty directories under /Users/wujun/d/workspace/giiwa/target/upgrade
     [copy] Copying 45 files to /Users/wujun/d/workspace/giiwa/target/upgrade/modules/default/WEB-INF
      [zip] Building zip: /Users/wujun/d/workspace/giiwa/target/giiwa-1.0.2-upgrade-1607240950.zip
     [copy] Copying 1 file to /Users/wujun/d/workspace/archives

package:

BUILD SUCCESSFUL
</pre>

<h2>License</h2>
<p>Giiwa is licensed under the terms of the <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache License, Version 2.0</a></p>
