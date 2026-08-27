# TrenYa 🚆

App de Android (Kotlin + Jetpack Compose) para ver en vivo cuándo llega tu
próximo tren de SOFSE (Mitre, Sarmiento, Roca, San Martín, Belgrano Norte,
Belgrano Sur, Urquiza...), con estaciones cercanas por GPS, favoritas,
notificaciones de demoras y un planificador de viaje simple.

**No es una app oficial de SOFSE / Trenes Argentinos ni está afiliada a
ellos.** Ver la sección "De dónde salen los datos" antes de publicarla o
distribuirla.

---

## 1. De dónde salen los datos (leer esto primero)

Pediste revisar `https://api-servicios.sofse.gob.ar/v1`: esa URL puntual
devuelve 404 porque no es un endpoint público — la app oficial "Trenes
Argentinos" (Android/iOS) habla con una API interna (`apiarribos.sofse.gob.ar`
en su versión de 2022) protegida con un esquema de firma/token embebido en el
propio APK, no con una API key pensada para terceros.

Un desarrollador (ariedro) decompiló la app oficial, documentó ese esquema y
publicó un **proxy público de código abierto** que replica esos mismos datos
sin necesidad de ese token:

- Repo: https://github.com/ariedro/api-trenes (licencia abierta)
- Instancia pública gratuita: `https://ariedro.dev/api-trenes`
- Cómo se descubrió: https://ariedro.dev/3-hack-trains-api/
- Otras apps no oficiales ya construidas sobre esta misma base: QuéTren
  (https://quetren.com) y Trencitos

**TrenYa usa esa instancia pública como backend** (`BuildConfig.API_BASE_URL`
en `app/build.gradle.kts`, una sola línea). Esto significa:

- ✅ No hubo que replicar ningún esquema de firma/token: la app solo hace
  `GET`s planos a una API que ya es pública e invita a este uso.
- ⚠️ Es un servidor de un solo desarrollador, no un servicio con SLA. Puede
  caerse, cambiar de forma o desaparecer sin aviso.
- ⚠️ Para una app real en Play Store con tráfico de verdad, lo correcto es
  **auto-hostear tu propia instancia** del proxy (es Node.js, `npm install &&
  npm start`, ver su repo) o, mejor todavía, escribirle a SOFSE para pedir
  acceso oficial. Cambiar de instancia es editar una sola constante.

Los endpoints reales que usa la app (verificados contra la instancia pública
mientras se armó este proyecto):

```
GET /infraestructura/estaciones?nombre=...     → lista de estaciones (nombre, lat/long, ramales)
GET /arribos/estacion/{id}?hasta=&cantidad=...  → próximos trenes de una estación
```

La API no tiene documentación oficial ni contrato formal, así que todos los
DTOs (`data/remote/ApiModels.kt`) son nullable a propósito y el mapeo de
"demora" (`TrainRepository.resolveStatus`) es una heurística basada en el
único ejemplo real observado (`tipo: "Normal"`, `leyenda: null`) — está
comentada en el código y conviene revisarla apenas se vea un caso real de
demora o cancelación.

---

## 2. Funciones

- **Estaciones cercanas automáticas**: detecta tu ubicación (GPS) y ordena
  las estaciones por distancia real (Haversine), sin que tengas que buscar
  nada.
- **Próximos trenes en vivo**: cuenta regresiva en minutos, andén, línea,
  ramal y estado, con auto-refresh cada 30 s mientras mirás la pantalla.
- **Favoritas**: marcá estaciones con la estrella; la primera favorita se
  muestra como card grande ("hero") arriba de todo en Inicio, con su próximo
  tren destacado.
- **Notificaciones de demoras**: un chequeo periódico (WorkManager, cada
  15/30/60 min a elección) revisa tus estaciones favoritas y te avisa si
  aparece una demora/alteración/cancelación nueva. No es push real -Android
  no permite menos de 15 min de intervalo para trabajo periódico y esta app
  no tiene servidor propio-, pero corre sola en segundo plano.
- **Buscador** con búsqueda difusa (ignora acentos) y búsquedas recientes.
- **Planificador de viaje**: elegís origen y destino, la app te muestra los
  próximos trenes directos y estima la duración del tramo cuando el recorrido
  completo del servicio trae los horarios de ambas paradas.
- **Filtro por ramal** dentro de una estación con varios ramales.
- **Ver recorrido completo** de un servicio (todas las paradas), tocando el
  tren en la lista.
- **Modo offline**: si falla la red, se muestran los últimos datos guardados
  con un aviso, en vez de una pantalla vacía.
- **Widget de pantalla de inicio** con el próximo tren de tu favorita
  principal (ver limitaciones más abajo).
- **Tema claro/oscuro/automático**, onboarding con pedido de permisos.

Ideas investigadas en otras apps del mundo (Citymapper, Moovit, DB
Navigator) y en la escena Argentina (QuéTren) que quedaron fuera del
alcance de esta primera versión, para una v2:

- Mapa con la posición en vivo de los trenes en viaje ("Viajando ahora" de
  QuéTren) — necesita Google Maps SDK + API key propia.
  - Notificación específica de "bajate en 2 paradas".
  - Widget/complicación para Wear OS.
  - Multi-idioma.

---

## 3. Arquitectura

Kotlin + **Jetpack Compose** (Material 3) sobre MVVM simple:

```
ui/            pantallas + ViewModels (uno por feature, en el mismo archivo)
data/          repositorios (TrainRepository, UserPreferencesRepository)
data/remote/   Retrofit + DTOs
data/model/    modelos de dominio limpios que consume la UI
location/      wrapper de FusedLocationProviderClient
notification/  canales, worker de demoras
widget/        widget Glance
core/          DI manual (AppContainer), constantes, utilidades
```

Decisiones deliberadas para que este proyecto sea fácil de abrir y compilar
sin sorpresas, ya que se generó sin poder compilarlo en el entorno donde se
escribió (ver sección 5):

- **Sin Hilt**: DI manual con un `AppContainer` simple (`core/AppContainer.kt`)
  en vez de un framework con procesador de anotaciones.
- **Sin Room**: favoritas y ajustes viven en DataStore Preferences
  (favoritas serializadas como JSON); la caché de estaciones es un archivo
  JSON plano en `filesDir`. Para el volumen de datos de esta app (una lista
  corta de favoritas, ~300 estaciones) alcanza de sobra y evita depender de
  KSP.
- **Retrofit + Gson** (no kotlinx.serialization): un converter sin plugin de
  compilador propio.

---

## 4. Cómo abrir y correr

1. Android Studio (Ladybug o más nuevo) → **Open** → carpeta `TrenYa/`.
2. Dejalo sincronizar Gradle. Si te ofrece actualizar el Android Gradle
   Plugin o alguna dependencia a una versión más nueva, aceptalo.
3. Corré en un emulador o dispositivo con **Android 8.0 (API 26) o superior**.
4. Al abrir la app por primera vez, te va a pedir permiso de ubicación y de
   notificaciones (podés omitirlos y activarlos después desde Ajustes del
   sistema).

No hay ninguna API key que configurar: `BuildConfig.API_BASE_URL` ya apunta
a la instancia pública.

---

## 5. Limitaciones conocidas / honestidad ante todo

Esto se escribió y armó en un entorno sin Android SDK ni emulador, así que
**no se pudo compilar ni correr de punta a punta acá**. Se usaron versiones
de Gradle/Kotlin/Compose/AGP confirmadas como reales y vigentes a mediados
de 2026, y cada archivo se revisó a mano buscando imports y APIs correctas,
pero es esperable que necesites algún ajuste menor (una versión de librería,
un import) la primera vez que sincronices. Si algo no compila, el lugar más
probable son:

- El **widget** (`widget/NextTrainWidget.kt`): usa Glance, la librería más
  nueva/menos común de todo el proyecto. Si da problemas, se puede borrar
  ese archivo, la entrada `<receiver>` del manifest y la dependencia
  `androidx.glance` sin afectar el resto de la app.
- Números de versión exactos en `gradle/libs.versions.toml`: si alguno ya
  quedó viejo para cuando lo abras, Android Studio te va a sugerir la
  actualización.
- La heurística de "demora" en `TrainRepository.resolveStatus`: no se pudo
  observar un caso real de tren demorado durante el desarrollo (solo
  servicios "Normal"), así que conviene validarla con casos reales.
- Los **colores por línea** (`TrenYaColors.forLine`) son una elección de
  diseño propia -la API no expone colores oficiales-, salvo el naranja de
  San Martín que sí replica su color identificatorio real.

## 6. Créditos

Datos: proxy comunitario de ariedro (https://github.com/ariedro/api-trenes)
sobre información pública de SOFSE. Para información oficial:
https://www.trenesargentinos.gob.ar/
