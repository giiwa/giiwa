![giiwa](https://www.giisoo.com/images/giiwa.png)

giiwa, Java Web 高性能服务器软件快速开发框架, 我们的目标是 *简单*，<strong>快速开发</strong>, 项目之间可以 <strong>重用</strong>， * “重载”*。

#### 缓存系统，临时文件系统，文件仓库
* 框架本身支持 Memcached，Redis 或直接文件系统缓存。
* 统一管理临时文件系统，能在分布式系统中共享临时文件，同时提供web访问接口，以便在下载中使用临时文件。框架会自动清除临时文件。
* 提供统一的文件仓库管理，以支持分布式系统中共享文件。
* 文件仓库和临时文件系统支持NFS，和Hadoop

#### 数据库系统支持
* 支持同时链接多数据库，高效链接池，同一线程共享一个链接。
* 针对关系型数据库和Mongo，提供统一的API访问，减少移植迁移成本。也提供数据库直接接口以便你使用某些数据库特有的特性。
* 关系型数据库支持 内嵌HSQLDB，和大部分流行数据库。

#### Web接口
* 提供统一请求参数获取APIs:getString, getHtml, getFile, etc. 以应对GET,POST, AJAX, application/json, File， multipart/form-data等对请求数据格式的不同。
* 使得编写应用更简单。

#### 分布式并行计算
* 提供分布式共享锁，多级缓存，分布式文件存储，分布式任务，MapReduce支持。
* 高效分布式任务协调。
* 大型计算任务分片执行。

#### 下载
最新Giiwa运行包 [giiwa-1.6.tgz](https://www.giiwa.org/archive/giiwa-1.6.tgz)，或者[giiwa-1.6.zip](https://www.giiwa.org/archive/giiwa-1.6.zip)。

#### 获取代码
所有最新源码已经托管在Github:
> https://github.com/giiwa/giiwa.git

使用 * git clone * 源码仓库 (或者在github的官网上直接克隆)， 你就可以获得Giiwa的全部最新代码。

#### 编译和打包
使用 Gradle 编译, 她会自动编译打包所有依赖包到 giiwa.tgz, giiwa.zip 和升级模块包。
> gradle clean release


#### License
Giiwa 以 Apache License V2 发布和许可授权，也即你可以任意使用代码， [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
