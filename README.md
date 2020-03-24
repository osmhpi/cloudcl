# CloudCL

CloudCL is an idea originating from a master's thesis that tries to solve the problem of computing a great workload on multiple machines within a network in order to receive a speedup. Most prominent existing solutions have one or multiple of the following shortcomings:

* Complicated distribution code (e.g. MPI) in addition to accelerator-specific code (e.g. OpenCL, CUDA)
* It is complicated to use all available resources (CPUs *and* GPUs)
* Maintaining a compute cluster is complex and expensive

CloudCL tries to work around these problems in the following way:

## Utilizing CPUs and GPUs

CloudCL is based on OpenCL, which allows to write code that runs in heterogeneous environments without any modifications (not a basic rule but generally possible). That means your code that runs on your Intel CPU may easily run on an AMD or NVIDIA GPU or even on an FPGA. Terrific!

## Easy Cluster Setup

If you think about distributing computation you might easily think about Hadoop MapReduce and likeminded frameworks. While these frameworks are being used in production around the world, building a cluster for them is not a trivial process. Additionally, these frameworks are usually focused on CPUs and bringing in GPUs to the equation may lead to some problems. Instead, CloudCL makes use of a library called dOpenCL (Distributed OpenCL), which forwards OpenCL commands within the network to remote machines. Through this API forwarding technique regular OpenCL code can easily be executed on remote machines without any changes. The only thing required are a running version of dOpenCL on the host as well as very lightweight dOpenCL daemons on the remote machines that execute the commands. Fantastic!

## Easy Distribution

Have you ever written OpenCL, OpenMP, MPI or any other code that distributes a task among multiple cores or even machines? Probably you have encountered some problems with it, except if you are some kind of prodigy. Distributing a task to multiple cores of a CPU can already be quite challenging due to synchronization, memory access etc. Fortunately OpenCL is built exactly for such a use case and therefore simplifies such a mechanism through a programming paradigma. Working with low level OpenCL code mandates you to write your code in C or C++. The APIs can seem quite intimidating to new users and include a steep learning curve. Now imagine adding some code to distribute your task across multiple machines within the network. Sounds quite complicated, right? CloudCL takes this burden away by integrating Aparapi and adding its own distribution framework on top of it. Aparapi allows programmers to write their OpenCL code in Java and abstracts all these low level OpenCL API calls away. The only thing that users have to take care of is to write their Java code and make sure that their big task is split into smaller subtasks that can be computed by the various available devices within the network.

## Quickstart

Build Aparapi:

```
cd aparapi
gradle build
cd ..
```

Add the Aparapi native library path to `LD_LIBRARY_PATH` (If you use an IDE, make sure this definition also applies there!):

```
# For Bash-like shells
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:+${LD_LIBRARY_PATH}:}$(realpath aparapi/com.amd.aparapi.jni/dist)"
# For Fish-like shells
set -gx LD_LIBRARY_PATH $LD_LIBRARY_PATH (realpath aparapi/com.amd.aparapi.jni/dist)
```

Build dOpenCL:
```
gradle build
```

NOTE: The dOpenCL build process will automatically run the unit tests, which require a working OpenCL enviornment. If you are building CloudCL for use in an OpenCL-incabable environment, you can safely skip the tests with `gradle build -x test`.
