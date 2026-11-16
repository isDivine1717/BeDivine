## ✨ Novedades en BeDivine v1.0.4

### 🏠 Rediseño Completo de la Pantalla Principal
- **3 Plantillas Visuales Seleccionables**: Personaliza la experiencia y apariencia de tu inicio según tu estilo:
  - **Estándar Dinámica**: Diseño completo y enriquecido con carrusel de favoritos (*Top Picks*), buscador rápido de estados de ánimo y todas tus secciones musicales en un flujo continuo.
  - **Mochy Compacto**: Interfaz minimalista de alta densidad inspirada en Mochy-Music, con cabecera ambiental y cuadrícula de accesos rápidos 2x2 para iniciar tu música favorita con un solo toque.
  - **Editorial Hero**: Cabecera visual de impacto cinematográfico con tarjeta destacada de gran tamaño (*Hero Spotlight*), gradiente difuminado en tiempo real, insignia de estación y reproducción directa.

### ⚙️ Nuevo Menú de Personalización del Inicio (Ajustes > Personalización)
- **Control Total del Orden y Visibilidad**: Inspirado en los avanzados sistemas de personalización de Prism y Mochy:
  - **Reorganización interactiva**: Cambia la posición de cualquiera de las 8 secciones del inicio hacia arriba o hacia abajo con toques directos.
  - **Visibilidad personalizable**: Activa o desactiva de forma individual cada sección (*Tus favoritos / Para ti, Escuchado recientemente, Estados de ánimo, Mezclas personalizadas, Escuchar de nuevo, Videos recomendados, Listas destacadas y Nuevos lanzamientos*).
  - **Previsualización en tiempo real**: Selector visual con miniaturas interactivas para elegir tu plantilla favorita al instante.
  - **Restablecimiento rápido**: Botón para restaurar la configuración predeterminada en cualquier momento con confirmación de seguridad.

### ⚡ Rendimiento de Alto Nivel y Fluidez a 60/120 FPS
- **Renderizado Reactivo y Claves Estables**: Optimización integral en Jetpack Compose para garantizar transiciones instantáneas, animaciones fluidas y desplazamiento a 60/120 fps sin tirones (*jank*) ni recomposiciones innecesarias.
- **Pausa Inteligente de Animaciones**: Suspensión automática del movimiento de carátulas durante el desplazamiento para un uso eficiente de memoria y batería.

### 🌐 Localización Integral en Español
- **Experiencia 100% en Español**: Todos los nuevos controles, nombres de plantillas, descripciones, opciones de ordenación, estados y mensajes del sistema han sido redactados e integrados nativamente en español.

---

## ✨ Novedades en BeDivine v1.0.3

### 🎴 Reorganización y Limpieza de la Biblioteca
- **"Tu Experiencia Musical" en la cima**: Las 4 tarjetas de estadísticas y resumen musical personalizadas ahora se muestran hasta arriba del todo, encabezando la Biblioteca con tu tiempo de reproducción, canción favorita, artista principal y fecha de ingreso.
- **Eliminación de elementos duplicados**: Se limpiaron todas las listas repetidas de BitChord (Descargas, Música local, Listas de reproducción y Artistas redundantes en la vista general), manteniendo una interfaz minimalista, ordenada y limpia al 100%.
- **Acceso directo e interactivo desde la cuadrícula 2x2**:
  - Al tocar **Playlists**, se abre el explorador completo con todas tus listas locales, importadas y sincronizadas con botón para crear nueva lista.
  - Al tocar **Artists**, se despliegan todos tus artistas favoritos en pantalla completa.
  - Al tocar **Albums**, se muestran todos tus álbumes guardados.
  - Al tocar **Songs**, accedes directamente a tus canciones locales y descargadas en el dispositivo.
- **Pestaña Local dedicada**: Encuentra tus carpetas de descargas y archivos locales ordenadamente en la pestaña **Local**.

---

## ✨ Novedades en BeDivine v1.0.2

### 📚 Nueva Pantalla de Biblioteca (Estilo Mochy)
- **Diseño estético y minimalista**: Renovación total de la pestaña Biblioteca con una interfaz moderna y fluida inspirada en Mochy-Music.
- **Barra de búsqueda integrada**: Nuevo buscador rápido *"Search your library"* en la parte superior para filtrar al instante canciones, playlists, álbumes y artistas.
- **Pestañas de filtrado (Chips)**: Cambia rápidamente entre **All**, **Local**, **Cached** e **Imported** con suaves transiciones visuales.
- **Cuadrícula 2x2 de categorías**: Tarjetas elegantes con esquinas redondeadas y carátulas apiladas:
  - **Songs** (Total de canciones con portadas)
  - **Playlists** (Total de listas con previsualización de carátulas)
  - **Albums** (Total de álbumes guardados)
  - **Artists** (Total de artistas con avatares circulares)

### 🔄 Sistema de Importación de Cuentas Multiplataforma
- **Banner interactivo de importación**: Destacado bloque con marquesina animada continua de iconos squircle para las plataformas compatibles (*Spotify, Apple Music, Tidal y YouTube Music*).
- **Conexión directa de cuentas**: Conecta de forma segura tu cuenta de **Spotify**, **Apple Music**, **Tidal** o **YouTube Music** directamente dentro de BeDivine mediante navegador integrado.
- **Selector inteligente de playlists**: La app detecta tus listas remotas con nombre, carátula y número de canciones, permitiéndote seleccionar exactamente cuáles deseas transferir con un solo toque o elegir todas.
- **Importación y búsqueda en tiempo real**: Las canciones se vinculan y emparejan automáticamente para que estén listas para escuchar y descargar sin esfuerzo, mostrando el progreso de importación en vivo directamente en la biblioteca.
- **Importación por enlace**: Opción para pegar enlaces directos de playlists públicas de Spotify.

---

## ✨ Novedades en BeDivine v1.0.1

### 💳 Nueva Tarjeta de Identificación 3D Interactiva (Estilo Mochy)
- **Tarjeta 3D en el menú de perfil**: Al tocar tu foto de perfil arriba a la derecha en la pantalla principal, el menú ahora muestra tu **Tarjeta de Identificación 3D interactiva** (`PROFILE USR#...`, foto circular, `ID CARD OF`, tu nombre, fecha de ingreso y cinta lateral vertical animada `CARD · ...`) en lugar de la fila estática de cuenta.
- **Física 3D táctil y por movimiento**: Arrastra el dedo sobre la tarjeta en cualquier dirección para inclinarla en 3D en tiempo real con reflejos dinámicos de luz y retorno elástico suave.

### 🎨 Menú de Personalización de la Tarjeta 3D
- **Edición en vivo al tocar la tarjeta**: Toca directamente tu tarjeta 3D para abrir el nuevo estudio de personalización con vista previa 3D interactiva en tiempo real:
  - **Identidad**: Cambia tu nombre visible en la tarjeta y edita o genera aleatoriamente tu código `USR#`.
  - **Foto personalizada**: Elige cualquier imagen de tu galería para la tarjeta (y para el avatar de la barra superior) o restaura la foto de tu cuenta.
  - **8 Temas de Color**: *Ónix Oscuro*, *Plata Cromo*, *Violeta Divino*, *Carmesí*, *Cian Neón*, *Oro Real*, *Esmeralda* y *Cuarzo Rosa*.
  - **6 Efectos Visuales 3D**: *Holográfico*, *Metal Cepillado*, *Matriz de Puntos*, *Borde Neón*, *Polvo Estelar* y *Mate Minimalista*.
  - **Opciones extra**: Activa o desactiva el marco de matriz de puntos estilo Mochy detrás de la tarjeta y la etiqueta `"ID CARD OF"`.
