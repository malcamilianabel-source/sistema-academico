-- Esquema MySQL 8 - Avance 2: funcionalidades 1 a 7
CREATE TABLE IF NOT EXISTS usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN','DOCENTE','ESTUDIANTE'))
);

CREATE TABLE IF NOT EXISTS estudiante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    fecha_inscripcion DATE,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS docente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    especialidad VARCHAR(150),
    email VARCHAR(150) NOT NULL UNIQUE,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS curso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    creditos INT,
    ciclo INT,
    horas INT
);

CREATE TABLE IF NOT EXISTS periodo_academico (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE
);

CREATE TABLE IF NOT EXISTS curso_periodo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    curso_id INT NOT NULL,
    docente_id INT NOT NULL,
    periodo_id INT NOT NULL,
    FOREIGN KEY (curso_id) REFERENCES curso(id) ON DELETE CASCADE,
    FOREIGN KEY (docente_id) REFERENCES docente(id) ON DELETE CASCADE,
    FOREIGN KEY (periodo_id) REFERENCES periodo_academico(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS matricula (
    id INT AUTO_INCREMENT PRIMARY KEY,
    estudiante_id INT NOT NULL,
    curso_periodo_id INT NOT NULL,
    fecha_matricula DATE,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    FOREIGN KEY (estudiante_id) REFERENCES estudiante(id) ON DELETE CASCADE,
    FOREIGN KEY (curso_periodo_id) REFERENCES curso_periodo(id) ON DELETE CASCADE,
    CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVO','RETIRADO','CULMINADO'))
);

CREATE TABLE IF NOT EXISTS evaluacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    matricula_id INT NOT NULL,
    tipo_evaluacion VARCHAR(50),
    nota DECIMAL(4,2),
    fecha DATE,
    FOREIGN KEY (matricula_id) REFERENCES matricula(id) ON DELETE CASCADE
);
