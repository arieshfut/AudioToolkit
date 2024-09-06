# AudioToolkit

AudioToolkit is a audio tool set to test audio record, play and acoustic etc on android platform.

## 1. 工程说明

AudioToolkit是音频工具集apk，apk主要包含4个界面对应工具集的4个模块，用于验证Android终端设备的音频基本功能；

## 2. 相关说明

### 2.1 下载安装说明

 可直接下载apk [DownLoad](./docs/apk), 详细下载说明参见[AudioToolkit使用说明](./docs/apk/README.md)

### 2.2 使用说明

AudioToolkit是音频工具集apk，apk主要包含4个界面对应工具集的4个模块，可通过右上角切换对应模块，如下图示

   ![img](https://github.com/arieshfut/AudioToolkit/blob/main/docs/imgs/basic_ui.png)

1. 基本功能：主要包含常见的音频采集播放功能、音量、音频设备、蓝牙、共享音频等等；

   ![img](https://github.com/arieshfut/AudioToolkit/blob/main/docs/imgs/first_fragment.png)

2. 预研功能：主要包含oboe采集播放、alsa采集、听筒检测等调研功能；

   ![img](https://github.com/arieshfut/AudioToolkit/blob/main/docs/imgs/pre_research.png)

3. 声学测试：主要包含音频声学相关测试流程或者功能，比如AEC测试等；

   ![img](https://github.com/arieshfut/AudioToolkit/blob/main/docs/imgs/account_test.png)

4. 使用说明：版权声明；

除此外，该apk同样适配了Android的高版本、低版本如Android4.4

 常见使用场景：
 1. 采集系统音频数据
 
    1.1 采集音频数据：配置音频模式和录音参数，打开“是否录音”开关，点击开始测试，即可录音，录音文件路径见UI提示；
	
	1.2 开关3A算法：录音前切换“内置3A算法”只是将算法开关参数保存，等到录音启动时才会真的生效，是否生效见界面的内置3A状态。录音过程中切换“内置3A算法”，仅切换On或Off时才能有效，且选择后会立刻生效；
	
 2. 播放音频
 
    2.1 播放音频：设置播放音频参数，打开“播放音频”，点击开始测试即可，目前不支持播放数据手动选择路径，是使用的apk自带语料；
	
	2.2 同时采集和播放：设置好采集和播放音频参数后，打开“是否录音”和“播放音频”开关，点击开始测试，即可同时录音和播放；
	
 3. 采集播放与共享音频
 
    3.1 同时采集和共享音频：打开“是否录音”和“共享音频”开关，点击开始测试即可；

 4. 预研功能测试
 
     目前仅支持alsa采集功能，该功能在root系统版本中可通过alsa，针对特定的设备节点，采集对应格式的音频数据
	 
 5. 音频声学测试

     AEC测试和声学测试暂未实现，待后续补充功能。

## 3. 开发说明

作者Android studio版本信息

   ![img](https://github.com/arieshfut/AudioToolkit/blob/main/docs/imgs/as_version.png)

Android Studio加载工程：
* Android studio通过settings.gradle加载当前工程；
* 直接使用Android studio的debug功能进行调试；
* 编译生成的apk，安装到用户Android设备上；
* 打开apk选择对应的界面，进行测试和debug等；

代码路径说明：
1. 基本UI布局：AudioToolkit\app\src\main\java\com\aries\audiotools
2. 音频基本功能：AudioToolkit\app\src\main\java\com\aries\audiotools\AudioModule
3. 音频预研功能：AudioToolkit\app\src\main\java\com\aries\audiotools\PreResearch
4. 音频声学功能：AudioToolkit\app\src\main\java\com\aries\audiotools\AcousticModule
5. Native C++模块：AudioToolkit\app\src\main\cpp


## 4. 联系我们

 当遇到如下问题时，请联系我们。
- 无法满足你的使用要求时；
  - 当前最新apk没有某种功能供你使用时，请联系我们进行添加实现；
  - 当前最新apk存在某种功能满足你的需求，但是你从使用说明查不到如何使用时，请联系我们更新使用说明；
  - 当前最新apk在使用过程中无法正常工作，出现了卡死或者异常闪退时，请邮件联系我们(在邮件中描述bug出现的步骤或者附带异常日志)；
  - 其他的使用问题；
- 无法满足你的开发需求时；
  - 当你定制开发代码时，针对现有代码框架有疑惑时，请联系我们；
  - 当定制开发功能，但是不知道如何实现想咨询时，请联系我们；
  - 其他的开发问题；
- 其他问题;

 #### 问题反馈方式一：
    e-mail: 602488125@qq.com
    说明：请在邮件中详细描述你的需求和问题说明，非常感谢；
#### 问题反馈方式二：
    通过https://github.com/arieshfut/AudioToolkit/issues创建new issue反馈给我们；
