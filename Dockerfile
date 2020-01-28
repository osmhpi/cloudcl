FROM mplauth/dopencl
WORKDIR /tmp/

RUN DEBIAN_FRONTEND=noninteractive apt-get update \
    && apt-get install --yes --no-install-recommends \
        openjdk-8-jdk \
        ant \
        openjdk-11-jre-headless-

# aparapi
RUN git clone https://github.com/osmhpi/aparapi /tmp/aparapi --recursive \
    && cd /tmp/aparapi \
    && ant dist

FROM mplauth/dopencl
WORKDIR /tmp/
COPY --from=0 /tmp/aparapi/dist_linux_x86_64 /tmp/aparapi

# Install dependencies
RUN DEBIAN_FRONTEND=noninteractive apt-get update \
    && apt-get install --yes --no-install-recommends \
        fontconfig \
        unzip \
        wget \
        bzr \
        git \
        git-lfs \
        mercurial \
        openssh-client \
        subversion \
        openjdk-8-jdk \
        vim \
        libpocl2 \
        pocl-opencl-icd
#   && rm -rf /var/lib/apt/lists/*

ENV GRADLE_HOME /opt/gradle
ENV GRADLE_VERSION 6.0.1
ARG GRADLE_DOWNLOAD_SHA256=d364b7098b9f2e58579a3603dc0a12a1991353ac58ed339316e6762b21efba44

RUN set -o errexit -o nounset \
    && echo "Downloading Gradle" \
    && wget --no-verbose --output-document=gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    \
    && echo "Checking download hash" \
    && echo "${GRADLE_DOWNLOAD_SHA256} *gradle.zip" | sha256sum --check - \
    \
    && echo "Installing Gradle" \
    && unzip gradle.zip \
    && rm gradle.zip \
    && mv "gradle-${GRADLE_VERSION}" "${GRADLE_HOME}/" \
    && ln --symbolic "${GRADLE_HOME}/bin/gradle" /usr/bin/gradle \
    \
    && echo "Testing Gradle installation" \
    && gradle --version



ENV LD_LIBRARY_PATH=/tmp/aparapi:$LD_LIBRARY_PATH
RUN echo 127.0.0.1 > /tmp/nodes.dcl
ENV DCL_NODE_FILE=/tmp/nodes.dcl 
#ENV LD_PRELOAD=/usr/local/lib/libdOpenCL.so
#ENTRYPOINT LD_PRELOAD="" dcld --hostname 127.0.0.1 &


# cloudcl
RUN git clone https://github.com/osmhpi/cloudcl /tmp/cloudcl --recursive \
    && cd /tmp/cloudcl \
    && rm -Rf aparapi \
#    gradle build

