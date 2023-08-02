FROM ubuntu:22.04

LABEL maintainer="Vinayak"

RUN apt-get update \ 
    && apt-get install -y libgl1-mesa-glx python3 python3-pip libxkbcommon-x11-0

RUN pip3 install --upgrade pip 

ENV TZ="America/New_York"

RUN apt-get install -y qt6-base-dev ffmpeg libsm6 libxext6 libegl1-mesa 

# FROM vinayak1998th/face_filter:builder

LABEL maintainer="Vinayak"

RUN pip3 install --upgrade pip 

COPY requirements.txt /tmp/
RUN pip3 install --requirement /tmp/requirements.txt

WORKDIR /pyapp
COPY . /pyapp

CMD ["python3", "app.py"]