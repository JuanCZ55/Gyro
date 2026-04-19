# Gyro

**Gyro** es una aplicación para Android que permite forzar la rotación de pantalla mediante un botón en los Ajustes Rápidos (panel de notificaciones), sin depender del sensor de rotación automática del sistema.

---

## Características

- **Botón en Ajustes Rápidos**: Añade un tile al panel de notificaciones para cambiar la rotación con un solo toque.
- **Tres modos de funcionamiento** configurables desde la app:
  - **Modo Inteligente (Sensor)**: Detecta cómo sostienes el móvil en ese momento y fija la rotación correspondiente.
  - **Modo Alternar (V/H)**: Alterna entre Vertical y Horizontal con cada toque.
  - **Modo Reloj (Secuencial)**: Gira 90° en sentido horario en cada toque, pasando por las cuatro orientaciones.
- **Indicador de rotación actual** en la pantalla principal, actualizado en tiempo real.
- Compatible con modo oscuro.

---

## Requisitos

| Requisito | Detalle |
|---|---|
| Android mínimo | Android 12 (API 31) |
| Android objetivo | Android 16 (API 36) |
| Permisos necesarios | Escritura de ajustes del sistema (`WRITE_SETTINGS`) |
| Permisos opcionales | Ignorar optimización de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) |

> **Nota**: El permiso de escritura de ajustes del sistema es obligatorio para que la app pueda cambiar la rotación. Sin él, el botón de Ajustes Rápidos abrirá la app para que lo concedas.

---

## Instalación

1. Descarga o compila el APK del proyecto.
2. Instala el APK en tu dispositivo Android 12+.
3. Abre la app **Gyro** y concede los permisos solicitados:
   - **Permiso de Escritura de Sistema**: Necesario para modificar la rotación.
   - **Ignorar Optimización de Batería** *(opcional)*: Recomendado para que el servicio no sea interrumpido.
4. Selecciona el modo de funcionamiento deseado.
5. Añade el tile **"Girar"** a tu panel de Ajustes Rápidos y úsalo para cambiar la rotación.

> Si el botón aparece oscuro o no responde, abre la app una vez para reactivarlo.

---

## Compilación

El proyecto usa **Gradle** con Kotlin DSL. Para compilarlo:

```bash
./gradlew assembleRelease
```

O importa el proyecto en **Android Studio** y compila desde el IDE.

---

## Permisos

| Permiso | Motivo |
|---|---|
| `WRITE_SETTINGS` | Modificar la rotación del sistema |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Mantener el servicio activo en segundo plano |

---

## Licencia

Este proyecto no incluye una licencia explícita. Todos los derechos reservados al autor.
