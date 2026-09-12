# 📺 NeawStreamV-player

**NeawStreamV-player** es un reproductor multimedia y gestor de listas M3U ligero, moderno y optimizado específicamente para **Android TV** y dispositivos de TV Box. Desarrollado nativamente utilizando **Jetpack Compose**, ofrece una interfaz fluida, minimalista y completamente adaptada para la navegación mediante control remoto.

---

## 🚀 Características Principales

* **Optimizado para Android TV:** Navegación por control remoto fluida, gestión precisa de estados de foco (`FocusRequester`) y efectos visuales de selección.
* **Restauración de Posición y Foco:** Al regresar del reproductor de video, la aplicación recuerda exactamente el canal en el que estabas, posicionando el scroll y devolviendo el foco automáticamente para evitar molestos desplazamientos desde el inicio.
* **Reloj en Tiempo Real:** Visualización discreta y centralizada de la hora actual directamente en la barra superior (`TopAppBar`).
* **Paginación Inteligente:** Carga progresiva de canales (bloques de 50 ítems) para garantizar un rendimiento óptimo incluso con listas M3U masivas.
* **Búsqueda Instantánea:** Filtrado en tiempo real de canales por nombre.
* **Registro de Logs Interno:** Panel flotante opcional para la depuración de errores de red y carga de listas.
* **Aviso Legal / Disclaimer Integrado:** Mensaje de advertencia inicial conforme a las políticas de uso de reproductores multimedia genéricos.

---

## 🛠️ Tecnologías Utilizadas

* **Kotlin** - Lenguaje de programación principal.
* **Jetpack Compose & Material 3** - Construcción de la interfaz gráfica adaptada a pantallas grandes.
* **Coil** - Carga eficiente de logotipos e imágenes de canales.
* **OkHttp** - Gestión de peticiones de red para la descarga de listas M3U remotas.
* **Coroutinas & Flows** - Manejo de tareas asíncronas y sincronización de estados reactivos.

---

## ⚙️ Estructura del Proyecto

El proyecto está diseñado bajo una arquitectura limpia orientada a componentes de Compose:
* `HomeScreen`: Pantalla principal que gestiona la carga de la URL, la grilla de canales (`LazyVerticalGrid`), la barra superior y los diálogos.
* `CanalItem`: Componente visual individual para cada canal con efectos de escala y bordes iluminados al recibir el foco del control remoto.
* `LogManager`: Objeto singleton encargado del registro y visualización en tiempo real de los eventos y errores del sistema.

---

## ⚖️ Aviso Legal (Disclaimer)

> *"NeawStreamV-player" es un reproductor multimedia de propósito general. El software no contiene, proporciona, ni preinstala ningún tipo de lista de canales, contenido multimedia, o enlaces a fuentes externas.*
>
> *El usuario es el único responsable de la legalidad, propiedad y uso de los contenidos que decida cargar o reproducir mediante la aplicación. El autor no apoya ni fomenta el uso de material protegido por derechos de autor sin la debida licencia.*

---

## 👤 Autor

Desarrollado por [WykosVx](https://github.com/WykosVx).
