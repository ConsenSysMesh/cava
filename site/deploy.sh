#!/bin/sh
# script copied from https://github.com/eldarlabs/ghpages-deploy-script, under MIT license.

# abort the script if there is a non-zero error
set -ue

if [ $# != 1 ]; then
    echo "Usage: $0 version" 1>&2
    exit 1;
fi

version=$1

rootdir=$(cd `dirname $0`/.. && pwd)
if ! [ -d "$rootdir/.git" ]; then
    echo "Missing .git directory" 1>&2
    exit 1
fi

echo "Publishing for version $version"

remote=$(git config remote.origin.url)
javadocSource="$rootdir/build/docs/javadoc"
kotlinSource="$rootdir/build/docs/dokka"
readmeFile="$rootdir/README.md"

if [ ! -d "$javadocSource" ]
then
    echo "$javadocSource missing"
    exit 1
fi

if [ ! -d "$kotlinSource" ]
then
    echo "$kotlinSource missing"
    exit 1
fi

if [ ! -f "$readmeFile" ]
then
    echo "$readmeFile missing"
    exit 1
fi

# make a temporary directory to put the gp-pages branch
builddir=$(mktemp -d $TMPDIR/site-XXXXXX)
trap 'rm -rf $builddir' EXIT
cd $builddir

# now lets setup a new repo so we can update the gh-pages branch
echo "Checking out into $builddir/cava"
git clone --reference "$rootdir" -n "$remote"
cd cava

# switch into the gh-pages branch
if git rev-parse --verify origin/gh-pages > /dev/null 2>&1
then
    git checkout gh-pages

    # cleanup previous pushes
    if [ -e docs/java/latest ]; then
        git rm -rf docs/java/latest
    fi
    if [ -e "docs/java/${version}" ]; then
        git rm -rf "docs/java/${version}"
    fi
    if [ -e docs/kotlin/latest ]; then
        git rm -rf docs/kotlin/latest
    fi
    if [ -e "docs/kotlin/${version}" ]; then
        git rm -rf "docs/kotlin/${version}"
    fi
else
    git checkout --orphan gh-pages
    git reset --hard
fi

# copy documents into place
mkdir -p docs/java
(cd docs/java &&
cp -r "$javadocSource" "./$version" &&
ln -s "$version" latest)

mkdir -p docs/kotlin
(cd docs/kotlin &&
cp -r "$kotlinSource" "./$version" &&
ln -s "$version" latest)

cp "${readmeFile}" "docs/kotlin/${version}/"

# stage any changes and new files
git add -A > /dev/null
# now commit:
git commit --allow-empty -m "Deploy ${version} to GitHub pages"
# and push, but send any output to /dev/null to hide anything sensitive
git push --force --quiet origin gh-pages > /dev/null 2>&1

echo "Finished Deployment!"
