FROM ubuntu:20.04


ENV USER_NAME masbench
ENV UID 1000
RUN useradd -m -u ${UID} ${USER_NAME}
ENV HOME /home/${USER_NAME}


ENV DEBIAN_FRONTEND=noninteractive
WORKDIR ${HOME}
ENV PYENV_ROOT ${HOME}/.pyenv
ENV PATH $PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH
# Update packages
RUN apt-get update && \
    apt-get upgrade -y

# Install command
RUN set -x && \
    apt-get install -y build-essential && \
    apt-get install -y libffi-dev && \
    apt-get install -y libssl-dev && \
    apt-get install -y zlib1g-dev && \
    apt-get install -y libbz2-dev && \
    apt-get install -y libreadline-dev && \
    apt-get install -y libsqlite3-dev && \
    apt-get install -y wget && \
    apt-get install -y sudo && \
    apt-get install -y zip && \
    apt-get install -y git && \
    apt-get install -y gradle && \
    apt-get install -y vim && \
    apt-get install -y imagemagick && \
    apt-get install -y curl && \
    apt-get install -y openjdk-11-jdk && \
    apt-get install -y ruby
# Install python packages
RUN git clone https://github.com/pyenv/pyenv.git .pyenv && \
    curl -sL https://deb.nodesource.com/setup_12.x | bash - && \
    apt-get install -y --no-install-recommends nodejs
RUN pyenv install 3.8.3 && \
    pyenv global 3.8.3 && \
    pyenv rehash
RUN pip install --upgrade pip && \
    pip3 install numpy && \
    pip3 install pandas && \
    pip3 install scipy
#Clone pyenv and CrowdWalk
RUN cd ~ && \
    git clone https://github.com/crest-cassia/CrowdWalk.git && \
    cd ~/CrowdWalk/crowdwalk/ && \
    gradle
RUN  chown -R ${USER_NAME}: /home/${USER_NAME}/