-- Esquema PostgreSQL (Supabase) - coincide con las entidades JPA (id SERIAL/INTEGER y nota NUMERIC, como en la BD).
-- Con spring.jpa.hibernate.ddl-auto=validate Hibernate NO crea tablas: ejecuta este script
-- una vez en el SQL Editor de Supabase antes de iniciar la aplicación.
-- Las claves foráneas no borran en cascada: la aplicación impide eliminar registros con dependencias.
CREATE TABLE IF NOT EXISTS usuario (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN','DOCENTE','ESTUDIANTE'))
);

CREATE TABLE IF NOT EXISTS estudiante (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    fecha_inscripcion DATE
);

CREATE TABLE IF NOT EXISTS docente (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER UNIQUE REFERENCES usuario(id) ON DELETE SET NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(20) UNIQUE,
    especialidad VARCHAR(150),
    email VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS curso (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    creditos INTEGER,
    ciclo INTEGER,
    horas INTEGER
);

CREATE TABLE IF NOT EXISTS periodo_academico (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE
);

CREATE TABLE IF NOT EXISTS curso_periodo (
    id SERIAL PRIMARY KEY,
    curso_id INTEGER NOT NULL REFERENCES curso(id),
    docente_id INTEGER NOT NULL REFERENCES docente(id),
    periodo_id INTEGER NOT NULL REFERENCES periodo_academico(id),
    CONSTRAINT uk_curso_periodo_asignacion UNIQUE (curso_id, docente_id, periodo_id)
);

CREATE TABLE IF NOT EXISTS matricula (
    id SERIAL PRIMARY KEY,
    estudiante_id INTEGER NOT NULL REFERENCES estudiante(id),
    curso_periodo_id INTEGER NOT NULL REFERENCES curso_periodo(id),
    fecha_matricula DATE,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    CONSTRAINT uk_matricula_estudiante_curso_periodo UNIQUE (estudiante_id, curso_periodo_id),
    CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVO','RETIRADO','CULMINADO'))
);

CREATE TABLE IF NOT EXISTS evaluacion (
    id SERIAL PRIMARY KEY,
    matricula_id INTEGER NOT NULL REFERENCES matricula(id),
    tipo_evaluacion VARCHAR(50),
    nota NUMERIC,
    fecha DATE
);

CREATE TABLE IF NOT EXISTS sesion (
    id SERIAL PRIMARY KEY,
    curso_periodo_id INTEGER NOT NULL REFERENCES curso_periodo(id),
    fecha_sesion DATE NOT NULL,
    hora_inicio TIME,
    hora_fin TIME,
    aula VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    CONSTRAINT chk_sesion_tipo CHECK (tipo IN ('PRESENCIAL','VIRTUAL'))
);

CREATE TABLE IF NOT EXISTS asistencia (
    id SERIAL PRIMARY KEY,
    matricula_id INTEGER NOT NULL REFERENCES matricula(id),
    sesion_id INTEGER NOT NULL REFERENCES sesion(id),
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT uk_asistencia_matricula_sesion UNIQUE (matricula_id, sesion_id),
    CONSTRAINT chk_asistencia_estado CHECK (estado IN ('PRESENTE','AUSENTE','TARDANZA'))
);
