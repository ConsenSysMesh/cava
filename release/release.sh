#!/usr/bin/env bash

set -ue

if ! ssh-add -l > /dev/null; then
    echo "No identities in ssh-agent (run ssh-add?)" >&2
    exit 1
fi

pinata-gpg-forward

workingdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
if [ ! -f $workingdir/release.config ]
then
  echo "$workingdir/release.config is missing. Exiting now."
  exit 1
fi

source $workingdir/release.config

builddir=$(mktemp -d $TMPDIR/release-XXXXXX)
trap 'rm -rf $builddir' EXIT

rm -Rf $builddir/build-checkout
mkdir $builddir/build-checkout
git clone $git_repo $builddir/checkout
exitstatus=$?

if [ $exitstatus != 0 ]; then
  echo "Error while checking out the git repository"
  exit 1
fi

pushd $builddir/checkout
git checkout $git_branch
exitstatus=$?

if [ $exitstatus != 0 ]; then
  echo "Error while checking out the branch"
  exit 1
fi

git submodule update --init --recursive
exitstatus=$?

if [ $exitstatus != 0 ]; then
  echo "Error while updating submodules"
  exit 1
fi

popd

cat <<-EOF > $builddir/run_release.sh
#!/bin/bash

set -xe
echo "running release now"
chmod og-rwx /root/.gnupg
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"
export GPG_TTY=/dev/console
export TMPDIR=/tmp
./gradlew build :dokka :javadoc
git tag -a $tag_name -m "$tag_comment" -s
BINTRAY_DEPLOY=true ./gradlew sign deploy publishSite
git push origin $git_branch --tags
EOF
chmod +x $builddir/run_release.sh

docker build -t release_image:1.0 -f $workingdir/Dockerfile --build-arg git_name="$git_name" --build-arg git_email="$git_email" --build-arg gpg_key="$signing_key_id" $builddir
exitstatus=$?

if [ $exitstatus != 0 ]; then
  echo "Error while building the release image"
  exit 1
fi

docker run -it -v $ssh_private_key:/root/.ssh/id_rsa \
           -v $ssh_public_key:/root/.ssh/id_rsa.pub \
           -v gpg-agent:/root/.gnupg \
           -e BUILD_RELEASE="true" \
           -e BINTRAY_USER=$bintray_user \
           -e BINTRAY_KEY=$bintray_key \
           -e ENABLE_SIGNING=true \
           -v ~/.gradle/caches:/root/.gradle/caches \
           release_image:1.0

# Remove docker pgp-agent:
docker rm -f "pinata-gpg-agent" >/dev/null 2>&1 || true
