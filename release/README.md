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

> git clone git://github.com/transifex/gpg-agent-forward
> cd gpg-agent-forward
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

7.2. Check bintray to make sure the files are there. Check the .asc files are there.

7.3. Check the tag on github.