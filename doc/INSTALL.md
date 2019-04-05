### 安装配置

#### 安装JDK
* [安装JDK 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* 配置环境

#### 安装数据库
* 使用内置数据库
* 安装 [Mysql](https://dev.mysql.com/downloads/os-linux.html), [Postgresql](https://www.postgresql.org/download/), [Oracle](https://www.oracle.com/database/) 数据库 *(可选)*
* 安装 [Monogo](https://www.mongodb.com/download-center/community) 数据库 *(可选)*

#### 安装 giiwa
* [下载giiwa](https://www.giiwa.org/home/)
* 直接解压 giiwa
> tar xzf giiwa-1.6.tgz -C giiwa
* 启动 giiwa
> giiwa.sh start

#### 配置 giiwa
* 使用浏览器访问 http://[ip]:8080/, 能看到正常界面说明已经安装成功，点击“控制面板”进行配置，按照步骤配置数据库（只需一个，其他可以忽略）。
* 初始系统密码，如果使用的新数据库，则系统会创建一个初始系统维护用户root，密码则保存在系统安装目录root.pwd， 使用完后，请删除该文件。
* 如果使用的旧数据库，则依然会使用旧数据库中的维护账号。
