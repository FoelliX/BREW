![Java 17](https://img.shields.io/badge/java-17-brightgreen.svg) ![Maven 3.8.1](https://img.shields.io/badge/maven-3.8.1-brightgreen.svg)
---
<p align="center">
	<img src="https://FoelliX.github.io/BREW/logo.png" width="300px"/>
</p>

# BREW
The Benchmark Refinement and Execution Wizard (BREW) can be used to do what the name suggests, first refine and then execute a benchmark.
For the refinement step it guides the user through 3 steps, all adding additional and typically missing information to a benchmark.
<p align="center">
	<a href="screens.png" target="_blank"><img src="screens.png" /></a>
</p>
The benchmark execution can be started directly via the GUI shown above or via command line.

## Usage
Our wiki contains [tutorials](https://github.com/FoelliX/BREW/wiki) on how to use BREW.
However, in order to execute benchmarks the underlying [AQL-System](https://github.com/FoelliX/AQL-System) must be configured.
A tutorial that explains the configuration process can be found: [here](https://github.com/FoelliX/AQL-System/wiki/Configuration).

### Execution
In general BREW can be started with the following command ([Launch parameters](https://github.com/FoelliX/BREW/wiki/Launch_parameters)):  
```bash
java -jar BREW-2.0.0.jar
```
and build by:
```bash
cd /path/to/BREW
mvn
```

### Video Tutorial
[![Video](https://FoelliX.de/videos/tutorials/AQL/splash.png)](https://FoelliX.de/videos/tutorials/BREW/video_00.mp4)

**Material:**
- AQL-System [[Link]](https://github.com/FoelliX/AQL-System)
- Configuration Wiki Page [[Link]](https://github.com/FoelliX/AQL-System/wiki/Configuration)
- Configuration Tutorial Video [[Link]](https://github.com/FoelliX/AQL-System/wiki/Video_tutorials#video-00-configuring-an-aql-system)
- AQL-WebService [[Github]](https://github.com/FoelliX/AQL-WebService)
	- Online status [[Link]](http://vm-fpauck.cs.upb.de/AQL-WebService)
	- Credentials (free account - limited query depth and number of queries per day):
		- URL: `http://vm-fpauck.cs.upb.de/AQL-WebService/config`
		- Username: `free`
		- Password: *blank* (no password required)
	- Credentials (private account):
		- Contact FoelliX [[Mail]](mailto:aql-private-account@FoelliX.de)
- Android platform files [[Download]](https://github.com/Sable/android-platforms)
- DroidBench app `DirectLeak1.apk` [[Download]](https://github.com/secure-software-engineering/DroidBench/blob/develop/apk/AndroidSpecific/DirectLeak1.apk)


## Publications
- *Do Android Taint Analysis Tools Keep Their Promises?* (Felix Pauck, Eric Bodden, Heike Wehrheim)  
ESEC/FSE 2018 [https://dl.acm.org/citation.cfm?id=3236029](https://dl.acm.org/citation.cfm?id=3236029)
- *Together Strong: Cooperative Android App Analysis* (Felix Pauck, Heike Wehrheim)  
ESEC/FSE 2019 [https://dl.acm.org/citation.cfm?id=3338915](https://dl.acm.org/citation.cfm?id=3338915)
- *TaintBench: Automatic real-world malware benchmarking of Android taint analyses* (Linghui Luo, Felix Pauck, ...)  
EMSE 2022 [https://link.springer.com/article/10.1007%2Fs10664-021-10013-5](https://link.springer.com/article/10.1007%2Fs10664-021-10013-5)

## License
BREW is licensed under the *GNU General Public License v3* (see [LICENSE](https://github.com/FoelliX/AQL-System/blob/master/LICENSE)).

# Contact
**Felix Pauck** (FoelliX)  
Paderborn University  
fpauck@mail.uni-paderborn.de  
[http://www.FelixPauck.de](http://www.FelixPauck.de)

# Links
- BREW is part of the ReproDroid toolchain: [https://github.com/FoelliX/ReproDroid](https://github.com/FoelliX/ReproDroid)
- and internally uses the AQL-System: [https://github.com/FoelliX/AQL-System](https://github.com/FoelliX/AQL-System)