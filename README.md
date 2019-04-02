### 轻量级分布式应用框架 
[https://www.giiwa.org](https://www.giiwa.org)


#### 模块化
* 模块之间可以 *复用*和*重载*。
* 最大化的复用代码，更加容易的构建跨项目之间的模块共用。



#### 快速开发
* 直观简单的程序入口/出口，更容易开发和维护。
* 避免冗重的设计模式，更加快速的构建模块，快速开发和部署。



#### 轻量级分布式
* 并行对等分布式计算节点关系。
* 更容易从一个节点扩展到N个节点，构建可伸缩的分布式并行计算环境。
* 支持 MapReduce， 支持分布式锁， 分布式文件系统。


#### 安装使用
* 下载 [giiwa-1.6.tgz](https://www.giiwa.org/archive/giiwa-1.6.tgz)，或者[giiwa-1.6.zip](https://www.giiwa.org/archive/giiwa-1.6.zip)
* [快速安装](doc/INSTALL.md)
* [入门教程](doc/FIRST.md)


#### 获取代码
所有最新源码已经托管在Github:
> https://github.com/giiwa/giiwa.git

使用 *git clone* 源码仓库 (或者在github的官网上直接克隆)， 你就可以获得Giiwa的全部最新代码。



#### 编译和打包
使用 Gradle 编译, 她会自动编译打包所有依赖包到 giiwa.tgz, giiwa.zip 和升级模块包。
> gradle clean release



#### License
giiwa 支持 [Apache V2](LICENSE-2.0.html) 许可协议。
