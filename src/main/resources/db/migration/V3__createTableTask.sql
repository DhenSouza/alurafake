-- V3__createTableTask.sql

CREATE TABLE Task (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  statement VARCHAR(255) NOT NULL,
  task_order INT NOT NULL, -- Mapeia para o campo 'order' da sua entidade, com nome 'task_order'
  course_id BIGINT(20) NOT NULL, -- Chave estrangeira para a tabela Course
  PRIMARY KEY (id),
  CONSTRAINT FK_Task_Course FOREIGN KEY (course_id) REFERENCES Course(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;