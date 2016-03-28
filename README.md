# PennStation   [ ![Download](https://api.bintray.com/packages/edisonw/android/PennStation/images/download.svg) ](https://bintray.com/edisonw/android/PennStation/_latestVersion) ![Build Status](https://travis-ci.org/edisonw/PennStation.svg?branch=master)

[PennStation] is an event service that I use as a template to build apps that require IPC.

# Why?

[EventBus] is a great communication pattern on apps and have great library adoptions such as [EventBus by greenrobot] and [Otto by Square] on Android.

However, as applications scale, you write too many boilerplate code, code gets refactored but not cleaned up, and in the rare case do we want inter-process communication, Event Bus pattern falls short because you will be writing boilerplate code for most of your days.

PennStation is an inter-process event service library that supports the following usage pattern:

 * UI process/thread sends a request to a remote service. 
 * Remote service process the request and sends the result back to the requesting process. (can be extended to broadcast to all bounded processes)
 * Requesting process broadcast the event to all listeners in order. 
 * Bundle parceling is enforced cross process boundaries.

And during this entire process....write only the [implementation code for the action] and the [event listeners].

It turns out, it does a great job scaling apps as it uses a Fully Decopuled MVC model so it's great when all the components are agnostic of what the other two components are but only cared about the tasks given to them. 
 
# Usages

* [Simple Usage]
* [Tumblr Search Demo]
 
# Setup

* Add the APT plugin to the file where you declare com.android.tools.build:gradle version. 
```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}
```

* Add the following to your build.gradle for the app module. 
```gradle
apply plugin: 'com.neenbedankt.android-apt'

repositories {
    jcenter()
}
apt {
    processor "com.edisonwang.ps.processors.PennStationProcessor"
}
dependencies {
    apt 'com.edisonwang.ps:ps_processors:1.1.+'
    compile 'com.edisonwang.ps:ps_lib:1.1.+'
}
```

* Add the following to your AndroidManifest.xml (or extend a new service class), you can also add a :process tag to make it run on a separate process.
```xml
        <service android:name="com.edisonwang.ps.lib.EventService" />
```
* In your custom Application class or Activity.onCreate(), add 
```java
        PennStation.init(getApplication(), new PennStation.PennStationOptions(EventService.class /* or extended class */ ));
```

For each action the service needs to perform: 

* Write the actions you want the event service to take. ([Full SampleAction])
* Tag it with @EventProducer with the events it will emit.
* Tag it with @RequestAction so it will be registered.
* Optional: Tag it with @RequestActionHelper so convenience factories will be created.
* Optional: Tag it with @RequestActionHelperFactory so convenience methods will be added.

For class that owns [event listeners]:

* Annotate with @EventListener with list of producers.
* Write the listeners that listens to those events and XXXEventListener will be generated.
* Implement the listeners and (un)register it via PennStation.registerListener().

[Tumblr Search Demo]: https://github.com/edisonw/PennStationTumblrDemo
[Simple Usage]: https://github.com/edisonw/PennStation/wiki/Simple-Usage
[PennStation]: https://github.com/edisonw/Ipes
[EventBus]: https://github.com/google/guava/wiki/EventBusExplained
[Otto by Square]: http://square.github.io/otto/
[EventBus by greenrobot]: https://github.com/greenrobot/
[implementation code for the action]: https://github.com/edisonw/PennStation/blob/master/sample-app/src/main/java/com/edisonwang/ps/sample/SimpleAction.java
[event listeners]: https://github.com/edisonw/PennStation/blob/master/sample-app/src/main/java/com/edisonwang/ps/sample/SampleActivity.java
[Full SampleAction]: https://github.com/edisonw/PennStation/blob/master/sample-app/src/main/java/com/edisonwang/ps/sample/ComplicatedAction.java
