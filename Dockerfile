FROM ubuntu:latest
LABEL authors="kalon"

ENTRYPOINT ["top", "-b"]