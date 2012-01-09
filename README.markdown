A Unity3d Jenkins Plugin
========================

Overview
--------

[Jenkins](http://www.jenkins-ci.org/) is a continuous integration server. [Unity3d](http://unity3d.com/) is a powerful 3d game creation editor and engine that runs on Mac and Windows.

Automating Unity3d builds from the command line is [possible](http://unity3d.com/support/documentation/Manual/Command%20Line%20Arguments.html). There are a few problems though:

* the unity runner writes its output to a separate log file, instead of the output
* tool and file locations are platform specific
* only one project can be built at a time per machine

This plugin aims to make it easier to run Unity3d builds easily in Jenkins, by adding the following features

* log file redirection

More to come...

The plugin was tested with unity3d 3.4.2 and unity3d 3.5 beta. Tested on distributed and single server environments


Installing
----------

Follow (https://wiki.jenkins-ci.org/display/JENKINS/Plugins)

Building
--------

mvn install

Links
-----

* (https://github.com/mcmarkb/Teamcity-unity3d-build-runner-plugin)

