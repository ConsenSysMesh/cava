FROM ubuntu:18.04 as intermediate

RUN apt-get update && apt-get install -y git openjdk-8-jdk gpg && apt-get clean

ARG git_email
ARG git_name
ARG gpg_key

RUN git config --global user.name $git_name \
 && git config --global user.email $git_email \
 && git config --global user.signingkey $gpg_key

RUN mkdir -p /root/.ssh && touch /root/.ssh/known_hosts
RUN ssh-keyscan github.com >> /root/.ssh/known_hosts

RUN echo "signing.keyId=$gpg_key\n\
  signing.gnupg.executable=gpg\n\
  signing.gnupg.useLegacyGpg=true\n\
  signing.gnupg.keyName=$gpg_key\n"\
  > /gradle.properties

FROM ubuntu
COPY --from=intermediate /usr/lib /usr/lib
COPY --from=intermediate /usr/bin /usr/bin
COPY --from=intermediate /usr/local /usr/local
COPY --from=intermediate /usr/share/git-core/templates /usr/share/git-core/templates

COPY --from=intermediate /etc /etc
COPY --from=intermediate /lib /lib

COPY --from=intermediate /root/.ssh /root/.ssh

COPY /checkout /checkout
COPY /run_release.sh /checkout
COPY --from=intermediate /gradle.properties /checkout

COPY --from=intermediate /root/.gitconfig /root/.gitconfig

WORKDIR "/checkout"

ENTRYPOINT ["./run_release.sh"]