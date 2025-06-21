# Wfit Heart - Aplicación de Ritmo Cardíaco para WearOS

## Descripción
Esta aplicación para WearOS mide las pulsaciones del corazón usando los sensores reales del dispositivo y muestra la tasa en la pantalla.

## Características
- ✅ **Sensores reales**: Utiliza únicamente sensores reales de ritmo cardíaco (no simula valores)
- ✅ **Detección automática**: Detecta automáticamente si el dispositivo tiene sensores de ritmo cardíaco
- ✅ **UI moderna**: Interfaz moderna y responsive para WearOS
- ✅ **Gestión de permisos**: Solicita automáticamente los permisos necesarios
- ✅ **Estados visuales**: Muestra diferentes colores según el estado (monitoreando, detenido, sensor no disponible)

## Permisos Requeridos
- `BODY_SENSORS`: Para acceder a los sensores de ritmo cardíaco
- `ACCESS_FINE_LOCATION`: Requerido por algunos sensores de WearOS
- `ACCESS_COARSE_LOCATION`: Ubicación aproximada
- `WAKE_LOCK`: Para mantener la pantalla encendida durante el monitoreo

## Arquitectura de la Aplicación

### Componentes Principales

1. **HeartRateService** (`data/HeartRateService.kt`)
   - Maneja la comunicación con los sensores del dispositivo
   - Busca sensores reales de ritmo cardíaco
   - NO simula valores (solo usa sensores reales)
   - Filtra valores válidos (entre 0 y 300 BPM)

2. **HeartRateViewModel** (`presentation/HeartRateViewModel.kt`)
   - Gestiona el estado de la UI
   - Comunica con el servicio de sensores
   - Maneja la lógica de negocio

3. **MainActivity** (`presentation/MainActivity.kt`)
   - UI principal usando Jetpack Compose
   - Solicita permisos automáticamente
   - Muestra el ritmo cardíaco en tiempo real

### Flujo de Funcionamiento

1. **Inicialización**:
   - La aplicación busca sensores de ritmo cardíaco disponibles
   - Verifica permisos y los solicita si es necesario
   - Muestra el estado del sensor (disponible/no disponible)

2. **Monitoreo**:
   - Al presionar "Iniciar", registra listeners en los sensores
   - Recibe datos del sensor cada segundo
   - Filtra valores válidos y actualiza la UI

3. **Visualización**:
   - Círculo verde: Monitoreando activamente
   - Círculo gris: Detenido
   - Círculo rojo: Sensor no disponible
   - Muestra BPM en tiempo real

## Verificación de Sensores Reales

La aplicación **NO simula** valores de ritmo cardíaco. Solo utiliza:

1. **Sensor.TYPE_HEART_RATE**: Sensor nativo de ritmo cardíaco
2. **Sensores personalizados**: Busca sensores con nombres que contengan "heart" o "cardiac"

Si no encuentra sensores reales, muestra "Sensor no disponible" y deshabilita el botón de monitoreo.

## Configuración del Proyecto

### Dependencias Incluidas
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0`
- `androidx.lifecycle:lifecycle-runtime-compose:2.7.0`
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`

### Configuración de Build
- `minSdk = 30` (Android 11)
- `targetSdk = 35` (Android 15)
- `compileSdk = 35`

## Instalación y Uso

1. Compilar la aplicación para WearOS
2. Instalar en un dispositivo WearOS con sensores de ritmo cardíaco
3. Conceder permisos cuando se soliciten
4. Presionar "Iniciar" para comenzar el monitoreo

## Notas Importantes

- La aplicación requiere un dispositivo WearOS con sensores de ritmo cardíaco reales
- Los valores se actualizan cada segundo para evitar sobrecarga
- La aplicación maneja automáticamente la disponibilidad de sensores
- No se simulan valores bajo ninguna circunstancia

## Estructura de Archivos

```
WfitHeart/
├── app/
│   ├── src/main/
│   │   ├── java/com/wfit/heart/
│   │   │   ├── data/
│   │   │   │   └── HeartRateService.kt
│   │   │   └── presentation/
│   │   │       ├── MainActivity.kt
│   │   │       ├── HeartRateViewModel.kt
│   │   │       └── theme/
│   │   │           └── Theme.kt
│   │   ├── res/values/
│   │   │   └── strings.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── README.md
``` 