-- [images, table] Stores the images per author/server to compare later on. The hash s
CREATE TABLE IF NOT EXISTS `images` (
    server  BIGINT            NOT NULL,
    channel BIGINT            NOT NULL,
    message BIGINT            NOT NULL,
    author  BIGINT            NOT NULL,
    content VARBINARY(64)     NOT NULL,
    UNIQUE (server, author, content)
);

-- [servers, table] Stores all servers that have been scraped, and if they are done yet
CREATE TABLE IF NOT EXISTS `servers` (
    server   BIGINT NOT NULL UNIQUE,
    complete BOOLEAN DEFAULT FALSE
);

-- [users, table] All users' repost data
CREATE TABLE IF NOT EXISTS `users` (
    author    BIGINT NOT NULL,
    server  BIGINT NOT NULL,
    reposts INTEGER DEFAULT 0,
    UNIQUE (author, server)
);

-- [select_image, query] Gets a single image by its hash in a server
SELECT * FROM `images` WHERE server = ? AND content = ?;

-- [add_image, update] Inserts an image
INSERT INTO `images` (server, channel, message, author, content) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE server = server;

-- [delete_image_server, update] Deletes all images from a server
DELETE FROM `images` WHERE server = ?;

-- [delete_image_user, update] Deletes all images from a author
DELETE FROM `images` WHERE author = ?;

-- [select_servers, query] Selects all servers with a given completion status
SELECT * FROM `servers` WHERE complete = ?;

-- [server_status, query] Gets a servers' scraping status
SELECT * FROM `servers` WHERE server = ?;

-- [add_server, update] Adds a server to be scraped
INSERT INTO `servers` (server) VALUES(?);

-- [update_server, update] Updates a server's scraping status
UPDATE `servers` SET complete = ? WHERE server = ?;

-- [delete_server, update] Deletes a server from the status table
DELETE FROM `servers` WHERE server = ?;

-- [select_users, query] Gets all users in a given server, sorted by reposts (High to low)
SELECT * FROM `users` WHERE server = ? ORDER BY reposts;

-- [select_user, query] Gets a single user by ID and server
SELECT * FROM `users` WHERE author = ? AND server = ?;

-- [add_user, update] Inserts a new user's reposts to a server
INSERT INTO `users` (author, server, reposts) VALUES (?, ?, ?);

-- [increment_user, update] Increments a user's reposts to a server
UPDATE `users` SET reposts = reposts + ? WHERE author = ? AND server = ?;

--