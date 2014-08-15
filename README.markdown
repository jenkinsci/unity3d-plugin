A Unity3d Jenkins Plugin
========================

Overview
--------

[Jenkins](http://www.jenkins-ci.org/) is a continuous integration server. [Unity3d](http://unity3d.com/) is a powerful 3d game creation editor and engine that runs on Mac and Windows.

Automating Unity3d builds from the command line is [possible](http://unity3d.com/support/documentation/Manual/Command%20Line%20Arguments.html). There are a few problems though:

* the unity runner writes its output to a separate log file, instead of the output
* tool and file locations are platform specific
* only one project can be built at a time per machine

This plugin aims to make it easier to run Unity3d builds easily in Jenkins, by adding the following features:

* log file redirection

More to come...

The plugin was tested with unity3d 3.4.2 and unity3d 3.5 beta. Tested on distributed and single server environments

Design
------

The Unity3dBuilder is the point of entry in the plugin. It creates a command that takes the command line parameter from your configuration and passes it to the launcher for execution on the targeted environment. The specified UnityInstallation is used.

For piping of the logFile output, 3 elements are created:
* a  org.jenkinsci.plugins.unity3d.io.Pipe consisting of a PipedInputStream and a PipedOutputStream. This outputstream is wrapped into a RemoteOutputStream if the launcher is to be executed remotely.
* a long running Async Callable is passed to the launcher channel. This closure, a PipeFileAfterModificationAction instance, detects that the log file is started being written and copies from it recursively into the pipe until interrupted. It then closes the output
* a org.jenkinsci.plugins.unity3d.io.StreamCopyThread reads from the pipe and copies it into the job console.

License
-------

MIT

Building
--------

If you wish to build from source

mvn install

Installing
----------

Follow (https://wiki.jenkins-ci.org/display/JENKINS/Plugins)

Configuration
-------------

On the node you will run Unity, add a Unity3d Installation and configure the installation path.
Then in the project you will run Unity, as a Unity3d build step, and add follow the inline help to configure it.
Because unity editor will not run properly if you run multiple builds, it is advised to take guards.
One simple albeit restrictive solution is to use a single executor on your unity build servers.

Links
-----

* for those using Teamcity, use https://github.com/mcmarkb/Teamcity-unity3d-build-runner-plugin

Contact
-------

jerome.lacoste@gmail.com

Pull requests appreciated !
