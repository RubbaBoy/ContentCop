-- [images, table] Stores the images per user/server to compare later on
CREATE TABLE IF NOT EXISTS `images` (
    server  BIGINT            NOT NULL,
    channel BIGINT            NOT NULL,
    message BIGINT            NOT NULL,
    author  BIGINT            NOT NULL,
    content VARBINARY(100000) NOT NULL,
    UNIQUE (server, author, content)
);

-- [servers, table] Stores all servers that have been scraped, and if they are done yet
CREATE TABLE IF NOT EXISTS `servers` (
    server   BIGINT NOT NULL UNIQUE,
    complete BOOLEAN DEFAULT FALSE
);

-- [users, table] All users' repost data
CREATE TABLE IF NOT EXISTS `users` (
    user    BIGINT NOT NULL,
    server  BIGINT NOT NULL,
    reposts INTEGER DEFAULT 0,
    UNIQUE (user, server)
);

-- [select_image, query] Gets a single image by its hash in a server
SELECT (channel, message, author) FROM `images` WHERE server = ? AND content = ?;

-- [add_image, update] Inserts an image
INSERT INTO `images` (server, channel, message, author, content) VALUES (?, ?, ?, ?, ?);

-- [delete_image_server, update] Deletes all images from a server
DELETE FROM `images` WHERE server = ?;

-- [delete_image_user, update] Deletes all images from a user
DELETE FROM `images` WHERE author = ?;

-- [server_status, query] Gets a servers' scraping status
SELECT (complete) FROM `servers` WHERE server = ?;

-- [add_server, update] Adds a server to be scraped
INSERT INTO `servers` (server) VALUES(?);

-- [update_server, update] Updates a server's scraping status
UPDATE `servers` SET complete = ? WHERE server = ?;

-- [delete_server, update] Deletes a server from the status table
DELETE FROM `servers` WHERE server = ?;

-- [select_users, query] Gets all users in a given server, sorted by reposts (High to low)
SELECT (user, reposts) FROM `users` WHERE server = ? ORDER BY reposts;

-- [select_user, query] Gets a single user by ID and server
SELECT (reposts) FROM `users` WHERE server = ? AND user = ?;
