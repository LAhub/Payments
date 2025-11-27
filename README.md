# Payment Initiation Service

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)
![R2DBC](https://img.shields.io/badge/R2DBC-Reactive-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)

Servicio reactivo REST para la iniciación de órdenes de pago, alineado con el estándar BIAN (Banking Industry Architecture Network).

---

## 📋 Tabla de Contenidos

- [Contexto y Migración](#-contexto-y-migración)
- [Ejecución Local](#-ejecución-local)
- [API Endpoints](#-api-endpoints)
- [Uso de IA en el Desarrollo](#-uso-de-ia-en-el-desarrollo)
---

## 🔄 Contexto y Migración

### Etapa Inicial: Servicio SOAP/WSDL

El proyecto originalmente fue concebido como un servicio SOAP tradicional basado en WSDL con las siguientes características:

- **Protocolo**: SOAP/XML sobre HTTP
- **Definición**: Contratos WSDL
- **Stack**: Spring Web Services, JAX-WS
- **Base de datos**: JDBC bloqueante con PostgreSQL
- **Arquitectura**: Monolítica con operaciones síncronas

### Decisiones de Migración a REST Reactivo

#### 1. **De SOAP a REST con OpenAPI**

**Razón**:
- Mayor adopción en la industria fintech
- Mejor integración con arquitecturas de microservicios
- Documentación interactiva con Swagger UI
- Menor overhead de mensajes (JSON vs XML)

**Implementación**:
- Definición de API en OpenAPI 3.0 (`payment-initiation-api.yaml`)
- Generación automática de DTOs con OpenAPI Generator Maven Plugin
- Contratos REST sobre HTTP/HTTPS

#### 2. **De JDBC Bloqueante a R2DBC Reactivo**

**Razón**:
- Mejor utilización de recursos (non-blocking I/O)
- Escalabilidad horizontal mejorada
- Manejo eficiente de alta concurrencia
- Alineación con el stack reactivo de Spring WebFlux

**Implementación**:
- Spring Data R2DBC para acceso reactivo a PostgreSQL
- Connection pooling con `r2dbc-pool`
- Transacciones reactivas con `@Transactional`

#### 3. **Arquitectura Hexagonal (Ports & Adapters)**

**Razón**:
- Separación clara de responsabilidades
- Independencia del dominio respecto a infraestructura
- Facilita testing con mocks
- Permite cambiar implementaciones sin afectar el core

**Estructura**:
- ├── domain/ # Core del negocio (sin dependencias externas) │ 
- ├── model/ # Entidades y Value Objects │ 
- └── port/ # Interfaces (input/output) 
- ├── application/ # Casos de uso │
- └── service/ # Implementación de puertos de entrada 
- └── infrastructure/# Adaptadores externos 
- ├── adapter/ │ 
- ├── input.rest/ # Controladores REST │ 
- └── output.persistence/ # Repositorios R2DBC 
- └── config/ # Configuración Spring


#### 4. **De Validación Manual a Bean Validation**

**Implementación**:
- Uso de `@Valid`, `@NotNull`, `@Pattern` en DTOs
- Validaciones de dominio en el modelo
- RFC 7807 Problem Details para errores estandarizados

#### 5. **Idempotencia y Resiliencia**

**Nuevas capacidades**:
- Header `Idempotency-Key` para prevenir duplicados
- Almacenamiento temporal de claves de idempotencia (24h TTL)
- Manejo de errores reactivo con `Mono.error()`

#### 6. **Observabilidad Moderna**

**Stack**:
- Micrometer para métricas
- Prometheus endpoint (`/actuator/prometheus`)
- Spring Boot Actuator para health checks
- Tracing distribuido con Brave/Zipkin

---

## 🏗️ Ejecución Local

### Paso 1: Clonar el Repositorio
bash git clone [https://github.com/your-org/payment-initiation-service.git](https://github.com/your-org/payment-initiation-service.git) cd payment-initiation-service

### Paso 2: Ejecución con Docker Compose
#### Archivo `docker-compose.yml`
#### Ejecutar con Docker Compose
- en terminal bash
#### Iniciar todos los servicios
- docker-compose up -d
#### Ver logs
- docker-compose logs -f payment-service
#### Detener servicios
- docker-compose down
#### Limpiar volúmenes
- docker-compose down -v

## 🔄 API Endpoints

### Health check
- curl http://localhost:8080/actuator/health

### Swagger UI
- open http://localhost:8080/swagger-ui.html

## 🐳 Uso de IA en el Desarrollo

"Genera la estructura de un proyecto Spring Boot 3.2 con arquitectura hexagonal
para un servicio de pagos bancarios. Incluye:
- Domain layer con entidades y value objects
- Application layer con casos de uso
- Infrastructure layer con adaptadores REST y R2DBC
- Configuración para PostgreSQL reactivo"


La IA generó:
Estructura de carpetas completa siguiendo principios DDD
Clases base para entidades (PaymentOrder, IBAN, Amount)
Interfaces de puertos (input/output)
Configuraciones de Spring para R2DBC
Archivos pom.xml con dependencias necesarias


"Implementa un repositorio R2DBC reactivo para PaymentOrder con:
- Métodos findById, save, findByReference
- Conversión entre entidad de dominio y entidad JPA
- Manejo de transacciones reactivas"

La IA proporcionó:
Interfaz R2dbcPaymentOrderRepository extendiendo ReactiveCrudRepository
Adaptador que implementa el puerto de salida del dominio
Row mappers para conversión manual de resultados
Configuración de pool de conexiones

"Implementa un servicio de idempotencia para prevenir pagos duplicados usando:
- Header 'Idempotency-Key' en requests
- Almacenamiento temporal en PostgreSQL (TTL 24h)
- Verificación antes de crear orden de pago
- Limpieza automática de keys expiradas"


Resumen de Respuesta:
  Generó:
  Tabla idempotency_keys con índice en expires_at
  Servicio IdempotencyService con métodos reactivos
  Integración con PaymentOrderService
  Scheduled task para limpieza (posteriormente removido por estrategia de TTL en queries)
