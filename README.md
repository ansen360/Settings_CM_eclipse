## 导入Settings到eclipse
> Settings路径: cm13.0/repo/packages/apps/Settings
## 一、通过Android.mk文件查找依赖库
```
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES := \
	android-support-v4 \
	android-support-v13 \
	jsr305 \
	org.cyanogenmod.platform.internal \
	libphonenumber

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/opt/setupwizard/navigationbar/common.mk
include frameworks/opt/setupwizard/library/common.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif

```
- 其中LOCAL_STATIC_JAVA_LIBRARIES为引用的静态库(静态库是需要编译进apk的)
> 静态库有: android-support-v4  android-support-v13  jsr305  org.cyanogenmod.platform.internal  libphonenumber
- LOCAL_JAVA_LIBRARIES为非静态库(非静态库是Android系统自带的库)
> 非静态库有: bouncycastle  conscrypt  telephony-common  ims-common
- include frameworks/opt/setupwizard/navigationbar/common.mk
- include frameworks/opt/setupwizard/library/common.mk
- include frameworks/base/packages/SettingsLib/common.mk

**其中setupwizard/navigationbar和setupwizard/library/和SettingsLib是包含res的工程库,因此不能导入jar包**

**从Android.mk文件得出结论是: Settings依赖上述的三个工程(带有res的工程),和静态库:android-support-v4  android-support-v13  jsr305  org.cyanogenmod.platform.internal  libphonenumber;非静态库: bouncycastle  conscrypt  telephony-common  ims-common**

## 二、查找对应的jar和源码
- **导入SettingsLib源码**
> 路径: /cm13.0/repo/frameworks/base/packages/SettingsLib/

(1). 将SettingsLib导入Eclipse,根据import报错,需要framework.jar,通过UserLibraries方式导入framework.jar(同时需要配置优先使用这个库(top一下))如下图
> framework.jar的路径:

>/cm13.0/repo/out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar修改名字即可

![](http://oma689k8f.bkt.clouddn.com/note/1/1.png)
(2). 根据如下报错找到对应源码,然后在/cm13.0/repo/out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/下找到class.jar,修改名字即可
> 报错代码路径: /cm13.0/repo/libcore/luni/src/main/java/libcore/icu

![](http://oma689k8f.bkt.clouddn.com/note/1/2.png)

SettingsLib报错解决,把SettingsLib设置成Is Library

- **导入setupwizard/navigationbar源码**

> 路径: /cm13.0/repo/frameworks/opt/setupwizard/navigationbar

设置Android6.0 SDk,报错解决;设置成Is Library
- **导入setupwizard/library源码**

> 路径: /cm13.0/repo/frameworks/opt/setupwizard/library

(1)这里缺少定义在library/eclair-mr1/res/values/styles.xml和drawable中的的资源文件,将styles.xml中的资源复制到setupwizardlib的styles.xml中,drawable等资源复制到setupwizardlib工程中

(2)发现缺失Theme.AppCompat.NoActionBar报错,全局搜索,发现在/cm13.0/repo3/frameworks/support/v7/appcompat/res/values/themes.xml中定义,导入v7工程(可能需要删除v7工程目录下的.classpath才能正常到导入到eclipse)

(3)根据v7工程的报错导入cm的v4包

设置Android6.0 SDk,报错解决;设置成Is Library

- **Settins引用SettingsLib,navigationbar和library源码**

上述步骤完成后如图:
![](http://oma689k8f.bkt.clouddn.com/note/4/1.png)
- **Settins同上导入framework.jar**

- **导入静态库**
> 在/cm13.0/repo/out/target/common/obj/JAVA_LIBRARIES目录下查找上述的静态库,如下图,将classes.jar修改对应名称之后复制到libs目录下

- **导入动态库**
> 在/cm13.0/repo/out/target/common/obj/JAVA_LIBRARIES目录下查找上述的动态库,如下图,将classes.jar修改对应名称后复制到ext_libs目录下
![](http://oma689k8f.bkt.clouddn.com/note/4/2.png)

**UserLibraries的方式导入ext_libs目录下的jar包**


- **根据报错,缺失EventLogTags.java,在源码从搜索找对该类复制到Settings对应包下**
> 路径:cm13.0/repo/out/target/common/obj/APPS/Settings_intermediates/src/src/com/android/settings

**clean一下,Settings报错全部解决完成**

![](http://oma689k8f.bkt.clouddn.com/note/4/3.png)
## 三.配置ANT编译脚本和系统签名
ANT编译脚本就不配置了,可参考以下博客配置

[Android系统源码framework SystemUI导入eclipse编译](http://blog.csdn.net/qq_25804863/article/details/48669667)


**上述基于CM13,由于cm有依赖自己的库及sdk,导入相对比较麻烦,以上只是提供一个导入的思路(具体导入高通和MTK平台可能报错不一致)**

解决编译报错的Settings的代码地址:
https://github.com/ansen360/Settings_CM_eclipse

[Android系统源码Settings导入eclipse](http://blog.csdn.net/qq_25804863/article/details/48669477)

[Android系统源码framework SystemUI导入eclipse编译](http://blog.csdn.net/qq_25804863/article/details/48669667)



