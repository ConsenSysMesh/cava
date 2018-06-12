#!/bin/sh
# script copied from https://github.com/eldarlabs/ghpages-deploy-script, under MIT license.

# abort the script if there is a non-zero error
set -e

# show where we are on the machine
pwd

version=$1
echo "Publishing for version $version"

remote=$(git config remote.origin.url)
workingdir="$( dirname "$0" )"
siteSource="$workingdir/../build/docs/javadoc"
readmeFile="$workingdir/../README.md"

if [ ! -d "$siteSource" ]
then
    echo "$siteSource missing"
    exit 1
fi

if [ ! -f "$readmeFile" ]
then
    echo "$readmeFile missing"
    exit 1
fi

# make a directory to put the gp-pages branch
mkdir gh-pages-branch
cd gh-pages-branch
# now lets setup a new repo so we can update the gh-pages branch
git init
git remote add --fetch origin "$remote"

# switch into the gh-pages branch
if git rev-parse --verify origin/gh-pages > /dev/null 2>&1
then
    git checkout gh-pages
    # delete any old site as we are going to replace it
    # Note: this explodes if there aren't any, so moving it here for now
    git rm docs/latest
    if [ -d "docs/${version}" ]
    then
        git rm -rf "docs/${version}"
    fi
else
    git checkout --orphan gh-pages
fi

# copy over or recompile the new site
mkdir -p docs/$version
cd docs
ln -s $version latest
cd ..
cp -a "${siteSource}/." "docs/${version}/"
cp "${readmeFile}" "docs/${version}/"

# stage any changes and new files
git add -A
# now commit:
git commit --allow-empty -m "Deploy ${version} to GitHub pages"
# and push, but send any output to /dev/null to hide anything sensitive
git push --force --quiet origin gh-pages > /dev/null 2>&1

# go back to where we started and remove the gh-pages git repo we made and used
# for deployment
cd ..
rm -rf gh-pages-branch

echo "Finished Deployment!"