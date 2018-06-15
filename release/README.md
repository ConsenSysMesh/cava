1. Install Docker on your machine
1.1. Share the location of your home folder (the program will mount your .gnupg and .ssh folders)

2. Install gpg on your machine

2.1. Edit the gpg agent to ensure pin entries use the tty:
> ~/.gnupg/gpg-agent.conf

Add the line: `pinentry-program /usr/local/bin/pinentry-tty` (adapt depending on where the program is located)

2.2. Reload gpg-agent:

> gpg-connect-agent reloadagent /bye

3. Install gpg-agent-forward

3.1 Generate a SSH key/pair to ssh forward the gpg-agent. Give it a name such as 'releasekey'

> ssh-keygen -t rsa

Add the resulting identity to your ssh agent trusted identities:

> ssh-add -K <private key>

3.2. Follow the instructions at https://github.com/transifex/docker-gpg-agent-forward, reproduced here:

> git clone git://github.com/transifex/docker-gpg-agent-forward
> cd docker-gpg-agent-forward
> make
> make install

The script runs `pinata-gpg-forward` for you.

4. Create your release configuration

4.1. Copy the release.config.sample file into release.config

> cp release.config.sample release.config

Fill in the information in release.config

5. Insert your Yubikey if you haven't already

5.1. Prepare the Yubikey and cache its PIN (it won't work during the script execution):

> gpg --status-fd=2 -bsau <YOUR KEY ID>

Enter some text, press ^D
Enter your PIN, then touch the key if needed.
Repeat. Notice the PIN prompt should not show.

6. run the release

> ./release.sh

6.1. Follow the prompts and enter your Yubikey PINs when prompted

7. Check the release took place

7.1. Check the website was updated.

7.2. Check bintray to make sure the files are there.

You may need to OK the publication of the files.

Check the .asc files are there.

Download the main distribution jar and its associated .asc file.

> gpg --verify <downloaded file>.asc <downloaded file>

If this is the first time, from the output, copy the RSA key used for signing and drop it in:

> gpg --keyserver pgp.mit.edu --recv-key <RSA key ID>

Then run again:

> gpg --verify <downloaded file>.asc <downloaded file>

7.3. Check the tag on github.

8. Prepare next iteration

8.1. Edit the README with the new version (both the download badge and the Maven coordinates)

8.2. Change the version in build.gradle

8.3. Commit and push to the version branch.
