
# 🚩 Track-Ubi - Sistema de Mapeo y Gestión de Terrenos

![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue?logo=kotlin)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Realtime%20DB-yellow?logo=firebase)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

---

## 📱 Descripción General
**Track-Ubi** es una aplicación móvil nativa para Android que permite trackear en tiempo real la ubicación de topógrafos, mapear terrenos, calcular áreas y gestionar usuarios, todo con geolocalización continua en segundo plano.

---

## ✨ Características Principales

- 📍 **Geolocalización en Tiempo Real:** Trackea la ubicación de hasta tres dispositivos usando OpenStreetMap (OSMDroid) y servicios en segundo plano.
- 📐 **Cálculo de Áreas:** Calcula el área de terrenos mapeados.
- 🔐 **Autenticación:** Sistema de login con Firebase Authentication.
- 👥 **Gestión de Usuarios:** Interfaz administrativa para agregar, eliminar o desactivar usuarios.
- 🗺️ **Visualización de Terrenos:** Muestra ubicaciones, polígonos y características de los terrenos.
- 🤝 **Colaboración:** Permite a los topógrafos ver la ubicación de sus compañeros en tiempo real.

---

## 🛠️ Tecnologías Utilizadas

| Lenguaje   | Framework      | Autenticación & DB         | Mapas                        | UI & Diseño         | Dependencias                  |
|------------|---------------|----------------------------|------------------------------|---------------------|-------------------------------|
| Kotlin, Java | Android SDK  | Firebase Auth, Realtime DB | OSMDroid, Google Maps        | Material Design     | Retrofit, RecyclerView, CardView |

---

## 💻 Requisitos del Sistema

- **Entorno:** Android Studio Hedgehog | 2023.1.1 o superior
- **JDK:** 11 o superior
- **API:** Android SDK API 24+ (Android 7.0+)
- **Gradle:** 8.9.1
- **Kotlin:** 2.0.21

---

## ⚡ Instalación y Configuración

1. **Clonar el Repositorio**
   ```sh
   git clone https://github.com/GuamanFrancis/TrackearUbicaciones.git
   cd TrackearUbicaciones
   ```
2. **Configuración de Firebase**
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
   - Habilita Authentication y Realtime Database.
   - Descarga `google-services.json` y colócalo en la carpeta `app/`.
3. **Configuración de Android**
   - Abre el proyecto en Android Studio.
   - Sincroniza Gradle y espera la configuración.
4. **Ejecución**
   ```sh
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

---

## 📂 Estructura del Proyecto

```text
TrackearUbicaciones/
├── app/
│   ├── src/main/java/com.example.trackearubicaciones/
│   │   ├── AdminMainActivity.kt
│   │   ├── FirebaseUtils.kt
│   │   ├── GestionarUsuariosActivity.kt
│   │   ├── HomeActivity.kt
│   │   ├── LocationForegroundService.kt
│   │   ├── LoginActivity.kt
│   │   ├── MapaColaborativoActivity.kt
│   │   ├── MisTerrenosActivity.kt
│   │   ├── PerfilActivity.kt
│   │   ├── RegistrarTerrenoActivity.kt
│   │   ├── TopografoMainActivity.kt
│   │   └── UnirseSesionActivity.kt
│   ├── build.gradle.kts
│   └── AndroidManifest.xml
├── gradle/
├── build.gradle.kts
└── README.md
```

---

## 🔒 Seguridad

> ⚠️ **Nota:** Archivos sensibles como `google-services.json` **no deben subirse** al repositorio.

---

## 📜 Permisos de Android

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 🖼️ Capturas de Pantalla

**Pantalla de Login:**

<img width="250" alt="Login" src="https://github.com/user-attachments/assets/3d58f292-4791-45c1-8370-e2289c0b2918" />

**Mapa Colaborativo:**

<img width="250" alt="Mapa Colaborativo" src="https://github.com/user-attachments/assets/6869ed84-3ca6-4a9f-867e-bb4e2292cd3e" />

**Gestión de Terrenos:**

<img width="250" alt="Gestión de Terrenos" src="https://github.com/user-attachments/assets/9f26fb1a-4de1-4186-9c35-dc9566dadeb4" />

**Dashboard Admin:**

<img width="250" alt="Dashboard Admin" src="https://github.com/user-attachments/assets/c350bfa5-5f2f-49a0-aa0c-87ca5e7c604f" />

**Publicación Tienda:**

<img width="1250" height="486" alt="image" src="https://github.com/user-attachments/assets/cbf34b01-b41b-43b2-bfe9-93b842f402a0" />


---

## 📚 Documentación

- 🎬 **Video de Funcionamiento:** Disponible en YouTube.
  
 [YouTube](https://www.youtube.com/watch?v=hlPPazDQRBw)

- 💻 **Código:** Incluye comentarios detallados.

---

## 📝 Licencia

MIT License - Ver archivo LICENSE para detalles.

---

## 👤 Contacto

- **Autor:** Francis Guaman  
  [GitHub](https://github.com/GuamanFrancis)
- **Autor:** Anderson Vilatuña
- [GitHub](https://github.com/SoyAndersonJoel)

---

## 🙏 Agradecimientos

OpenStreetMap, Firebase, Android SDK.






