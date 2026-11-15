# NOTAS — rica-microservicios

## Taller 3 — DDD: modelo rico para Investigador

### 1. Auditoría de Lenguaje Ubicuo

**¿Hay algún campo o método con nombre genérico (`data`, `info`, `value`, `item`) que debería tener un nombre del dominio?**
No. `InvestigadorController` expone `listar`, `buscarPorId`, `registrar` y `eliminar`, y `InvestigadorRequest` tiene `nombreCompleto`, `correoInstitucional` y `grupoInvestigacion`: todos son términos que usaría la coordinación de investigación. El único nombre técnico que queda es `InvestigadorMapper.aEntidad`/`aResponse`, pero vive en el adaptador web, fuera del dominio, y ahí es correcto que hable de "entidad" y "respuesta". El único nombre "neutro" del dominio es `CorreoInstitucional.valor()`: lo dejé así porque es la convención para el contenido de un Value Object de un solo campo.

**¿`correoInstitucional` y `grupoInvestigacion` los reconocería alguien de la Facultad sin traducción?**
Sí. Son los mismos términos de los formularios de la vicerrectoría de investigación ("correo institucional", "grupo de investigación"), así que no los renombro. Dejo constancia de que la revisión se hizo y la conclusión fue mantenerlos.

**¿Por qué `investigadorCorreo` en `Publicacion` es más preciso que un nombre genérico como `refId`?**
Porque dice exactamente qué identidad se usa para referenciar al autor: su correo institucional, que es la identidad de negocio del investigador (única y verificable), y no un id técnico de base de datos. `refId` no diría a qué apunta ni con qué identidad, y además el id numérico de `investigadores-service` no significa nada dentro de `publicaciones-service`.

### 2. Value Object `CorreoInstitucional`

Es un `record` `@Embeddable` en `dominio/`. Su constructor compacto rechaza `null` y cualquier correo que no termine en `@uptc.edu.co`, así la regla vive en un solo lugar. Lo prueban `CorreoInstitucionalTest` (acepta `ana.torres@uptc.edu.co`, rechaza `carlos@gmail.com`, `null` y `...@uptc.edu.co.falso.com`, igualdad por valor).

### 4. Límite del Agregado Investigador

| Pregunta | Respuesta |
|---|---|
| ¿Cuál es la raíz del Agregado? | `Investigador`, la única clase con `@Entity` en `dominio/`. |
| ¿Qué vive dentro del límite? | `id`, `nombreCompleto`, `correoInstitucional` (Value Object `CorreoInstitucional`), `grupoInvestigacion` y, desde el Taller 6, `estado` (`EstadoInvestigador`: `ACTIVO` / `EN_ELIMINACION`). |
| ¿Por qué `Publicacion` NO está dentro? | Un investigador puede tener muchas publicaciones; meterlas en el Agregado lo haría grande (contra la regla de Vernon de Agregados pequeños) y cada cambio a un investigador cargaría y bloquearía todas sus publicaciones. Por eso `Publicacion` referencia al investigador solo por identidad (`investigadorCorreo`, un `String`). |
| ¿Qué pasaría con un `List<Publicacion> publicaciones` dentro de `Investigador`? | Se rompería el límite: dos transacciones sobre el mismo investigador podrían pisarse la lista, el Agregado dejaría de ser pequeño y `publicaciones` ya no podría evolucionar ni desplegarse por separado (lo que justamente se hizo en el Taller 5). |

### 5. Factory

`InvestigadorFactory` (en `aplicacion/`) es el único punto que construye un `Investigador`: crea el `CorreoInstitucional` (valida el dominio) y consulta el puerto `RepositorioInvestigadores.existeCorreo` para lanzar `CorreoDuplicadoException` si ya existe. Lo prueba `InvestigadorFactoryTest`, con un repositorio en memoria (sin Spring ni base de datos).

---

## Taller 4 — Arquitectura hexagonal

### 1. Auditoría de puertos y adaptadores

**`InvestigadorRepository`, ¿puerto primario o secundario?**
Secundario: la llamada la inicia el núcleo (el caso de uso necesita guardar o buscar) hacia algo externo (la base de datos). Después del refactor, el puerto secundario real es `RepositorioInvestigadores`, que diseñé según el caso de uso; `InvestigadorRepository` (Spring Data JPA) quedó como detalle del adaptador `InvestigadorRepositoryJpaAdapter`.

**`InvestigadorController`, ¿adaptador primario o secundario? ¿Qué tecnología envuelve?**
Primario: algo externo (un cliente HTTP) inicia la llamada hacia el núcleo. Envuelve HTTP/JSON con Spring Web MVC (`@RestController`, `@Valid`).

**¿Qué pieza faltaba para tener un puerto primario explícito?**
Una interfaz de caso de uso entre el controlador y `InvestigadorService`. Es `InvestigadorUseCase`: el controlador depende de la interfaz y `InvestigadorService` la implementa.

**`InvestigadorFactory`, ¿núcleo o adaptador?**
Núcleo. Sus imports son del dominio (`CorreoInstitucional`, `Investigador`, `CorreoDuplicadoException`) y del puerto `RepositorioInvestigadores`; el único import de Spring es `@Component`, que solo sirve para registrarla y no la ata a ninguna tecnología de entrada o salida.

### Organización resultante (investigadores-service)

| Paquete | Clases | Rol |
|---|---|---|
| `dominio` | `Investigador`, `CorreoInstitucional`, `EstadoInvestigador`, `CorreoDuplicadoException` | Núcleo: Entidad y Value Object, sin imports de Spring (solo `jakarta.persistence`, tolerado). |
| `aplicacion` | `InvestigadorService`, `InvestigadorFactory` | Núcleo: casos de uso. |
| `aplicacion.puertos.entrada` | `InvestigadorUseCase` | Puerto primario. |
| `aplicacion.puertos.salida` | `RepositorioInvestigadores`, `PublicadorEventos` | Puertos secundarios (persistencia y eventos). |
| `infraestructura.entrada.web` / `.grpc` / `.mensajeria` | `InvestigadorController`, `InvestigadorGrpcService`, `PublicacionesMarcadasListener` | Adaptadores primarios (HTTP, gRPC, Kafka). |
| `infraestructura.salida.persistencia` / `.mensajeria` | `InvestigadorRepositoryJpaAdapter`, `OutboxPublicadorEventos` | Adaptadores secundarios (JPA, Outbox → Kafka). |

La prueba de que el adaptador es sustituible está en `InvestigadorFactoryTest`: el mismo núcleo funciona con `RepositorioEnMemoria` en lugar del adaptador JPA.

---

## Taller 5 — Descomposición en microservicios

### 1. El corte: qué va en cada servicio

| Paquete / clase de rica-api | Destino |
|---|---|
| `investigadores.*` (dominio, aplicación, infraestructura) | `investigadores-service` (puerto 8080, PostgreSQL propio en el puerto 5433). |
| `publicaciones.*` | `publicaciones-service` (puerto 8081, MongoDB propio `rica`). |
| `compartido.GlobalExceptionHandler`, `RecursoNoEncontradoException` | Se duplica en cada servicio: cada uno necesita su propio manejo de errores; ya no hay un único `@ControllerAdvice` compartido. |
| `plataforma.StatusController`, `CorsConfig` | `StatusController` se duplica adaptado: cada servicio expone su propio `/api/status`. `CorsConfig` no se trasladó a los servicios. |
| `plataforma.ArranqueInformativo`, `SaludoInstitucionalService` | Se descarta: era una demostración de IoC/DI del Tutorial 2, sin valor de negocio. |

### Auditoría del corte

- **Database per Service.** Cada servicio tiene su base y nadie lee la base del otro. Es lo que obliga a duplicar `compartido` y `plataforma`.
- **Shared kernel (idea anotada, no implementada).** La duplicación de `GlobalExceptionHandler`/`RecursoNoEncontradoException` se podría evitar con un módulo común versionado aparte. Lo descarté por ahora: con un solo desarrollador, una librería común añade versionado y acoplamiento de despliegue que todavía no compensan. Es la primera decisión de arquitectura real que forzó el corte.
- **La dependencia entre dominios se hizo visible.** En rica-api, `PublicacionService` llamaba a `investigadorRepository.existsByCorreoInstitucional_Valor(...)` como un método más. Al cortar, esa llamada se volvió una interfaz con nombre propio, `VerificadorInvestigador`, con un adaptador temporal (`VerificadorInvestigadorPendiente`) que aceptaba cualquier correo. Esa brecha la cerró el Taller 6 con `VerificadorInvestigadorGrpc`.
- **Monorepo.** Los servicios viven en un solo repositorio (`rica-microservicios`) como subcarpetas: con un equipo de una persona es más simple que un repo por servicio, y cada servicio sigue siendo un proyecto Maven independiente con su propio puerto.
- **Evidencia.** Los dos servicios arrancan a la vez: `investigadores-service` en 8080 y `publicaciones-service` en 8081. ⟨COMPLETAR: captura de las dos terminales corriendo, o enlace a ella en Moodle⟩.

---

## Taller 6 — Comunicación entre microservicios

Notas de arquitectura (limitaciones decididas, no descuidos):

- **gRPC no usa Eureka.** El canal gRPC de `publicaciones-service` apunta a `static://localhost:9090`. Eureka resuelve el descubrimiento para el HTTP balanceado del Gateway (`lb://`), pero integrar Eureka con el balanceo de canales gRPC añade complejidad fuera del alcance del taller. Consecuencia: si `investigadores-service` cambia de máquina o de puerto gRPC, hay que tocar la configuración.
- **Circuit Breaker en el lado del cliente.** `VerificadorInvestigadorGrpc` envuelve la llamada con Resilience4j: si `investigadores-service` no responde, el circuito se abre y la publicación falla rápido con `ServicioNoDisponibleException` en vez de dejar hilos colgados.
- **Outbox.** El evento se guarda en `outbox_event` en la misma transacción que el investigador, y `OutboxPoller` lo publica en Kafka. Si Kafka está caído, el registro igual responde 201 y el evento sale cuando Kafka vuelve.
- **Saga por coreografía.** Eliminar es asíncrono (202): `investigadores.en-eliminacion` → `publicaciones-service` marca las publicaciones huérfanas → `publicaciones.marcadas-huerfanas` → `investigadores-service` confirma y borra (`investigadores.eliminados`).

---

## Taller 8 — CQRS: consultas-service

### Qué se vería distinto si el modelo de lectura y el de escritura fueran el mismo

1. **Ficha con dos publicaciones (`GET /api/consultas/investigadores/{correo}`).** Para armarla habría que consultar a `investigadores-service` y a `publicaciones-service` en cada petición y unir los resultados en memoria; hoy es un solo documento `FichaInvestigador` en `rica_consultas`, ya armado. ⟨COMPLETAR: correo y `totalPublicaciones` de mi ficha⟩
2. **Ranking (`GET /api/consultas/ranking`).** Habría que contar las publicaciones de todos los investigadores en cada consulta, sobre la base de escritura; hoy `totalPublicaciones` ya está precalculado en cada ficha y el ranking solo ordena. ⟨COMPLETAR: primer lugar del ranking⟩
3. **Log `Proyección: ...`.** No existiría: la vista se calcularía al consultar, no al llegar cada evento. Ahora `ProyectorFichas` actualiza la ficha por cada evento, por ejemplo "ficha de ⟨correo⟩ ahora tiene ⟨N⟩ publicaciones". ⟨COMPLETAR: una línea real de mi log⟩
4. **404 de la ficha después de la Saga.** Con un único modelo, el investigador desaparecería en el mismo instante del borrado; con CQRS la ficha pasa por `EN_ELIMINACION` y desaparece unos segundos después, cuando llega `investigadores.eliminados` (consistencia eventual visible).
5. **Lista de fichas reconstruida.** Con un único modelo no habría nada que reconstruir; con CQRS la vista es desechable: se borra `rica_consultas` y se vuelve a llenar releyendo los eventos de Kafka desde el inicio, sin tocar las bases de escritura. ⟨COMPLETAR: cuántas fichas quedaron tras reconstruir⟩

Además, la escritura no depende de la lectura: con `consultas-service` detenido, registrar una publicación sigue respondiendo 201, y al volver el consumidor procesa el evento pendiente.

---

## Taller 9 — EDA: consumidores robustos

### Defensas implementadas (consultas-service)

- **Reintentos con backoff exponencial** (`ManejoErroresConfig`): 3 reintentos con espera de 1 s, 2 s y 4 s (máximo 8 s) para fallas transitorias.
- **Dead Letter Topic**: `DeadLetterPublishingRecoverer` publica en `<tópico>.DLT` (por ejemplo `investigadores.registrados.DLT`) con cabeceras que explican el error. `JacksonException` y `NullPointerException` van directo a la DLT, porque reintentar un JSON roto nunca lo arregla.
- **Consumidor idempotente** (`EstadisticasListener` + `RegistroEventosProcesados`): la identidad del evento es `tópico-partición-offset`. Se inserta en la colección de eventos procesados y, si ya existía (`DuplicateKeyException`), el evento se ignora y `ContadorAltas` no vuelve a sumar.

### Datos de mi ejecución

- `/api/consultas/estadisticas` antes del paso 5: ⟨COMPLETAR⟩ — después de repetir eventos sin idempotencia (contador descuadrado): ⟨COMPLETAR⟩.
- Tras repetir los eventos con el consumidor idempotente (paso 6): ⟨COMPLETAR⟩, y el log muestra "Estadísticas: evento ⟨id⟩ ya procesado, se ignora".

### Discusión: ¿qué hacemos con la DLT?

1. **¿Quién debe enterarse y cómo?** El responsable de `consultas-service` (en guardia), con una alerta automática cuando el número de mensajes en `*.DLT` crezca, más un tablero con el tamaño de cada DLT. Un correo por mensaje no sirve porque se ignora; la alerta debe salir de una métrica, que es lo que agrega la Lección 10 (observabilidad).
2. **¿Cómo se reprocesa después de corregir?** Reenviando el mensaje al tópico original con una herramienta aparte (un pequeño consumidor de la DLT o un endpoint administrativo), no a mano. Como el consumidor es idempotente, reenviar no duplica efectos.
3. **¿Cuándo es válido descartar un mensaje?** Cuando está irrecuperablemente mal formado o ya no tiene sentido de negocio (por ejemplo, el alta de un investigador que ya fue eliminado). Lo decide el dueño del dominio del evento, no quien opera Kafka, y se deja registro de la decisión.
4. **¿Y si es válido pero llegó a destiempo?** Antes de aplicarlo se compara con el estado actual: si un "registrado" llega después de "eliminado", aplicarlo resucitaría la ficha. Se necesita versión o fecha en el evento para descartar lo viejo. Con observabilidad (correlation id en los logs y eventos) se puede rastrear de qué operación venía.

### Por qué un mensaje venenoso reintentado sin fin es lo peor

Reintentarlo sin fin bloquea la partición: ningún mensaje bueno detrás de él se procesa, así que se pierde la disponibilidad de todo el consumidor por un solo mensaje. Perderlo en silencio es menos dañino pero oculta el error, y la DLT es mejor que ambos: libera la partición y conserva el mensaje con la causa del fallo para revisarlo y reprocesarlo.

---

## Evidencias 

| Taller | Evidencia | Dónde |
|---|---|---|
| 5 | Captura de los dos servicios corriendo a la vez | ..\rica-microservicios\Evidencias\Taller 5 |
| 6 | Captura de las cuatro terminales; logs de la Saga; consulta `SELECT id, correo_institucional, estado FROM investigadores;` | ..\rica-microservicios\Evidencias\Taller 6 |
| 8 | JSON de la ficha, ranking, log `Proyección: ...`, 404 tras la Saga, lista reconstruida | ..\rica-microservicios\Evidencias\Taller 8 |
| 9 | Log de reintentos, salida de `kafka-console-consumer` sobre la DLT con cabeceras, `/estadisticas` antes y después | ..\rica-microservicios\Evidencias\Taller 9 |