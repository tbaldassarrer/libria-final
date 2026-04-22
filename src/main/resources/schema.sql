CREATE TABLE IF NOT EXISTS registrolectura (
    estadoLectura VARCHAR(50),
    fechaInicio DATE,
    fechaFin DATE,
    idLibro INT,
    idUsuario INT,
    resenia TEXT,
    puntuacion INT,
    INDEX idx_registrolectura_usuario (idUsuario),
    INDEX idx_registrolectura_libro (idLibro),
    INDEX idx_registrolectura_estado (estadoLectura)
);
