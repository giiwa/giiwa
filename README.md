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

<h2>Build and package giiwa</h2>
<p><strong>Step 1</strong>), To build, by maven 2.0+, it will compile, package all dependences to giiwa.war</p>
<pre>mvn clean package install</pre>
<p><strong>Step 2</strong>) Package, by ant 1.8+, it will generate giiwa_x.x.x.tgz (binary package) and giiwa_upgrade_x.x.x.zip (module zip file)<p>
<pre>ant</pre>

<h2>License</h2>
<p>Giiwa is licensed under the terms of the <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache License, Version 2.0</a></p>
