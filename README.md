# Content Cop

Content Cop is a Discord bot to read all images in a server, and detect any reposts, tracking who reposts the most.

The bot works by initially scraping images sent in the server (Only the last 10k from each channel by default), generating a hash of each image.

When an image is posted, it will lookup the servers' images and compare their hashes to the sent one. If it is within a threshold, an emote is reacted to the reposted message. Clicking this will send an embed with details on the search. A leaderboard is available for all users who have reposted.

![Demo Gif](demo/demo.gif)

## Usage

The bot can be added to any Discord server here. Self hosting may also be done by following the steps below. No external database is required.

1. Ensure you have at least [Java 12](https://adoptopenjdk.net/?variant=openjdk12&jvmVariant=hotspot) installed and a [Discord bot](https://discordapp.com/developers/) created, with its token
2. Download the latest jar from the [releases page](https://github.com/RubbaBoy/ContentCop/releases)
3. Run `java -jar ContentCop.jar`
4. Edit the `config.conf` generated, inserting your bot's token and the ID of the emote you want to use to flag messages
5. Run `java -jar ContentCop.jar` again and `/setup` on your Discord server

## How it works

Content Cop works by creating dHashes, or Difference Hashes, for each image. This involves scaling the image to 128x128, and comparing pixel brightnesses to their surrounding pixels. This is only done once per image, and is stored in an internal map, backed by a database for persistent storage and speed. When an image is uploaded, it is hashed and compared to the hashes of all other images. If it is within a certain threshold, it is labeled as a repost. If not, its hash is added to the server's database.

