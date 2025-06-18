### 后端项目所需环境
Java17 + Springboot3.3.8 + redis 3.2.1+ mysql 8.0

需要自行下载java17、redis和mysql

### 后端项目启动 
1. 导入数据库：将大作业文件的根目录中的sky-take-out.sql导入到自己的mysql数据库
2. 打开项目
    1. 使用IDEA等IDE打开根目录下sky-take-out-ai文件，等待maven引入依赖。
    2. 或者使用git克隆 [https://github.com/Yuweiwei1001/sky-take-out-ai.git](https://github.com/Yuweiwei1001/sky-take-out-ai.git)
3. 打开sky-server/src/main/resources/application-dev.yml,按需修改相关配置：
    1. mysql数据库相关配置，比如用户名、密码等
    2. alioss相关配置，用于图片存储，由于我的alioss已到期，所以可能无法正常显示图片，可以不修改，只影响图片的显示。
    3. redis配置：我的redis没有设置密码，如果你的redis设置了密码，修改为自己的密码，并且在同路径下的application.yml将redis的password属性取消注释。
    4. 百炼大模型dashscope配置：需要在阿里百炼大模型平台创建自己的api-key，并修改为自己的api-key，默认为作者我的api-key，每天50w的免费token，大模型默认使用的是qwen3模型。
    5. 微信支付配置： 暂不涉及，无需修改。
3. 启动redis-server服务
4. 启动项目中SkyApplication的启动类
5. 如果没有意外，项目应该正常启动

### 前端项目所需环境
vue2 + typescript+node14+npm6.14.18

node14需自行下载，建议使用nvm，方便进行node版本管理 

### 前端项目启动
1. 使用VScode等IDE打开根目录下的project-sky-admin-vue-ts文件
2. 运行依赖脚本，分批安装依赖保证依赖能成功安装，脚本已在项目文件下中：
    1. 在项目目录右键选择 "Git Bash Here"或在 VS Code 终端中选择 Git Bash
    2. 输入命令，注：要在项目根目录下运行此命令   
`chmod +x install-all-deps.sh  
./install-all-deps.sh`
3. 终端输入命令 npm run serve 启动前端项目
4. 如何没有意外，项目应该正常启动，前端地址[http://localhost:8888/](http://localhost:8888/)



现在，你应该可以正常使用本系统了。

