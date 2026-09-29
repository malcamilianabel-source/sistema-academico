
-- TABLA: usuario
CREATE TABLE IF NOT EXISTS usuario (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    rol         VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'DOCENTE', 'ESTUDIANTE'))
);


-- TABLA: estudiante

CREATE TABLE IF NOT EXISTS estudiante (
    id                 SERIAL PRIMARY KEY,
    usuario_id         INT UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nombre             VARCHAR(100) NOT NULL,
    apellido           VARCHAR(100) NOT NULL,
    dni                VARCHAR(20)  NOT NULL UNIQUE,
    email              VARCHAR(150) NOT NULL UNIQUE,
    telefono           VARCHAR(20),
    fecha_inscripcion  DATE
);


-- TABLA: docente

CREATE TABLE IF NOT EXISTS docente (
    id           SERIAL PRIMARY KEY,
    usuario_id   INT UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nombre       VARCHAR(100) NOT NULL,
    apellido     VARCHAR(100) NOT NULL,
    dni          VARCHAR(20)  NOT NULL UNIQUE,
    telefono     VARCHAR(20),
    especialidad VARCHAR(150),
    email        VARCHAR(150) NOT NULL UNIQUE
);


-- TABLA: curso

CREATE TABLE IF NOT EXISTS curso (
    id       SERIAL PRIMARY KEY,
    nombre   VARCHAR(150) NOT NULL,
    codigo   VARCHAR(20)  NOT NULL UNIQUE,
    creditos INT,
    ciclo    INT,
    horas    INT
);


-- TABLA: periodo_academico

CREATE TABLE IF NOT EXISTS periodo_academico (
    id           SERIAL PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    fecha_inicio DATE,
    fecha_fin    DATE
);


-- TABLA: curso_periodo

CREATE TABLE IF NOT EXISTS curso_periodo (
    id          SERIAL PRIMARY KEY,
    curso_id    INT NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    docente_id  INT NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    periodo_id  INT NOT NULL REFERENCES periodo_academico(id) ON DELETE CASCADE
);


-- TABLA: matricula

CREATE TABLE IF NOT EXISTS matricula (
    id               SERIAL PRIMARY KEY,
    estudiante_id    INT NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    curso_periodo_id INT NOT NULL REFERENCES curso_periodo(id) ON DELETE CASCADE,
    fecha_matricula  DATE,
    estado           VARCHAR(20) DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'RETIRADO', 'CULMINADO'))
);


-- TABLA: sesion

CREATE TABLE IF NOT EXISTS sesion (
    id               SERIAL PRIMARY KEY,
    curso_periodo_id INT NOT NULL REFERENCES curso_periodo(id) ON DELETE CASCADE,
    fecha_sesion     DATE,
    hora_inicio      TIME,
    hora_fin         TIME,
    aula             VARCHAR(50),
    tipo             VARCHAR(20) DEFAULT 'PRESENCIAL' CHECK (tipo IN ('PRESENCIAL', 'VIRTUAL'))
);


-- TABLA: evaluacion

CREATE TABLE IF NOT EXISTS evaluacion (
    id               SERIAL PRIMARY KEY,
    matricula_id     INT NOT NULL REFERENCES matricula(id) ON DELETE CASCADE,
    tipo_evaluacion  VARCHAR(50),
    nota             DECIMAL(4,2),
    fecha            DATE
);


-- TABLA: asistencia

CREATE TABLE IF NOT EXISTS asistencia (
    id           SERIAL PRIMARY KEY,
    matricula_id INT NOT NULL REFERENCES matricula(id) ON DELETE CASCADE,
    sesion_id    INT NOT NULL REFERENCES sesion(id) ON DELETE CASCADE,
    estado       VARCHAR(20) DEFAULT 'PRESENTE' CHECK (estado IN ('PRESENTE', 'AUSENTE', 'TARDANZA'))
);

-- DATOS DE PRUEBA


INSERT INTO usuario (nombre, apellido, email, password, rol) VALUES
('Admin', 'Sistema', 'admin@academico.com', 'admin123', 'ADMIN'),
('Carlos', 'Quispe', 'carlos.quispe@academico.com', '1234', 'DOCENTE'),
('Maria', 'Torres', 'maria.torres@academico.com', '1234', 'DOCENTE'),
('Luis', 'Ramirez', 'luis.ramirez@academico.com', '1234', 'ESTUDIANTE'),
('Ana', 'Lopez', 'ana.lopez@academico.com', '1234', 'ESTUDIANTE')
ON CONFLICT DO NOTHING;

INSERT INTO docente (usuario_id, nombre, apellido, dni, telefono, especialidad, email) VALUES
(2, 'Carlos', 'Quispe', '45678901', '987654321', 'Ingeniería de Software', 'carlos.quispe@academico.com'),
(3, 'Maria', 'Torres', '56789012', '976543210', 'Base de Datos', 'maria.torres@academico.com')
ON CONFLICT DO NOTHING;

INSERT INTO estudiante (usuario_id, nombre, apellido, dni, email, telefono, fecha_inscripcion) VALUES
(4, 'Luis', 'Ramirez', '12345678', 'luis.ramirez@academico.com', '956781234', '2024-03-01'),
(5, 'Ana', 'Lopez', '23456789', 'ana.lopez@academico.com', '945672345', '2024-03-01')
ON CONFLICT DO NOTHING;

INSERT INTO curso (nombre, codigo, creditos, ciclo, horas) VALUES
('Marcos de Desarrollo Web', 'MDW-101', 4, 5, 64),
('Base de Datos Avanzada', 'BDA-201', 4, 6, 64),
('Algoritmos y Estructuras', 'AED-101', 3, 3, 48)
ON CONFLICT DO NOTHING;

INSERT INTO periodo_academico (nombre, fecha_inicio, fecha_fin) VALUES
('2024-I', '2024-03-01', '2024-07-31'),
('2024-II', '2024-08-01', '2024-12-20')
ON CONFLICT DO NOTHING;
