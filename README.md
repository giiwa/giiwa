<h1><img height='46' src="https://www.giiwa.org/images/giiwa.png" alt="giiwa"/></h1>
<p>Giiwa, Java Web 高性能服务器软件快速开发框架, 我们的目标是<strong>简单</strong>，<strong>快速开发</strong>, 项目之间可以 <strong>重用</strong>， <strong>“重载”</strong>。</p>
<p>为了实现这个目标, 我们使用模块化机制, 使得项目开发不用从头开始，通过模块管理允许模块之间的复用和重载；简化的MV（Model－View）开发模型使得功能设计更加简单高效稳定，并提供统一的web请求参数获取API， 避免花时间去研究不同请求对数据格式的封装不一样。 请参考 <a href="https://www.giiwa.org">giiwa.org</a> 以了解更多关于Giiwa。</p>
<p>View 支持 Velocity template, JSP page 和 FreeMarker template.</p>
<p><img src="https://www.giiwa.org/docs/images/mv.png" alt="MV developing"/></p>

<h2>缓存系统，临时文件系统，文件仓库</h2>
<p>框架本身支持 Memcached，Redis 或直接文件系统缓存。</p>
<p>统一管理临时文件系统，能在分布式系统中共享临时文件，同时提供web访问接口，以便在下载中使用临时文件。框架会自动清除临时文件。</p>
<p>提供统一的文件仓库管理，以支持分布式系统中共享文件。</p>
<p>文件仓库和临时文件系统支持NFS，和Hadoop</p>

<h2>数据库系统支持</h2>
<p>支持同时链接多数据库，高效链接池，同一线程共享一个链接。</p>
<p>针对关系型数据库和Mongo，提供统一的API访问，减少移植迁移成本。也提供数据库直接接口以便你使用某些数据库特有的特性。</p>
<p>关系型数据库支持 内嵌HSQLDB，和大部分流行数据库。</p>

<h2>Web接口</h2>
<p>提供统一请求参数获取APIs:getString, getHtml, getFile, etc. 以应对GET,POST, AJAX, application/json, File， multipart/form-data等对请求数据格式的不同。</p>
<p>使得编写应用更简单。</p>

<h2>下载</h2>
<p>最新Giiwa运行包 <a href="https://www.giiwa.org/archive/giiwa-1.4.tgz">giiwa-1.4.tgz</a>，或者<a href="https://www.giiwa.org/archive/giiwa-1.4.zip">giiwa-1.4.zip</a>。</p>

<h2>获取代码</h2>
<p>所有最新源码已经托管在Github:</p>
<pre>https://github.com/giiwa/giiwa.git</pre>
<p>使用 <code>git clone</code> 源码仓库 (或者在github的官网上直接克隆)， 你就可以获得Giiwa的全部最新代码。</p>

<h2>编译和打包</h2>
<p><strong>步骤 1</strong>), 使用 maven 2.0+ 编译, 她会自动编译打包所有依赖包到 giiwa.war， 不过这还没完成（请继续步骤2）。</p>
<pre>mvn clean package install</pre>
<p><strong>步骤 2</strong>) 使用 ant 1.8+ 打包, 她会自动生成运行包文件（giiwa_x.x.tgz， 内置一个Tomcat和已经能运行的配置文件）和升级包文件（giiwa_upgrade_x.x.zip)。<p>
<pre>ant</pre>

<h2>License</h2>
<p>Giiwa 以 Apache License V2 发布和许可授权，也即你可以任意使用代码， <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache License, Version 2.0</a></p>

<h2>加入我们</h2>
<p>你可以直接加入到我们的Github的giiwa项目中来，贡献你的程序代码。</p>
<p>也可以开源你的基于giiwa的某些模块代码。</p>
<p>也可以向我们报告你发现的问题，或改进建议。</p>
