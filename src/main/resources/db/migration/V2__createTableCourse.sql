CREATE TABLE Course (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    createdAt datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    title varchar(50) NOT NULL,
    description varchar(255) NOT NULL,
    instructor_id bigint(20) NOT NULL,
    status enum('BUILDING', 'PUBLISHED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'BUILDING',
    publishedAt datetime DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_Author FOREIGN KEY (instructor_id) REFERENCES User(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;