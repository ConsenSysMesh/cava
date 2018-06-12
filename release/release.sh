#!/usr/bin/env bash

pinata-gpg-forward

workingdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
if [ ! -f $workingdir/release.config ]
then
  echo "$workingdir/release.config is missing. Exiting now."
  exit 1
fi

source $workingdir/release.config

rm -Rf $TMPDIR/build-checkout
mkdir $TMPDIR/build-checkout
git clone $git_repo $TMPDIR/build-checkout/checkout
exitstatus=$?

if [ $exitstatus != 0 ]; then
  echo "Error while checking out the git repository"
  exit 1
fi

pushd $TMPDIR/build-checkout/checkout
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

cat <<-EOF > $TMPDIR/build-checkout/run_release.sh
#!/bin/bash

set -e
echo "running release now"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"
export GPG_TTY=/dev/console
git tag -a $tag_name -m "$tag_comment" -s
./gradlew build javadoc sign deploy publishSite
git push origin $git_branch --tags
EOF
chmod +x $TMPDIR/build-checkout/run_release.sh

docker build -t release_image:1.0 -f $workingdir/Dockerfile --build-arg git_name="$git_name" --build-arg git_email="$git_email" --build-arg gpg_key="$signing_key_id" $TMPDIR/build-checkout
exitstatus=$?

if [ $exitstatus != 0 ]; then
  echo "Error while building the release image"
  exit 1
fi

docker run -it -v $ssh_private_key:/root/.ssh/id_rsa \
           -v $ssh_public_key:/root/.ssh/id_rsa.pub \
           -v gpg-agent:/root/.gnupg \
           -e BUILD_TAG_MODE="release" \
           -e BINTRAY_USER=$bintray_user \
           -e BINTRAY_KEY=$bintray_key \
           -e ENABLE_SIGNING=true \
           release_image:1.0


# Remove docker pgp-agent:
docker rm -f "pinata-gpg-agent" >/dev/null 2>&1 || true