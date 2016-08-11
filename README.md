<h1><img height='46' src="http://giiwa.org/images/giiwa.png" alt="giiwa"/></h1>
<p>Giiwa, Java Web 高性能服务器软件快速开发框架, 我们的目标是<strong>简单</strong>，<strong>快速开发</strong>, 项目之间可以 <strong>重用</strong>， <strong>“重载”</strong>。</p>
<p>未来实现这个目标, Giiwa 使用模块化机制, 简化的MV（Model－View）开发模型。 请参考 <a href="http://giiwa.org">giiwa.org</a> 以了解更多关于Giiwa。</p>
<p>View can be Velocity template, JSP page or FreeMaker template.</p>
<p><img src="http://giiwa.org/docs/images/mv.png" alt="MV developing"/></p>

<h2>缓存系统</h2>
<p>框架本身支持 Memcached，Redis 或直接文件系统缓存。</p>

<h2>数据库系统支持</h2>
<p>框架为关系型数据库和Mongo提供统一的API访问。也提供数据库直接接口以便你使用某些数据库特有的特性。</p>
<p>关系型数据库支持 内嵌HSQLDB，和大部分流行数据库。</p>

<h2>下载</h2>
<p>最新Giiwa运行包 <a href="http://giiwa.org/archive/giiwa-1.1.tgz">giiwa-1.1.tgz</a>。</p>

<h2>获取代码</h2>
<p>所有最新源码已经托管在Github:</p>
<pre>https://github.com/giiwa/giiwa.git</pre>
<p>使用 <code>git clone</code> 源码仓库 (或者在github的官网上直接克隆)， 你就可以获得Giiwa的全部最新代码。</p>

<h2>编译和打包</h2>
<p><strong>步骤 1</strong>), 使用 maven 2.0+ 编译, 她会自动编译打包所有依赖包到 giiwa.war， 不过这还没完成（请继续步骤2）。</p>
<pre>mvn clean package install</pre>
<p><strong>步骤 2</strong>) 使用 ant 1.8+ 打包, 她会自动生成生成运行包文件（giiwa_x.x.tgz）和升级包文件（giiwa_upgrade_x.x.zip)。<p>
<pre>ant</pre>

<h2>License</h2>
<p>Giiwa 以 Apache License V2 发布和授权许可，你可以任意使用代码， <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache License, Version 2.0</a></p>
