# 15 — Coding Standards

## General
- nombres descriptivos;
- métodos pequeños;
- evitar estado global;
- preferir composición;
- SOLID sin dogmatismo;
- no abstraer antes de tener una razón.

## Java
- Java 21;
- records para DTOs cuando corresponda;
- Bean Validation;
- constructor injection;
- evitar field injection;
- Optional con criterio;
- excepciones de dominio controladas;
- no capturar `Exception` sin propósito.

## Spring
- controllers delgados;
- lógica en application/domain;
- transacciones explícitas;
- no devolver entidades JPA directamente.

## Kotlin
- null-safety;
- coroutines;
- immutable state;
- Compose state hoisting;
- no lógica de negocio en composables.

## Seguridad
- nunca secretos en código;
- nunca tokens en logs;
- parametrización SQL;
- validación server-side;
- autorización por recurso.
