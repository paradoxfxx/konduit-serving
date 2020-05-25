# Konduit Serving Packaging System

## Status
PROPOSED

Proposed by: Alex Black (25-05-2020)

Discussed with: 

## Context

Konduit Serving is a complex modular software package intended to be deployed in a number of different configurations, in multiple packaging formats.

For any given model/pipeline, the deployment/packaging scenarios can vary widely. For example, a user might want to deploy a Konduit Serving TensorFlow model in one of these configurations (and many more):
* Docker image packaging using TensorFlow + CUDA 10.1 on an Linux ARM64 system, with serving via HTTP/REST
* A self-contained .exe (with embedded JVM) using SameDiff TensorFlow import to run the model on CPU, on a Windows x86 + AVX2 system with Intel MKL + MKLDNN (OneDNN) included, with serving being performed via gRPC


Currently, packaging for Konduit Serving is done via Maven profiles and Maven modules.
A user selects the combination of dopendencies and functionality they need by enabling a number of profiles and system properties.
For example, building a Windows CPU uber JAR looks something like this:
```
mvn clean package -DskipTests -Puberjar -Pcpu -Ppython -Pnative -Ppmml -Ptensorflow -Dchip=cpu -Djavacpp.platform=windows-x86_64
```
The other packaging options are executed by adding different profiles.

This approach has got us quite far in terms of packaging (enabling flexible packaging options including uber-JARs, Docker, WARs, DEB/RPMs, tar files and .exe files), we are running up against the limits of this approach.

Specifically, this approach has the following problems:
* The combination of options available to users is only going to continue to grow (too many profiles and combinations for devs/users)
* Some combinations are difficult or impossible using just profiles and properties (for example, building a binary for both Windows and Linux, but not Mac or PPC etc)
* It is easy to leave performance on the table - i.e., using ND4J/SameDiff/TensorFlow etc binaries built without AVX support
* Many incompatibilities will only become apparent at runtime (example: build for a CUDA version only to find that TensorFlow only releases one CUDA version and hence we have a runtime problem)
* Now (with the Data/API rewrite) configuration and execution is separate; the one configuration can be run many different ways. For example, a TensorFlow model could be run with TensorFlow, SameDiff, TVM, or (possibly automated) conversion ONNX, etc. This will be challenging to support via a "profiles and properties" build approach.
* Usability issues: For example, users need to know a lot about the different profiles, configuration, etc to get an optimal (or even functional) deployment - or even know what is possible.
    - An example of this: the user might build an uber-JAR without the PMML profile being enabled, only to discover their JAR can't run their pipeline (that has a PMML model)
* Packaging of custom code, dependencies and other assets (inc. model) is difficult


## Proposal

The scope of this proposal is limited to the creation/packaging of a Konduit Serving uberjar, which may be deployed in many forms (Docker, RPM, WAR, etc)
Note that non-Java packaging/deployments of pipelines is out of scope (i.e., deploy a pure C++ binary); OSGi support is relevant but only in scope to the extent that an OSGi-based system could work with (or build on top of) the functionality described in this proposal.

**Proposal Goals*

The goals of this packaging proposal are as follows:
1. To retain and enhance the existing deployment options - uber-jar, docker, WAR, .exe, etc
2. Enable greater flexibility in the build/deployment configuration
3. To enable custom Java and Python code (and dependencies) to be easily included in a deployment
4. To improve usability and reliability of packaging, in the following ways
    - Remove the reliance on Maven profiles and properties (at least as the only option)
    - Automate the selection (or recommendation) of modules to include for a given pipeline (i.e., look at pipeline config, find what's necessary/useful to include)
    - Add validation and checking for common pitfalls such as dependency issues (incompatible with CPU architecture, wrong CUDA version, etc)
    - Make it clear to the user what requirements (in terms of hardware and software), if any, need to be satisfied on the deployment system (requires CUDA 10.1, Java 8+, etc)

**Proposal Overview**

This proposal has a number of parts:
1. A build tool (on top of Maven) that utilizes a configuration format to actually perform the required build
2. A Konduit Serving build configuration format
3. UI and command line tools for creating a build configuration for a given Pipeline configuration (and then if necessary triggering a build based on the generated build configuration file)
4. A system for packaging custom Java code and dependencies

Note that for usability, where possible we'll make it so the user doesn't have to be aware of the build configuration file - for example, a simple CLI might be used to configure and execute a build. The CLI would generate the configuration, and pass it to the build tool, without the user being aware of the configuration file.
However, for advanced users and use cases (such as system administrators, devops, etc) we will allow the configuration file to be written or modified directly.


### Part 1 - Build Tool

Given a configuration file that specifies what should be included in the build (details later), the build tool will execute the build/s necessary to create the requested artifacts (JAR/s, docker images, etc).
Note that the term "build tool" may not be an ideal name, as the proposed tool is simply a thin layer on top of Maven - and is not comparable to a "true" build tool like Maven, Gradle, Ant, etc.

Note also that in principle (though this is not proposed for right now) we can have multiple build tools for creating the final artifacts from these  - i.e., the configuration (definition) and the build tool (build execution) are separate.
Until (if) we look at pure C++ deployments, the main possible use for a second build tool would be for OSGi-based deployments. However, this would still be Maven based.

The proposed build tool will generate (and then execute via Maven) a pom.xml file based on the configuration file.
Similar to the current "modules and profiles" approach, we will continue to use Maven plugins for the actual packaging - i.e., creation of uberjars, etc.

This generated pom.xml file will include:
* A `<dependencies>` section, listing the direct dependencies:
    * The required konduit serving modules - konduit-serving-tensorflow, konduit-serving-nd4j, etc
    * Any "native library" / "backend" dependencies (ND4J native/CUDA backends, for example)
    * Logging etc dependencies
* If necessary, a `<dependencyManagement>` section
* A simple `<properties>` section, for the source encoding (UTF-8) and Java version
* A `<build><plugins>` section
    * Always included plugins for tasks such as enforcing dependency convergence
    * One or more plugins for each deployment type. For example, maven-shade-plugin for building uber-jars, and dockerfile-maven-plugin for building docker images

One consequence is that all of the "packaging" modules would be removed, in favor of a single `konduit-serving-build` module.

In the future, we will likely allow the build tool to create multiple different artifacts based on one configuration file - i.e., one uberjar for each of a users' target platforms (for example 3 JAR files, one for each of Linux x86, Linux armhf, Windows x86).


An alternative design would be to attempt to use profiles and properties, however this seems much less flexible and harder to understand/maintain especially when things go wrong.

### Part 2 - Configuration File

The configuration file should provide information necessary to determine for the build (via generated pom.xml file), the set of:
- direct dependencies
- plugins
- properties and profiles


To that end, the following information will be included as part of the configuration:
* The Konduit Serving modules to include
* The Konduit Serving version (optional, and defaults to latest if not specified)
* The deployment packaging type(s) - Uber-JAR, Docker, etc - and their associated configuration
* The deployment target(s) - OS, architecture, CPU vs. GPU, etc
* Selected or preferred pipeline step runners (where more than one option exists for one of the included pipeline steps)
* Information necessary to package any required external/custom code, dependencies, files, resources, etc
* Any additional dependency configuration or overrides (such as dependency management, exclusions, etc)
* Metadata such as timestamps, comments, author, etc

JSON/YAML is proposed to be used as the format for the build configuration files.


### Part 3 - CLI

A CLI build tool will be one way for users to configure and build their required deployment artifacts (uber-jar, docker images, etc). Internally (usually without the user being aware), the CLI tool will create a build configuration and pass it to the build tool for execution.

Two modes of operation are proposed for the CLI:
1. Command line style
2. "Wizard" style

The command line style will provide the information necessary to produce the configuration file in a short form. The exact configuration and options will be designed in more detail later, but it will likely look something like the following:
```bash
konduit-build myPipeline.json --modules tensorflow,nd4j,image --deploy docker --docker.config "name=x,version=y" --incudeJava "com.company:mylibrary:1.0.0"
```

The Wizard style of CLI use will guide users through selecting the options for their pipeline. This will be implementation after the "command line" style of use.
Again the specifics of the design need to be worked out, but it is suggested that usage will look something like the following

```
> konduit-build
Konduit Serving build tool, v0.2.1
Enter path to Pipeline .json or .yml file (or ctrl+c to exit)

> myFile.yml

Select deployment environment OS (comma or space delimited, case insensitive)
Options:
l = Linux x86-64
w = Windows x86-64
m = Mac OSX x86-64
lahf = Linux ARM - armhf
la64 = Linux ARM - arm64
...

> l,la64

...
```

The "wizard" style would then output (a) the "command line style" command line for what they entered, and optionally (b) the configuration file; it would then execute the build based on the configuration.


### Part 4 - Build UI

The Build UI would be a simple, single-page UI (nothing fancy or feature rich in the near term) that focused on doing three things:  
(a) Guiding users through the configuration process for their pipeline  
    The main goal here is to show the user what the required modules are for serving their pipeline, and the options they have for customing the deployment (target platform, selected model runner, configure each step, etc)  
(b) Creating the configuration file (though this would be implemented in the back-end based on what the user selects via the UI)  
(c) Triggering the build based on the generated configuration file  

Users should be able to load a previously-created build configuration file (partially or completely specified) as a starting point for their pipeline build.  


At a later date, we may add a way to visualize and create pipeline configurations using this UI also. If we were to look at that, it would be after a separate ADR has been proposed and accepted.


Starting and stopping the build UI should be straightforward (assuming the user has Konduit Python package or similar installed):
```
> konduit-build-ui

Konduit Serving build UI launched at: https://localhost:9123/
Use ctrl+c to exit
```

The UI workflow would for the user would be something like:
1. Launch the konduit-build-ui
2. Select the Konduit Serving pipeline to deploy (later: allow "generic TF model" and similar selections instead of providing a pipeline configuration)
3. Select the deployment environment(s) - OS, CPU architecture, CPU vs. GPU, AVX support or not, etc (later: device profiles)
4. Select the pipeline step runners (if multiple are available)  
   Example: when running a TensorFlow model - whether to use TF, SameDiff, TVM, etc to run the model
5. Optionally add custom Java code, Python code, and dependencies  
   Again, Java code/dependencies will be as simple as specifying the GAV coordinates of the user's project.  
   Python packaging and dependencies is TBD, but may be something like a directory + a requirements.txt
6. Optionally, embed files/resources (including the model file if required)
7. Select packaging (uberjar, docker, exe, etc)  
   Each selected option should then show configuration relevant to that packaging
8. Click "verify" to check all options and produce a final report
   This would check dependencies, estimate final file size, etc
9. If necessary, prompt the user for any things they need to explicitly approve (for example, if necessary, accepting licenses for any 3rd party software to be bundled)
10. Click "Build" to execute the build, which would pass the configuration to the build tool to create the final artifacts (uber-jar(s), docker images, etc)

At any point the user would be able to save the current configuration as a YAML/JSON file (and load it back in later).


At each stage, we would only allow the user to select options that are consistent with previous choices (with other options still visible but grayed out).


For step 2, regarding the "device profiles" idea - these would allow users to select things like ("Raspberry pi 4B", "Jetson Nano", "Generic Linux x86_64", and possibly even common cloud VMs) to reduce the amount of knowledge/configuration required to create the pipeline.


### Part 5 - Pipeline Analysis and Module Selection

An important component of both the CLI and UI would be determining what Konduit Serving modules need to be included to execute a pipeline - and what options are available (i.e., which runners could be used to execute the steps contained within).

In the near-term, we could add something to (semi-automatically) track/aggregate execution support/capabilities across all modules - i.e., we'd build a mapping between module names (or rather, PipelineStepRunners) and the PipelineStep (configurations) they can run.
A basic version should not be especially difficult, with the idea that we would encode information like: "SameDiffPipelineStepRunner in module konduit-serving-samediff can execute steps of type 'TensorFlowPipelineStep'".
A more advanced version that actually checks the configuration would be added at a later date (i.e., SameDiffPipelineStepRunner can run _most_ but not _all_ TensorFlow models - so we'll check this at configuration time).


One thing to keep in mind is extensibility - for example, we might have custom pipeline steps available via a "Konduit Serving Hub" - code/dependencies for these custom pipeline steps could be pulled in automatically. However this should not substantially alter the basic approach for doing analysis/module selection.



### Part 6: Custom/External Java Code and Dependencies

In some pipelines, a user will want to write custom Java code - for example custom pipeline steps, custom metrics, etc.
Some of this custom Java code will have dependencies (direct and transitive) that also need to be included in the built JAR.
We need an easy system for including both the code and the dependencies in a Konduit Serving pipeline.

For Java, the proposal for handling this is trivially simple:
1. Users package their code as a standard Maven project
2. Users `mvn install` their project, including their custom code, to their local Maven repository  
   Note this need not be an uber-JAR; a standard module/JAR with direct and transitive dependencies is fine
3. Users specify the GAV coordinate (group ID, artifact ID and version) for their custom functionality in the configuration file (or more likely, via the CLI/UI)

An additional (or possible alternative) mechanism that could be added later, would be to provide a way of building a GitHub repository.
i.e., "clone, install, add to Konduit Serving deployment" would be doable in just a couple of lines of configuration. This could be useful for CI/CD based pipelines.

This "install and provide GAV" approach should also work fine for OSGi-based builds/deployments in the future.

### Future ADRs

There are a number of aspects of this packaging system that would need to be worked out in future ADRs.
ADRs may or may not need to be produced for the following components:
* The configuration format
* UI design
* Custom Python code (and dependency) embedding
* Architecture compatibility checking for dependencies with native code (i.e., "if I include dependency X, will it actually work on ARM64, PPC64le, etc?")
* File/resource embedding (and usability isuses - how do user pipelines access these embedded files?)


## Consequences 

### Advantages

* We get a flexible and powerful build system that should enable most/all of our Java-based packaging needs
    - Including improved configuration options for users
* Improved build reliability via compatibility checks built into the system (move problems from run time to build/configuration time)
* Improved usability via guiding users through available and compatible options (via CLI or UI)
* Easier debugging of builds (we can see the exact generated standalone pom.xml - no need to work backwards when something goes wrong to try and figure out exactly what was included from where)
  
### Disadvantages

* Some checks will be difficult to implement, and may not be possible to always perform reliably
    - For example: does arbitrary Python library X work on ARM64?
* Adds yet another configuration file/format for (some) users to know about and learn

## Discussion
