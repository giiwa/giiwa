# The module is base on giiwa framework
Please refer http://giiwa.org to learn more about giiwa

#### 功能列表
* 1) feature1
* 2) feature2

#### 编译和打包
使用 Gradle 7.2 编译打包

生成Eclipse环境
> gradle eclipse

测试包依赖关系
> gradle :dependencyInsight --configuration runtimeClasspath --dependency commons-codec:commons-codec

生成GIIWA模块包: build/xxx_version_buildno.zip。
> gradle clean release -x test
