# MAS-Bench


## Requirements
・python3.8 (3.8.3)

・numpy 1.19.4

・scipy 1.5.4

・pandas 1.1.4

```
pip install numpy
pip install scipy
pip install pandas
```

・CrowdWalk

・gradle

・java

・ruby

```
git clone https://github.com/crest-cassia/CrowdWalk.git
```

## Implementation
Run from termianl by (one example):

```
#current directory: Moving from MAS-Bench to UseCase.
cd UseCase

#CWPATH in main.sh need modified.
#CWPATH={full path of CrowdWalk}
sh main.sh
```

##Use docker
```
#terminal
pwd  #check your {MAS-Bench path}
docker build -f ./Dockerfile -t masbench .
docker run -it -u ${UID}:${GID} -v /{MAS-Bench path}/:/mount_data/ -e LOCAL_UID=$(id -u $USER) -e LOCAL_GID=$(id -g $USER) --name masbench masbench:latest bash

#docker
cd /mount_data/UseCase
sh main.sh
```
