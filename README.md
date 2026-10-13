# IoT SmartMonitor — TI3042 Unidad 2

Aplicación Android nativa en Kotlin para monitoreo demostrativo de temperatura/humedad y sincronización entre dos dispositivos Android a través de Firebase Realtime Database. La conexión de red puede usar Wi‑Fi o datos móviles; ambos teléfonos deben tener acceso a Internet y usar el mismo proyecto Firebase.

## Importante: alcance real de esta entrega
- Login y registro con Firebase Authentication (correo y contraseña).
- Lectura en tiempo real de la última medición compartida y estado de un dispositivo lógico.
- Publicación manual de mediciones de prueba para demostrar el flujo entre dos teléfonos.
- Control remoto demostrativo de un estado booleano.
- **No incluye todavía lectura física de DHT22 ni accionamiento de hardware real.** Para eso se debe integrar un ESP32/DHT22 y un canal de telemetría autenticado.
- La guía solicita revisión y validación del docente. Confirma que la problemática y las funcionalidades sean aprobadas.

## Requisitos
- Android Studio compatible con Android Gradle Plugin 8.7.3.
- JDK 17.
- Dos teléfonos Android (API 26 o superior) o un teléfono y un emulador.
- Cuenta/proyecto Firebase.

## Configuración Firebase
1. Crea un proyecto en Firebase Console.
2. Agrega una app Android con package name `cl.ti3042.smartmonitor`.
3. Descarga `google-services.json` y cópialo en `app/google-services.json` (no lo subas públicamente si contiene datos que no deseas divulgar).
4. En Authentication > Sign-in method, habilita Email/Password.
5. Crea Realtime Database y copia su URL en la consola si tu región la requiere; si el SDK no detecta la base de datos, cambia `FirebaseDatabase.getInstance()` por `FirebaseDatabase.getInstance("https://TU-BASE-DE-DATOS")`.
6. Para pruebas iniciales, configura reglas que exijan autenticación. Ejemplo conceptual de reglas; ajusta la ruta y valida antes de producción:

```json
{
  "rules": {
    ".read": false,
    ".write": false,
    "users": { "$uid": { ".read": "auth != null && auth.uid == $uid", ".write": "auth != null && auth.uid == $uid" } },
    "latestReading": { ".read": "auth != null", ".write": "auth != null" },
    "device": { ".read": "auth != null", ".write": "auth != null" }
  }
}
```

Las reglas de Firebase deben ser revisadas y probadas desde Firebase Rules Simulator. Las rutas compartidas permiten a cualquier usuario autenticado publicar datos/control; para producción limita escritura a roles autorizados y valida rangos del lado del servidor.

## Ejecutar
1. Abre esta carpeta desde Android Studio.
2. Agrega `app/google-services.json`.
3. Sincroniza Gradle y ejecuta la app.
4. Registra un usuario en el teléfono A y otro en el teléfono B.
5. En ambos inicia sesión. En A ingresa temperatura y humedad de prueba y pulsa “Publicar medición de prueba”. B debe mostrar los nuevos valores.
6. Pulsa “Alternar dispositivo (demo)” desde cualquiera; el estado compartido debe cambiar en ambos.
7. Captura evidencia de registro/login, dos dispositivos mostrando el mismo dato, actualización del estado y reglas de Firebase.

## Seguridad y referencia ISO 27400
La ISO/IEC 27400:2022 proporciona directrices de seguridad y privacidad para IoT; esta demo aplica medidas alineadas con esos objetivos, pero no afirma certificación ni conformidad formal. Medidas: autenticación, reglas de acceso, mínimo privilegio, HTTPS/TLS gestionado por Firebase, no almacenar contraseñas en texto plano, validación de entradas, manejo de errores y desactivación de backup. Antes de producción, añade roles, reglas restrictivas, App Check, control de tasa, auditoría y pruebas de amenazas.

## Limitaciones y siguientes pasos
- El panel usa una sola medición más reciente, no un historial con gráficos.
- El valor del dispositivo es simulado y no activa hardware físico.
- La publicación manual sirve para validar la sincronización de extremo a extremo en esta etapa.
- Próxima mejora: guardar cada lectura bajo `readings/$timestamp`, crear historial, integrar sensor ESP32 y exigir autorización de operador para comandos.

## Entrega académica
Sube a la plataforma AAI un archivo de texto con el enlace al repositorio GitHub, tal como indica la guía. No inventes un enlace: crea tu repositorio y reemplaza el marcador en `ENTREGA_GITHUB.txt`.
