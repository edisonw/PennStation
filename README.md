# PennStation   [ ![Download](https://api.bintray.com/packages/edisonw/android/PennStation/images/download.svg) ](https://bintray.com/edisonw/android/PennStation/_latestVersion) ![Build Status](https://travis-ci.org/edisonw/PennStation.svg?branch=master)

[PennStation] is an event service that I use as a template to build apps that require IPC.

It supports usages with both EventBus pattern and Rx pattern. 

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

It also supports Rx so that while you can declare your logics in one place and have the benefit of the Rx world, you can also get the best of the Event Bus world. It compliments Rx by isolating the exectuion queues and bounding them away from a particular Activity or Fragment.

While PennStation is very efficient, PennStation calls are more expensive than just launching a thread (especially when used without IPC), so it should be used for async operations but not small (<100ms) tasks such as sorting a local finite list. Those tasks should run via Rx operators. 
 
# Usages

* [Simple Usage]
* [Tumblr Search By Tag Example]
 
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
dependencies {
    apt 'com.edisonwang.ps:ps_processors:{VERSION}'
    compile 'com.edisonwang.ps:ps_lib:{VERSION}'
}
```

* Add the following to your AndroidManifest.xml (or extend a new service class).
```xml
        /* optional: android:process="ps" */
        <service android:exported="false" android:name="com.edisonwang.ps.lib.EventService" /> 
```
* In your custom Application.onCreate() or Activity.onCreate(), add 
```java
        PennStation.init(getApplication(), new PennStation.PennStationOptions(EventService.class /* or extended class */ ));
```

For each action the service needs to perform: 

* Write the actions you want the event service to take. ([Full SampleAction])
* Tag it with @EventProducer with the @Event(s) that it will emit.
* Tag it with @Action so it will be registered.
* E.g: Tag it with @ActionHelper so convenience factories will be created.

For class that owns [event listeners]:

* Annotate with @EventListener with list of producers.
* Write the listeners that listens to those events and XXXEventListener will be generated.
* Implement the listeners and (un)register it via PennStation.registerListener().

Alternatively, you can use it to process Rx streams:
* Add the dependency to your build.gradle 
```java
dependencies {
    compile 'com.edisonwang.ps:ps_rx:{VERSION}'
}
```

To emit a certain type of Event:
```java
 //Generate Observable<SimpleActionEvent> that makes a new request onEach.
    SimpleActionEvent.Rx.from(PsSimpleAction.helper()).subscribe(new Observer<SimpleActionEvent>() {

      @Override
      public void onError(Throwable throwable) {
        //If this action has emitted an event that is Not the target event but also an error. 
      }

      @Override
      public void onNext(SimpleActionEvent event) {
        //When this event is emitted from this action. 
      }
    });
```

To listen for a single event type for a specific action:
```java
SimpleActionEvent.Rx.observable().subscribe(new Subscriber<SimpleActionEvent>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onNext(SimpleActionEvent event) {
                //Do things.
            }
        });
```

To listen for all event types from a specific action:
```java
    SimpleActionObserver.create().subscribe(actionResult -> {  
       //Do something.
    }});
```

[Simple Usage]: https://github.com/edisonw/PennStation/wiki/Simple-Usage
[Tumblr Search By Tag Example]: https://github.com/edisonw/PennStationTumblrDemo
[PennStation]: https://github.com/edisonw/Ipes
[EventBus]: https://github.com/google/guava/wiki/EventBusExplained
[Otto by Square]: http://square.github.io/otto/
[EventBus by greenrobot]: https://github.com/greenrobot/
[implementation code for the action]: https://github.com/edisonw/PennStation/blob/master/sample-app/src/main/java/com/edisonwang/ps/sample/SimpleAction.java
[event listeners]: https://github.com/edisonw/PennStation/blob/master/sample-app/src/main/java/com/edisonwang/ps/sample/SampleActivity.java
[Full SampleAction]: https://github.com/edisonw/PennStation/blob/master/sample-app/src/main/java/com/edisonwang/ps/sample/ComplicatedAction.java
